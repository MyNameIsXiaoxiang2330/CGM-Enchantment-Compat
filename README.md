# MrCrayfish's Gun: Enchantment Compat

MrCrayfish的枪: 附魔兼容 — 为 CGM v0.15.3 (1.12.2) 还原高版本枪械附魔。

## 附魔列表

| 附魔 | 等级 | 效果 |
|------|------|------|
| 加速器 | I~II | 弹速 +15%/级，伤害 +10%/级 |
| 穿甲弹 | I~IV | 忽视目标护甲 25%/级 |
| 间接伤害 | I~III | 子弹穿透多目标，白色弹道粒子 |
| 纵火者 | I | 子弹消失时生成火焰，爆炸扩散火焰 |
| 弧光引导 | I~III | 闪电，充能苦力怕，魔法伤害 |
| 高爆弹 | I~V | 火箭大爆炸，普通子弹小爆 |
| 轻装上阵 | I | 开镜移速 +20%，散布减小 |
| 超容量 | I~III | 弹匣容量 +50%/级，每枪独立 |
| 熟练手 | I~II | 装弹间隔减少 3 tick/级 |
| 弹药回收 | I~III | 1/3、1/2、7/8 概率返还弹药 |
| 快速扳机 | I~III | ❌ CGM v0.15.3 冷却系统不兼容 |

## 依赖

- [MrCrayfish's Gun Mod](https://www.curseforge.com/minecraft/mc-mods/mrcrayfishs-gun-mod) v0.15.3 (1.12.2)
- [Obfuscate](https://www.curseforge.com/minecraft/mc-mods/obfuscate) v0.4.2

## 安装

1. 安装 Forge 14.23.5.2864
2. 放入 `.minecraft/mods/` 目录
3. 不需要 MixinBootstrap

## 编译

```bash
gradlew build
```

需要 JDK 8 + Gradle 4.x。

## 已知问题

- **附魔台**无法直接附魔枪械，请使用铁砧 + 附魔书
- **快速扳机**与 CGM 冷却系统冲突，已禁用
- **熟练手**装弹时直接修改 NBT，可能导致装弹动画异常

## 许可证

GNU General Public License v3.0
