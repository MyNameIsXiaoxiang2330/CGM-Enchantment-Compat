/*
 * CGM Enchantment Addon — 为 MrCrayfish's Gun Mod 添加枪械附魔
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
 * 凶弹-贯霰形 — 击杀时在目标背后生成同心环，蓄力完成后沿弹道方向锥形散射。
 *
 * 参考: 边狱公司 (Limbus Company) EGO "FELLBULLET"
 *
 * 等级 I ~ IV:
 * - 蓄力时间 = 等级 × 0.65 秒
 * - 散射波数 = 等级
 * - 每波随机 8~24 发，锥形集中在弹道方向
 * - 伤害 = 枪械原始伤害
 * - 霰弹枪每发凶弹产生额外子弹出
 */
public class EnchantmentFellbulletPiercer extends EnchantmentGunBase {

    public EnchantmentFellbulletPiercer() {
        super(Rarity.VERY_RARE, EnchantmentType.PROJECTILE, 4);
    }

    @Override
    public String getEnchantmentName() {
        return "fellbullet_piercer";
    }

    @Override
    public String getDisplayName() {
        return "凶弹-贯霰形";
    }

    @Override
    public int getMinEnchantability(int level) {
        return 20 + level * 8;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return getMinEnchantability(level) + 30;
    }

    @Override
    public boolean canApply(net.minecraft.item.ItemStack stack) {
        if (!super.canApply(stack)) return false;
        String id = stack.getItem().getRegistryName().toString().toLowerCase();
        return id.contains("shotgun") || id.equals("cgm:rifle");
    }

    /** 宝藏附魔：不可附魔台获得，只能村民/战利品 */
    @Override
    public boolean isTreasureEnchantment() { return true; }

    @Override
    protected boolean canApplyTogether(net.minecraft.enchantment.Enchantment other) {
        if (other instanceof EnchantmentGunBase) {
            String n = ((EnchantmentGunBase) other).getEnchantmentName();
            return n.equals("lightweight") || n.equals("accelerator") || n.equals("reclaimed");
        }
        return super.canApplyTogether(other);
    }
}
