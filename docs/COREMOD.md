# CoreMod Architecture

> For developers and reviewers: why the CoreMod exists, what it does, and how the modules work together.

## 1. Why CoreMod?

CGM v0.15.3 left two gaps that forced us down to bytecode:

| Gap | CGM behavior | Why events/reflection can't fix it | Affected enchantments |
|-----|-------------|-----------------------------------|----------------------|
| `ItemGun` doesn't override `getItemEnchantability()` | Inherits `Item`, returns 0 | Reflection cannot **add a method** to a class at load time. Forge has no "add method to Item" event. | **All 11 enchantments** |
| `ReloadTracker.isWeaponFull()` reads static fields | Reads `Gun.general.maxAmmo` directly | We can reflectively write `Gun.general.maxAmmo`, but `isWeaponFull()`'s `this.gun` may reference a **different Gun instance** | **Over Capacity** |

Both gaps are baked in at class-load time. Java reflection cannot retroactively add methods or alter bytecode inside existing methods → the only option is to intercept before class loading → CoreMod.

## 2. The Two Patches

### 2.1 Patch A: `getItemEnchantability()` on ItemGun

```
CGM original:                   After our injection:
┌─────────────────────────┐    ┌─────────────────────────┐
│ class ItemGun           │    │ class ItemGun           │
│   extends Item          │    │   extends Item          │
│                         │    │                         │
│   fire(...)             │    │   fire(...)             │
│   reload(...)           │    │   reload(...)           │
│   (no enchantability)   │    │                         │
│                         │    │ + getItemEnchantability()│
│                         │    │     return 30;          │
└─────────────────────────┘    └─────────────────────────┘

Enchanting Table logic:
  if (item.getItemEnchantability() > 0) → can enchant
  if (item.getItemEnchantability() == 0) → cannot enchant  ← CGM falls here
```

**Why 30?** Iron-tier enchantability. Guns are primary weapons — should not be as generous as gold (22), nor as stingy as diamond (10).

### 2.2 Patch B: Max-ammo read path in `isWeaponFull()`

Two modules working in precise coordination:

```
                     ┌─ CGM's original data flow (severed by CoreMod) ──┐
                     │                                                   │
   Gun JSON config ─→ Gun.serverGun.general.maxAmmo (= 30)              │
                                                ↑                        │
                                   isWeaponFull() reads this             │ ← always static
                                                                          │
                     └───────────────────────────────────────────────────┘

                     ┌─ Our replacement data flow ─────────────────────────┐
                     │                                                     │
   GunStateHandler (PlayerTickEvent START)                                 │
     │                                                                     │
     ├─ Reflection: read Gun.general.maxAmmo                              │
     ├─ Compute: boosted = base * (1 + 0.5 * ocLevel)                     │
     ├─ Reflection: write Gun.general.maxAmmo = boosted                   │
     └─ NBT: write tag["MaxAmmo"] = boosted                               │
                              │                                             │
                              ▼                                             │
                     isWeaponFull() [after CoreMod patch]                  │
                        reads tag["MaxAmmo"]  ← dynamic NBT value          │
                              │                                             │
                              ▼                                             │
                     Return: ammo >= boosted ? stop : keep reloading       │
                     └─────────────────────────────────────────────────────┘
```

**Key insight:** The CoreMod doesn't change the Gun object's value — it changes **where** `isWeaponFull()` reads from (field chain → NBT tag). This lets GunStateHandler control max-ammo entirely through the NBT system.

### 2.3 Why both reflection AND NBT?

Two paths, each serving different CGM consumers:

| Data path | Who reads it | Why it's needed |
|-----------|-------------|-----------------|
| `Gun.general.maxAmmo` (reflection) | CGM's own `fire()`, HUD display | CGM's internal code doesn't read NBT "MaxAmmo" — it reads Gun object fields |
| `tag["MaxAmmo"]` (NBT) | `isWeaponFull()` (after CoreMod patch) | This is the path we control precisely, unaffected by `this.gun` instance differences |

Both paths are synced to the same value — seemingly redundant, but each serves different code paths inside CGM.

## 3. Module Collaboration

```
Startup (class-loading phase):
  FML
   ├─ loads GunTransformer (IFMLLoadingPlugin)
   │    └─ registers GunClassTransformer
   │         ├─ intercepts ItemGun → injects getItemEnchantability()
   │         └─ intercepts ReloadTracker → patches isWeaponFull()
   │
   └─ loads CGMEnchantmentMod (@Mod)
         └─ preInit: registers 8 event handlers

Runtime (game tick loop):
  PlayerTickEvent.START
   └─ GunStateHandler.onPlayerTick()
        ├─ initializes tag["MaxAmmo"] from Gun config
        ├─ Over Capacity: reflect-write Gun + write NBT
        └─ → next isWeaponFull() call reads NBT value

  PlayerTickEvent.END
   └─ GunStateHandler.onPlayerTick()
        ├─ Reclaimed: detects ammo consumption → chance-based refund
        └─ Quick Hands: detects reload state → accelerated loading
```

