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

package com.example.cgmenchant;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.enchantment.EnchantmentData;

public class TabCGMEnchant extends CreativeTabs {

    public static final TabCGMEnchant INSTANCE = new TabCGMEnchant();

    private static final String[] ICONS = {
        "cgm:basic_ammo", "cgm:advanced_ammo",
        "cgm:shell", "cgm:missile", "cgm:grenade"
    };

    public TabCGMEnchant() {
        super("cgmenchant");
    }

    @Override
    public String getTranslatedTabLabel() {
        return "\u9644\u9B54\u517C\u5BB9"; // "附魔兼容"
    }

    @Override
    public ItemStack getTabIconItem() {
        int idx = (int)((System.currentTimeMillis() / 1500) % ICONS.length);
        Item item = Item.getByNameOrId(ICONS[idx]);
        return item != null ? new ItemStack(item) : new ItemStack(Items.ENCHANTED_BOOK);
    }

    @Override
    public void displayAllRelevantItems(NonNullList<ItemStack> items) {
        super.displayAllRelevantItems(items);
        for (net.minecraft.enchantment.Enchantment ench : net.minecraft.enchantment.Enchantment.REGISTRY) {
            if (ench instanceof com.example.cgmenchant.enchant.EnchantmentGunBase) {
                // 列出所有等级
                for (int level = 1; level <= ench.getMaxLevel(); level++) {
                    items.add(ItemEnchantedBook.getEnchantedItemStack(
                        new EnchantmentData(ench, level)));
                }
            }
        }
    }
}
