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
 * 凶弹-贯霰形 (FELLBULLET Piercer) 事件处理器。
 *
 * 击杀时在目标背后生成同心环（朝向弹道方向），蓄力完成后
 * 沿弹道方向锥形散射，伤害按枪械原始值计算。
 * 霰弹枪每发凶弹产生额外子弹出。
 */
public class FellbulletPiercerHandler {

    private static boolean processingPiercer = false;

    private final Map<UUID, PiercerCharge> charges = new HashMap<>();
    private final List<PiercerRay> activeRays = new ArrayList<>();

    // ===================================================================
    //  击杀触发
    // ===================================================================

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (processingPiercer) return;

        EntityLivingBase target = event.getEntityLiving();
        if (target.world.isRemote) return;

        EntityPlayer shooter = EnchantHelper.getShooterFromDamageSource(event.getSource());
        if (shooter == null) return;

        ItemStack gun = findGunWithPiercer(shooter);
        if (gun == null) return;

        int level = EnchantHelper.getLevel(gun, ModEnchantments.FELLBULLET_PIERCER);
        if (level <= 0) return;

        // 排除爆炸物发射器
        String gunId = gun.getItem().getRegistryName().toString().toLowerCase();
        if (gunId.contains("bazooka") || gunId.contains("grenade_launcher")) return;

        FellbulletTracker.onKill(shooter.getUniqueID());

        // 同一玩家不能叠加蓄力
        UUID uid = shooter.getUniqueID();
        if (charges.containsKey(uid)) return;

        // 计算弹道方向
        Vec3d shooterPos = new Vec3d(shooter.posX, shooter.posY + shooter.getEyeHeight(), shooter.posZ);
        Vec3d targetPos = new Vec3d(target.posX, target.posY + target.height * 0.5, target.posZ);
        Vec3d bulletDir = targetPos.subtract(shooterPos).normalize();

        // 环的位置：目标背后 1.5 格 + 头顶上方 1.5 格
        double cx = targetPos.x + bulletDir.x * 1.5;
        double cy = targetPos.y + 1.5 + bulletDir.y * 1.5;
        double cz = targetPos.z + bulletDir.z * 1.5;

        // 获取枪械基础伤害
        float baseDamage = getGunBaseDamage(gun);
        if (baseDamage <= 0) baseDamage = 5.0f;

        boolean isShotgun = gunId.contains("shotgun");

        CommandCGMEnchant.sendDialogue(shooter, CommandCGMEnchant.getRandomDialogue());

