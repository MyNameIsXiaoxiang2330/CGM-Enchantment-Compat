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

package com.example.cgmenchant.enchant;

import com.example.cgmenchant.Reference;
import net.minecraft.enchantment.Enchantment;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.example.cgmenchant.enchant.EnchantmentGunBase.EnchantmentType.*;

public class ModEnchantments {

    public static final List<EnchantmentGunBase> ENCHANTMENTS = new ArrayList<>();

    // WEAPON
    public static final EnchantmentGunBase TRIGGER_FINGER = add(
        new EnchantmentGunBase(Enchantment.Rarity.RARE, WEAPON, 3) {
            public String getEnchantmentName() { return "trigger_finger"; }
            public String getDisplayName() { return "快速扳机"; }
        });

    public static final EnchantmentGunBase QUICK_HANDS = add(
        new EnchantmentGunBase(Enchantment.Rarity.RARE, WEAPON, 2) {
            public String getEnchantmentName() { return "quick_hands"; }
            public String getDisplayName() { return "熟练手"; }
        });

    public static final EnchantmentGunBase LIGHTWEIGHT = add(
        new EnchantmentGunBase(Enchantment.Rarity.UNCOMMON, WEAPON, 1) {
            public String getEnchantmentName() { return "lightweight"; }
            public String getDisplayName() { return "轻装上阵"; }

            @Override
            public boolean canApply(net.minecraft.item.ItemStack stack) {
                if (!super.canApply(stack)) return false;
                // 检查是否能开镜（榴弹发射器、火箭筒等不能）
                try {
                    Object gun = stack.getItem().getClass().getMethod("getGun").invoke(stack.getItem());
                    // 尝试读 modules.zoom
                    try {
                        java.lang.reflect.Field modF = gun.getClass().getDeclaredField("modules");
                        modF.setAccessible(true);
                        Object modules = modF.get(gun);
                        if (modules != null) {
                            java.lang.reflect.Field zoomF = modules.getClass().getDeclaredField("zoom");
                            zoomF.setAccessible(true);
                            if (zoomF.get(modules) != null) return true;
                        }
                    } catch (Exception ignored) {}
                    // 没有 zoom → 检查是否有 scope 配件槽
                    String id = stack.getItem().getRegistryName().toString().toLowerCase();
                    return !id.contains("bazooka") && !id.contains("grenade_launcher");
                } catch (Exception e) {
                    return true; // 反射失败默认允许
                }
            }
        });

    // AMMO
    public static final EnchantmentGunBase OVER_CAPACITY = add(
        new EnchantmentGunBase(Enchantment.Rarity.RARE, AMMO, 3) {
            public String getEnchantmentName() { return "over_capacity"; }
            public String getDisplayName() { return "超容量"; }
        });

    public static final EnchantmentGunBase RECLAIMED = add(
        new EnchantmentGunBase(Enchantment.Rarity.UNCOMMON, AMMO, 3) {
            public String getEnchantmentName() { return "reclaimed"; }
            public String getDisplayName() { return "勤俭节约"; }
        });

    // PROJECTILE
    public static final EnchantmentGunBase ACCELERATOR = add(
        new EnchantmentGunBase(Enchantment.Rarity.RARE, PROJECTILE, 2) {
            public String getEnchantmentName() { return "accelerator"; }
            public String getDisplayName() { return "加速器"; }
        });

    public static final EnchantmentGunBase PUNCTURING = add(
        new EnchantmentGunBase(Enchantment.Rarity.RARE, PROJECTILE, 4) {
            public String getEnchantmentName() { return "puncturing"; }
            public String getDisplayName() { return "穿甲弹"; }
        });

    public static final EnchantmentGunBase COLLATERAL = add(
        new EnchantmentGunBase(Enchantment.Rarity.VERY_RARE, PROJECTILE, 3) {
            public String getEnchantmentName() { return "collateral"; }
            public String getDisplayName() { return "间接伤害"; }

            @Override
            protected boolean canApplyTogether(net.minecraft.enchantment.Enchantment other) {
                // 与高爆弹、纵火者互斥
                if (other instanceof EnchantmentHighExplosive) return false;
                if (other instanceof EnchantmentFireStarter) return false;
                return super.canApplyTogether(other);
            }
        });

    public static final EnchantmentGunBase FIRE_STARTER = add(
        new EnchantmentFireStarter());

    // ==== 弧光引导 ====
    public static final EnchantmentGunBase ARC_LIGHT = add(
        new EnchantmentArcLightStarter());

    // ==== 高爆弹 ====
    public static final EnchantmentGunBase HIGH_EXPLOSIVE = add(
        new EnchantmentHighExplosive());

    // ==== 凶弹-地霰形 (FELLBULLET) — 地面红圈蓄力 → 360° 散射 ====
    public static final EnchantmentGunBase FELLBULLET = add(
        new EnchantmentSinBullet());

    // ==== 凶弹-贯霰形 (FELLBULLET Piercer) — 背后红圈蓄力 → 锥形散射 ====
    public static final EnchantmentGunBase FELLBULLET_PIERCER = add(
        new EnchantmentFellbulletPiercer());

    // ==== 庄严哀悼 (Solemn Mourning) — 子弹追踪 + 黑白拖尾 ====
    public static final EnchantmentGunBase SOLEMN_MOURNING = add(
        new EnchantmentSolemnMourning());

    private static EnchantmentGunBase add(EnchantmentGunBase e) {
        ENCHANTMENTS.add(e);
        return e;
    }

    /**
     * 注册所有附魔。
     *
     * @return true 表示 ItemGun 类可用（附魔将正常工作）；
     *         false 表示 CGM 未加载，所有附魔注册但 canApply() 始终返回 false。
     */
    public static boolean register() {
        boolean available = EnchantmentGunBase.isItemGunAvailable();
        net.minecraftforge.registries.IForgeRegistry<Enchantment> registry =
                GameRegistry.findRegistry(Enchantment.class);
        for (EnchantmentGunBase e : ENCHANTMENTS) {
            registry.register(e);
        }
        return available;
    }

    public static List<EnchantmentGunBase> getAll() {
        return Collections.unmodifiableList(ENCHANTMENTS);
    }
}
