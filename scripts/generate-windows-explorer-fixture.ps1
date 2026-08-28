[CmdletBinding()]
param(
    [string]$OutputPath = "app\src\test\resources\archive-fixtures\windows-explorer-11-deflate-unicode.zip"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repositoryRoot = [IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
$sourceRoot = [IO.Path]::GetFullPath(
    (Join-Path $repositoryRoot "compatibility\fixture-sources\7zip-22")
)
$resolvedOutput = if ([IO.Path]::IsPathRooted($OutputPath)) {
    [IO.Path]::GetFullPath($OutputPath)
} else {
    [IO.Path]::GetFullPath((Join-Path $repositoryRoot $OutputPath))
}
$fixtureRoot = [IO.Path]::GetFullPath(
    (Join-Path $repositoryRoot "app\src\test\resources\archive-fixtures")
)
$fixturePrefix = $fixtureRoot + [IO.Path]::DirectorySeparatorChar

if (-not $resolvedOutput.StartsWith($fixturePrefix, [StringComparison]::OrdinalIgnoreCase)) {
    throw "Output must stay inside the archive fixture directory: $resolvedOutput"
}
if (Test-Path -LiteralPath $resolvedOutput) {
    throw "Refusing to replace an existing fixture: $resolvedOutput"
}

$sourceNames = @("ascii.txt", "目录")
foreach ($name in $sourceNames) {
    if (-not (Test-Path -LiteralPath (Join-Path $sourceRoot $name))) {
        throw "Fixture source is missing: $name"
    }
}

$outputDirectory = Split-Path -Parent $resolvedOutput
if (-not (Test-Path -LiteralPath $outputDirectory -PathType Container)) {
    throw "Fixture output directory is missing: $outputDirectory"
}

$emptyZip = [byte[]](
    0x50, 0x4B, 0x05, 0x06,
    0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00,
    0x00, 0x00
)
[IO.File]::WriteAllBytes($resolvedOutput, $emptyZip)

$shell = $null
$sourceNamespace = $null
$archiveNamespace = $null
try {
    $shell = New-Object -ComObject Shell.Application
    $sourceNamespace = $shell.NameSpace($sourceRoot)
    $archiveNamespace = $shell.NameSpace($resolvedOutput)
    if ($null -eq $sourceNamespace -or $null -eq $archiveNamespace) {
        throw "Windows Explorer ZIP namespace is unavailable"
    }

    for ($index = 0; $index -lt $sourceNames.Count; $index++) {
        $name = $sourceNames[$index]
        $item = $sourceNamespace.ParseName($name)
        if ($null -eq $item) {
            throw "Windows Explorer cannot resolve fixture source: $name"
        }
        $archiveNamespace.CopyHere($item, 0x14)

        $itemDeadline = [DateTime]::UtcNow.AddSeconds(30)
        $expectedItemCount = $index + 1
        do {
            Start-Sleep -Milliseconds 200
            $copiedItemCount = $archiveNamespace.Items().Count
        } while ($copiedItemCount -lt $expectedItemCount -and [DateTime]::UtcNow -lt $itemDeadline)
        if ($copiedItemCount -lt $expectedItemCount) {
            throw "Windows Explorer did not copy fixture source within the time limit: $name"
        }
    }

    $deadline = [DateTime]::UtcNow.AddSeconds(30)
    $lastLength = -1L
    $stableSamples = 0
    do {
        Start-Sleep -Milliseconds 200
        $currentLength = (Get-Item -LiteralPath $resolvedOutput).Length
        $topLevelCount = $archiveNamespace.Items().Count
        if ($topLevelCount -eq $sourceNames.Count -and $currentLength -eq $lastLength) {
            $stableSamples++
        } else {
            $stableSamples = 0
        }
        $lastLength = $currentLength
    } while ($stableSamples -lt 5 -and [DateTime]::UtcNow -lt $deadline)

    if ($stableSamples -lt 5 -or $lastLength -le $emptyZip.Length) {
        throw (
            "Windows Explorer did not finish the ZIP fixture within the time limit " +
                "(top-level items: $topLevelCount, bytes: $lastLength)"
        )
    }
} catch {
    if (Test-Path -LiteralPath $resolvedOutput -PathType Leaf) {
        Remove-Item -LiteralPath $resolvedOutput -Force
    }
    throw
} finally {
    foreach ($value in @($archiveNamespace, $sourceNamespace, $shell)) {
        if ($null -ne $value -and [Runtime.InteropServices.Marshal]::IsComObject($value)) {
            [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($value)
        }
    }
}

$zipHandler = Get-Item -LiteralPath (Join-Path $env:SystemRoot "System32\zipfldr.dll")
$digest = (Get-FileHash -LiteralPath $resolvedOutput -Algorithm SHA256).Hash.ToLowerInvariant()
Write-Host "Created $resolvedOutput"
Write-Host "zipfldr.dll $($zipHandler.VersionInfo.FileVersion)"
Write-Host "SHA-256 $digest"
