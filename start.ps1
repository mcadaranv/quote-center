#Requires -Version 5.1
<#
.SYNOPSIS
    Starts the Quoting Spring Boot application.
.DESCRIPTION
    Verifies Java 21+ is available, checks the Metals Pricing API is running,
    locates the quoting JAR in the same directory as this script, and launches it.
    Once the app is ready the default browser is opened automatically.
.PARAMETER PassThruArgs
    Any additional arguments are forwarded to the JAR (e.g. --server.port=9090).
#>
param(
    [Parameter(ValueFromRemainingArguments)]
    [string[]] $PassThruArgs
)
$ErrorActionPreference = 'Stop'
function Write-Step { param([string]$msg) Write-Host "  --> $msg" -ForegroundColor Cyan }
function Write-Ok   { param([string]$msg) Write-Host "  [OK] $msg" -ForegroundColor Green }
function Write-Fail { param([string]$msg) Write-Host "  [FAIL] $msg" -ForegroundColor Red }
function Write-Warn { param([string]$msg) Write-Host "  [WARN] $msg" -ForegroundColor Yellow }
function Write-Info { param([string]$msg) Write-Host "  $msg" -ForegroundColor Gray }
# Returns the major Java version for a given java.exe path, or -1 if it cannot be determined.
function Get-JavaMajorVersion {
    param([string]$javaPath)
    try {
        $raw = cmd /c "`"$javaPath`" -version 2>&1"
        $first = ($raw -split "`n")[0].Trim()
        if ($first -match '"(\d+)(?:\.(\d+))?') {
            $major = [int]$Matches[1]
            if ($major -eq 1 -and $Matches[2]) { $major = [int]$Matches[2] }
            return $major
        }
    } catch { }
    return -1
}
# Prompts the user for a JDK root directory and returns the path to java.exe,
# or $null if the user types quit or presses Ctrl+C.
function Prompt-JavaPath {
    Write-Host ""
    Write-Host "  Java 21 or higher is required." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  If you need to install Java 21, get it from:" -ForegroundColor Gray
    Write-Host "    Microsoft Build of OpenJDK : https://aka.ms/download-jdk" -ForegroundColor Gray
    Write-Host "    Eclipse Temurin (Adoptium)  : https://adoptium.net" -ForegroundColor Gray
    Write-Host "    Oracle JDK 21               : https://www.oracle.com/java/technologies/downloads/" -ForegroundColor Gray
    Write-Host ""
    Write-Host "  After installing, enter the JDK root directory below." -ForegroundColor Gray
    Write-Host "  Example: C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot" -ForegroundColor Gray
    Write-Host "  The script appends \bin\java.exe automatically." -ForegroundColor Gray
    Write-Host "  Type quit to stop." -ForegroundColor Gray
    Write-Host ""
    while ($true) {
        $userInput = Read-Host "  JDK root directory"
        $trimmed = $userInput.Trim().Trim('"')
        if ($trimmed -eq '' -or $trimmed -eq 'quit') {
            return $null
        }
        if (-not (Test-Path $trimmed)) {
            Write-Warn "Directory not found: $trimmed"
            Write-Info "Check the path and try again, or type quit to exit."
            continue
        }
        $candidate = Join-Path $trimmed 'bin\java.exe'
        if (-not (Test-Path $candidate)) {
            Write-Warn "java.exe not found at: $candidate"
            Write-Info "Enter the JDK root (not the bin subfolder), or type quit to exit."
            continue
        }
        $ver = Get-JavaMajorVersion $candidate
        if ($ver -lt 21) {
            Write-Warn "That JDK is version $ver - need 21 or higher."
            Write-Info "Provide a different JDK root, or type quit to exit."
            continue
        }
        Write-Ok "Java $ver confirmed at: $candidate"
        return $candidate
    }
}
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "============================================" -ForegroundColor DarkCyan
Write-Host "  Quoting Application Launcher" -ForegroundColor DarkCyan
Write-Host "============================================" -ForegroundColor DarkCyan
Write-Host ""
# --- Step 1: locate Java 21+ ---
Write-Step "Locating Java..."
$javaExe = $null
if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $candidate = "$env:JAVA_HOME\bin\java.exe"
    $ver = Get-JavaMajorVersion $candidate
    if ($ver -ge 21) {
        $javaExe = $candidate
        Write-Ok "Found via JAVA_HOME (Java $ver): $javaExe"
    } else {
        Write-Warn "JAVA_HOME points to Java $ver - need Java 21+."
    }
}
if (-not $javaExe) {
    $found = Get-Command java -ErrorAction SilentlyContinue
    if ($found) {
        $candidate = $found.Source
        $ver = Get-JavaMajorVersion $candidate
        if ($ver -ge 21) {
            $javaExe = $candidate
            Write-Ok "Found on PATH (Java $ver): $javaExe"
        } else {
            Write-Warn "Java on PATH is version $ver - need Java 21+."
        }
    }
}
if (-not $javaExe) {
    $javaExe = Prompt-JavaPath
    if (-not $javaExe) {
        Write-Host ""
        Write-Info "Exiting. Install Java 21+ and re-run the script."
        Write-Host ""
        exit 1
    }
}
# --- Step 2: verify Metals Pricing API is running ---
# Equivalent curl command: curl "http://localhost:8888/price?metal=nickel"
Write-Step "Checking Metals Pricing API at http://localhost:8888 ..."
$metalsPricingUp = $false
try {
    $resp = Invoke-WebRequest -Uri 'http://localhost:8888/price?metal=nickel' `
                              -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
    if ($resp.StatusCode -lt 500) {
        $metalsPricingUp = $true
    }
} catch { }
if (-not $metalsPricingUp) {
    Write-Fail "Metals Pricing API is not running on port 8888."
    Write-Host ""
    Write-Host "  Start it in a separate terminal with:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "    uv run --with 'fastapi[standard]' -- fastapi dev metals_pricing_api.py --host localhost --port 8888" -ForegroundColor White
    Write-Host ""
    Write-Host "  Then re-run this script." -ForegroundColor Yellow
    Write-Host ""
    exit 1
}
Write-Ok "Metals Pricing API is up."
# --- Step 3: locate the JAR ---
Write-Step "Locating quoting JAR..."
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$jars = Get-ChildItem -Path $scriptDir -Filter "quoting-*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike '*-plain.jar' } |
        Sort-Object LastWriteTime -Descending
if ($jars.Count -eq 0) {
    Write-Fail "No quoting JAR found in: $scriptDir"
    Write-Info "Run ./gradlew bootJar to build the JAR first."
    exit 1
}
$jar = $jars[0].FullName
if ($jars.Count -gt 1) {
    Write-Warn "Multiple JARs found - using newest:"
    $jars | ForEach-Object { Write-Info "  $($_.Name)" }
}
Write-Ok "Using: $(Split-Path -Leaf $jar)"
# --- Step 4: spin up browser-opener job ---
$appUrl    = 'http://localhost:8080/battery-quote-center/'
$healthUrl = 'http://localhost:8080/battery-quote-center/api/info'
Write-Info "Browser will open at $appUrl once the application is ready."
Write-Host ""
$browserJob = Start-Job -ScriptBlock {
    param($healthUrl, $appUrl)
    $maxWaitSeconds = 120
    $pollInterval   = 3
    $elapsed        = 0
    while ($elapsed -lt $maxWaitSeconds) {
        Start-Sleep -Seconds $pollInterval
        $elapsed += $pollInterval
        try {
            $resp = Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
            if ($resp.StatusCode -lt 500) {
                Start-Process $appUrl
                return
            }
        } catch { }
    }
} -ArgumentList $healthUrl, $appUrl
# --- Step 5: launch (blocks; Ctrl+C stops the app) ---
Write-Host "  Starting application  -  press Ctrl+C to stop" -ForegroundColor DarkCyan
Write-Host ""
$processArgs = @('-jar', $jar) + $PassThruArgs
try {
    & $javaExe @processArgs
    $exitCode = $LASTEXITCODE
} catch {
    $msg = $_.Exception.Message
    Write-Fail "Failed to launch: $msg"
    $exitCode = 1
} finally {
    Stop-Job  -Job $browserJob -ErrorAction SilentlyContinue
    Remove-Job -Job $browserJob -ErrorAction SilentlyContinue
}
exit $exitCode
