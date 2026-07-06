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
            public String getDisplayName() { return "弹药回收"; }
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
        });

    public static final EnchantmentGunBase FIRE_STARTER = add(
        new EnchantmentFireStarter());

    // ==== 弧光引导 ====
    public static final EnchantmentGunBase ARC_LIGHT = add(
        new EnchantmentArcLightStarter());

    // ==== 高爆弹 ====
    public static final EnchantmentGunBase HIGH_EXPLOSIVE = add(
        new EnchantmentHighExplosive());

    private static EnchantmentGunBase add(EnchantmentGunBase e) {
        ENCHANTMENTS.add(e);
        return e;
    }

    public static void register() {
        net.minecraftforge.registries.IForgeRegistry<Enchantment> registry =
                GameRegistry.findRegistry(Enchantment.class);
        for (EnchantmentGunBase e : ENCHANTMENTS) {
            registry.register(e);
        }
    }

    public static List<EnchantmentGunBase> getAll() {
        return Collections.unmodifiableList(ENCHANTMENTS);
    }
}