## 4. Risk Assessment

| Scenario | Likelihood | Impact |
|----------|-----------|--------|
| Another mod also modifies CGM internals | Low but real | Bytecode collision at class load → crash |
| CGM update moves ItemGun to a different package | Medium | Transformer can't find target class → enchantments disabled (no crash) |
| CGM update changes `isWeaponFull()` internals | Medium | Bytecode match fails → Over Capacity stops working (no crash) |
| MCP mapping version changes SRG names | Low | `func_74762_e` no longer maps to `getInteger` → NoSuchMethodError |

### Why CoreMod instead of alternatives?

The decision traces back to a real-world dependency catastrophe. An addon for **Scape and Run: Parasites** (Cotesia Glomerata) required **three** separate pre-installed mods just to function:

- **Scape and Run: Parasites** (locked at patch version V1.9.21)
- **GeckoLib** (animation library)
- **MixinBooter** (Mixin loader for 1.12.2 Forge)

The result: the four mods — SRP, Cotesia Glomerata, GeckoLib, and MixinBooter — **sometimes failed to detect each other, sometimes actively rejected each other**, despite all version requirements being technically satisfied. This was not a bug in any single mod. It was the dependency chain itself that was fragile.

We have concrete proof. Here is the actual crash, reproduced on demand:

```
SRP V1.10.7          Cotesia Glomerata V1.3.4h1
     │                        │
     │  applyBonuses(          │  Mixin injects against OLD signature:
     │    SRPSaveData,         │  applyBonuses(int, int, boolean, CallbackInfo)
     │    World,               │
     │    CallbackInfo)   ✗    │  SRP V1.9.21 → V1.10.7 changed the method.
     │                        │  Cotesia never knew.
     └──── Signature mismatch ─┘
              │
     InvalidInjectionException:
       Expected (SRPSaveData, World, CallbackInfo)
       but found (int, int, boolean, CallbackInfo)
              │
     EntityParasiteBase fails to load → Game crashes at startup.

This is not a hypothetical. It happened.

> **This is not a critique of Mixin.** Mixin is a proven tool and CGM 1.19+ uses it extensively without issue. The problem is specific to the 1.12.2 Forge ecosystem, where loading a bytecode patching framework requires installing a separate loader mod, which in turn must coexist with other mods that may also depend on incompatible versions of the same loader.

> If you ask "why not use the obvious alternative?", the story above is what you'll hear. Every single time. It is the root of every technical decision in this project.

That experience cemented a hard rule:

> **The mod should depend on as little as possible.**

CoreMod is the only approach that satisfies this rule:

1. **Zero extra dependencies**: Our runtime dependency list is exactly two items — CGM and Obfuscate (the latter only because CGM itself requires it). No additional libraries, no patching frameworks. Every added dependency is another point of failure in the user's mods folder.
2. **Only two injection points**: Both are narrow — one is a method addition, the other a read-path replacement. Neither rewrites core logic. Small surface area means small blast radius.
3. **Fully self-contained**: All bytecode manipulation lives in `GunClassTransformer.java`, written in plain ASM against `org.objectweb.asm` (which ships inside Minecraft itself). No external tooling needed at build time or runtime.
4. **No transitive dependency risk**: Since we own the ASM code directly, there is no chain of third-party libraries each demanding their own specific versions. What compiles today will compile tomorrow.

## 5. License & Dependencies

### Our License

This mod is released under **GNU General Public License v3.0 (GPL-3.0)**.

- All 17 Java source files carry GPL-3.0 license headers
- Full license text: `LICENSE` in the project root

### Runtime Dependency Licensing

| Dependency | Loading | Bundled in this mod? | Notes |
|-----------|---------|---------------------|-------|
| **CGM** | `required-after` — installed by user | No | © MrCrayfish. We declare a runtime dependency only; no CGM code is included or redistributed. |
| **Obfuscate** | Loaded automatically by CGM | No | MrCrayfish's proprietary library. This mod makes zero direct Obfuscate API calls. |

**Important:**

1. This mod does **not** include, modify, or redistribute any CGM or Obfuscate code. Users must obtain both mods from official sources (e.g. CurseForge).
2. GPL-3.0 copyleft applies to combined works distributed together. This mod interacts with CGM at runtime via Forge's mod-loading mechanism — this does not constitute a "combined work" under the GPL.
3. CoreMod bytecode injection happens at class-load time as **runtime adaptation**, not code merging. All injection logic is implemented in our own GPL-3.0 code.
4. When publishing on CurseForge / Modrinth:
   - Do not bundle CGM or Obfuscate JARs in your release
   - Clearly list CGM as a required dependency
   - Note that Obfuscate is required by CGM (covered by CGM's own installation instructions)

## 6. File Index

| File | Role |
|------|------|
| `core/GunTransformer.java` | FMLCorePlugin entry point, registers the transformer with FML |
| `core/GunClassTransformer.java` | ASM bytecode transformer, performs both patches |
| `handler/GunStateHandler.java` | Runtime event handler, maintains ammo state via reflection + NBT |
| `handler/EnchantHelper.java` | Utility for enchantment level queries |
| `enchant/ModEnchantments.java` | Registry for all 11 enchantments |
| `CGMEnchantmentMod.java` | @Mod main class, registers all event handlers |

---

# CoreMod 架构原理（中文）

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

### 为什么在上述风险下仍然选择 CoreMod？

这个决定的根源来自一次真实的前置灾难。一个叫 **Cotesia Glomerata**（寄生蜂）的 **逃逸：寄生体 (SRP)** 插件，需要**三个**独立前置模组才能运行：

- **逃逸：寄生体 (SRP)**（锁死补丁版本 V1.9.21）
- **GeckoLib**（动画库）
- **MixinBooter**（1.12.2 Forge 的 Mixin 加载器）

结果：SRP、寄生蜂、GeckoLib、MixinBooter 这四个模组——**有时候互相检测不到，有时候又直接互相排斥**——尽管所有版本要求都满足了。这不是任何一个模组的 bug，是依赖链本身就脆弱。

我们有确凿证据。以下是在要求下当场复现的真实崩溃：

```
SRP V1.10.7          寄生蜂 V1.3.4h1
     │                        │
     │  applyBonuses(          │  Mixin 注入点写死了旧版签名：
     │    SRPSaveData,         │  applyBonuses(int, int, boolean, CallbackInfo)
     │    World,               │
     │    CallbackInfo)   ✗    │  SRP 从 V1.9.21 升级到 V1.10.7
     │                        │  方法签名变了。寄生蜂不知道。
     └─────── 签名不匹配 ──────┘
              │
     InvalidInjectionException:
       Expected (SRPSaveData, World, CallbackInfo)
       but found (int, int, boolean, CallbackInfo)
              │
     EntityParasiteBase 加载失败 → 游戏启动即崩溃。