        PiercerCharge charge = new PiercerCharge(
            target.world, uid,
            cx, cy, cz, level,
            bulletDir, baseDamage, isShotgun
        );
        charges.put(uid, charge);
    }

    // ===================================================================
    //  世界 tick
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
                "[cgmenchant perf] FELLBULLET Piercer: {} active rays", activeRays.size());
        }

        // ---- 蓄力 / 逐波散射 ----
        Iterator<Map.Entry<UUID, PiercerCharge>> cit = charges.entrySet().iterator();
        while (cit.hasNext()) {
            Map.Entry<UUID, PiercerCharge> entry = cit.next();
            PiercerCharge ch = entry.getValue();

            if (!ch.world.equals(event.world)) continue;

            if (!ch.chargeComplete) {
                spawn3DCircle(ws, ch);
                ch.elapsedTicks++;
                if (ch.elapsedTicks >= ch.totalTicks) {
                    ws.playSound(null, ch.x, ch.y, ch.z,
                        SoundEvents.ENTITY_FIREWORK_LAUNCH, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    ch.chargeComplete = true;
                    ch.waveCooldown = 5;
                }
            } else {
                if (ch.waveCooldown > 0) {
                    ch.waveCooldown--;
                } else if (ch.wavesSpawned < ch.level) {
                    spawnConeWave(ws, ch);
                    ch.wavesSpawned++;
                    ch.waveCooldown = 3;
                } else {
                    cit.remove();
                }
            }
        }

        // ---- 射线更新 ----
        Iterator<PiercerRay> rit = activeRays.iterator();
        while (rit.hasNext()) {
            PiercerRay ray = rit.next();
            if (ray.dimId != dimId) continue;

            ray.lifetime--;
            if (ray.lifetime <= 0) {
                rit.remove();
                continue;
            }

            advanceRay(ray, ws);
            if (ray.lifetime <= 0) rit.remove();
        }
    }

    // ===================================================================
    //  3D 空间同心环（垂直于弹道方向）
    // ===================================================================

    /** EGO 魔法阵动画（3D 空间）：交错旋转环 + 十字线 + 光点 */
    private void spawn3DCircle(WorldServer ws, PiercerCharge ch) {
        float progress = (float)ch.elapsedTicks / ch.totalTicks;
        float t = 1.0f - progress;
        float rot = (float)(progress * Math.PI * 2);
        Vec3d center = new Vec3d(ch.x, ch.y, ch.z);
        Vec3d[] basis = ParticleUtils.getBasis(ch.dir);
        Vec3d right = basis[0], forward = basis[1];

        // -- 4 层交错旋转环 --
        float[] radii = {2.0f * t, 1.5f * t, 1.0f * t, 0.5f * t};
        for (int r = 0; r < 4; r++) {
            float cr = radii[r];
            if (cr < 0.05f) continue;
            int dir = (r % 2 == 0) ? 1 : -1;
            for (int i = 0; i < 8; i++) {
                double a = 2.0 * Math.PI * i / 8 + rot * dir;
                double px = center.x + right.x * Math.cos(a) * cr + forward.x * Math.sin(a) * cr;
                double py = center.y + right.y * Math.cos(a) * cr + forward.y * Math.sin(a) * cr;
                double pz = center.z + right.z * Math.cos(a) * cr + forward.z * Math.sin(a) * cr;
                ParticleUtils.spawnParticleAt(ws, EnumParticleTypes.REDSTONE, (float)px, (float)py, (float)pz);
            }
        }

        // -- 旋转十字线 --
        if (t > 0.1f) {
            for (int d = 0; d < 4; d++) {
                double angle = rot + d * Math.PI / 2;
                for (float s = 0.3f; s < 2.0f * t; s += 0.7f) {
                    double px = center.x + (right.x * Math.cos(angle) + forward.x * Math.sin(angle)) * s;
                    double py = center.y + (right.y * Math.cos(angle) + forward.y * Math.sin(angle)) * s;
                    double pz = center.z + (right.z * Math.cos(angle) + forward.z * Math.sin(angle)) * s;
                    ParticleUtils.spawnParticleAt(ws, EnumParticleTypes.REDSTONE, (float)px, (float)py, (float)pz);
                }
            }
        }

    }

    // ===================================================================
    //  锥形散射
    // ===================================================================

    private void spawnConeWave(WorldServer ws, PiercerCharge ch) {
        Random rand = ws.rand;
        int bulletCount = 8 + rand.nextInt(17);

        // 霰弹枪：每发凶弹变为额外子弹出（乘 2~3 倍）
        if (ch.isShotgun) {
            bulletCount *= 2 + rand.nextInt(2);
        }

        // TNT 爆炸声（每波一次）
        ws.playSound(null, ch.x, ch.y, ch.z,
            SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS,
            1.0f, 0.9f + rand.nextFloat() * 0.2f);

        // 黑雾扬起
        ParticleUtils.spawnBurst(ws, ch.x, ch.y, ch.z,
            EnumParticleTypes.SMOKE_NORMAL, 0.6f, 20, 0.08f);

        // 弹道方向的角度
        double baseYaw = Math.atan2(ch.dir.z, ch.dir.x);
        double basePitch = Math.asin(Math.max(-1, Math.min(1, ch.dir.y)));
        double coneAngle = Math.toRadians(20); // ±20° 锥形

        for (int i = 0; i < bulletCount; i++) {
            double yaw = baseYaw + (rand.nextDouble() - 0.5) * coneAngle * 2;
            double pitch = basePitch + (rand.nextDouble() - 0.5) * coneAngle;

            Vec3d rayDir = new Vec3d(
                Math.cos(yaw) * Math.cos(pitch),
                Math.sin(pitch),
                Math.sin(yaw) * Math.cos(pitch)
            ).normalize();

            PiercerRay ray = new PiercerRay();
            ray.origin = new Vec3d(ch.x, ch.y, ch.z);
            ray.dir = rayDir;
            ray.damage = 1.0f;
            ray.fellLevel = ch.level;
            ray.shooterId = ch.shooterId;
            ray.lifetime = 120;
            ray.dimId = ws.provider.getDimension();
            ray.hitEntities = new HashSet<>();
            activeRays.add(ray);
        }
    }

    // ===================================================================
    //  射线前进（与地霰形共用逻辑）
    // ===================================================================

    private void advanceRay(PiercerRay ray, WorldServer ws) {
        Vec3d next = ray.origin.addVector(ray.dir.x * 1.5, ray.dir.y * 1.5, ray.dir.z * 1.5);
        BlockPos bp = new BlockPos(next);

        if (!ws.isAirBlock(bp) && ws.getBlockState(bp).isFullBlock()) {
            ray.lifetime = 0;
            return;
        }

        // 弹道烟雾（黑色浓烟）
        SPacketParticles trail = new SPacketParticles(
            EnumParticleTypes.SMOKE_LARGE, false,
            (float)next.x, (float)next.y + 0.05f, (float)next.z,
            0.04f, 0.04f, 0.04f, 0.0f, 2);
        sendToNearby(ws, next.x, next.y, next.z, trail);

        AxisAlignedBB box = new AxisAlignedBB(
            next.x - 0.5, next.y - 0.5, next.z - 0.5,
            next.x + 0.5, next.y + 0.5, next.z + 0.5);

        for (EntityLivingBase target : ws.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            int eid = target.getEntityId();
            if (ray.hitEntities.contains(eid)) continue;
            if (target.getUniqueID().equals(ray.shooterId)) continue;
            if (target.isDead) continue;

            EntityPlayer shooter = findPlayer(ws, ray.shooterId);
            processingPiercer = true;
            float fellDmg = target.getMaxHealth() * ray.fellLevel;
            if (fellDmg > 30f) fellDmg *= 0.5f;
            target.attackEntityFrom(
                shooter != null
                    ? DamageSource.causeIndirectDamage(shooter, shooter)
                    : DamageSource.GENERIC,
                fellDmg);
            processingPiercer = false;

            ray.hitEntities.add(eid);

            // 贯穿命中粒子
            SPacketParticles hit = new SPacketParticles(
                EnumParticleTypes.CRIT, false,
                (float)next.x, (float)next.y + 0.5f, (float)next.z,
                0.1f, 0.1f, 0.1f, 0.2f, 5);
            sendToNearby(ws, next.x, next.y, next.z, hit);
        }
        ray.origin = next;
    }

    // ===================================================================
    //  工具方法
    // ===================================================================

    private ItemStack findGunWithPiercer(EntityPlayer player) {
        ItemStack main = player.getHeldItemMainhand();
        if (EnchantHelper.isGun(main) && EnchantHelper.getLevel(main, ModEnchantments.FELLBULLET_PIERCER) > 0)
            return main;
        ItemStack off = player.getHeldItemOffhand();
        if (EnchantHelper.isGun(off) && EnchantHelper.getLevel(off, ModEnchantments.FELLBULLET_PIERCER) > 0)
            return off;
        return null;
    }

    /** 从 CGM Gun 对象反射读取基础伤害 */
    private float getGunBaseDamage(ItemStack gun) {
        try {
            Object gunObj = gun.getItem().getClass().getMethod("getGun").invoke(gun.getItem());
            Object general = gunObj.getClass().getField("general").get(gunObj);
            int damage = general.getClass().getField("damage").getInt(general);
            return Math.max(1, damage);
        } catch (Exception e) {
            return 5.0f;
        }
    }

    private EntityPlayer findPlayer(World world, UUID uuid) {
        for (EntityPlayer p : world.playerEntities) {
            if (p.getUniqueID().equals(uuid)) return p;
        }
        return null;
    }

    private void sendToNearby(WorldServer ws, double x, double y, double z, SPacketParticles packet) {
        for (EntityPlayerMP player : ws.getMinecraftServer().getPlayerList().getPlayers()) {
            if (player.dimension == ws.provider.getDimension()
                && player.getDistanceSq(x, y, z) < 16384) {
                player.connection.sendPacket(packet);
            }
        }
    }

    // ===================================================================
    //  内部类
    // ===================================================================

    private static class PiercerCharge {
        final World world;
        final UUID shooterId;
        final double x, y, z;
        final int level;
        final int totalTicks;
        final Vec3d dir;
        final float baseDamage;
        final boolean isShotgun;
        int elapsedTicks;
        boolean chargeComplete;
        int wavesSpawned;
        int waveCooldown;

        PiercerCharge(World world, UUID shooterId,
                      double x, double y, double z, int level,
                      Vec3d dir, float baseDamage, boolean isShotgun) {
            this.world = world;
            this.shooterId = shooterId;
            this.x = x; this.y = y; this.z = z;
            this.level = level;
            this.totalTicks = level * 13;
            this.dir = dir;
            this.baseDamage = baseDamage;
            this.isShotgun = isShotgun;
            this.elapsedTicks = 0;
            this.chargeComplete = false;
            this.wavesSpawned = 0;
            this.waveCooldown = 0;
        }
    }

    private static class PiercerRay {
        Vec3d origin;
        Vec3d dir;
        float damage;
        UUID shooterId;
        int lifetime;
        int dimId;
        int fellLevel; // 凶弹等级
        Set<Integer> hitEntities; // 已贯穿的实体 ID
    }
}
