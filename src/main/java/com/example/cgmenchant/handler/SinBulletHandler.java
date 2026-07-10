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
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketParticles;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

/**
 * 凶弹 (Sin Bullet) 事件处理器。
 *
 * <h3>流程</h3>
 * <ol>
 *   <li><b>击杀触发</b> — LivingDeathEvent 检测玩家持枪击杀，启动蓄力序列</li>
 *   <li><b>蓄力阶段</b> — 4 个同心红环嵌套，每 tick 一起向中心收缩</li>
 *   <li><b>散射阶段</b> — 蓄力完成后，从击杀点射出（等级）波子弹，
 *       每波随机 8~24 发，360° 散射</li>
 *   <li><b>防无限循环</b> — 散射中的射线击杀不会再次触发凶弹</li>
 *   <li><b>超时自毁</b> — 射线存活 6 秒（120 ticks）后自动消失</li>
 * </ol>
 */
public class SinBulletHandler {

    /** 凶弹自身造成的击杀标记，用于防止无限循环 */
    private static boolean processingSinBullet = false;

    /** 活跃蓄力序列: 射手 UUID → 蓄力数据 */
    private final Map<UUID, SinCharge> charges = new HashMap<>();

    /** 活跃散射射线 */
    private final List<SinRay> activeRays = new ArrayList<>();

    // ===================================================================
    //  击杀触发
    // ===================================================================

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        // 防凶弹子弹触发的击杀再次触发凶弹
        if (processingSinBullet) return;

        EntityLivingBase target = event.getEntityLiving();
        if (target.world.isRemote) return;

        EntityPlayer shooter = EnchantHelper.getShooterFromDamageSource(event.getSource());
        if (shooter == null) return;

        // 检查主手/副手枪械是否有凶弹附魔
        ItemStack gun = findGunWithSinBullet(shooter);
        if (gun == null) return;

        int level = EnchantHelper.getLevel(gun, ModEnchantments.FELLBULLET);
        if (level <= 0) return;

        FellbulletTracker.onKill(shooter.getUniqueID());

        // 同一玩家不能叠加蓄力
        UUID uid = shooter.getUniqueID();
        if (charges.containsKey(uid)) return;

