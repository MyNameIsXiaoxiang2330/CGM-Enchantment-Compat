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
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public abstract class EnchantmentGunBase extends Enchantment {

    private static Class<?> itemGunClass;
    static {
        try {
            itemGunClass = Class.forName(Reference.CGM_ITEM_GUN_CLASS);
        } catch (ClassNotFoundException e) {}
    }

    protected final EnchantmentType type;
    private final int maxLevel;

    public EnchantmentGunBase(Rarity rarity, EnchantmentType type, int maxLevel) {
        super(rarity, EnumEnchantmentType.WEAPON,
              new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND, EntityEquipmentSlot.OFFHAND});
        this.type = type;
        this.maxLevel = maxLevel;
        setRegistryName(new ResourceLocation(Reference.MOD_ID, getEnchantmentName()));
        setName(Reference.MOD_ID + "." + getEnchantmentName());
    }

    @Override
    public int getMaxLevel() { return maxLevel; }

    public abstract String getEnchantmentName();
    public abstract String getDisplayName();

    @Override
    public boolean canApply(ItemStack stack) {
        return itemGunClass != null && itemGunClass.isInstance(stack.getItem());
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return canApply(stack);
    }

    @Override
    protected boolean canApplyTogether(Enchantment other) {
        if (other instanceof EnchantmentGunBase) {
            return ((EnchantmentGunBase) other).type != this.type;
        }
        return super.canApplyTogether(other);
    }

    @Override
    public String getTranslatedName(int level) {
        String name = getDisplayName();
        if (getMaxLevel() > 1) {
            name += " " + getRomanNumeral(level);
        }
        return name;
    }

    private static String getRomanNumeral(int n) {
        switch (n) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            default: return "" + n;
        }
    }

    @Override
    public int getMinEnchantability(int level) { return 10 + level * 5; }

    @Override
    public int getMaxEnchantability(int level) { return getMinEnchantability(level) + 20; }

    public enum EnchantmentType {
        WEAPON, AMMO, PROJECTILE
    }
}
