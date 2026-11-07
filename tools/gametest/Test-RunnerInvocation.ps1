$ErrorActionPreference = 'Stop'
$fixture = Join-Path ([System.IO.Path]::GetTempPath()) ('hoppers-runner-' + [guid]::NewGuid())
$previousOS = $env:OS
try {
    $tools = Join-Path $fixture 'tools/gametest'
    New-Item -ItemType Directory -Path $tools -Force | Out-Null
    Copy-Item (Join-Path $PSScriptRoot 'Run-GameTests.ps1'), (Join-Path $PSScriptRoot 'Assert-GameTestReport.ps1'), (Join-Path $PSScriptRoot 'scenarios.json') -Destination $tools
    $env:OS = 'Linux'
    function sh {
        $expected = Join-Path $fixture 'gradlew'
        if ($args.Count -ne 6 -or $args[0] -cne $expected -or $args[1] -cne ($script:platform + ':runGameTest') -or $args[2] -cne '-PgameTestEulaAccepted=true' -or $args[3] -cne '-PgameTestFilter=hoppersadditions_gametest:*' -or $args[4] -cne '--console=plain' -or $args[5] -cne '--no-daemon') {
            throw "Incorrect Unix Gradle arguments: $($args -join ' | ')"
        }
        $manifest = Get-Content (Join-Path $tools 'scenarios.json') -Raw | ConvertFrom-Json
        $xml = '<testsuite>' + (($manifest | ForEach-Object { '<testcase classname="hoppersadditions_gametest" name="' + $_.id + '"/>' }) -join '') + '</testsuite>'
        $xml | Set-Content -LiteralPath (Join-Path $fixture "$script:platform/build/gametest/results.xml") -Encoding UTF8
        $global:LASTEXITCODE = 0
    }
    foreach ($platform in @('fabric', 'neoforge')) {
        & (Join-Path $tools 'Run-GameTests.ps1') -Loader $platform -EulaAccepted
    }
    Write-Host 'PASS: Unix runner passes the complete wrapper path and Gradle arguments for both loaders (mock launcher; no server started).'
} finally {
    $env:OS = $previousOS
    if (Test-Path -LiteralPath $fixture) { Remove-Item -LiteralPath $fixture -Recurse -Force }
}
