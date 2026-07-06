$modsDir = "E:\MinecraftEAT\.minecraft\versions\1.12.2-Forge_14.23.5.2864CGM\mods"

Write-Host "=== Files in mods folder ===" -ForegroundColor Cyan
Get-ChildItem $modsDir -Filter "*.jar" | ForEach-Object { Write-Host $_.FullName }
""

# Find CGM jar
$cgmJar = Get-ChildItem $modsDir -Filter "*guns*.jar" | Select-Object -First 1
if (-not $cgmJar) {
    $cgmJar = Get-ChildItem $modsDir -Filter "*Gun*.jar" | Select-Object -First 1
}
if (-not $cgmJar) {
    $cgmJar = Get-ChildItem $modsDir -Filter "*cgm*.jar" | Select-Object -First 1
}

if ($cgmJar) {
    Write-Host "=== Found CGM jar: $($cgmJar.Name) ===" -ForegroundColor Green
    ""
    Write-Host "=== All classes ===" -ForegroundColor Yellow
    & jar tf $cgmJar.FullName | Sort-Object
} else {
    Write-Host "[ERROR] CGM jar not found!" -ForegroundColor Red
}
""
Read-Host "Press Enter to exit"
