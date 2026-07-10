# 技术问题与解决记录

> 本文档记录了 cgmenchant 开发过程中遇到的所有技术问题、尝试过的解决方案以及最终结果。
> 最后更新: 2026-07-07

---

## 1. 弹药回收在退弹时错误触发

**附魔**: 弹药回收 (Reclaimed)
**问题**: 按退弹键（R）时，弹药回收的返还逻辑会错误触发，导致退弹后子弹不减反增。



### 尝试的解决方案

| 版本 | 方案 | 结果 |
|------|------|------|
| .006 | **弹药减少量阈值** (`ammoDelta <= 8`) | ❌ CGM 退弹是一次减 1 发，不是一次性清空，阈值拦不住 |
| .007 | **`bulletFiredThisTick` 静态标记** — BulletHandler 检测到 CGM 子弹生成时设标记 | ❌ `findShooter()` 反射调用在某些场景下返回 null，标记永远不会设 |
| .008 | **`player.isHandActive()`** — 开枪时玩家按着右键 | ❌ CGM 没有对所有武器设置 `isHandActive()`，部分武器无状态 |
| .008 | **CGM 冷却反射** `CommonEvents.getCooldownTracker(UUID)` | ❌ CooldownTracker API 不确定，反射调用找不到方法 |
| .009 | **世界 tick 追踪** `Map<UUID, Long> lastFireWorldTick` — BulletHandler 记录 `玩家→当前世界 tick`，GunStateHandler 对比同 tick 是否开了枪 | ⏳ 待测试 |

### 当前方案原理

```
EntityJoinWorldEvent (子弹生成)
  └─ BulletHandler: lastFireWorldTick.put(playerUUID, world.getTotalWorldTime())
  
PlayerTickEvent.END
  └─ GunStateHandler: 对比 lastFireWorldTick 是否 == 当前 world tick
       ├─ 匹配 → 弹药减少来自开火 → 执行回收
       └─ 不匹配 → 弹药减少来自退弹 → 跳过
```

**关键**: `world.getTotalWorldTime()` 在同一服务器 tick 内返回值一致，无时序问题。

所有者注：其实还有通过击杀计算的解决方案，如果实在是修不好，那就做一个通过击杀返回弹药的新附魔作为平替

---

## 2. 附魔台不识别枪械

**问题**: CoreMod 成功注入了 `getItemEnchantability()` 返回 30，但附魔台仍然无法为 ItemGun 生成附魔。

**状态**: ⚠️ 未解决

### 怀疑的根因

`EnchantmentGunBase` 构造器传入 `EnumEnchantmentType.WEAPON`，而 1.12.2 中 `WEAPON.canEnchantItem()` 会检查 `item instanceof ItemSword`。ItemGun 不是剑，某些内部路径可能提前拒绝。

### 可能的修复方向

- 使用自定义 `EnumEnchantmentType`（需 Forge 注册扩展）
- 目前 README 已告知用户**使用铁砧+附魔书**作为临时方案
所有者注：这个蠢货没有告诉你们怎么拿到附魔书（实际上我也没想好，也许通过附魔台用书进行附魔30级应该就会有。）
---

## 3. Collateral 间接伤害粒子

**问题**: 子弹穿透弹道的粒子效果不可见。

**根因**: 1.12.2 服务端 `World.spawnParticle()` 是空操作，`WorldServer` 没有覆写此方法。

**修复**: 改为 `SPacketParticles` 手动发包给同维度 128m 内的玩家。

**迭代过程**:

| 版本 | 粒子 | 频率 | 每波数量 | 偏移 | 效果 |
|------|------|------|---------|------|------|
| 初版 | FLAME | 5格 | 3 | 0.05 | 不可见 |
| 二版 | FLAME | 5格 | 3 | 0.05 | 改用 Packet 后可见，但粒子大 |
| 三版 | FIREWORKS_SPARK | 2格 | 1 | 0.001 | 白色烟花，贴紧弹道 |

