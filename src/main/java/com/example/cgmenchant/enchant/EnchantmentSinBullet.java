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
 * 凶弹 — 击杀时召唤红圈蓄力，蓄力完成后散射多重子弹。
 *
 * 参考: 边狱公司 (Limbus Company) EGO "凶弹"
 *
 * 等级 I ~ IV:
 * - 红圈数量 = 等级
 * - 蓄力时间 = 等级（秒）
 * - 散射子弹数 = 等级 × 2
 * - 子弹自动消失 = 6 秒未命中
 */
public class EnchantmentSinBullet extends EnchantmentGunBase {

    public EnchantmentSinBullet() {
        super(Rarity.VERY_RARE, EnchantmentType.PROJECTILE, 4);
    }

    @Override
    public String getEnchantmentName() {
        return "fellbullet";
    }

    @Override
    public String getDisplayName() {
        return "凶弹-地霰形";
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
            // 仅与轻装上阵、加速器、勤俭节约兼容
            return n.equals("lightweight") || n.equals("accelerator") || n.equals("reclaimed");
        }
        return super.canApplyTogether(other);
    }
}
