# CGM Enchantment Addon

_[English](#english) | [中文](#中文)_

---

<a name="english"></a>

# English

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.12.2-brightgreen.svg)](https://www.minecraft.net)
[![Forge](https://img.shields.io/badge/Forge-14.23.5.2859-orange.svg)](https://files.minecraftforge.net)

Restores high-version gun enchantments for [MrCrayfish's Gun Mod](https://www.curseforge.com/minecraft/mc-mods/mrcrayfishs-gun-mod) (CGM) v0.15.3 on Minecraft 1.12.2, plus original enchantments inspired by *Limbus Company* EGO.

> **Current build:** `0.0.7.009` (Dev) — versions 0.0.7.000~009 are development builds.
>
> **Note:** CGM v0.15.3 (1.12.2) does not expose `getItemEnchantability()` on `ItemGun`, nor does it provide a hook for dynamic max-ammo in the reload system. This mod works around both limitations with a minimal CoreMod. See [`docs/COREMOD.md`](docs/COREMOD.md) for the technical details.

---

## Enchantments

### Standard — CGM official (9)

| Enchantment | Max Level | Effect |
|-------------|-----------|--------|
| **Accelerator** | II | Bullet speed +15%/lvl, damage +10%/lvl |
| **Puncturing**\* | IV | Ignores 25% armor per level |
| **Collateral**\* | III | Bullets pierce through targets (firework trail) |
| **Fire Starter** | I | Ignites targets; explosions spread fire |
| **Lightweight** | I | +20% move speed while aiming, reduced spread |
| **Over Capacity** | III | Magazine +50% capacity per level (per-gun) |
| **Quick Hands** | II | Reload interval -3 ticks per level |
| **Thrifty** (勤俭节约) | III | 33% / 50% / 87.5% chance to refund ammo on hit |
| **Trigger Finger** | III | -4 tick cooldown per level, min 1 tick |

### Original (2)

| Enchantment | Max Level | Effect |
|-------------|-----------|--------|
| **Arc Light** | III | Lightning strike on hit/explosion, bonus magic damage, charges creepers |
| **High Explosive** | V | Explosion AoE scales with weapon type (rocket/grenade/shotgun/bullet) |

### Recreated — *Limbus Company* EGO (2)

| Enchantment | Max Level | Effect |
|-------------|-----------|--------|
| **FELLBULLET** (地霰形) | IV | On kill: crosshair circles on ground converge, then 360° scatter |
| **FELLBULLET Piercer** (贯霰形) | IV | On kill: 3D ring behind target converges, then cone scatter + piercing |

| Enchantment | Max Level | Effect |
|-------------|-----------|--------|
| **FELLBULLET** (地霰形) | IV | On kill: crosshair circles on ground converge, then 360° scatter |
| **FELLBULLET Piercer** (贯霰形) | IV | On kill: 3D ring behind target converges, then cone scatter along trajectory + piercing |

> **Note:** Enchanting Table does not support guns directly. Use an **Anvil + Enchanted Book** to apply enchantments.
>
> \* = differs from official CGM v1.0.1+. See [`docs/COMPARISON.md`](docs/COMPARISON.md) for details.

## Dependencies

| Mod | Version | Required |
|-----|---------|----------|
| [MrCrayfish's Gun Mod](https://www.curseforge.com/minecraft/mc-mods/mrcrayfishs-gun-mod) | **v0.15.3** | ✅ Required |
| [Obfuscate](https://www.curseforge.com/minecraft/mc-mods/obfuscate) | v0.4.2 | ✅ Required (CGM dependency) |
| Forge | **14.23.5.2859** | ✅ Required |

No other dependencies. MixinBootstrap is **NOT** required.

## Installation

1. Install **Forge 14.23.5.2859** for Minecraft 1.12.2
2. Install **CGM v0.15.3** and **Obfuscate v0.4.2** into `mods/`
3. Place `cgmenchant-<version>.jar` into `mods/`
4. Launch the game — enchantments are applied via **Anvil + Enchanted Book**

## Architecture

This mod uses a **minimal CoreMod** (FMLCorePlugin + ASM) to inject two capabilities into CGM that are not exposed via Forge events or reflection:

| Injection | Target Class | Purpose |
|-----------|-------------|---------|
| `getItemEnchantability()` | `ItemGun` | Allows guns to accept enchantments (returns 30, iron-tier) |
| `isWeaponFull()` patch | `ReloadTracker` | Reads max ammo from NBT instead of static config, enabling dynamic capacity |

All 14 enchantment effects are implemented as standard `@SubscribeEvent` handlers — no other bytecode modifications.

For a deep dive, see [`docs/COREMOD.md`](docs/COREMOD.md).

See [`docs/COMPARISON.md`](docs/COMPARISON.md) for details on how our enchantments differ from official CGM, including WIP features and known bugs.

## Building

```bash
gradlew build
```

Requirements:
- **JDK 8** (tested with OpenJDK 8)
- **Gradle 4.10** (wrapper included)
- The project compiles standalone — no CGM jar needed at build time

## Known Issues

- **Enchanting Table** cannot enchant guns — use Anvil + Enchanted Book instead
- **CoreMod** may conflict with other mods that also modify CGM internals

## License

**GNU General Public License v3.0** — see [LICENSE](LICENSE) for full text.

---

<a name="中文"></a>

# 中文

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.12.2-brightgreen.svg)](https://www.minecraft.net)
[![Forge](https://img.shields.io/badge/Forge-14.23.5.2859-orange.svg)](https://files.minecraftforge.net)

为 [MrCrayfish's Gun Mod](https://www.curseforge.com/minecraft/mc-mods/mrcrayfishs-gun-mod) (CGM) v0.15.3 (Minecraft 1.12.2) 还原高版本枪械附魔，并包含受 *边狱公司 (Limbus Company)* EGO 启发的原创附魔。

> **当前版本:** `0.0.8` (公开测试版)

---

## 附魔列表

### 标准附魔 — CGM 官方（9 个）

| 附魔 | 最大等级 | 效果 |
|------|---------|------|
| 加速器 | II | 弹速 +15%/级，伤害 +10%/级 |
| 穿甲弹\* | IV | 忽视目标护甲 25%/级 |
| 间接伤害\* | III | 子弹穿透多目标，附带白色烟花弹道 |
| 纵火者 | I | 点燃目标，爆炸扩散火焰 |
| 轻装上阵 | I | 开镜移速 +20%，散布减小 |
| 超容量 | III | 弹匣容量 +50%/级 |
| 熟练手 | II | 装弹间隔 -3 tick/级 |
| 勤俭节约 | III | 命中目标后 33%/50%/87.5% 概率返还弹药 |
| 快速扳机 | III | 每级减4 tick射速，最低1 tick |

### 原创附魔（2 个）

| 附魔 | 最大等级 | 效果 |
|------|---------|------|
| 弧光引导 | III | 命中/爆炸时召唤闪电 + 魔法伤害，充能苦力怕 |
| 高爆弹 | V | 爆炸范围/伤害随武器类型变化（火箭/榴弹/霰弹/普通）|

### EGO 还原附魔（2 个）— 基于边狱公司

| 附魔 | 最大等级 | 效果 |
|------|---------|------|
| **凶弹-地霰形** (FELLBULLET) | IV | 击杀时地面生成准星同心圈，收缩后 360° 逐波散射 |
| **凶弹-贯霰形** (FELLBULLET Piercer) | IV | 击杀时目标背后生成 3D 红环，收缩后沿弹道锥形散射 + 贯穿 |

> 附魔台无法直接附魔枪械，请使用「**铁砧 + 附魔书**」组合。
>
> \* = 与官方 CGM v1.0.1+ 机制不同。详见 [`docs/COMPARISON.md`](docs/COMPARISON.md)。

## 安装

1. 安装 Forge 14.23.5.2859
2. 将 CGM v0.15.3 和 Obfuscate v0.4.2 放入 `mods/`
3. 将 `cgmenchant-<版本>.jar` 放入 `mods/`
4. 启动游戏，通过铁砧给枪上附魔

## 技术架构

最小 CoreMod（FMLCorePlugin + ASM）注入两处能力；全部 14 个附魔效果由 @SubscribeEvent 事件驱动实现。

详见 [`docs/COREMOD.md`](docs/COREMOD.md)、[`docs/COMPARISON.md`](docs/COMPARISON.md)（附魔差异详解）和 [`docs/TECHNICAL_ISSUES.md`](docs/TECHNICAL_ISSUES.md)。

## 编译

```bash
gradlew build
```

环境：JDK 8 + Gradle 4.10 (wrapper 已包含)。编译无需 CGM jar。

## 已知问题

- **附魔台**无法直接附魔枪械，请使用铁砧 + 附魔书
- CoreMod 可能与其他修改 CGM 的模组冲突

## 开源 & 技术原理

本模组基于 **GNU General Public License v3.0 (GPL-3.0)** 开源。
源码：https://github.com/MyNameIsXiaoxiang2330/CGM-Enchantment-Compat

**技术架构：**
使用 Forge CoreMod（FMLCorePlugin + ASM）在类加载时对 CGM 进行两处最小化字节码注入——让 ItemGun 可以被附魔，以及让弹容量支持动态修改。其余全部附魔效果通过标准 @SubscribeEvent 事件驱动实现，无需 MixinBootstrap 等额外前置。

## 致谢

- 感谢 MrCrayfish 开发了优秀的 CGM 模组
- 附魔名称"凶弹"参考了边狱公司 (Limbus Company) 的 EGO
- 台词文本翻译参考了都市零协会 (City Zero Association) 的译介作品


