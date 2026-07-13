/*
 * CGM Enchantment Addon — 庄严哀悼 (Solemn Mourning)
 * Copyright (C) 2026 CGM Enchantment Addon Team
 *
 * EGO 还原附魔。子弹自动追踪最近的敌对目标，
 * 弹道拖尾为黑白双色交替粒子，寿命 10 秒。
 * 仅可附魔于手枪和加特林。
 */
package com.example.cgmenchant.enchant;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;

public class EnchantmentSolemnMourning extends EnchantmentGunBase {

    public EnchantmentSolemnMourning() {
        super(Rarity.VERY_RARE, EnchantmentType.PROJECTILE, 5);
    }

    @Override
    public String getEnchantmentName() {
        return "Soleme_lament";
    }

    @Override
    public String getDisplayName() {
        return "庄严哀悼";
    }

    @Override
    public int getMinEnchantability(int level) {
        return 20 + level * 8;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return getMinEnchantability(level) + 30;
    }

    /**
     * 仅限手枪和加特林。
     */
    @Override
    public boolean canApply(ItemStack stack) {
        if (!super.canApply(stack)) return false;
        String id = stack.getItem().getRegistryName().toString().toLowerCase();
        return id.contains("pistol") || id.contains("gatling") || id.contains("minigun");
    }

    /** 宝藏附魔：不可附魔台获得 */
    @Override
    public boolean isTreasureEnchantment() { return true; }

    @Override
    protected boolean canApplyTogether(Enchantment other) {
        if (other instanceof EnchantmentGunBase) {
            String n = ((EnchantmentGunBase) other).getEnchantmentName();
            // 仅与轻装上阵、加速器、勤俭节约兼容
            return n.equals("lightweight") || n.equals("accelerator") || n.equals("reclaimed");
        }
        return super.canApplyTogether(other);
    }
}
