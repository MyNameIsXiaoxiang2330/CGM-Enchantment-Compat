Write-Host "============================================"
Write-Host " CGM Enchantment Addon - Build"
Write-Host "============================================"
Write-Host ""

# Version checker: must be Java 8 update 101+
function Test-Jdk8Version($path) {
    if (-not (Test-Path $path)) { return $false }
    $ver = & $path -version 2>&1
    $verStr = ($ver | Out-String)
    if ($verStr -match '1\.8\.0_(\d+)') {
        $update = [int]$Matches[1]
        return $update -ge 101
    }
    return $false
}

# Find usable Java 8
$javaExe = $null

# 1. Check JAVA_HOME
if ($env:JAVA_HOME) {
    $testPath = Join-Path $env:JAVA_HOME "bin\java.exe"
    if (Test-Jdk8Version $testPath) { $javaExe = $testPath }
}

# 2. Search common JDK 8 paths
if (-not $javaExe) {
    $searchPaths = @(
        "$env:ProgramFiles\Eclipse Adoptium\jdk-8.0*\bin\java.exe",
        "$env:ProgramFiles\Eclipse Adoptium\jdk-1.8*\bin\java.exe",
        "$env:ProgramFiles\Adoptium\jdk-8*\bin\java.exe",
        "$env:ProgramFiles\Java\jdk1.8*\bin\java.exe",
        "$env:ProgramFiles (x86)\Java\jdk1.8*\bin\java.exe",
        "$env:LOCALAPPDATA\Programs\Eclipse Adoptium\jdk-8*\bin\java.exe",
        "C:\Program Files\Eclipse Adoptium\*\bin\java.exe",
        "D:\Program Files\Eclipse Adoptium\*\bin\java.exe",
        "E:\Program Files\Eclipse Adoptium\*\bin\java.exe",
        "E:\mod-workspace\Java8NEW\bin\java.exe"
    )
    foreach ($pattern in $searchPaths) {
        foreach ($match in (Get-ChildItem -Path $pattern -ErrorAction SilentlyContinue)) {
            if (Test-Jdk8Version $match.FullName) {
                $javaExe = $match.FullName; break
            }
        }
        if ($javaExe) { break }
    }
}

# 3. Check Minecraft legacy JRE (last resort)
if (-not $javaExe) {
    $jreLegacy = "$env:APPDATA\.minecraft\runtime\jre-legacy\bin\java.exe"
    if (Test-Jdk8Version $jreLegacy) { $javaExe = $jreLegacy }
}

# 4. Manual input
if (-not $javaExe) {
    Write-Host "[WARN] JDK 8 (8u101+) not found automatically." -ForegroundColor Yellow
    Write-Host ""
    $manualPath = Read-Host "Enter path to Java 8's bin\java.exe (or press Enter to exit)"
    if ($manualPath) {
        $p = $manualPath.Trim('"')
        if (Test-Jdk8Version $p) { $javaExe = $p }
    }
}

if (-not $javaExe) {
    Write-Host "[ERROR] No valid Java 8 found. Install from:" -ForegroundColor Red
    Write-Host "  https://mirrors.tuna.tsinghua.edu.cn/Adoptium/8/jdk/x64/windows/" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "[OK] Using: $javaExe" -ForegroundColor Green
$ver = & $javaExe -version 2>&1
$verLine = ($ver -split "`n" | Select-String "version" | Select-Object -First 1).ToString().Trim()
Write-Host "     $verLine" -ForegroundColor Green
Write-Host ""

# Gradle: local or gradlew
$manualGradle = Join-Path $PSScriptRoot ".gradle\gradle-4.10\bin\gradle.bat"
$gradlew = Join-Path $PSScriptRoot "gradlew.bat"
$gradleCmd = $null

if (Test-Path $manualGradle) {
    $gradleCmd = $manualGradle
    Write-Host "[OK] Using local Gradle 4.10" -ForegroundColor Green
} elseif (Test-Path $gradlew) {
    $gradleCmd = $gradlew
    Write-Host "[OK] Using gradlew (from MDK)" -ForegroundColor Yellow
} else {
    Write-Host "[ERROR] No Gradle found!"
    Read-Host "Press Enter to exit"
    exit 1
}
Write-Host ""

Write-Host "[..] Running Gradle build..." -ForegroundColor Yellow
Write-Host "     First build downloads Forge + Minecraft (about 200MB)"
Write-Host "     This may take a while. Please wait."
Write-Host ""

Set-Location $PSScriptRoot
$env:JAVA_HOME = Split-Path (Split-Path $javaExe -Parent) -Parent
& $gradleCmd clean build --no-daemon --stacktrace

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Build failed (exit code: $LASTEXITCODE)" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host " [SUCCESS] Build complete!" -ForegroundColor Green
Write-Host " Output: build\libs\cgmenchant-1.0.0.jar" -ForegroundColor Green
Write-Host ""
Write-Host " Copy the jar to your mods folder." -ForegroundColor White
Write-Host " CGM mod is required at runtime." -ForegroundColor White
Write-Host "============================================" -ForegroundColor Green
Read-Host "Press Enter to exit"