这不是假设，它发生了。

> **这不是对 Mixin 的批评。** Mixin 是经过验证的工具，CGM 1.19+ 大量使用了它且运行良好。问题出在 1.12.2 Forge 生态：加载字节码补丁框架需要额外安装一个加载器模组，而这个加载器又必须和其他可能依赖同一加载器（但版本不同）的模组共存。

> 如果你问"为什么不用那个显而易见的替代方案？"——上面这段故事就是你会听到的回答。每一次都是。它是这个项目一切技术决策的根源。

那次经历确立了一条铁律：

> **模组应该尽可能不依赖任何东西。**

CoreMod 是唯一满足这条规则的方案：

1. **零额外前置**：本模组的运行时依赖只有两项——CGM 和 Obfuscate（后者仅因 CGM 本身需要）。没有额外的库，没有补丁框架。每多一个前置，就是用户 mods 文件夹里多一个潜在的爆炸点。
2. **修改点极少**：只有两处，且都是"追加"或"替换读取路径"，不是"重写核心逻辑"。接触面越小，出事的概率越小。
3. **完全自包含**：所有字节码操作用纯 ASM（`org.objectweb.asm`，Minecraft 自带）手写实现。不需要任何外部工具。
4. **零传递依赖风险**：因为 ASM 代码是我们自己写的，不存在"第三方库 A 要版本 X、库 B 要版本 Y"的传递依赖冲突。今天能编译的，明天照样能编译。

## 5. 文件索引

| 文件 | 角色 |
|------|------|
| `core/GunTransformer.java` | FMLCorePlugin 入口，向 FML 注册变压器 |
| `core/GunClassTransformer.java` | ASM 字节码转换器，执行两处手术 |
| `handler/GunStateHandler.java` | 运行时事件处理器，反射 + NBT 维护弹容量状态 |
| `handler/EnchantHelper.java` | 附魔等级查询工具类 |
| `enchant/ModEnchantments.java` | 11 个附魔的注册中心 |
| `CGMEnchantmentMod.java` | @Mod 主类，注册所有事件处理器 |

---

*Last updated: 2026-07-07 · CGM v0.15.3 + Forge 1.12.2-14.23.5.2859*
