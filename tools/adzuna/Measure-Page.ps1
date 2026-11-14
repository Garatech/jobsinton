# Measure a normalized search response already held in memory. Never request a page.
[CmdletBinding()]
param([Parameter(Mandatory)] [pscustomobject]$Page)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Test-Count($Value) {
    return (($Value -is [int] -or $Value -is [long]) -and $Value -ge 0 -and $Value -le 10)
}

function Get-Token($Value, [string[]]$Allowed) {
    if ($null -eq $Value) { return '-' }
    if ($Value -isnot [string] -or $Value -cnotin $Allowed) { throw 'Unrecognised mapped fact; no report produced.' }
    return $Value
}

if ($Page.source -cne 'Adzuna' -or $Page.listings -isnot [array] -or
    !(Test-Count $Page.received) -or !(Test-Count $Page.duplicates) -or !(Test-Count $Page.rejected) -or
    $Page.received -ne ($Page.listings.Count + $Page.duplicates + $Page.rejected)) {
    throw 'Expected one complete JobSinton Adzuna search response.'
}

$roles = @('BACKEND', 'FRONTEND', 'FULL_STACK', 'SAP', 'DATA', 'PLATFORM')
$modes = @('REMOTE', 'HYBRID', 'ONSITE')
$ids = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
$rows = @(
    foreach ($listing in $Page.listings) {
        if ($listing.id -isnot [string] -or !$listing.id.StartsWith('adzuna:') -or !$ids.Add($listing.id)) {
            throw 'Expected distinct mapped listings.'
        }
        $facts = $listing.matchingFacts
        if ($null -eq $facts) { throw 'Missing mapped facts. Use a normalized page from step 5A or later.' }
        foreach ($field in @('technologies', 'experience', 'office', 'residencePolicy')) {
            if ($null -ne $facts.$field) { throw 'The mapped facts have changed. Review the study before measuring this page.' }
        }
        $role = Get-Token $facts.role $roles
        $mode = Get-Token $facts.mode $modes
        $days = $facts.officeDays
        if ($null -eq $days) { $days = '-' }
        elseif (!(Test-Count $days) -or $days -gt 5) { throw 'Invalid weekly attendance.' }
        "${role}:${mode}:$days"
    }
)

$classes = Join-Path $PSScriptRoot '../../backend/target/classes'
if (!(Test-Path -LiteralPath (Join-Path $classes 'io/github/garatech/jobsinton/ranking/CompatibilityRanker.class'))) {
    throw 'Compile the current backend first: mvn -B -ntp -f backend/pom.xml compile'
}

$source = Join-Path $PSScriptRoot 'PageStudy.java'
$report = & java --class-path $classes $source @rows
if ($LASTEXITCODE -ne 0) { throw 'Page measurement failed. Check Java 21 and recompile the current backend.' }
$report -join [Environment]::NewLine
