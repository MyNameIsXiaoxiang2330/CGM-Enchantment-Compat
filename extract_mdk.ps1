# Extract Gradle Wrapper from Forge MDK zip
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead("$PSScriptRoot\forge-1.12.2-14.23.5.2859-mdk.zip")

# Extract gradle wrapper files
$targets = @(
    "gradle/wrapper/gradle-wrapper.jar",
    "gradle/wrapper/gradle-wrapper.properties",
    "gradlew.bat",
    "build.gradle"
)

foreach ($target in $targets) {
    $entry = $zip.Entries | Where-Object { $_.FullName -eq $target }
    if ($entry) {
        $outPath = "$PSScriptRoot\$target"
        $outDir = Split-Path $outPath -Parent
        if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Force -Path $outDir | Out-Null }
        [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $outPath, $true)
        Write-Host "[OK] Extracted: $target"
    } else {
        Write-Host "[SKIP] Not found: $target"
    }
}
$zip.Dispose()
Write-Host ""
Write-Host "Done! Gradle wrapper is now ready."
Read-Host "Press Enter to exit"
