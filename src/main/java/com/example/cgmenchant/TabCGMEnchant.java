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
