[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repositoryRoot = [IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
$manifestPath = Join-Path $repositoryRoot "compatibility\fixtures.json"
$fixtureRoot = [IO.Path]::GetFullPath(
    (Join-Path $repositoryRoot "app\src\test\resources\archive-fixtures")
)
$manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json

if ($manifest.schemaVersion -ne 2) {
    throw "Unsupported fixture manifest schema: $($manifest.schemaVersion)"
}

$declaredFiles = [Collections.Generic.HashSet[string]]::new(
    [StringComparer]::OrdinalIgnoreCase
)
$verifiedPhysicalFiles = 0

function Test-DeclaredFile {
    param(
        [Parameter(Mandatory)]
        [string]$RelativePath,

        [Parameter(Mandatory)]
        [string]$ExpectedSha256,

        [long]$ExpectedSize = -1
    )

    $absolutePath = [IO.Path]::GetFullPath((Join-Path $repositoryRoot $RelativePath))
    $fixturePrefix = $fixtureRoot + [IO.Path]::DirectorySeparatorChar
    if (-not $absolutePath.StartsWith($fixturePrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Fixture path escapes the archive fixture directory: $RelativePath"
    }
    if (-not $declaredFiles.Add($absolutePath)) {
        throw "Fixture is declared more than once: $RelativePath"
    }
    if (-not (Test-Path -LiteralPath $absolutePath -PathType Leaf)) {
        throw "Declared fixture is missing: $RelativePath"
    }

    $item = Get-Item -LiteralPath $absolutePath
    if ($ExpectedSize -ge 0 -and $item.Length -ne $ExpectedSize) {
        throw "Fixture size mismatch for $RelativePath"
    }
    $actualSha256 = (Get-FileHash -LiteralPath $absolutePath -Algorithm SHA256).Hash
    if (-not $actualSha256.Equals($ExpectedSha256, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Fixture SHA-256 mismatch for $RelativePath"
    }

    $script:verifiedPhysicalFiles++
}

foreach ($sample in $manifest.samples) {
    $primarySize = if ($sample.PSObject.Properties.Name -contains "size") {
        [long]$sample.size
    } else {
        -1L
    }
    Test-DeclaredFile `
        -RelativePath $sample.file `
        -ExpectedSha256 $sample.sha256 `
        -ExpectedSize $primarySize

    $companionVolumes = if ($sample.PSObject.Properties.Name -contains "companionVolumes") {
        @($sample.companionVolumes)
    } else {
        @()
    }
    foreach ($companion in $companionVolumes) {
        Test-DeclaredFile `
            -RelativePath $companion.file `
            -ExpectedSha256 $companion.sha256 `
            -ExpectedSize ([long]$companion.size)
    }
}

$physicalFixtures = @(
    Get-ChildItem -LiteralPath $fixtureRoot -File -Recurse | ForEach-Object {
        [IO.Path]::GetFullPath($_.FullName)
    }
)
$undeclaredFiles = @($physicalFixtures | Where-Object { -not $declaredFiles.Contains($_) })
if ($undeclaredFiles.Count -gt 0) {
    throw "Archive fixture files are not declared in compatibility/fixtures.json: $($undeclaredFiles -join ', ')"
}

Write-Host "Verified $($manifest.samples.Count) samples and $verifiedPhysicalFiles physical files."
