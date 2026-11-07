param(
    [Parameter(Mandatory)][string]$Report,
    [string]$Filter = 'hoppersadditions_gametest:*'
)

$ErrorActionPreference = 'Stop'
if (!(Test-Path -LiteralPath $Report)) { throw "Missing GameTest report: $Report (startup or discovery failed)." }
[xml]$document = Get-Content -LiteralPath $Report -Raw
$cases = @($document.SelectNodes('//testcase'))
$manifest = Get-Content (Join-Path $PSScriptRoot 'scenarios.json') -Raw | ConvertFrom-Json
$expected = @($manifest | Where-Object { "hoppersadditions_gametest:$($_.id)" -like $Filter })
if ($expected.Count -eq 0) { throw "Zero project tests selected by '$Filter'." }
$matched = @{}
foreach ($case in $cases) {
    $identity = "$($case.classname) $($case.name)"
    if ($identity -notmatch 'hoppersadditions_gametest') { continue }
    foreach ($scenario in $expected) {
        if ($identity -match ('(?<![a-z0-9_])' + [regex]::Escape($scenario.id) + '(?![a-z0-9_])')) {
            if ($matched.ContainsKey($scenario.id)) { throw "Duplicate GameTest result: $($scenario.id)" }
            $matched[$scenario.id] = $case
        }
    }
}
foreach ($scenario in $expected) {
    if (!$matched.ContainsKey($scenario.id)) { throw "Required project test not discovered: $($scenario.id)" }
    $case = $matched[$scenario.id]
    if ($case.SelectNodes('failure|error|skipped').Count -gt 0) {
        throw "Required GameTest failed or skipped: $($scenario.id): $($case.InnerXml)"
    }
}
if ($document.SelectNodes('//failure|//error|//skipped').Count -gt 0) { throw 'GameTest report includes failed, errored or skipped tests.' }
Write-Host "PASS: $($matched.Count) required project GameTests in $Report"
