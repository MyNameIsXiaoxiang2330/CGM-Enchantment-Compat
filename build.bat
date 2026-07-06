@echo off
title CGM Enchantment Addon - Build

echo ============================================
echo  CGM Enchantment Addon - Build
echo ============================================
echo.

:: Check Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found!
    echo Install JDK 8 from: https://adoptium.net/temurin/releases/?version=8
    pause
    exit /b 1
)
echo [OK] Java found
echo.

:: Download and prepare Gradle
set GRADLE_DIR=%cd%\.gradle\gradle-4.10
if exist "%GRADLE_DIR%\bin\gradle.bat" goto build

echo [..] This is the first run. Setting up Gradle...
echo.

:: Download Gradle 4.10 using bitsadmin (Windows built-in)
if not exist .gradle mkdir .gradle

echo  Downloading Gradle 4.10 (60MB) from Tencent Cloud...
echo  This may take a while depending on your network.
echo.

bitsadmin /rawreturn /transfer gradle_download /download /priority high https://mirrors.cloud.tencent.com/gradle/gradle-4.10-bin.zip "%cd%\.gradle\gradle-4.10-bin.zip"
if errorlevel 1 (
    echo [ERROR] Download failed.
    echo.
    echo Manual workaround:
    echo  Step 1 - Download: https://mirrors.cloud.tencent.com/gradle/gradle-4.10-bin.zip
    echo  Step 2 - Extract "gradle-4.10" folder to: .gradle\
    echo  Step 3 - Run build.bat again
    pause
    exit /b 1
)
echo [OK] Download complete

echo [..] Extracting...
powershell -Command "Add-Type -AssemblyName System.IO.Compression.FileSystem; [System.IO.Compression.ZipFile]::ExtractToDirectory('.gradle\gradle-4.10-bin.zip', '.gradle'); Remove-Item '.gradle\gradle-4.10-bin.zip'"
echo [OK] Extraction complete
echo.

:build
echo [..] Running Gradle build. This downloads Forge+Minecraft+Mixin.
echo      First build is SLOW (several hundred MB). Please wait.
echo      If stuck, press Ctrl+C and retry later.
echo.

"%GRADLE_DIR%\bin\gradle" build --no-daemon
if errorlevel 1 (
    echo [ERROR] Build failed.
    echo Check the error messages above for details.
    pause
    exit /b 1
)

echo.
echo ============================================
echo  [SUCCESS] Build complete!
echo  Output: build\libs\cgmenchant-1.0.0.jar
echo.
echo  Copy the jar to your mods folder.
echo  CGM mod is required at runtime.
echo ============================================
pause
