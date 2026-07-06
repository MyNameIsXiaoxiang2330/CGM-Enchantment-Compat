Write-Host "=== CGM Jar Classes ===" -ForegroundColor Cyan

$cgmJar = "E:\MinecraftEAT\.minecraft\versions\1.12.2-Forge_14.23.5.2864CGM\mods\[MrCrayfish的枪] guns-0.15.3-1.12.2.jar"

Write-Host "1. Item classes:" -ForegroundColor Yellow
& jar tf "$cgmJar" | Select-String "Item"
""

Write-Host "2. Common classes:" -ForegroundColor Yellow
& jar tf "$cgmJar" | Select-String "common"
""

Write-Host "3. Enchantment classes:" -ForegroundColor Yellow
& jar tf "$cgmJar" | Select-String "enchant"
""

Write-Host "4. Entity classes:" -ForegroundColor Yellow
& jar tf "$cgmJar" | Select-String "entity"
""

Write-Host "5. Gun config/registry:" -ForegroundColor Yellow
& jar tf "$cgmJar" | Select-String "Gun"
""

Read-Host "Press Enter to exit"
