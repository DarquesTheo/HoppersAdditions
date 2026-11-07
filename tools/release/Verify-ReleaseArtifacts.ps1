param([string]$ArtifactRoot, [string]$OutputPath)

$ErrorActionPreference = 'Stop'
$repository = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (!$ArtifactRoot) { $ArtifactRoot = $repository }
if (!$OutputPath) { $OutputPath = Join-Path $repository 'build/issue8-evidence/artifacts.json' }
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Assert-Release {
    param([bool]$Condition, [string]$Message)
    if (!$Condition) { throw $Message }
}

function Read-Entry {
    param($Archive, [string]$Path)
    $entry = $Archive.GetEntry($Path)
    Assert-Release ($null -ne $entry) "Missing packaged entry: $Path"
    $reader = [IO.StreamReader]::new($entry.Open())
    try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
}

function Get-EntryHash {
    param($Entry)
    $stream = $Entry.Open()
    $algorithm = [Security.Cryptography.SHA256]::Create()
    try { return [BitConverter]::ToString($algorithm.ComputeHash($stream)).Replace('-', '') }
    finally { $stream.Dispose(); $algorithm.Dispose() }
}

function Test-References {
    param($Value, [string]$Key, [string]$Document, $Archive)
    if ($null -eq $Value) { return }
    if ($Value -is [string]) {
        if (!$Value.StartsWith('hoppersadditions:')) { return }
        $relative = $Value.Substring('hoppersadditions:'.Length)
        if (($Document -like 'assets/*/models/*' -and $Key -eq 'parent') -or $Key -eq 'model') {
            $target = "assets/hoppersadditions/models/$relative.json"
        } elseif ($Document -like 'assets/*/models/*') {
            $target = "assets/hoppersadditions/textures/$relative.png"
        } elseif ($Key -eq 'tag') {
            $target = "data/hoppersadditions/tags/item/$relative.json"
        } elseif ($Key -in @('type', 'condition', 'function')) {
            throw "Custom runtime type needs manual review: $Document -> $Value"
        } else {
            $target = "assets/hoppersadditions/models/item/$relative.json"
        }
        Assert-Release ($null -ne $Archive.GetEntry($target)) "Unresolved custom reference: $Document -> $Value ($target)"
    } elseif ($Value -is [System.Management.Automation.PSCustomObject]) {
        foreach ($property in $Value.PSObject.Properties) { Test-References $property.Value $property.Name $Document $Archive }
    } elseif ($Value -is [System.Collections.IEnumerable]) {
        foreach ($element in $Value) { Test-References $element $Key $Document $Archive }
    }
}

function Test-Model {
    param([string]$Path, $Json, [string[]]$Ancestors = @())
    Assert-Release ($Path -notin $Ancestors) "Cyclic model parent: $Path"
    $model = $Json[$Path]
    $textures = @{}
    $externalParent = $false
    if ($model.parent -like 'hoppersadditions:*') {
        $parentPath = 'assets/hoppersadditions/models/' + $model.parent.Substring('hoppersadditions:'.Length) + '.json'
        $parent = Test-Model $parentPath $Json ($Ancestors + $Path)
        foreach ($key in $parent.textures.Keys) { $textures[$key] = $parent.textures[$key] }
        $externalParent = $parent.externalParent
    } elseif ($model.parent) { $externalParent = $true }
    if ($model.textures) {
        foreach ($property in $model.textures.PSObject.Properties) { $textures[$property.Name] = $property.Value }
    }
    $references = @($textures.Values) + @($model.elements | ForEach-Object { $_.faces.PSObject.Properties.Value.texture })
    foreach ($reference in $references) {
        $visited = @()
        while ($reference -like '#*') {
            $alias = $reference.Substring(1)
            Assert-Release ($alias -notin $visited) "Cyclic texture alias: $Path -> $alias"
            $visited += $alias
            if (!$textures.ContainsKey($alias) -and $externalParent) { break }
            Assert-Release ($textures.ContainsKey($alias)) "Missing texture alias: $Path -> $alias"
            $reference = $textures[$alias]
        }
    }
    return @{ textures = $textures; externalParent = $externalParent }
}

