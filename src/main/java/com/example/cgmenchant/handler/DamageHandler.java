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

import com.example.cgmenchant.enchant.ModEnchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class DamageHandler {

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        EntityLivingBase target = event.getEntityLiving();
        if (target.world.isRemote) return;

        EntityPlayer shooter = EnchantHelper.getShooterFromDamageSource(event.getSource());
        if (shooter == null) return;

        if (!EnchantHelper.isGun(shooter.getHeldItemMainhand()) &&
            !EnchantHelper.isGun(shooter.getHeldItemOffhand())) return;

        float damage = event.getAmount();

        // 加速器 (Accelerator): 伤害 +10%/级
        int accelLevel = EnchantHelper.getLevel(shooter, ModEnchantments.ACCELERATOR);
        if (accelLevel > 0) {
            damage += damage * 0.1f * accelLevel;
        }

        // 穿甲弹 (Puncturing): 忽视护甲
        int punctLevel = EnchantHelper.getLevel(shooter, ModEnchantments.PUNCTURING);
        if (punctLevel > 0) {
            float armorIgnore = 0.25f * punctLevel;
            float armorReduction = getArmorReduction(target);
            damage += damage * armorReduction * armorIgnore;
        }

        // 纵火者 (Fire Starter): 点燃（方块火焰由 BulletHandler 在子弹消失时放置）
        int fireLevel = EnchantHelper.getLevel(shooter, ModEnchantments.FIRE_STARTER);
        if (fireLevel > 0) {
            target.setFire(5);
        }

        // 弧光引导 (ArcLight): 纯视觉闪电 + 充能苦力怕 + 魔法伤害
        int arcLevel = EnchantHelper.getLevel(shooter, ModEnchantments.ARC_LIGHT);
        if (arcLevel > 0) {
            BlockPos pos = target.getPosition();
            EntityLightningBolt bolt = new EntityLightningBolt(target.world, pos.getX(), pos.getY(), pos.getZ(), true);
            target.world.addWeatherEffect(bolt);

            double r = 4.0;
            for (Entity e : target.world.getEntitiesWithinAABB(Entity.class,
                    target.getEntityBoundingBox().grow(r, r, r))) {
                if (e instanceof EntityCreeper && !e.isDead) {
                    ((EntityCreeper) e).onStruckByLightning(bolt);
                }
            }

            if (target.world.getBlockState(pos).getBlock() == Blocks.FIRE)
                target.world.setBlockToAir(pos);

            damage += 5.0f * arcLevel;
        }

        event.setAmount(damage);
    }

    private float getArmorReduction(EntityLivingBase entity) {
        int armor = entity.getTotalArmorValue();
        if (armor <= 0) return 0f;
        return Math.min(1.0f, armor * 0.04f);
    }
}
