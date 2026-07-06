Write-Host "=== Java Detection ===" -ForegroundColor Cyan
Write-Host ""

$java = Get-Command java -ErrorAction SilentlyContinue
if ($java) {
    Write-Host "Java location: $($java.Source)" -ForegroundColor Green
    Write-Host ""
    Write-Host "Java version:" -ForegroundColor Yellow
    java -version 2>&1
    Write-Host ""
    Write-Host "JAVA_HOME env:" -ForegroundColor Yellow
    Write-Host "$env:JAVA_HOME"
} else {
    Write-Host "[ERROR] Java not found in PATH" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== Looking for Java installations ===" -ForegroundColor Cyan
$paths = @(
    "C:\Program Files\Java\*",
    "C:\Program Files (x86)\Java\*",
    "C:\Program Files\Eclipse*",
    "C:\Program Files\Adoptium*",
    "C:\Program Files\Eclipse Adoptium*",
    "$env:LOCALAPPDATA\Programs\*",
    "C:\Users\*\AppData\Local\Programs\*"
)

foreach ($p in $paths) {
    $matches = Get-ChildItem -Path $p -ErrorAction SilentlyContinue
    if ($matches) {
        Write-Host "Found in $p:" -ForegroundColor Green
        $matches | ForEach-Object { Write-Host "  $($_.FullName)" }
    }
}

Read-Host "Press Enter to exit"
