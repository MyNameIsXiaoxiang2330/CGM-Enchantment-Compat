# CoreMod 架构原理

> 本文档面向本模组的开发者和代码审查者，解释 CoreMod 为什么必须存在、做了什么事、以及如何与其他模块协作。

## 1. 为什么非用 CoreMod 不可？

CGM v0.15.3 是两个缺口把我们逼到字节码层面的：

| 缺口 | CGM 现状 | 为什么事件/反射搞不定 | 附魔影响范围 |
|------|---------|---------------------|-------------|
| `ItemGun` 不覆写 `getItemEnchantability()` | 继承 `Item`，返回 0 | 反射无法在**类加载时**向类中追加方法；Forge 没有"给 Item 加方法"的事件 | **全部 11 个附魔** |
| `ReloadTracker.isWeaponFull()` 读静态字段 | 直接从 `Gun.general.maxAmmo` 读 | 有办法反射改 `Gun.general.maxAmmo`，但 `isWeaponFull()` 里的 `this.gun` 引用可能指向**不同的 Gun 实例** | **超容量 (Over Capacity)** |

这两个缺口都在 CGM 的**类加载时**就已经定型，Java 反射在运行时无法追加方法或修改已有方法的内部字节码 → 只能在类加载之前动手 → CoreMod。

## 2. 两处手术的技术原理

### 2.1 手术 A：ItemGun 注入附魔能力

```
CGM 原始类:                    本模组注入后:
┌─────────────────────────┐    ┌─────────────────────────┐
│ class ItemGun           │    │ class ItemGun           │
│   extends Item          │    │   extends Item          │
│                         │    │                         │
│   fire(...)             │    │   fire(...)             │
│   reload(...)           │    │   reload(...)           │
│   (没有 enchantability) │    │                         │
│                         │    │ + getItemEnchantability()│
│                         │    │     return 30;          │
└─────────────────────────┘    └─────────────────────────┘

附魔台逻辑:
  if (item.getItemEnchantability() > 0) → 可以附魔
  if (item.getItemEnchantability() == 0) → 不可附魔 ← CGM 原版落在这里
```

**返回 30（铁质工具级）** 的理由：枪是主力武器，不应该像金质（22）那么容易出好附魔，也不该像钻石（10）那么难。30 折中——可用，但不溢出。

### 2.2 手术 B：换弹逻辑的弹容量路径替换

这是两个模块精密配合的完整链路：

```
                     ┌─ CGM 原版数据流（被 CoreMod 切断）──┐
                     │                                      │
   Gun JSON 配置 ──→ Gun.serverGun.general.maxAmmo (= 30)   │
                                               ↑            │
                                     isWeaponFull() 读取    │ ← 永远读静态值
                                                             │
                     └──────────────────────────────────────┘

                     ┌─ 本模组的替换数据流 ─────────────────────┐
                     │                                          │
   GunStateHandler (PlayerTickEvent START)                      │
     │                                                          │
     ├─ 反射: 读 Gun.general.maxAmmo                            │
     ├─ 计算: boosted = base * (1 + 0.5 * ocLevel)              │
     ├─ 反射: 写 Gun.general.maxAmmo = boosted                  │
     └─ NBT:  写 tag["MaxAmmo"] = boosted                       │
                              │                                  │
                              ▼                                  │
                     isWeaponFull() [CoreMod 替换后]             │
                       读取 tag["MaxAmmo"]  ← NBT 动态值         │
                              │                                  │
                              ▼                                  │
                     返回: ammo >= boosted ? 停止 : 继续装填     │
                     └──────────────────────────────────────────┘
```

**关键点**：CoreMod 的替换不是"改 Gun 对象的值"，而是"改 isWeaponFull 读值的**来源**"——从字段链改为 NBT。这让 GunStateHandler 可以通过 NBT 标签系统完全控制弹容量的表现值。

### 2.3 为什么既要反射改 Gun 又要写 NBT？

两条路各有用途，缺一不可：

| 数据路径 | 谁在用它 | 为什么需要 |
|---------|---------|-----------|
| `Gun.general.maxAmmo`（反射修改） | CGM 自己的 fire()、HUD 显示 | CGM 内部代码不读 NBT "MaxAmmo"，它们只读 Gun 对象的字段 |
| `tag["MaxAmmo"]`（NBT） | isWeaponFull()（CoreMod 替换后） | 这是我们能精确控制的路径，不受 this.gun 引用实例差异的影响 |

两条路同步写入同一值 → 表面上看是冗余的，实际上各自服务于 CGM 的不同代码路径。

## 3. 三个模块的协作关系

