@echo off
echo Installing JDK 8 using winget (Windows built-in)...
echo.
winget install EclipseAdoptium.Temurin.8.JDK
echo.
if %ERRORLEVEL% EQU 0 (
    echo [OK] JDK 8 installed!
    echo.
    echo Now set JAVA_HOME before building:
    echo.
    echo In PowerShell:
    echo   $env:JAVA_HOME = "$env:ProgramFiles\Eclipse Adoptium\jdk-8.0.442.6-hotspot"
    echo   .\build.ps1
) else (
    echo.
    echo Manual download: https://mirrors.tuna.tsinghua.edu.cn/Adoptium/8/jdk/x64/windows/
    echo Look for the latest OpenJDK8U-jdk_x64_windows_hotspot_8u*.msi
)
pause
