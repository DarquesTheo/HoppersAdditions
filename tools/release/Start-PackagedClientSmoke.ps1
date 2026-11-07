param(
    [Parameter(Mandatory = $true)][ValidateSet('fabric', 'neoforge')][string]$Loader,
    [string]$AssetsDirectory = "$env:USERPROFILE/.gradle/caches/fabric-loom/assets"
)

$ErrorActionPreference = 'Stop'
$repository = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$properties = ConvertFrom-StringData (Get-Content "$repository/gradle.properties" -Raw)
if ($properties.minecraft_version -ne '1.21.11') { throw 'This smoke launcher is scoped to Minecraft 1.21.11.' }
if (!(Test-Path "$env:JAVA_HOME/bin/java.exe")) { throw 'Select the documented JDK 21 using JAVA_HOME.' }
if (!(Test-Path "$AssetsDirectory/indexes/1.21.11-29.json")) { throw 'Run ./gradlew.bat :fabric:downloadAssets first.' }
$instance = Join-Path $repository "build/issue8/client-$Loader"
New-Item -ItemType Directory -Path "$instance/mods", "$instance/libraries" -Force | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Get-Download {
    param([string]$Url, [string]$Destination, [string]$Sha1)
    if (Test-Path -LiteralPath $Destination) {
        if (!$Sha1 -or (Get-FileHash -LiteralPath $Destination -Algorithm SHA1).Hash -eq $Sha1) { return }
    }
    New-Item -ItemType Directory -Path (Split-Path $Destination -Parent) -Force | Out-Null
    & curl.exe -fsSL --retry 2 -o $Destination $Url
    if ($LASTEXITCODE -ne 0) { throw "Download failed: $Url" }
    if ($Sha1 -and (Get-FileHash -LiteralPath $Destination -Algorithm SHA1).Hash -ne $Sha1) { throw "SHA-1 mismatch: $Destination" }
}

