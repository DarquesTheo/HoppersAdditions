param([string]$ArtifactRoot, [string]$OutputPath)

$ErrorActionPreference = 'Stop'
$repository = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (!$ArtifactRoot) { $ArtifactRoot = $repository }
if (!$OutputPath) { $OutputPath = Join-Path $repository 'build/issue7-evidence/artifacts.json' }
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
    Assert-Release ($properties.mod_version -eq '0.3') 'This release gate expects version 0.3.'
    $sourceRoot = (Resolve-Path common/src/main/resources).Path
    $sourceResources = @(Get-ChildItem "$sourceRoot/assets", "$sourceRoot/data" -Recurse -File)
    $sourceClasses = @(Get-ChildItem common/src/main/java -Recurse -Filter '*.java' -File)
    $javaRoot = (Resolve-Path common/src/main/java).Path
    $artifacts = foreach ($loader in @('fabric', 'neoforge')) {
        $relativePath = "$loader/build/libs/hoppers-additions-$loader-0.3.jar"
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
                $actualIngredients = @($recipe.ingredients | ForEach-Object { $_.item } | Sort-Object)
                Assert-Release (($actualIngredients -join ',') -eq (@($ingredients[$feature] | Sort-Object) -join ',')) "Wrong crafting ingredients: $id"
                $loot = $json["data/hoppersadditions/loot_table/blocks/$id.json"]
                Assert-Release ($loot.type -eq 'minecraft:block' -and "hoppersadditions:$id" -in @($loot.pools.entries.name)) "Missing block loot: $id"
            }
            if ($loader -eq 'fabric') {
                $metadata = $json['fabric.mod.json']
                Assert-Release ($metadata.license -eq 'GPL-3.0-only') 'Wrong Fabric license.'
                Assert-Release ($metadata.id -eq 'hoppersadditions' -and $metadata.version -eq '0.3') 'Wrong Fabric ID/version.'
                $expected = @{ minecraft = '>=1.21.1 <1.21.2'; fabricloader = '>=0.18.4'; java = '>=21'; architectury = '>=13.0.8'; 'fabric-api' = '*' }
                foreach ($key in $expected.Keys) { Assert-Release ($metadata.depends.$key -eq $expected[$key]) "Wrong Fabric dependency: $key" }
                Assert-Release (![string]::IsNullOrWhiteSpace($metadata.description) -and !$metadata.suggests) 'Empty Fabric description or placeholder suggestions.'
                foreach ($className in @($metadata.entrypoints.main) + @($metadata.entrypoints.client)) {
                    Assert-Release ($null -ne $archive.GetEntry($className.Replace('.', '/') + '.class')) "Missing Fabric entrypoint: $className"
                }
                $descriptor = Read-Entry $archive 'fabric.mod.json'
            } else {
                $descriptor = Read-Entry $archive 'META-INF/neoforge.mods.toml'
                Assert-Release ($descriptor -match '(?m)^license\s*=\s*"GPL-3\.0-only"\s*$') 'Wrong NeoForge license.'
                Assert-Release ($descriptor -match '(?m)^version\s*=\s*"0\.3"\s*$' -and $descriptor -match '(?m)^modId\s*=\s*"hoppersadditions"\s*$') 'Wrong NeoForge version/ID.'
                $expected = @{ neoforge = '[21.1.220,21.2)'; minecraft = '[1.21.1,1.21.2)'; architectury = '[13.0.8,)' }
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
        gameplay = 'UNVERIFIED; compare these hashes with issue #6 evidence.'
    }
    $parent = Split-Path ([IO.Path]::GetFullPath($OutputPath)) -Parent
    New-Item -ItemType Directory -Path $parent -Force | Out-Null
    $report | ConvertTo-Json -Depth 8 | Set-Content $OutputPath -Encoding UTF8
    if (@($artifacts | Where-Object staticInspection -eq 'FAIL').Count -gt 0) {
        throw "Static artifact inspection FAILED; see $OutputPath"
    }
    Write-Host "Static artifact inspection passed: $OutputPath"
} finally { Pop-Location }
