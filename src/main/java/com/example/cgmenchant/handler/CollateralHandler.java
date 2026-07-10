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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketParticles;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class CollateralHandler {

    // 性能统计：每 100 ticks 输出一次粒子发包量
    private static int packetCount = 0;
    private static int tickCounter = 0;

    private final Queue<Rayan> pendingRays = new LinkedList<>();

    @SubscribeEvent
    public void onBulletSpawn(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (event.getWorld().isRemote) return;
        if (!entity.getClass().getName().startsWith("com.mrcrayfish.guns.entity")) return;
        if (isExplosive(entity)) return;

        EntityPlayer shooter = findShooter(entity);
        if (shooter == null) return;

        ItemStack main = shooter.getHeldItemMainhand();
        if (!EnchantHelper.isGun(main)) {
            main = shooter.getHeldItemOffhand();
            if (!EnchantHelper.isGun(main)) return;
        }

        int collLevel = EnchantHelper.getLevel(main, ModEnchantments.COLLATERAL);
        if (collLevel <= 0) return;

        float baseDamage = 5.0f;
        try {
            baseDamage = (float) entity.getClass().getMethod("getDamage").invoke(entity);
        } catch (Exception ignored) {}

        Vec3d dir = new Vec3d(entity.motionX, entity.motionY, entity.motionZ).normalize();
        Vec3d origin = new Vec3d(entity.posX, entity.posY + 0.3, entity.posZ);

        pendingRays.add(new Rayan(shooter, origin, dir, baseDamage, collLevel, event.getWorld()));
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.world.isRemote) return;

        // 性能统计：每 100 ticks 输出粒子发包量
        tickCounter++;
        if (tickCounter >= 100) {
            if (packetCount > 0) {
                net.minecraftforge.fml.common.FMLLog.log.info(
                    "[cgmenchant perf] Collateral: {} packets in last 100 ticks ({} per tick)",
                    packetCount, (float)packetCount / 100f);
            }
            packetCount = 0;
            tickCounter = 0;
        }

        if (pendingRays.isEmpty()) return;

        while (!pendingRays.isEmpty()) {
            Rayan ray = pendingRays.poll();
            processRay(ray, event.world);
        }
    }

    private void processRay(Rayan ray, World world) {
        Set<Integer> hitEntities = new HashSet<>();

        for (double d = 0; d < 120; d += 0.5) {
            Vec3d current = ray.origin.addVector(ray.direction.x * d, ray.direction.y * d, ray.direction.z * d);
            BlockPos bp = new BlockPos(current);

            // 方块碰撞 → 停止
            if (!world.isAirBlock(bp) && world.getBlockState(bp).isFullBlock()) break;

            // 每隔 2 格发送可见粒子到客户端
            if (d > 1.0 && d % 2.0 < 0.5) {
                SPacketParticles packet = new SPacketParticles(
                    EnumParticleTypes.FIREWORKS_SPARK, true,
                    (float) current.x, (float) current.y, (float) current.z,
                    0.001f, 0.001f, 0.001f, 0.0f, 1);
                if (world instanceof WorldServer) {
                    WorldServer ws = (WorldServer) world;
                    for (EntityPlayerMP player : ws.getMinecraftServer().getPlayerList().getPlayers()) {
                        if (player.dimension == ws.provider.getDimension()
                            && player.getDistanceSq(current.x, current.y, current.z) < 16384) {
                            player.connection.sendPacket(packet);
                            packetCount++;
                        }
                    }
                }
            }

            // 伤害检测
            AxisAlignedBB box = new AxisAlignedBB(
                current.x - 0.5, current.y - 0.5, current.z - 0.5,
                current.x + 0.5, current.y + 0.5, current.z + 0.5);

            for (EntityLivingBase target : world.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
                int eid = target.getEntityId();
                if (hitEntities.contains(eid)) continue;
                if (target == ray.shooter) continue;
                if (target.isDead) continue;

                float bonus = 1.0f + 0.2f * ray.level;
                target.attackEntityFrom(
                    DamageSource.causeIndirectDamage(ray.shooter, ray.shooter),
                    ray.damage * bonus);
                hitEntities.add(eid);
            }
        }
    }

    private boolean isExplosive(Entity entity) {
        String name = entity.getClass().getName().toLowerCase();
        return name.contains("missile") || name.contains("grenade");
    }

    private EntityPlayer findShooter(Entity bullet) {
        try {
            Object s = bullet.getClass().getMethod("getShooter").invoke(bullet);
            if (s instanceof EntityPlayer) return (EntityPlayer) s;
        } catch (Exception ignored) {}
        try {
            Object s = bullet.getClass().getMethod("getOwner").invoke(bullet);
            if (s instanceof EntityPlayer) return (EntityPlayer) s;
        } catch (Exception ignored) {}
        return null;
    }

    private static class Rayan {
        final EntityPlayer shooter;
        final Vec3d origin;
        final Vec3d direction;
        final float damage;
        final int level;
        final World world;

        Rayan(EntityPlayer shooter, Vec3d origin, Vec3d direction, float damage, int level, World world) {
            this.shooter = shooter;
            this.origin = origin;
            this.direction = direction;
            this.damage = damage;
            this.level = level;
            this.world = world;
        }
    }
}