$manifest = Invoke-RestMethod 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'
$game = Invoke-RestMethod ($manifest.versions | Where-Object id -eq '1.21.11').url
$versionDirectory = "$instance/versions/1.21.11"
New-Item -ItemType Directory -Path $versionDirectory -Force | Out-Null
$game | ConvertTo-Json -Depth 50 | Set-Content "$versionDirectory/1.21.11.json" -Encoding UTF8
Get-Download $game.downloads.client.url "$versionDirectory/1.21.11.jar" $game.downloads.client.sha1
if ($Loader -eq 'fabric') {
    $profile = Invoke-RestMethod "https://meta.fabricmc.net/v2/versions/loader/1.21.11/$($properties.fabric_loader_version)/profile/json"
    Get-Download "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$($properties.fabric_api_version)/fabric-api-$($properties.fabric_api_version).jar" "$instance/mods/fabric-api.jar"
    Get-Download "https://maven.architectury.dev/dev/architectury/architectury-fabric/$($properties.architectury_api_version)/architectury-fabric-$($properties.architectury_api_version).jar" "$instance/mods/architectury.jar"
} else {
    $installer = "$repository/build/issue8/neoforge-$($properties.neoforge_version)-installer.jar"
    Get-Download "https://maven.neoforged.net/releases/net/neoforged/neoforge/$($properties.neoforge_version)/neoforge-$($properties.neoforge_version)-installer.jar" $installer
    if (!(Test-Path "$instance/launcher_profiles.json")) { '{"profiles":{}}' | Set-Content "$instance/launcher_profiles.json" -Encoding UTF8 }
    & "$env:JAVA_HOME/bin/java.exe" -jar $installer --installClient $instance *> "$instance/install.log"
    if ($LASTEXITCODE -ne 0) { throw "NeoForge client installation failed; inspect $instance/install.log" }
    $profile = Get-Content "$instance/versions/neoforge-$($properties.neoforge_version)/neoforge-$($properties.neoforge_version).json" -Raw | ConvertFrom-Json
    Get-Download "https://maven.architectury.dev/dev/architectury/architectury-neoforge/$($properties.architectury_api_version)/architectury-neoforge-$($properties.architectury_api_version).jar" "$instance/mods/architectury.jar"
}
Copy-Item -LiteralPath "$repository/$Loader/build/libs/hoppers-additions-$Loader-$($properties.mod_version).jar" -Destination "$instance/mods/"
$libraries = @($profile.libraries) + @($game.libraries)
$seen = @{}
$classpath = @()
foreach ($library in $libraries) {
    $allowed = !$library.rules
    foreach ($rule in $library.rules) {
        $matchesOs = !$rule.os -or ((!$rule.os.name -or $rule.os.name -eq 'windows') -and (!$rule.os.arch -or $rule.os.arch -eq 'amd64' -or $rule.os.arch -eq 'x86_64'))
        if ($matchesOs -and !$rule.features) { $allowed = $rule.action -eq 'allow' }
    }
    if (!$allowed) { continue }
    $parts = $library.name.Split(':')
    $identity = $parts[0] + ':' + $parts[1] + ':' + ($parts | Select-Object -Skip 3)
    if ($seen.ContainsKey($identity)) { continue }
    $seen[$identity] = $true
    $artifact = $library.downloads.artifact
    if ($artifact) {
        $relative = $artifact.path
        $url = $artifact.url
        $sha1 = $artifact.sha1
    } else {
        $classifier = if ($parts.Length -gt 3) { '-' + $parts[3] } else { '' }
        $relative = $parts[0].Replace('.', '/') + '/' + $parts[1] + '/' + $parts[2] + '/' + $parts[1] + '-' + $parts[2] + $classifier + '.jar'
        $url = $library.url.TrimEnd('/') + '/' + $relative
        $sha1 = $null
    }
    $destination = "$instance/libraries/$relative"
    if (!(Test-Path -LiteralPath $destination) -or $sha1) { Get-Download $url $destination $sha1 }
    $classpath += $destination.Replace('\', '/')
}
$classpath += "$versionDirectory/1.21.11.jar"
$arguments = @('-Xmx2G', '-Dminecraft.launcher.brand=issue8-packaged-smoke', '-Dminecraft.launcher.version=1')
foreach ($argument in @($profile.arguments.jvm)) {
    if ($argument -is [string]) { $arguments += $argument.Replace('${library_directory}', "$instance/libraries") }
}
$arguments += @('-cp', ($classpath -join ';'), $profile.mainClass, '--username', 'Issue8Smoke', '--version', $profile.id, '--gameDir', $instance, '--assetsDir', $AssetsDirectory, '--assetIndex', '1.21.11-29', '--uuid', '00000000000000000000000000000008', '--accessToken', '0', '--userType', 'msa', '--versionType', 'release')
$arguments += @($profile.arguments.game | Where-Object { $_ -is [string] })
$argFile = "$instance/client-args.txt"
$quoted = @($arguments | ForEach-Object { '"' + $_.Replace('\', '/').Replace('"', '\"') + '"' })
[IO.File]::WriteAllLines($argFile, $quoted, [Text.UTF8Encoding]::new($false))
$process = Start-Process -FilePath "$env:JAVA_HOME/bin/java.exe" -ArgumentList ('@"' + $argFile + '"') -WorkingDirectory $instance -WindowStyle Hidden -RedirectStandardOutput "$instance/stdout.log" -RedirectStandardError "$instance/stderr.log" -PassThru
$receipt = [ordered]@{
    loader = $Loader
    pid = $process.Id
    launchedAt = (Get-Date -Format o)
    instance = $instance
    artifactSha256 = (Get-FileHash "$instance/mods/hoppers-additions-$Loader-$($properties.mod_version).jar" -Algorithm SHA256).Hash
    command = 'java @client-args.txt'
    scope = 'Packaged production JAR; no development classes. Offline smoke identity is not authenticated multiplayer or gameplay evidence. No worlds or EULA files are edited.'
}
$receipt | ConvertTo-Json -Depth 5 | Set-Content "$instance/receipt.json" -Encoding UTF8
$receipt | ConvertTo-Json -Depth 5
