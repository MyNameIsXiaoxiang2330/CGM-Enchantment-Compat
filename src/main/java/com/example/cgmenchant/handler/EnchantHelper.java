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

package com.example.cgmenchant.handler;

import com.example.cgmenchant.Reference;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

import javax.annotation.Nullable;
import java.util.Map;

public class EnchantHelper {

    /**
     * 从玩家双手获取附魔等级（不区分是否枪械）。
     * 注意：此方法会检查双手所有物品，包括非枪械。
     * 在需要确保附魔只来自枪械的场景中，请改用 getGunLevel()。
     */
    public static int getLevel(EntityPlayer player, Enchantment enchantment) {
        if (player == null) return 0;
        int level = getLevel(player.getHeldItem(EnumHand.MAIN_HAND), enchantment);
        if (level > 0) return level;
        return getLevel(player.getHeldItem(EnumHand.OFF_HAND), enchantment);
    }

    /**
     * 安全地获取玩家持枪手上的附魔等级。
     * 只在持有枪械的那只手上查附魔，避免副手枪械附魔错误渗透到主手非枪械攻击。
     * 优先检查主手，如果主手不是枪则查副手。
     *
     * @param player      玩家
     * @param enchantment 要查询的附魔
     * @return 附魔等级（0 表示没有该附魔或没持枪）
     */
    public static int getGunLevel(EntityPlayer player, Enchantment enchantment) {
        if (player == null) return 0;
        ItemStack main = player.getHeldItem(EnumHand.MAIN_HAND);
        if (isGun(main)) return getLevel(main, enchantment);
        ItemStack off = player.getHeldItem(EnumHand.OFF_HAND);
        if (isGun(off)) return getLevel(off, enchantment);
        return 0;
    }

    public static int getLevel(ItemStack stack, Enchantment enchantment) {
        if (stack.isEmpty()) return 0;
        return EnchantmentHelper.getEnchantmentLevel(enchantment, stack);
    }

    public static Map<Enchantment, Integer> getAll(EntityPlayer player) {
        if (player == null) return java.util.Collections.emptyMap();
        ItemStack main = player.getHeldItem(EnumHand.MAIN_HAND);
        if (!main.isEmpty() && isGun(main))
            return EnchantmentHelper.getEnchantments(main);
        ItemStack off = player.getHeldItem(EnumHand.OFF_HAND);
        if (!off.isEmpty() && isGun(off))
            return EnchantmentHelper.getEnchantments(off);
        return java.util.Collections.emptyMap();
    }

    public static boolean isGun(ItemStack stack) {
        if (stack.isEmpty()) return false;
        try {
            Class<?> gunClass = Class.forName(Reference.CGM_ITEM_GUN_CLASS);
            return gunClass.isInstance(stack.getItem());
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Nullable
    public static EntityPlayer getShooterFromDamageSource(net.minecraft.util.DamageSource source) {
        if (source.getTrueSource() instanceof EntityPlayer)
            return (EntityPlayer) source.getTrueSource();
        if (source.getImmediateSource() instanceof EntityPlayer)
            return (EntityPlayer) source.getImmediateSource();
        return null;
    }
}