        CommandCGMEnchant.sendDialogue(shooter, CommandCGMEnchant.getRandomDialogue());
        BlockPos pos = target.getPosition();
        SinCharge charge = new SinCharge(
            target.world, uid,
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            level
        );
        charges.put(uid, charge);
    }

    // ===================================================================
    //  世界 tick: 蓄力动画 + 射线更新
    // ===================================================================

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.world.isRemote) return;

        WorldServer ws = (WorldServer) event.world;
        int dimId = ws.provider.getDimension();

        // 凶弹 Miss 惩罚检查
        FellbulletTracker.tick(ws);

        // 性能统计：每 100 ticks 输出活跃射线数
        if (event.world.getTotalWorldTime() % 100 == 0 && !activeRays.isEmpty()) {
            net.minecraftforge.fml.common.FMLLog.log.info(
                "[cgmenchant perf] FELLBULLET: {} active rays", activeRays.size());
        }

        // ---- 阶段 1: 蓄力 / 逐波散射 ----
        Iterator<Map.Entry<UUID, SinCharge>> cit = charges.entrySet().iterator();
        while (cit.hasNext()) {
            Map.Entry<UUID, SinCharge> entry = cit.next();
            SinCharge ch = entry.getValue();

            if (!ch.world.equals(event.world)) continue;

            if (!ch.chargeComplete) {
                // 蓄力阶段：红圈动画
                spawnRedCircles(ws, ch);
                ch.elapsedTicks++;
                if (ch.elapsedTicks >= ch.totalTicks) {
                    // 蓄力完成 → 烟花发射声 → 进入散射阶段
                    ws.playSound(null, ch.x, ch.y, ch.z,
                        SoundEvents.ENTITY_FIREWORK_LAUNCH, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    ch.chargeComplete = true;
                    ch.waveCooldown = 5; // 0.25s 后打出第一波
                }
            } else {
                // 散射阶段：逐波发射
                if (ch.waveCooldown > 0) {
                    ch.waveCooldown--;
                } else if (ch.wavesSpawned < ch.level) {
                    spawnWave(ws, ch);
                    ch.wavesSpawned++;
                    ch.waveCooldown = 3; // 每波间隔 ~0.15s
                } else {
                    cit.remove(); // 全部波数发射完毕
                }
            }
        }

        // ---- 阶段 2: 射线 ----
        Iterator<SinRay> rit = activeRays.iterator();
        while (rit.hasNext()) {
            SinRay ray = rit.next();

            // 维度过滤
            if (ray.dimId != dimId) continue;

            ray.lifetime--;
            if (ray.lifetime <= 0) {
                rit.remove();
                continue;
            }

            advanceRay(ray, ws);
            if (ray.lifetime <= 0) {
                rit.remove();
            }
        }
    }

    // ===================================================================
    //  粒子：红圈收缩动画
    // ===================================================================

    /** EGO 魔法阵动画：交错旋转的同心环 + 旋转准星 + 光点 */
    private void spawnRedCircles(WorldServer ws, SinCharge ch) {
        float progress = (float)ch.elapsedTicks / ch.totalTicks;
        float t = 1.0f - progress;
        float rot = (float)(progress * Math.PI * 2); // 整体旋转角度

        // -- 4 层交错旋转的环 --
        // 每层间隔 12 个粒子，旋转方向交替
        float[] radii = {3.0f * t, 2.2f * t, 1.4f * t, 0.6f * t};
        for (int r = 0; r < 4; r++) {
            float cr = radii[r];
            if (cr < 0.05f) continue;
            int dir = (r % 2 == 0) ? 1 : -1; // 交替旋转方向
            for (int i = 0; i < 8; i++) {
                double a = 2.0 * Math.PI * i / 8 + rot * dir;
                double px = ch.x + Math.cos(a) * cr;
                double pz = ch.z + Math.sin(a) * cr;
                ParticleUtils.spawnParticleAt(ws, EnumParticleTypes.REDSTONE,
                    (float)px, (float)ch.y + 0.05f, (float)pz);
            }
        }

        // -- 旋转准星线 --
        for (int d = 0; d < 4; d++) {
            double angle = rot + d * Math.PI / 2;
            for (float s = 0.3f; s < 3.0f * t; s += 0.7f) {
                double px = ch.x + Math.cos(angle) * s;
                double pz = ch.z + Math.sin(angle) * s;
                ParticleUtils.spawnParticleAt(ws, EnumParticleTypes.REDSTONE,
                    (float)px, (float)ch.y + 0.05f, (float)pz);
            }
        }

    }

    // ===================================================================
    //  散射阶段：逐波发射
    // ===================================================================

    /**
     * 发射一轮散弹：随机 8~24 发，360° 方向，附带 TNT 爆炸声 + 黑红爆发粒子。
     */
    private void spawnWave(WorldServer ws, SinCharge ch) {
        Random rand = ws.rand;
        int bulletCount = 8 + rand.nextInt(17);

        // TNT 爆炸声（每波一次）
        ws.playSound(null, ch.x, ch.y, ch.z,
            SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS,
            1.0f, 0.9f + rand.nextFloat() * 0.2f);

        // 黑雾扬起
        ParticleUtils.spawnBurst(ws, ch.x, ch.y + 0.3f, ch.z,
            EnumParticleTypes.SMOKE_NORMAL, 0.6f, 20, 0.08f);

        for (int i = 0; i < bulletCount; i++) {
            double yaw = 2.0 * Math.PI * rand.nextDouble();
            double pitch = (rand.nextDouble() - 0.5) * 0.6;

            Vec3d dir = new Vec3d(
                Math.cos(yaw) * Math.cos(pitch),
                Math.sin(pitch) + 0.05,
                Math.sin(yaw) * Math.cos(pitch)
            ).normalize();

            SinRay ray = new SinRay();
            ray.origin = new Vec3d(ch.x, ch.y + 0.3, ch.z);
            ray.dir = dir;
            ray.damage = 1.0f; // 占位，实际命中时计算 maxHP × level
            ray.fellLevel = ch.level;
            ray.shooterId = ch.shooterId;
            ray.lifetime = 120;
            ray.dimId = ws.provider.getDimension();
            activeRays.add(ray);
        }
    }

    // ===================================================================
    //  射线前进 + 碰撞检测
    // ===================================================================

    /**
     * 将射线向前推进 1.5 格（每 tick 一次大步）。
     * - 方块碰撞 → 销毁 + 粒子
     * - 实体碰撞 → 造成伤害 → 销毁
     * - 否则 → 刷粒子 → 继续
     */
    private void advanceRay(SinRay ray, WorldServer ws) {
        Vec3d next = ray.origin.addVector(
            ray.dir.x * 1.5,
            ray.dir.y * 1.5,
            ray.dir.z * 1.5
        );

        // 方块碰撞
        BlockPos bp = new BlockPos(next);
        if (!ws.isAirBlock(bp) && ws.getBlockState(bp).isFullBlock()) {
            SPacketParticles hit = new SPacketParticles(
                EnumParticleTypes.SMOKE_NORMAL, false,
                (float) next.x, (float) next.y, (float) next.z,
                0.1f, 0.1f, 0.1f, 0.02f, 5);
            sendToNearby(ws, next.x, next.y, next.z, hit);
            ray.lifetime = 0;
            return;
        }

        // 弹道烟雾（黑色浓烟）
        SPacketParticles trail = new SPacketParticles(
            EnumParticleTypes.SMOKE_LARGE, false,
            (float) next.x, (float) next.y + 0.05f, (float) next.z,
            0.04f, 0.04f, 0.04f, 0.0f, 2);
        sendToNearby(ws, next.x, next.y, next.z, trail);

        // 实体碰撞
        AxisAlignedBB box = new AxisAlignedBB(
            next.x - 0.5, next.y - 0.5, next.z - 0.5,
            next.x + 0.5, next.y + 0.5, next.z + 0.5
        );

        for (EntityLivingBase target : ws.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            if (target.getUniqueID().equals(ray.shooterId)) continue;
            if (target.isDead) continue;

            EntityPlayer shooter = findPlayerByUUID(ws, ray.shooterId);

            processingSinBullet = true;
            float fellDmg = target.getMaxHealth() * ray.fellLevel;
            if (fellDmg > 30f) fellDmg *= 0.5f;
            target.attackEntityFrom(
                shooter != null
                    ? DamageSource.causeIndirectDamage(shooter, shooter)
                    : DamageSource.GENERIC,
                fellDmg);
            processingSinBullet = false;

            SPacketParticles hitParticle = new SPacketParticles(
                EnumParticleTypes.CRIT, false,
                (float) next.x, (float) next.y + 0.5f, (float) next.z,
                0.1f, 0.1f, 0.1f, 0.2f, 5);
            sendToNearby(ws, next.x, next.y, next.z, hitParticle);

            ray.lifetime = 0;
            return;
        }

        ray.origin = next;
    }

    // ===================================================================
    //  工具方法
    // ===================================================================

    /** 找玩家主/副手上有凶弹附魔的枪 */
    private ItemStack findGunWithSinBullet(EntityPlayer player) {
        ItemStack main = player.getHeldItemMainhand();
        if (EnchantHelper.isGun(main) && EnchantHelper.getLevel(main, ModEnchantments.FELLBULLET) > 0)
            return main;
        ItemStack off = player.getHeldItemOffhand();
        if (EnchantHelper.isGun(off) && EnchantHelper.getLevel(off, ModEnchantments.FELLBULLET) > 0)
            return off;
        return null;
    }

    /** 按 UUID 查找在线玩家 */
    private EntityPlayer findPlayerByUUID(World world, UUID uuid) {
        for (EntityPlayer p : world.playerEntities) {
            if (p.getUniqueID().equals(uuid)) return p;
        }
        return null;
    }

    /** 发送粒子包到附近 128m 内的玩家 */
    private void sendToNearby(WorldServer ws, double x, double y, double z, SPacketParticles packet) {
        for (EntityPlayerMP player : ws.getMinecraftServer().getPlayerList().getPlayers()) {
            if (player.dimension == ws.provider.getDimension()
                && player.getDistanceSq(x, y, z) < 16384) {
                player.connection.sendPacket(packet);
            }
        }
    }

    // ===================================================================
    //  内部数据类型
    // ===================================================================

    /** 蓄力序列数据 */
    private static class SinCharge {
        final World world;
        final UUID shooterId;
        final double x, y, z;
        final int level;
        final int totalTicks;
        int elapsedTicks;
        boolean chargeComplete;   // 蓄力是否完成，进入散射阶段
        int wavesSpawned;         // 已发射的波数
        int waveCooldown;         // 距下一波还有几 tick

        SinCharge(World world, UUID shooterId, double x, double y, double z, int level) {
            this.world = world;
            this.shooterId = shooterId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.level = level;
            this.totalTicks = level * 13; // 等级 × 0.65 秒（20 ticks/秒）
            this.elapsedTicks = 0;
            this.chargeComplete = false;
            this.wavesSpawned = 0;
            this.waveCooldown = 0;
        }
    }

    /** 散射射线数据 */
    private static class SinRay {
        Vec3d origin;
        Vec3d dir;
        float damage;
        UUID shooterId;
        int lifetime;  // ticks 剩余存活时间
        int dimId;     // 维度 ID
        int fellLevel; // 凶弹等级（用于最大生命值倍率伤害）
    }
}
