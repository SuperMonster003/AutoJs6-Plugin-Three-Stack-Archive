#Requires -Version 7.0

[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$SourceDirectory,

    [Parameter(Mandatory)]
    [string]$ToolchainDirectory,

    [string]$OutputPath =
        "app\src\test\resources\archive-fixtures\infozip-3.0-deflate-unicode.zip"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$expectedSourceTreeSha256 =
    "78d68057069fa76f05af685b339b299344a36da8ff42224f4ef082c73cb7f345"
$expectedFixtureSha256 =
    "cce11cb11fda466a73f547aa01b412253534210b4748d16e31be6ce5d8d72eef"
$expectedFixtureSize = 451L

function ConvertTo-HexString {
    param(
        [Parameter(Mandatory)]
        [byte[]]$Bytes
    )

    return (($Bytes | ForEach-Object { $_.ToString("x2") }) -join "")
}

function Get-SourceTreeSha256 {
    param(
        [Parameter(Mandatory)]
        [string]$Root
    )

    $prefix = $Root.TrimEnd(
        [IO.Path]::DirectorySeparatorChar,
        [IO.Path]::AltDirectorySeparatorChar
    ) + [IO.Path]::DirectorySeparatorChar
    $records = [Collections.Generic.List[string]]::new()
    Get-ChildItem -LiteralPath $Root -File -Recurse | ForEach-Object {
        if (-not $_.FullName.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
            throw "Source file escapes the expected tree: $($_.FullName)"
        }
        $relativePath = $_.FullName.Substring($prefix.Length).Replace("\", "/")
        $fileSha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        $records.Add("$relativePath`0$fileSha256`n")
    }
    $records.Sort([StringComparer]::Ordinal)
    $payload = [Text.Encoding]::UTF8.GetBytes(($records -join ""))
    $sha256 = [Security.Cryptography.SHA256]::Create()
    try {
        return ConvertTo-HexString -Bytes $sha256.ComputeHash($payload)
    } finally {
        $sha256.Dispose()
    }
}

$repositoryRoot = [IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
$fixtureRoot = [IO.Path]::GetFullPath(
    (Join-Path $repositoryRoot "app\src\test\resources\archive-fixtures")
)
$resolvedOutput = if ([IO.Path]::IsPathRooted($OutputPath)) {
    [IO.Path]::GetFullPath($OutputPath)
} else {
    [IO.Path]::GetFullPath((Join-Path $repositoryRoot $OutputPath))
}
$fixturePrefix = $fixtureRoot + [IO.Path]::DirectorySeparatorChar
if (-not $resolvedOutput.StartsWith($fixturePrefix, [StringComparison]::OrdinalIgnoreCase)) {
    throw "Output must stay inside the archive fixture directory: $resolvedOutput"
}
if (Test-Path -LiteralPath $resolvedOutput) {
    throw "Refusing to replace an existing fixture: $resolvedOutput"
}

$sourceRoot = [IO.Path]::GetFullPath($SourceDirectory)
$toolchainRoot = [IO.Path]::GetFullPath($ToolchainDirectory)
if (-not (Test-Path -LiteralPath $sourceRoot -PathType Container)) {
    throw "Info-ZIP source directory is missing: $sourceRoot"
}
if (-not (Test-Path -LiteralPath $toolchainRoot -PathType Container)) {
    throw "MinGW toolchain directory is missing: $toolchainRoot"
}

$requiredSourceFiles = @(
    "LICENSE",
    "revision.h",
    "zip.c",
    "win32\makefile.gcc"
)
foreach ($relativePath in $requiredSourceFiles) {
    if (-not (Test-Path -LiteralPath (Join-Path $sourceRoot $relativePath) -PathType Leaf)) {
        throw "Info-ZIP source file is missing: $relativePath"
    }
}

$gcc = Join-Path $toolchainRoot "gcc.exe"
$make = Join-Path $toolchainRoot "mingw32-make.exe"
$windres = Join-Path $toolchainRoot "windres.exe"
foreach ($tool in @($gcc, $make, $windres)) {
    if (-not (Test-Path -LiteralPath $tool -PathType Leaf)) {
        throw "Required MinGW tool is missing: $tool"
    }
}

$sourceTreeSha256 = Get-SourceTreeSha256 -Root $sourceRoot
if (-not $sourceTreeSha256.Equals($expectedSourceTreeSha256, [StringComparison]::Ordinal)) {
    throw (
        "Info-ZIP source tree does not match the frozen official Zip 3.0 tree. " +
            "Expected $expectedSourceTreeSha256, found $sourceTreeSha256"
    )
}

$systemLocale = Get-WinSystemLocale
if ($systemLocale.TextInfo.OEMCodePage -ne 936) {
    throw (
        "This fixture requires the frozen zh-CN OEM code page 936 environment; " +
            "found $($systemLocale.Name) / $($systemLocale.TextInfo.OEMCodePage)"
    )
}

$sourceInputRoot = Join-Path $repositoryRoot "compatibility\fixture-sources\7zip-22"
$sourceInputFiles = @(
    "ascii.txt",
    "目录\文件.txt"
)
foreach ($relativePath in $sourceInputFiles) {
    if (-not (Test-Path -LiteralPath (Join-Path $sourceInputRoot $relativePath) -PathType Leaf)) {
        throw "Fixture source is missing: $relativePath"
    }
}

$temporaryParent = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd(
    [IO.Path]::DirectorySeparatorChar,
    [IO.Path]::AltDirectorySeparatorChar
) + [IO.Path]::DirectorySeparatorChar
$temporaryRoot = [IO.Path]::GetFullPath(
    (Join-Path $temporaryParent ("archive-manager-infozip-" + [Guid]::NewGuid().ToString("N")))
)
if (-not $temporaryRoot.StartsWith($temporaryParent, [StringComparison]::OrdinalIgnoreCase)) {
    throw "Temporary directory escapes the system temporary root: $temporaryRoot"
}

$originalPath = $env:Path
$generationStarted = $false
try {
    New-Item -ItemType Directory -Path $temporaryRoot -ErrorAction Stop | Out-Null
    $buildRoot = Join-Path $temporaryRoot "zip30"
    Copy-Item -LiteralPath $sourceRoot -Destination $buildRoot -Recurse -ErrorAction Stop

    $env:Path = $toolchainRoot + [IO.Path]::PathSeparator + $originalPath
    Push-Location $buildRoot
    try {
        & $make `
            -f win32/makefile.gcc `
            "ZIPS=zip.exe" `
            "OBJA=" `
            "CRCA_O=" `
            "CRCAUO=" `
            "LOCAL_ZIP=-DNO_ASM -include windows.h" `
            zip.exe
        if ($LASTEXITCODE -ne 0) {
            throw "Info-ZIP build failed with exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }

    $zip = Join-Path $buildRoot "zip.exe"
    $versionOutput = (& $zip -v 2>&1) -join "`n"
    if ($LASTEXITCODE -ne 0 -or $versionOutput -notmatch "This is Zip 3\.0") {
        throw "Built executable does not identify itself as Info-ZIP Zip 3.0"
    }

    $stageRoot = Join-Path $temporaryRoot "fixture-stage"
    New-Item -ItemType Directory -Path (Join-Path $stageRoot "目录") -Force | Out-Null
    foreach ($relativePath in $sourceInputFiles) {
        Copy-Item `
            -LiteralPath (Join-Path $sourceInputRoot $relativePath) `
            -Destination (Join-Path $stageRoot $relativePath) `
            -ErrorAction Stop
    }
    $normalizedTime = [DateTime]::ParseExact(
        "2024-01-02 03:04:06",
        "yyyy-MM-dd HH:mm:ss",
        [Globalization.CultureInfo]::InvariantCulture
    )
    Get-ChildItem -LiteralPath $stageRoot -File -Recurse | ForEach-Object {
        $_.CreationTime = $normalizedTime
        $_.LastAccessTime = $normalizedTime
        $_.LastWriteTime = $normalizedTime
    }

    Push-Location $stageRoot
    try {
        if (Test-Path -LiteralPath $resolvedOutput) {
            throw "Fixture output appeared while the isolated build was running: $resolvedOutput"
        }
        $generationStarted = $true
        & $zip -X $resolvedOutput ".\ascii.txt" ".\目录\文件.txt"
        if ($LASTEXITCODE -ne 0) {
            throw "Info-ZIP fixture generation failed with exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }

    $fixture = Get-Item -LiteralPath $resolvedOutput
    $fixtureSha256 = (Get-FileHash -LiteralPath $resolvedOutput -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($fixture.Length -ne $expectedFixtureSize -or $fixtureSha256 -ne $expectedFixtureSha256) {
        throw (
            "Generated fixture does not match the frozen bytes. " +
                "Expected $expectedFixtureSize bytes / $expectedFixtureSha256, " +
                "found $($fixture.Length) bytes / $fixtureSha256"
        )
    }

    $gccVersion = (& $gcc -dumpfullversion 2>&1) -join ""
    $gccTarget = (& $gcc -dumpmachine 2>&1) -join ""
    Write-Host "Created $resolvedOutput"
    Write-Host "Info-ZIP Zip 3.0 source tree SHA-256 $sourceTreeSha256"
    Write-Host "MinGW GCC $gccVersion target $gccTarget"
    Write-Host "System locale $($systemLocale.Name), OEM code page 936"
    Write-Host "SHA-256 $fixtureSha256"
} catch {
    if ($generationStarted -and (Test-Path -LiteralPath $resolvedOutput -PathType Leaf)) {
        Remove-Item -LiteralPath $resolvedOutput -Force
    }
    throw
} finally {
    $env:Path = $originalPath
    $resolvedTemporaryRoot = [IO.Path]::GetFullPath($temporaryRoot)
    if (
        (Test-Path -LiteralPath $resolvedTemporaryRoot -PathType Container) -and
        $resolvedTemporaryRoot.StartsWith($temporaryParent, [StringComparison]::OrdinalIgnoreCase) -and
        ([IO.Path]::GetFileName($resolvedTemporaryRoot) -like "archive-manager-infozip-*")
    ) {
        Remove-Item -LiteralPath $resolvedTemporaryRoot -Recurse -Force
    }
}
