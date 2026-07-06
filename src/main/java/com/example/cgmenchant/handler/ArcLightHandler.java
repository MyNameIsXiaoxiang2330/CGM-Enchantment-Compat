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
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

/**
 * 弧光引导 (ArcLight) 爆炸处理器 — 榴弹/火箭弹的范围电弧效果。
 *
 * 闪电为纯视觉（不燃方块），苦力怕通过 onStruckByLightning 手动充能。
 * 子弹命中的单目标效果在 DamageHandler 中处理。
 */
public class ArcLightHandler {

    /**
     * 爆炸事件 — 处理榴弹/火箭弹的范围电弧。
     */
    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getWorld().isRemote) return;

        EntityPlayer shooter = findShooter(event);
        if (shooter == null) return;

        int arcLevel = EnchantHelper.getLevel(shooter, ModEnchantments.ARC_LIGHT);
        if (arcLevel <= 0) return;

        World world = event.getWorld();
        Vec3d center = event.getExplosion().getPosition();
        BlockPos centerPos = new BlockPos(center.x, center.y, center.z);

        // 纯视觉闪电（不燃方块、不破坏地形）
        EntityLightningBolt bolt = new EntityLightningBolt(world, centerPos.getX(), centerPos.getY(), centerPos.getZ(), true);
        world.addWeatherEffect(bolt);

        // 手动充能范围内的苦力怕
        double chargeRadius = 4.0;
        AxisAlignedBB chargeBox = new AxisAlignedBB(
                center.x - chargeRadius, center.y - chargeRadius, center.z - chargeRadius,
                center.x + chargeRadius, center.y + chargeRadius, center.z + chargeRadius);
        for (Entity e : world.getEntitiesWithinAABB(Entity.class, chargeBox)) {
            if (e instanceof EntityCreeper && !e.isDead) {
                ((EntityCreeper) e).onStruckByLightning(bolt);
            }
        }

        // 清除闪电可能产生的火焰（安全兜底）
        clearFire(world, centerPos);

        // 半径 8 格 AOE 魔法伤害
        double radius = 8.0;
        AxisAlignedBB aabb = new AxisAlignedBB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb);

        for (EntityLivingBase target : targets) {
            if (target == shooter) continue;
            target.attackEntityFrom(DamageSource.causeIndirectMagicDamage(
                    event.getExplosion().getExplosivePlacedBy(), shooter),
                    10.0f * arcLevel);
        }
    }

    private void clearFire(World world, BlockPos pos) {
        if (world.getBlockState(pos).getBlock() == Blocks.FIRE)
            world.setBlockToAir(pos);
        if (world.getBlockState(pos.up()).getBlock() == Blocks.FIRE)
            world.setBlockToAir(pos.up());
        if (world.getBlockState(pos.down()).getBlock() == Blocks.FIRE)
            world.setBlockToAir(pos.down());
    }

    private EntityPlayer findShooter(ExplosionEvent.Detonate event) {
        Entity source = event.getExplosion().getExplosivePlacedBy();
        if (source instanceof EntityPlayer) return (EntityPlayer) source;
        // 从爆炸影响的实体中找抛射物来源
        for (Entity entity : event.getAffectedEntities()) {
            if (entity instanceof net.minecraft.entity.projectile.EntityThrowable) {
                Entity thrower = ((net.minecraft.entity.projectile.EntityThrowable) entity).getThrower();
                if (thrower instanceof EntityPlayer) return (EntityPlayer) thrower;
            }
        }
        return null;
    }
}
