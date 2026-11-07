$ErrorActionPreference = 'Stop'
$directory = Join-Path (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent) 'build/gametest-report-fixtures'
New-Item -ItemType Directory -Path $directory -Force | Out-Null
$report = Join-Path $directory 'fixture.xml'
$gate = Join-Path $PSScriptRoot 'Assert-GameTestReport.ps1'
$manifest = Get-Content (Join-Path $PSScriptRoot 'scenarios.json') -Raw | ConvertFrom-Json
$valid = '<testsuite>' + (($manifest | ForEach-Object { '<testcase classname="hoppersadditions_gametest" name="' + $_.id + '"/>' }) -join '') + '</testsuite>'
$valid | Set-Content -LiteralPath $report -Encoding UTF8
& $gate -Report $report
$fixtures = @{
    zero = '<testsuite tests="0"/>'
    missing = '<testsuite><testcase name="reinforced_cooldown"/></testsuite>'
    failed = $valid.Replace('name="reinforced_cooldown"/>', 'name="reinforced_cooldown"><failure>controlled assertion</failure></testcase>')
    skipped = $valid.Replace('name="reinforced_cooldown"/>', 'name="reinforced_cooldown"><skipped/></testcase>')
    errored = $valid.Replace('name="reinforced_cooldown"/>', 'name="reinforced_cooldown"><error>startup</error></testcase>')
    malformed = '<testsuite'
}
foreach ($fixture in $fixtures.GetEnumerator()) {
    $fixture.Value | Set-Content -LiteralPath $report -Encoding UTF8
    $rejected = $false
    try { & $gate -Report $report } catch { $rejected = $true }
    if (!$rejected) { throw "Gate accepted $($fixture.Key) fixture." }
}
Remove-Item -LiteralPath $report
$rejected = $false
try { & $gate -Report $report } catch { $rejected = $true }
if (!$rejected) { throw 'Gate accepted a missing report.' }
Write-Host 'PASS: valid report accepted; seven invalid/missing report fixtures rejected (not Minecraft execution).'
