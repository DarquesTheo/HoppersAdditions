param(
    [ValidateSet('both', 'fabric', 'neoforge')][string]$Loader = 'both',
    [string]$Filter = 'hoppersadditions_gametest:*',
    [switch]$EulaAccepted,
    [switch]$ValidateGuards
)

$ErrorActionPreference = 'Stop'
if (!$EulaAccepted) {
    throw 'Read https://www.minecraft.net/eula and explicitly authorize disposable GameTest servers with -EulaAccepted. Neither loader is started without authorization; their headless runners bypass the normal EULA prompt.'
}
$repository = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$gradle = if ($env:OS -eq 'Windows_NT') { Join-Path $repository 'gradlew.bat' } else { Join-Path $repository 'gradlew' }
$launcher = if ($env:OS -eq 'Windows_NT') { $gradle } else { 'sh' }
$prefix = @(if ($env:OS -ne 'Windows_NT') { $gradle })
$loaders = if ($Loader -eq 'both') { @('fabric', 'neoforge') } else { @($Loader) }

function Invoke-Suite {
    param([string]$Platform, [string]$Selection, [string]$Label, [switch]$ControlledFailure)
    $directory = Join-Path $repository "$Platform/build/gametest"
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
    $report = Join-Path $directory 'results.xml'
    if (Test-Path -LiteralPath $report) { Remove-Item -LiteralPath $report }
    $arguments = @("$($Platform):runGameTest", '-PgameTestEulaAccepted=true', "-PgameTestFilter=$Selection", '--console=plain', '--no-daemon')
    if ($ControlledFailure) { $arguments += '-PgameTestControlledFailure' }
    $log = Join-Path $directory "$Label.log"
    try {
        $ErrorActionPreference = 'Continue'
        & $launcher @prefix @arguments 2>&1 | Tee-Object -FilePath $log | Out-Host
        $exitCode = $LASTEXITCODE
    } finally { $ErrorActionPreference = 'Stop' }
    $gateError = $null
    try { & (Join-Path $PSScriptRoot 'Assert-GameTestReport.ps1') -Report $report -Filter $Selection }
    catch { $gateError = $_.Exception.Message }
    if (Test-Path -LiteralPath $report) { Copy-Item -LiteralPath $report -Destination (Join-Path $directory "$Label.xml") }
    return [pscustomobject]@{ ExitCode = $exitCode; GateError = $gateError; Report = $report; Log = $log }
}

Push-Location $repository
try {
    foreach ($platform in $loaders) {
        $normal = Invoke-Suite $platform $Filter 'suite'
        if ($normal.ExitCode -ne 0 -or $normal.GateError) { throw "$platform GameTests failed (exit $($normal.ExitCode)): $($normal.GateError). Log: $($normal.Log)" }
        if ($ValidateGuards) {
            $failure = Invoke-Suite $platform 'hoppersadditions_gametest:reinforced_cooldown' 'controlled-failure' -ControlledFailure
            if ($failure.ExitCode -eq 0 -or !$failure.GateError -or !(Test-Path -LiteralPath $failure.Report)) { throw "$platform controlled-failure guard was not established." }
            if ((Get-Content -LiteralPath $failure.Report -Raw) -notmatch 'Controlled runner validation') { throw "$platform failed for a reason other than the deliberate assertion." }
            $empty = Invoke-Suite $platform 'hoppersadditions_gametest:does_not_exist' 'zero-discovery'
            if (!$empty.GateError) { throw "$platform accepted zero project tests." }
            if ((Get-Content -LiteralPath $empty.Log -Raw) -notmatch '(?i)(no.*tests|0 tests|empty.*test)') { throw "$platform zero-discovery run did not reach test selection; inspect $($empty.Log)." }
            Write-Host "PASS: $platform required failure and zero-discovery guards. Normal results: $platform/build/gametest/suite.xml"
        }
    }
} finally { Pop-Location }
