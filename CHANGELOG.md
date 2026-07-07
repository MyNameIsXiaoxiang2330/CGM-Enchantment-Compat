# CHANGELOG — CGM Enchantment Addon

## 版本规则

`主版本.次版本.构建.封装号`
- 主版本.次版本.构建: 功能迭代
- 封装号: 每次构建测试的递增编号

---

## 0.0.7.009 (2026-07-07)

### 改动
- **弹药回收**: 移除所有检测逻辑（世界 tick 追踪、isHandActive、阈值等），退回最简方案——弹药减少就触发回收
- ⚠ 已知问题: 退弹时也会触发回收，等待重新设计

### 文件变更
- `handler/GunStateHandler.java` — 移除 `lastFireWorldTick` map、`UUID` import、检测条件

---

## 0.0.7.008 (2026-07-07)

### 改动
- **弹药回收**: 改为 `player.isHandActive()` 检测（❌ 失败，CGM 未对所有武器设置 handActive）
- **弹药回收**: 尝试 CGM 冷却反射 `isGunOnCooldown()`（❌ 失败，CGM CooldownTracker API 未知）
- **概率修正**: Ⅲ级 50% → **87.5%**

### 文件变更
- `handler/GunStateHandler.java` — 新增 `isGunOnCooldown()` 方法
- `handler/GunStateHandler.java` — 概率 0.50f → 0.875f

---

## 0.0.7.007 (2026-07-07)

### 改动
- **弹药回收退弹 Bug**: 新增 `bulletFiredThisTick` 静态标记（❌ 失败，`findShooter()` 反射可能返回 null）
- **弹药回收退弹 Bug**: 改为 CGM 冷却检测（❌ 失败）

### 文件变更
- `handler/GunStateHandler.java` — 新增 `bulletFiredThisTick` 静态标记、`isGunOnCooldown()`
- `handler/BulletHandler.java` — 设置 `GunStateHandler.bulletFiredThisTick = true`

---

## 0.0.7.006 (2026-07-07)

### 改动
- **凶弹-地霰形 (FELLBULLET)** — 准星+同心圆地面蓄力，360° 逐波散射
- **凶弹-贯霰形 (FELLBULLET Piercer)** — 目标背后 3D 红环，锥形散射，贯穿，枪械原始伤害
- **重命名**: `sin_bullet` → `fellbullet`，显示名"凶弹"→"凶弹-地霰形"
- **弹药回收**: 阈值判断 `ammoDelta <= 8`（❌ 失败，退弹是逐 tick 减）

### 新增文件
- `enchant/EnchantmentFellbulletPiercer.java`
- `handler/FellbulletPiercerHandler.java`

### 文件变更
- `enchant/EnchantmentSinBullet.java` — 注册名 `sin_bullet` → `fellbullet`
- `enchant/ModEnchantments.java` — `SIN_BULLET` → `FELLBULLET`，新增 `FELLBULLET_PIERCER`
- `handler/SinBulletHandler.java` — 引用 `SIN_BULLET` → `FELLBULLET`
- `handler/GunStateHandler.java` — 退弹检测阈值
- `CGMEnchantmentMod.java` — 注册 `FellbulletPiercerHandler`
- 语言文件、README 更新

---

## 0.0.7.005 (2026-07-07)

### 改动
- **凶弹蓄力**: 完成时播放 🎇 烟花发射声
- **凶弹散射**: 改为逐波发射，每波间隔 ~0.15s，每波播放 💥 烟花爆炸声
- **同心环修复**: 粒子 8→24 个，错开角度，LAVA 粒子（后又改回 REDSTONE + 准星线）

### 文件变更
- `handler/SinBulletHandler.java` — 新增 `spawnWave()`、逐波逻辑、音效、`SoundEvents`/`SoundCategory` import
- `handler/SinBulletHandler.java` — `SinCharge` 新增 `wavesSpawned`/`waveCooldown`/`chargeComplete` 字段

---

## 0.0.7.004 (2026-07-07)

### 改动
- **凶弹**: 改为 4 个同心环收缩（而不是圈绕中心旋转），解决花瓣视觉效果
- **凶弹**: 蓄力时间 等级×1.0s → **等级×0.65s**
- **凶弹**: 散射改为等级波 × 每波 8~24 发随机
- **凶弹**: 射线每 tick 1.5 格（性能优化）
- **凶弹弹道**: REDSTONE → **SMOKE_NORMAL**（黑色烟雾）

### 文件变更
- `handler/SinBulletHandler.java` — 重写 `spawnRedCircles()`、`spawnScatterRays()`→`spawnWave()`、`advanceRay()`

---

## 0.0.7.003 (2026-07-07)

### 改动
- **凶弹 (Sin Bullet) 初版** — 击杀时地面生成红圈蓄力，蓄力完成后 360° 散射
- 射流为穿透射线，6 秒自毁，防无限循环

### 新增文件
- `enchant/EnchantmentSinBullet.java`
- `handler/SinBulletHandler.java`

### 文件变更
- `enchant/ModEnchantments.java` — 注册 `SIN_BULLET`
- `CGMEnchantmentMod.java` — 注册 `SinBulletHandler`
- 语言文件、README 更新

---

## 0.0.7.002 (2026-07-07)

### 改动
- **Collateral 粒子**: 偏移 0.02→**0.001**，速度 0.02→**0.0**（解决粒子散射问题）
- **弹药回收**: 新增阈值判断 `ammoDelta <= 8`，防止退弹触发返还（❌ 不完全解决）

### 文件变更
- `handler/CollateralHandler.java` — 粒子参数调整
- `handler/GunStateHandler.java` — 退弹阈值

---

## 0.0.7.001 (2026-07-07)

### 改动
- **Collateral 粒子**: FLAME → **FIREWORKS_SPARK**（白色烟花），频率 5 格→2 格，密度 3→1
- **快速扳机**: `canApplyAtEnchantingTable()` → `false`（从附魔台移除）
- **itemGunClass 懒加载**: EnchantmentGunBase 改为懒加载，启动日志 `CGM available: true/false`
- **弧光 vs 纵火者冲突**: 修复 `canApplyTogether` 条件写反
- **构建优化**: `build.gradle` 新增 `copyToTest` 自动删旧 jar

### 文件变更
- `handler/CollateralHandler.java` — 粒子类型/频率/密度
- `enchant/ModEnchantments.java` — 快速扳机 `canApplyAtEnchantingTable=false`
- `enchant/EnchantmentGunBase.java` — 懒加载 `itemGunClass`
- `enchant/EnchantmentArcLightStarter.java` — 修复冲突条件
- `handler/GunStateHandler.java` — 退出阈值
- `build.gradle` — `copyToTest` 删旧 jar

---

## 0.0.7.000 (2026-07-07)

首个封装版本。包含 11 个附魔的 CoreMod 基础架构。

### 核心功能
- CoreMod (ASM) 注入 ItemGun.getItemEnchantability() → 30
- CoreMod (ASM) 替换 ReloadTracker.isWeaponFull() 弹容量路径
- 11 个附魔: 加速器、穿甲弹、间接伤害、纵火者、弧光引导、高爆弹、轻装上阵、超容量、熟练手、弹药回收、快速扳机
- 8 个 @SubscribeEvent 事件处理器
- GPL-3.0 开源，COREMOD.md 架构文档，中英双语 README
