# Offline acceptance check for the complete PowerShell-to-Java path.
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function New-Row($Id, $Role, $Mode, $Days) {
    [pscustomobject]@{ id = "adzuna:$Id"; title = 'Private fixture title'; excerpt = 'Private fixture excerpt'
        matchingFacts = [pscustomobject]@{ role = $Role; mode = $Mode; officeDays = $Days
            technologies = $null; experience = $null; office = $null; residencePolicy = $null } }
}
function New-Page([array]$Rows) {
    [pscustomobject]@{ source = 'Adzuna'; page = 3; received = $Rows.Count; rejected = 0; duplicates = 0; listings = $Rows }
}
function Invoke-FixtureStudy($Page) {
    $outputJson = & (Join-Path $PSScriptRoot 'Measure-Page.ps1') -Page $Page
    if ($outputJson -match 'Private fixture|adzuna:|comparisonRefs') { throw 'Unexpected listing data in report.' }
    return $outputJson | ConvertFrom-Json
}
function Assert-Count($Expected, $Actual) {
    if ($Expected -ne $Actual) { throw "Expected $Expected but got $Actual" }
}

# Mimic the JSON objects produced by Invoke-RestMethod. Two known roles tie even though
# just one confirms attendance; the unknown schedule is not counted as within the limit.
$page = New-Page @(
    (New-Row 'first' 'BACKEND' 'HYBRID' 1)
    (New-Row 'second' 'BACKEND' 'HYBRID' $null)
    (New-Row 'third' 'BACKEND' 'HYBRID' 2)
    (New-Row 'fourth' $null $null $null)
    (New-Row 'fifth' $null $null $null)
    (New-Row 'sixth' $null $null $null)
)
$report = Invoke-FixtureStudy ($page | ConvertTo-Json -Depth 6 | ConvertFrom-Json)
Assert-Count 6 $report.listings
Assert-Count 3 $report.comparable.role
Assert-Count 3 $report.comparable.workMode
Assert-Count 2 $report.comparable.weeklyAttendance
Assert-Count 1 $report.attendance.withinLimit
Assert-Count 1 $report.attendance.exceedsLimit
Assert-Count 4 $report.attendance.unknown
Assert-Count 1 $report.explicitConflicts
Assert-Count 3 $report.withoutComparableFacts
Assert-Count 5 $report.ties.listings
Assert-Count 4 $report.ties.pairs
Assert-Count 15 $report.ties.possiblePairs

$empty = Invoke-FixtureStudy (New-Page @())
Assert-Count 0 $empty.listings
Assert-Count 0 $empty.ties.possiblePairs
Assert-Count 0 $empty.ties.listings

$page.listings[1].id = $page.listings[0].id
try { $null = Invoke-FixtureStudy $page; throw 'Duplicate fixture unexpectedly accepted.' }
catch { if ($_.Exception.Message -notmatch 'distinct mapped listings') { throw } }
'Page study checks passed.'
