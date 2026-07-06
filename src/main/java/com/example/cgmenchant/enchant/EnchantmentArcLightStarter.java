package com.example.cgmenchant.enchant;

import net.minecraft.enchantment.Enchantment;

/**
 * 弧光引导 — 子弹命中/爆炸时召唤闪电并造成额外魔法伤害。
 * 冲突附魔: Fire Starter (纵火者)
 */
public class EnchantmentArcLightStarter extends EnchantmentGunBase {

    public EnchantmentArcLightStarter() {
        super(Rarity.VERY_RARE, EnchantmentType.PROJECTILE, 3);
    }

    @Override
    public String getEnchantmentName() {
        return "arc_light";
    }

    @Override
    public String getDisplayName() {
        return "弧光引导";
    }

    @Override
    protected boolean canApplyTogether(Enchantment other) {
        // 默认与纵火者冲突，可通过配置覆盖
        if (other instanceof EnchantmentFireStarter) {
            return net.minecraftforge.fml.common.Loader.instance().getIndexedModList()
                    .get("cgmenchant").getMetadata().version != null
                    && Boolean.parseBoolean(System.getProperty("cgmenchant.allowArcLightWithFire", "false"));
        }
        return super.canApplyTogether(other);
    }

    @Override
    public int getMinEnchantability(int level) {
        return 15 + level * 7;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return getMinEnchantability(level) + 25;
    }
}
