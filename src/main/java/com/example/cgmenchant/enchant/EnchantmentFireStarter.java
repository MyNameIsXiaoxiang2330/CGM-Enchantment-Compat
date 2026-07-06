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

/**
 * 纵火者 — 用于冲突检测的占位类。
 * 实际效果在 DamageHandler / BulletHandler 中实现。
 */
public class EnchantmentFireStarter extends EnchantmentGunBase {

    public EnchantmentFireStarter() {
        super(Rarity.UNCOMMON, EnchantmentType.PROJECTILE, 1);
    }

    @Override
    public String getEnchantmentName() {
        return "fire_starter";
    }

    @Override
    public String getDisplayName() {
        return "纵火者";
    }
}
