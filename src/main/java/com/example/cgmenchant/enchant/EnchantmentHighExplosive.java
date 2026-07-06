package com.example.cgmenchant.enchant;

import net.minecraft.enchantment.Enchantment;

/**
 * 高爆弹 (High Explosive) — 根据武器类型提供不同的爆炸效果。
 *
 * 等级: I ~ V
 * - 普通子弹: 小型爆炸 + 2×等级 爆炸伤害
 * - 霰弹: 小型爆炸 + 1×等级/弹丸 爆炸伤害 + 射程增加
 * - 火箭/榴弹: 爆炸范围扩大 + 3×等级 爆炸伤害
 */
public class EnchantmentHighExplosive extends EnchantmentGunBase {

    public EnchantmentHighExplosive() {
        super(Rarity.RARE, EnchantmentType.PROJECTILE, 5);
    }

    @Override
    public String getEnchantmentName() {
        return "high_explosive";
    }

    @Override
    public String getDisplayName() {
        return "高爆弹";
    }

    @Override
    protected boolean canApplyTogether(Enchantment other) {
        // 与间接伤害冲突
        if (other instanceof EnchantmentGunBase &&
            "collateral".equals(((EnchantmentGunBase) other).getEnchantmentName())) {
            return false;
        }
        return super.canApplyTogether(other);
    }

    @Override
    public int getMinEnchantability(int level) {
        return 12 + level * 6;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return getMinEnchantability(level) + 25;
    }
}