Push-Location $repository
try {
    $properties = ConvertFrom-StringData (Get-Content gradle.properties -Raw)
    Assert-Release ($properties.mod_version -eq '0.4.0+mc1.21.11' -and $properties.minecraft_version -eq '1.21.11') 'This release gate expects 0.4.0+mc1.21.11 on Minecraft 1.21.11.'
    $sourceRoot = (Resolve-Path common/src/main/resources).Path
    $sourceResources = @(Get-ChildItem "$sourceRoot/assets", "$sourceRoot/data" -Recurse -File)
    $sourceClasses = @(Get-ChildItem common/src/main/java -Recurse -Filter '*.java' -File)
    $javaRoot = (Resolve-Path common/src/main/java).Path
    $artifacts = foreach ($loader in @('fabric', 'neoforge')) {
        $relativePath = "$loader/build/libs/hoppers-additions-$loader-$($properties.mod_version).jar"
        $path = Join-Path $ArtifactRoot $relativePath
        $archive = [IO.Compression.ZipFile]::OpenRead($path)
        $inspection = 'PASS'
        $failure = $null
        $classes = @()
        $json = @{}
        try {
            $names = @($archive.Entries.FullName)
            foreach ($licensePath in @('LICENSE', 'LICENSE.txt')) {
                $sourceLicense = [IO.File]::ReadAllText((Join-Path $repository $licensePath))
                Assert-Release ((Read-Entry $archive $licensePath) -ceq $sourceLicense) "Missing or mismatched packaged license: $licensePath"
            }
            Assert-Release (@($names | Group-Object | Where-Object Count -gt 1).Count -eq 0) "Duplicate ZIP entries: $path"
            $unexpected = @($names | Where-Object { $_ -match '(^|/)(logs?|tools|\.git|\.idea|\.env)(/|$)|\.(log|log\.gz|java|ps1|key|pem)$' })
            Assert-Release ($unexpected.Count -eq 0) "Unexpected development/private content: $unexpected"
            $testContent = @($names | Where-Object {
                $_ -match '(?i)(gametest|test_instance|test_environment|HopperGameTests|FabricTests|NeoForgeTests|junit|mockito|testng)'
            })
            Assert-Release ($testContent.Count -eq 0) "Test-only content leaked into production JAR: $testContent"
            foreach ($nested in @($archive.Entries | Where-Object FullName -like '*.jar')) {
                $nestedStream = $nested.Open()
                $nestedArchive = [IO.Compression.ZipArchive]::new($nestedStream, [IO.Compression.ZipArchiveMode]::Read)
                try {
                    $nestedTests = @($nestedArchive.Entries.FullName | Where-Object { $_ -match '(?i)(gametest|junit|mockito|testng)' })
                    Assert-Release ($nestedTests.Count -eq 0) "Test-only dependency leaked into $($nested.FullName): $nestedTests"
                } finally { $nestedArchive.Dispose(); $nestedStream.Dispose() }
            }
            foreach ($resource in $sourceResources) {
                $entryPath = $resource.FullName.Substring($sourceRoot.Length + 1).Replace('\', '/')
                $entry = $archive.GetEntry($entryPath)
                Assert-Release ($null -ne $entry) "Missing resource: $entryPath"
                Assert-Release ((Get-EntryHash $entry) -eq (Get-FileHash $resource.FullName -Algorithm SHA256).Hash) "Stale resource: $entryPath"
            }
            foreach ($source in $sourceClasses) {
                $classPath = $source.FullName.Substring($javaRoot.Length + 1).Replace('\', '/').Replace('.java', '.class')
                Assert-Release ($null -ne $archive.GetEntry($classPath)) "Missing common class: $classPath"
            }
            $classes = @($archive.Entries | Where-Object FullName -like 'me/Wikos/hoppersadditions/*.class')
            foreach ($entry in $classes) {
                $reader = [IO.BinaryReader]::new($entry.Open())
                try { $header = $reader.ReadBytes(8) } finally { $reader.Dispose() }
                Assert-Release ($header.Length -eq 8 -and [BitConverter]::ToString($header[0..3]) -eq 'CA-FE-BA-BE' -and $header[6] -eq 0 -and $header[7] -eq 65) "Expected Java 21 bytecode: $($entry.FullName)"
            }
            $json = @{}
            foreach ($entry in ($archive.Entries | Where-Object FullName -like '*.json')) {
                $json[$entry.FullName] = (Read-Entry $archive $entry.FullName) | ConvertFrom-Json
            }
            foreach ($document in @($json.Keys)) {
                Test-References $json[$document] '' $document $archive
                if ($document -like 'assets/*/models/*') { $null = Test-Model $document $json }
            }
            $language = $json['assets/hoppersadditions/lang/en_us.json']
            $ingredients = @{ splitting = @('minecraft:hopper', 'minecraft:iron_ingot', 'minecraft:quartz'); overflow = @('minecraft:hopper', 'minecraft:comparator'); pulse = @('minecraft:hopper', 'minecraft:repeater') }
            foreach ($feature in @('splitting', 'overflow', 'pulse')) {
                $id = "$($feature)_hopper"
                foreach ($entryPath in @("assets/hoppersadditions/blockstates/$id.json", "assets/hoppersadditions/models/item/$id.json", "data/hoppersadditions/recipe/$id.json", "data/hoppersadditions/loot_table/blocks/$id.json")) {
                    Assert-Release ($json.ContainsKey($entryPath)) "Missing feature JSON: $entryPath"
                }
                foreach ($key in @("block.hoppersadditions.$id", "container.$id", "tooltip.hoppersadditions.$id.description")) {
                    Assert-Release (![string]::IsNullOrWhiteSpace($language.$key)) "Missing translation: $key"
                }
                foreach ($tag in @('mineable/pickaxe', 'needs_iron_tool')) {
                    Assert-Release ("hoppersadditions:$id" -in $json["data/minecraft/tags/block/$tag.json"].values) "Missing mining tag entry: $tag -> $id"
                }
                $recipe = $json["data/hoppersadditions/recipe/$id.json"]
                Assert-Release ($recipe.type -eq 'minecraft:crafting_shapeless' -and $recipe.result.id -eq "hoppersadditions:$id" -and $recipe.result.count -eq 1) "Wrong recipe result/type: $id"
                $actualIngredients = @($recipe.ingredients | Sort-Object)
                Assert-Release (($actualIngredients -join ',') -eq (@($ingredients[$feature] | Sort-Object) -join ',')) "Wrong crafting ingredients: $id"
                $loot = $json["data/hoppersadditions/loot_table/blocks/$id.json"]
                Assert-Release ($loot.type -eq 'minecraft:block' -and "hoppersadditions:$id" -in @($loot.pools.entries.name)) "Missing block loot: $id"
            }
            foreach ($variant in @('reinforced', 'pickaxe', 'dropping', 'magnetic', 'filter', 'monster', 'animal', 'batch', 'small_batch', 'splitting', 'overflow', 'pulse')) {
                $id = "$($variant)_hopper"
                foreach ($entryPath in @("assets/hoppersadditions/blockstates/$id.json", "assets/hoppersadditions/models/item/$id.json", "assets/hoppersadditions/items/$id.json", "data/hoppersadditions/loot_table/blocks/$id.json")) {
                    Assert-Release ($json.ContainsKey($entryPath)) "Missing hopper JSON: $entryPath"
                }
                $definition = $json["assets/hoppersadditions/items/$id.json"]
                Assert-Release ($definition.model.type -eq 'minecraft:model' -and $definition.model.model -eq "hoppersadditions:item/$id") "Wrong item definition: $id"
                Assert-Release (![string]::IsNullOrWhiteSpace($language."block.hoppersadditions.$id")) "Missing hopper translation: $id"
            }
            foreach ($recipePath in @($json.Keys | Where-Object { $_ -like 'data/hoppersadditions/recipe/*' })) {
                $recipe = $json[$recipePath]
                $recipeIngredients = if ($recipe.key) { @($recipe.key.PSObject.Properties.Value) } else { @($recipe.ingredients) }
                Assert-Release ($recipeIngredients.Count -gt 0) "Missing ingredients: $recipePath"
                foreach ($ingredient in $recipeIngredients) {
                    Assert-Release ($ingredient -is [string] -and $ingredient -match '^#?[a-z0-9_.-]+:[a-z0-9_./-]+$') "Expected 1.21.11 ingredient string: $recipePath"
                }
            }
            if ($loader -eq 'fabric') {
                $metadata = $json['fabric.mod.json']
                Assert-Release ($metadata.license -eq 'GPL-3.0-only') 'Wrong Fabric license.'
                Assert-Release ($metadata.id -eq 'hoppersadditions' -and $metadata.version -eq $properties.mod_version) 'Wrong Fabric ID/version.'
                $expected = @{ minecraft = '=1.21.11'; fabricloader = '>=0.20.1'; java = '>=21'; architectury = '>=19.0.1 <20.0.0'; 'fabric-api' = '>=0.141.6+1.21.11' }
                foreach ($key in $expected.Keys) { Assert-Release ($metadata.depends.$key -eq $expected[$key]) "Wrong Fabric dependency: $key" }
                Assert-Release (![string]::IsNullOrWhiteSpace($metadata.description) -and !$metadata.suggests) 'Empty Fabric description or placeholder suggestions.'
                foreach ($className in @($metadata.entrypoints.main) + @($metadata.entrypoints.client)) {
                    Assert-Release ($null -ne $archive.GetEntry($className.Replace('.', '/') + '.class')) "Missing Fabric entrypoint: $className"
                }
                Assert-Release ($metadata.accessWidener -eq 'hoppersadditions.accesswidener') 'Missing Fabric access widener declaration.'
                $widener = Read-Entry $archive $metadata.accessWidener
                Assert-Release ($widener -match 'accessWidener\s+v1\s+intermediary' -and $widener -match 'accessible\s+field\s+net/minecraft/class_2614\s+field_12023\s+I' -and $widener -match 'accessible\s+class\s+net/minecraft/class_2591\$class_5559' -and $widener.Contains('(Lnet/minecraft/class_2591$class_5559;Ljava/util/Set;)V')) 'Incomplete runtime access widener.'
                $descriptor = Read-Entry $archive 'fabric.mod.json'
            } else {
                $transformer = Read-Entry $archive 'META-INF/accesstransformer.cfg'
                Assert-Release ($transformer -match 'BlockEntityType' -and $transformer -match 'cooldownTime' -and $transformer.Contains('<init>(Lnet/minecraft/world/level/block/entity/BlockEntityType$BlockEntitySupplier;Ljava/util/Set;)V')) 'Incomplete NeoForge runtime access transformer.'
                $descriptor = Read-Entry $archive 'META-INF/neoforge.mods.toml'
                Assert-Release ($descriptor -match '(?m)^license\s*=\s*"GPL-3\.0-only"\s*$') 'Wrong NeoForge license.'
                Assert-Release ($descriptor -match '(?m)^version\s*=\s*"0\.4\.0\+mc1\.21\.11"\s*$' -and $descriptor -match '(?m)^modId\s*=\s*"hoppersadditions"\s*$') 'Wrong NeoForge version/ID.'
                $expected = @{ neoforge = '[21.11.48,21.12)'; minecraft = '[1.21.11]'; architectury = '[19.0.1,20)' }
                $blocks = [regex]::Matches($descriptor, '(?s)\[\[dependencies\.hoppersadditions\]\](.*?)(?=\[\[|\z)')
                foreach ($key in $expected.Keys) {
                    $block = @($blocks | Where-Object { $_.Groups[1].Value -match ('modId\s*=\s*"' + $key + '"') })
                    Assert-Release ($block.Count -eq 1) "Missing/duplicate NeoForge dependency: $key"
                    $text = $block[0].Groups[1].Value
                    Assert-Release ($text -match ('versionRange\s*=\s*"' + [regex]::Escape($expected[$key]) + '"') -and $text -match 'type\s*=\s*"required"' -and $text -match 'side\s*=\s*"BOTH"') "Wrong NeoForge dependency: $key"
                }
                Assert-Release ($descriptor -match "description\s*=\s*'''[^']+'''") 'Missing NeoForge description.'
            }
            Assert-Release ($descriptor -notmatch '\$\{') "Unexpanded metadata: $loader"
            Assert-Release ($descriptor.Contains('https://github.com/DarquesTheo/HoppersAdditions/issues')) "Missing issue URL: $loader"
            if ($loader -eq 'fabric' -and $metadata.icon) {
                Assert-Release ($null -ne $archive.GetEntry($metadata.icon)) 'Missing Fabric icon.'
            }
        } catch {
            $inspection = 'FAIL'
            $failure = $_.Exception.Message
        } finally { $archive.Dispose() }
        [ordered]@{
            path = $relativePath
            sizeBytes = (Get-Item $path).Length
            sha256 = (Get-FileHash $path -Algorithm SHA256).Hash
            expectedSourceResources = $sourceResources.Count
            expectedCommonTopLevelClasses = $sourceClasses.Count
            inspectedJava21Classes = $classes.Count
            parsedJsonDocuments = $json.Count
            staticInspection = $inspection
            failure = $failure
        }
    }
    $status = @(& git status --porcelain=v1)
    Assert-Release ($LASTEXITCODE -eq 0) 'Cannot read worktree status.'
    $report = [ordered]@{
        recordedAt = (Get-Date -Format o)
        head = (& git rev-parse HEAD)
        dirty = ($status.Count -gt 0)
        worktreeStatus = $status
        artifacts = @($artifacts)
        scope = 'Static package checks only; vanilla references and runtime registry/schema loading require live checks.'
        finalCommittedCandidate = 'NOT ESTABLISHED by this script; record a clean build from an authorized final commit.'
        gameplay = 'UNVERIFIED; match these hashes with issue #8 runtime evidence.'
    }
    $parent = Split-Path ([IO.Path]::GetFullPath($OutputPath)) -Parent
    New-Item -ItemType Directory -Path $parent -Force | Out-Null
    $report | ConvertTo-Json -Depth 8 | Set-Content $OutputPath -Encoding UTF8
    if (@($artifacts | Where-Object staticInspection -eq 'FAIL').Count -gt 0) {
        throw "Static artifact inspection FAILED; see $OutputPath"
    }
    Write-Host "Static artifact inspection passed: $OutputPath"
} finally { Pop-Location }
