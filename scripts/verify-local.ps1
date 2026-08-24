[CmdletBinding()]
param(
    [string]$DeviceSerial
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repositoryRoot = [IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
$gradleWrapper = Join-Path $repositoryRoot "gradlew.bat"

function Invoke-CheckedCommand {
    param(
        [Parameter(Mandatory)]
        [scriptblock]$Command,

        [Parameter(Mandatory)]
        [string]$Description
    )

    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw "$Description failed with exit code $LASTEXITCODE"
    }
}

Push-Location $repositoryRoot
try {
    & (Join-Path $PSScriptRoot "verify-fixture-manifest.ps1")

    Invoke-CheckedCommand `
        -Description "Local Gradle quality gate" `
        -Command {
            & $gradleWrapper `
                :app:testDebugUnitTest `
                :app:lintDebug `
                :app:assembleDebug `
                :app:assembleRelease `
                --stacktrace `
                --no-daemon
        }

    if (-not [string]::IsNullOrWhiteSpace($DeviceSerial)) {
        $connectedSerials = @(
            adb devices |
                Select-String "\tdevice$" |
                ForEach-Object { ($_.Line -split "\s+")[0] }
        )
        if ($DeviceSerial -notin $connectedSerials) {
            throw "Requested Android device is not connected: $DeviceSerial"
        }

        $previousAndroidSerial = $env:ANDROID_SERIAL
        try {
            $env:ANDROID_SERIAL = $DeviceSerial
            Invoke-CheckedCommand `
                -Description "Android instrumentation tests on $DeviceSerial" `
                -Command {
                    & $gradleWrapper `
                        :app:connectedDebugAndroidTest `
                        --stacktrace `
                        --no-daemon
                }
        } finally {
            $env:ANDROID_SERIAL = $previousAndroidSerial
        }
    }
} finally {
    Pop-Location
}