所有者注：作者因为很多原因没法测试模组在多人游戏下的性能，所以，额。

---

## 4. 弧光引导 vs 纵火者冲突逻辑写反

**文件**: `EnchantmentArcLightStarter.java`

**问题**:
```java
// 原代码（逻辑反了）
return Loader.instance().getIndexedModList()
    .get("cgmenchant").getMetadata().version != null  // 永远为 true
    && Boolean.parseBoolean(System.getProperty("allowArcLightWithFire", "false"));
```

**修复**: 改为直接返回系统属性值，默认 `false`（冲突）：
```java
return Boolean.parseBoolean(System.getProperty("cgmenchant.allowArcLightWithFire", "false"));
```

---

## 5. 快速扳机（已修复）

**附魔**: Trigger Finger (快速扳机)

**状态**: ✅ 已修复（v0.0.7.012）

**修复方案**: 和超容量相同，每 tick 在 PlayerTickEvent.START 反射修改 `Gun.general.rate` 字段，降低射速冷却。`BASE_RATE` 缓存防共享 Gun 污染。最低 1 tick，不会崩。

**效果**: 每级减 4 tick 射速。

---

## 6. 超容量共享 Gun 对象污染

**附魔**: Over Capacity (超容量)

**问题**: CGM 的 `Gun.general.maxAmmo` 是共享可变字段——同一种枪的所有实例共享同一个 `Gun` 对象。超容量附魔写入 boosted 值后，会导致没有超容量附魔的同型号枪也读到被污染的值，弹药显示错误。

**修复方案**（三层防御）:

```
1. BASE_MAX_AMMO 缓存 (Item → Integer)
   └─ 首次遇到某枪型时快照原始值，之后所有"原始值"查询走缓存

2. oc_base NBT 隔离
   └─ 每把枪独立记录原始弹容量，不受共享 Gun 字段影响

3. Phase.START 每 tick 刷新
   └─ 在每个 tick 开始时将枪的 maxAmmo 设为正确值（原始值 or BOOSTED）
```

所有者注:额 至少可以正常用，大概？



---

## 7. 已知未解决的问题

| 问题 | 影响 | 优先级 |
|------|------|--------|
| 附魔台不识别枪械 | 玩家无法在附魔台直接附魔 | 低 |

---

## 8. 版本演进（开发者版）

| 版本 | 日期 | 主要变更 |
|------|------|---------|
| 0.0.7 | - | 初始 CoreMod 架构 |
| 0.0.7.001 | 07-07 | Collateral 粒子 FIREWORKS_SPARK；快速扳机移除；itemGunClass 懒加载；弧光冲突修复 |
| 0.0.7.002 | 07-07 | 粒子偏移缩紧；弹药回收阈值判断 |
| 0.0.7.003 | 07-07 | 凶弹 (FELLBULLET) 初版 |
| 0.0.7.004 | 07-07 | 同心环 + 烟雾子弹 + 射速优化 |
| 0.0.7.005 | 07-07 | 逐波发射 + 烟花音效 |
| 0.0.7.006 | 07-07 | 准星同心环 + 弹道追踪；FELLBULLET Piercer 贯霰形 |
| 0.0.7.007 | 07-07 | 回收退弹 flag 标记；概率 87.5% |
| 0.0.7.008 | 07-07 | isHandActive() + 连续减少检测 + 冷却反射（均失败）|
| 0.0.7.009 | 07-07 | 世界 tick 追踪方案（失败），退回最简逻辑 |
| 0.0.7.010 | 07-08 | 勤俭节约/命中返还；指令系统；台词效果 |
| 0.0.7.011 | 07-09 | 粒子工具函数；魔法阵动画；伤害系统重做 |
| 0.0.7.012 | 07-09 | 快速扳机修复；冲突系统重构；凶弹限制；宝藏附魔 |
| 0.0.8 | 07-09 | 公开测试版 |
