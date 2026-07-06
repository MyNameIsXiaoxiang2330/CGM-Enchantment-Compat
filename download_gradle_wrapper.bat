@echo off
echo Downloading Gradle Wrapper...

if not exist gradle\wrapper mkdir gradle\wrapper

powershell -Command "& {
    $url = 'https://github.com/gradle/gradle/raw/v4.10.0/gradle/wrapper/gradle-wrapper.jar';
    $out = 'gradle\wrapper\gradle-wrapper.jar';
    try {
        Invoke-WebRequest -Uri $url -OutFile $out -UseBasicParsing -ErrorAction Stop;
        Write-Host '[OK] Gradle Wrapper downloaded'
    } catch {
        Write-Host '[FAILED] Download failed:' $_.Exception.Message;
        Write-Host 'Try manually:';
        Write-Host '  Download: https://github.com/gradle/gradle/raw/v4.10.0/gradle/wrapper/gradle-wrapper.jar';
        Write-Host '  Save to: gradle\wrapper\gradle-wrapper.jar';
        exit 1
    }
}"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCCESS] Ready to build! Run: gradlew build
)
pause
