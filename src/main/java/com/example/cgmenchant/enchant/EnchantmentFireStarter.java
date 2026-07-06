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