```
启动时（类加载阶段）：
  FML
   ├─ 加载 GunTransformer（IFMLLoadingPlugin）
   │    └─ 注册 GunClassTransformer
   │         ├─ 拦截 ItemGun → 注入 getItemEnchantability()
   │         └─ 拦截 ReloadTracker → 替换 isWeaponFull() 的弹容量读取
   │
   └─ 加载 CGMEnchantmentMod（@Mod）
         └─ preInit: 注册 8 个事件处理器

运行时（游戏 tick 循环）：
  PlayerTickEvent.START
   └─ GunStateHandler.onPlayerTick()
        ├─ 初始化 tag["MaxAmmo"]（从 Gun 配置读取初值）
        ├─ 超容量: 反射改 Gun + 写 NBT
        └─ → isWeaponFull() 下次被调用时将读取 NBT 中的新值

  PlayerTickEvent.END
   └─ GunStateHandler.onPlayerTick()
        ├─ 弹药回收: 检测弹药消耗 → 概率补弹
        └─ 熟练手: 检测换弹状态 → 加速装填
```

## 4. 风险评估

### 什么时候会炸？

| 场景 | 概率 | 后果 |
|------|------|------|
| 用户安装了两个都修改 CGM 的模组 | 低但存在 | 类加载时字节码冲突，游戏崩溃 |
| CGM 更新后 ItemGun 移动到其他包 | 中等 | GunClassTransformer 匹配不到目标类，附魔失效但不崩溃 |
| CGM 更新后 isWeaponFull() 的内部字段链改变 | 中等 | bytecode 匹配失败，不影响其他功能但不改弹容量 |
| MCP 映射版本变化导致 SRG 名改变 | 低 | func_74762_e 不再对应 getInteger，报 NoSuchMethodError |

### 为什么在上述风险下仍然选择 CoreMod 而非 Mixin？

1. **1.12.2 Forge 生态现实**：Mixin 在此版本需要额外安装 MixinBootstrap 模组。每多一个前置，用户安装失败的概率就翻倍。
2. **CGM 本身也不用 Mixin**：如果 CGM 自己用了 Mixin，我们跟随用 Mixin 是自然的；但 CGM 不用，再引入 Mixin 显得过度设计。
3. **修改点极少**：只有两处，且都是"追加"或"替换读取路径"，不是"重写核心逻辑"。字节码注入的杀伤半径很小。

### 未来方向

如果 CGM 后续版本（比如 1.16+ 的 CGM 重写版）引入了 Mixin 或有官方附魔 API，本 CoreMod 应该在第一时间退役，迁移到官方扩展点。

## 5. 许可证与依赖说明

### 本模组的许可证

本模组（CGM Enchantment Addon）以 **GNU General Public License v3.0 (GPL-3.0)** 发布。

- 所有 Java 源文件顶部均包含 GPL-3.0 版权声明头
- 完整的许可证文本见项目根目录 `LICENSE` 文件
- 您可以自由使用、修改和重新分发本模组，但衍生作品也必须以 GPL-3.0 兼容的许可证发布

### 运行时依赖的许可兼容性

| 依赖 | 加载方式 | 是否包含在本模组中 | 许可说明 |
|------|---------|-------------------|---------|
| **CGM** (MrCrayfish's Gun Mod) | `required-after` — 用户自行安装 | 否 | MrCrayfish 版权所有。本模组仅声明运行时依赖，不包含或分发 CGM 的任何代码 |
| **Obfuscate** | CGM 的附属库，由 CGM 自动加载 | 否 | MrCrayfish 的闭源库。本模组不直接调用 Obfuscate API，依赖关系通过 CGM 间接传递 |

**重要说明：**

1. 本模组不包含、不修改、不重新分发 CGM 或 Obfuscate 的任何代码。用户必须从 CGM 官方渠道（如 CurseForge）单独下载安装这两个模组。
2. GPL-3.0 的"传染性"仅适用于将 GPL 代码与其他代码**合并为一个程序**后分发的情况。本模组通过 Forge 的模组加载机制在运行时与 CGM 交互，不构成 GPL 意义上的"组合作品"。
3. CoreMod 字节码注入发生在类加载阶段，是对 CGM 类的**运行时适配**而非**代码合并**。注入逻辑完全由本模组的 GPL-3.0 代码实现。
4. 如果您计划在 CurseForge / Modrinth 等平台发布本模组，请确保：
   - 不将 CGM 或 Obfuscate 的 JAR 文件包含在您的发布包中
   - 在模组页面明确列出 CGM 为必需前置
   - 在模组页面说明 Obfuscate 是 CGM 的前置（由 CGM 的安装说明覆盖）

## 6. 文件索引

| 文件 | 角色 |
|------|------|
| `core/GunTransformer.java` | FMLCorePlugin 入口，向 FML 注册变压器 |
| `core/GunClassTransformer.java` | ASM 字节码转换器，执行两处手术 |
| `handler/GunStateHandler.java` | 运行时事件处理器，反射 + NBT 维护弹容量状态 |
| `handler/EnchantHelper.java` | 附魔等级查询工具类 |
| `enchant/ModEnchantments.java` | 11 个附魔的注册中心 |
| `CGMEnchantmentMod.java` | @Mod 主类，注册所有事件处理器 |

---

*最后更新: 2026-07-07 · 适用于 CGM v0.15.3 + Forge 1.12.2-14.23.5.2859*
