/*
 * CGM Enchantment Addon — 为 MrCrayfish's Gun Mod 添加 11 种枪械附魔
 * Copyright (C) 2026 CGM Enchantment Addon Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.example.cgmenchant.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import java.util.Map;

/**
 * <h1>CoreMod 入口 — CGM 枪械附魔系统的基石</h1>
 *
 * <h2>为什么必须用 CoreMod？</h2>
 * <p>
 * CGM v0.15.3 没有为附魔系统预留任何扩展点：
 * <ul>
 *   <li>{@code ItemGun} 不覆写 {@code getItemEnchantability()} → 原版附魔台拒绝给枪附魔</li>
 *   <li>{@code ReloadTracker.isWeaponFull()} 直接从 {@code Gun.general.maxAmmo} 静态字段读取弹容量
 *       → 附魔无法在运行时动态改变弹容量</li>
 * </ul>
 * 这两个缺口在 Java 层面无法通过反射或事件监听弥补，只能通过字节码注入。
 * </p>
 *
 * <h2>注入什么？</h2>
 * <p>
 * FMLCorePlugin 机制在 Minecraft 启动的最早阶段（类加载前）加载本 Transformer，
 * 由 {@link GunClassTransformer} 通过 ASM 精确修改 CGM 的类字节码：
 * <ol>
 *   <li><b>ItemGun</b> — 注入 {@code getItemEnchantability()} 方法，返回附魔能力值 30（等同铁质工具）</li>
 *   <li><b>ReloadTracker.isWeaponFull()</b> — 将弹容量读取路径从"静态字段链"替换为"NBT 标签读取"</li>
 * </ol>
 * </p>
 *
 * <h2>架构全景</h2>
 * <pre>
 *   GunTransformer (IFMLLoadingPlugin)
 *        │  声明于 jar manifest: FMLCorePlugin
 *        │  声明于 gradle 运行参数: fml.coreMods.load
 *        │
 *        └── GunClassTransformer (IClassTransformer)
 *                │  拦截类加载，返回修改后的字节码
 *                │
 *                ├── [注入 A] ItemGun.getItemEnchantability() → 30
 *                │       依赖方: 全部 11 个附魔（无此方法则枪无法进入附魔台）
 *                │
 *                └── [注入 B] ReloadTracker.isWeaponFull()
 *                        原始: this.gun.general.maxAmmo   (静态字段链)
 *                        替换: tag.getString("MaxAmmo")    (NBT 动态读取)
 *                        依赖方: 超容量 (Over Capacity) 附魔
 *                        配合: GunStateHandler 通过反射写 Gun.general.maxAmmo + NBT
 *   </pre>
 *
 * <h2>兼容性风险</h2>
 * <ul>
 *   <li>任何其他修改 CGM 的模组（尤其是也修改 ItemGun 或 CommonEvents 的）会与本 CoreMod 冲突</li>
 *   <li>CGM 版本更新时，如果目标类的内部结构（字段名、方法签名）改变，字节码注入可能定位失败</li>
 *   <li>本模组当前锁定 CGM v0.15.3（com.mrcrayfish.guns.item.ItemGun 路径），不兼容旧版 common 路径</li>
 * </ul>
 *
 * @author CGM Enchantment Addon Team
 * @since 0.0.1
 * @see GunClassTransformer
 */
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class GunTransformer implements IFMLLoadingPlugin {

    /**
     * 向 FML 注册本模组的类转换器。
     * FML 会在每个类加载时回调 {@link GunClassTransformer#transform}。
     *
     * @return 类转换器的全限定名数组
     */
    @Override
    public String[] getASMTransformerClass() {
        return new String[]{"com.example.cgmenchant.core.GunClassTransformer"};
    }

    /** 不使用独立 ModContainer，直接复用 @Mod 注解的容器 */
    @Override
    public String getModContainerClass() { return null; }

    /** 不使用 */
    @Override
    public String getSetupClass() { return null; }

    /** 不使用 */
    @Override
    public void injectData(Map<String, Object> data) {}

    /** 不使用 Access Transformer */
    @Override
    public String getAccessTransformerClass() { return null; }
}
