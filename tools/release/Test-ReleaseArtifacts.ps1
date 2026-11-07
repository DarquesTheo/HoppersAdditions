param([string]$OutputRoot)

$ErrorActionPreference = 'Stop'
$repository = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (!$OutputRoot) { $OutputRoot = Join-Path $repository ('build/issue8-fixtures/' + [guid]::NewGuid().ToString('N')) }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$properties = ConvertFrom-StringData (Get-Content (Join-Path $repository 'gradle.properties') -Raw)
$verifier = Join-Path $PSScriptRoot 'Verify-ReleaseArtifacts.ps1'
& $verifier -OutputPath (Join-Path $OutputRoot 'valid.json')
$cases = @('missing-license', 'missing-class', 'wrong-bytecode', 'missing-item-definition', 'legacy-recipe', 'wrong-metadata', 'missing-access-rule')
$results = foreach ($loader in @('fabric', 'neoforge')) {
    foreach ($case in $cases) {
        $fixtureRoot = Join-Path $OutputRoot "$loader-$case"
        foreach ($copyLoader in @('fabric', 'neoforge')) {
            $destination = Join-Path $fixtureRoot "$copyLoader/build/libs"
            New-Item -ItemType Directory -Path $destination -Force | Out-Null
            Copy-Item -LiteralPath (Join-Path $repository "$copyLoader/build/libs/hoppers-additions-$copyLoader-$($properties.mod_version).jar") -Destination $destination
        }
        $jarPath = Join-Path $fixtureRoot "$loader/build/libs/hoppers-additions-$loader-$($properties.mod_version).jar"
        $archive = [IO.Compression.ZipFile]::Open($jarPath, [IO.Compression.ZipArchiveMode]::Update)
        try {
            $entryPath = switch ($case) {
                'missing-license' { 'LICENSE' }
                'missing-class' { 'me/Wikos/hoppersadditions/Registration.class' }
                'wrong-bytecode' { 'me/Wikos/hoppersadditions/Registration.class' }
                'missing-item-definition' { 'assets/hoppersadditions/items/pulse_hopper.json' }
                'legacy-recipe' { 'data/hoppersadditions/recipe/pulse_hopper.json' }
                'wrong-metadata' { if ($loader -eq 'fabric') { 'fabric.mod.json' } else { 'META-INF/neoforge.mods.toml' } }
                'missing-access-rule' { if ($loader -eq 'fabric') { 'hoppersadditions.accesswidener' } else { 'META-INF/accesstransformer.cfg' } }
            }
            $entry = $archive.GetEntry($entryPath)
            if (!$entry) { throw "Fixture input missing: $entryPath" }
            if ($case -eq 'wrong-bytecode') {
                $memory = [IO.MemoryStream]::new()
                $source = $entry.Open()
                try { $source.CopyTo($memory) } finally { $source.Dispose() }
                $bytes = $memory.ToArray()
                $memory.Dispose()
                $bytes[7] = 64
            } elseif ($case -eq 'legacy-recipe') {
                $bytes = [Text.Encoding]::UTF8.GetBytes('{"type":"minecraft:crafting_shapeless","ingredients":[{"item":"minecraft:hopper"},{"item":"minecraft:repeater"}],"result":{"id":"hoppersadditions:pulse_hopper","count":1}}')
            } elseif ($case -eq 'wrong-metadata') {
                $reader = [IO.StreamReader]::new($entry.Open())
                try { $content = $reader.ReadToEnd().Replace($properties.mod_version, '0.3') } finally { $reader.Dispose() }
                $bytes = [Text.Encoding]::UTF8.GetBytes($content)
            }
            $entry.Delete()
            if ($case -in @('wrong-bytecode', 'legacy-recipe', 'wrong-metadata')) {
                $stream = $archive.CreateEntry($entryPath).Open()
                try { $stream.Write($bytes, 0, $bytes.Length) } finally { $stream.Dispose() }
            }
        } finally { $archive.Dispose() }
        $report = Join-Path $fixtureRoot 'result.json'
        $rejected = $false
        try { & $verifier -ArtifactRoot $fixtureRoot -OutputPath $report }
        catch {
            if (!(Test-Path -LiteralPath $report)) { throw }
            $evidence = Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
            $rejected = @($evidence.artifacts | Where-Object { $_.staticInspection -eq 'FAIL' }).Count -gt 0
        }
        if (!$rejected) { throw "Verifier accepted corrupt $loader fixture: $case" }
        [ordered]@{ loader = $loader; case = $case; result = 'REJECTED' }
    }
}
$results | ConvertTo-Json -Depth 4 | Set-Content (Join-Path $OutputRoot 'fixtures.json') -Encoding UTF8
Write-Host "PASS: $($results.Count) corrupt artifact fixtures rejected; unmodified artifacts passed. $OutputRoot"
