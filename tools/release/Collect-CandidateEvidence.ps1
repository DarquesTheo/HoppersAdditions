param()

$ErrorActionPreference = 'Stop'
$repository = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (!$env:JAVA_HOME -or !(Test-Path "$env:JAVA_HOME/bin/javac.exe")) {
    throw 'Select a full build JDK using the README JAVA_HOME instructions first.'
}

function Invoke-LoggedCommand {
    param([string]$Executable, [string[]]$Arguments, [string]$Log)
    $ErrorActionPreference = 'Continue'
    & $Executable @Arguments 2>&1 | Tee-Object -FilePath $Log | Out-Host
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed ($LASTEXITCODE): $Executable $Arguments. See $Log"
    }
}

function Read-ArchiveText {
    param($Archive, [string]$Path)
    $entry = $Archive.GetEntry($Path)
    if (!$entry) { throw "Missing packaged entry: $Path" }
    $reader = [IO.StreamReader]::new($entry.Open())
    try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
}

Push-Location $repository
try {
    $env:PATH = "$env:JAVA_HOME/bin;$env:PATH"
    $output = Join-Path $repository ("build/issue8-evidence/" + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0, 8))
    New-Item -ItemType Directory -Path $output -Force | Out-Null
    Invoke-LoggedCommand "$env:JAVA_HOME/bin/java.exe" @('--version') "$output/java.log"
    Invoke-LoggedCommand './gradlew.bat' @('--version') "$output/gradle.log"
    Invoke-LoggedCommand './gradlew.bat' @('clean', ':common:build', ':fabric:build', ':neoforge:build', '--console=plain') "$output/build.log"

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $properties = ConvertFrom-StringData (Get-Content gradle.properties -Raw)
    $artifacts = foreach ($loader in @('fabric', 'neoforge')) {
        $path = "$loader/build/libs/hoppers-additions-$loader-$($properties.mod_version).jar"
        $archive = [IO.Compression.ZipFile]::OpenRead((Join-Path $repository $path))
        try {
            $descriptorPath = if ($loader -eq 'fabric') { 'fabric.mod.json' } else { 'META-INF/neoforge.mods.toml' }
            $descriptor = Read-ArchiveText $archive $descriptorPath
            if ($loader -eq 'fabric') {
                $metadata = $descriptor | ConvertFrom-Json
                if ($metadata.version -ne $properties.mod_version -or $metadata.depends.minecraft -ne '=1.21.11') {
                    throw 'Unexpected packaged Fabric version or Minecraft range.'
                }
            } elseif ($descriptor -notmatch 'version\s*=\s*"0\.4\.0\+mc1\.21\.11"' -or $descriptor -notmatch '\[1\.21\.11\]' -or $descriptor -notmatch '\[21\.11\.48,21\.12\)') {
                throw 'Unexpected packaged NeoForge version or compatibility ranges.'
            }
            $descriptor | Set-Content "$output/$loader-descriptor.txt" -Encoding UTF8
            $resourceCount = 0
            foreach ($feature in @('splitting', 'overflow', 'pulse')) {
                $resourceRoot = (Resolve-Path common/src/main/resources).Path
                $resources = Get-ChildItem $resourceRoot -File -Recurse | Where-Object { $_.Name -like "${feature}_hopper*" }
                if (!$resources) { throw "No source resources found for $feature" }
                foreach ($resource in $resources) {
                    $entryPath = $resource.FullName.Substring($resourceRoot.Length + 1).Replace('\', '/')
                    $entry = $archive.GetEntry($entryPath)
                    if (!$entry) { throw "Missing $loader resource: $entryPath" }
                    $stream = $entry.Open()
                    $algorithm = [Security.Cryptography.SHA256]::Create()
                    try {
                        $packagedHash = [BitConverter]::ToString($algorithm.ComputeHash($stream)).Replace('-', '')
                    } finally { $stream.Dispose(); $algorithm.Dispose() }
                    if ($packagedHash -ne (Get-FileHash $resource.FullName -Algorithm SHA256).Hash) {
                        throw "Stale $loader resource: $entryPath"
                    }
                    $resourceCount++
                }
                $className = (Get-Culture).TextInfo.ToTitleCase($feature) + 'Hopper'
                foreach ($classPath in @("me/Wikos/hoppersadditions/Block/Hoppers/${className}Block.class", "me/Wikos/hoppersadditions/BlockEntity/Hoppers/${className}BlockEntity.class")) {
                    $entry = $archive.GetEntry($classPath)
                    if (!$entry) { throw "Missing class: $classPath" }
                    $reader = [IO.BinaryReader]::new($entry.Open())
                    try { $header = $reader.ReadBytes(8) } finally { $reader.Dispose() }
                    if ($header.Length -ne 8 -or $header[6] -ne 0 -or $header[7] -ne 65) {
                        throw "Expected Java 21 class: $classPath"
                    }
                }
            }
            $loaderVersion = if ($loader -eq 'fabric') { $properties.fabric_loader_version } else { $properties.neoforge_version }
            [ordered]@{
                loader = $loader
                loaderVersion = $loaderVersion
                path = $path
                sha256 = (Get-FileHash $path -Algorithm SHA256).Hash
                featureResourcesMatched = $resourceCount
                featureClassesJava21 = 6
            }
        } finally { $archive.Dispose() }
    }

    & "$PSScriptRoot/Verify-ReleaseArtifacts.ps1" -OutputPath "$output/artifacts.json"
    Invoke-LoggedCommand './gradlew.bat' @('-I', 'tools/pulse-regression.gradle', ':common:pulseRegression', '--console=plain') "$output/regressions.log"

    $sourceFiles = foreach ($path in (& git ls-files --cached --others --exclude-standard)) {
        if (Test-Path -LiteralPath $path -PathType Leaf) {
            [ordered]@{ path = $path; sha256 = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash }
        }
    }
    $status = @(& git status --porcelain=v1)
    if ($LASTEXITCODE -ne 0) { throw 'Cannot capture worktree identity.' }
    $evidence = [ordered]@{
        recordedAt = (Get-Date -Format o)
        head = (& git rev-parse HEAD)
        dirty = ($status.Count -gt 0)
        worktreeStatus = $status
        minecraft = $properties.minecraft_version
        architectury = $properties.architectury_api_version
        fabricApi = $properties.fabric_api_version
        artifacts = @($artifacts)
        sourceFiles = @($sourceFiles)
        buildAndPackage = 'PASS'
        dedicatedServerStartup = 'UNVERIFIED: record separate runtime logs and Ready/Done state'
        clientGameplay = 'UNVERIFIED: use the per-loader matrix in docs/1.21.11-migration.md'
        worldUpgrade = 'UNVERIFIED: needs a backed-up representative 1.21.1 world'
    }
    $evidence | ConvertTo-Json -Depth 8 | Set-Content "$output/candidate.json" -Encoding UTF8
    Write-Host "Build/package evidence only: $output/candidate.json"
    Write-Host 'No gameplay, dedicated-server startup, or world-upgrade acceptance is marked passed.'
} finally { Pop-Location }
