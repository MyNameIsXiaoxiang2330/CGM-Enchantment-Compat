package com.example.cgmenchant.handler;

import com.example.cgmenchant.enchant.ModEnchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class CollateralHandler {

    // 服务端：待处理的射线
    private final Queue<Rayan> pendingRays = new LinkedList<>();
    // 客户端：子弹追踪（用于粒子）
    private final java.util.Map<Integer, java.util.Set<Integer>> collBullets = new java.util.HashMap<>();

    private static final int MAX_RANGE = 120;
    private static final double STEP = 0.5;

    private static class Rayan {
        final EntityPlayer shooter;
        final Vec3d origin;
        final Vec3d direction;
        final float damage;
        final int level;
        final int worldId;
        final World world;
        final int bulletId;

        Rayan(EntityPlayer shooter, Vec3d origin, Vec3d direction, float damage, int level, World world, int bulletId) {
            this.shooter = shooter;
            this.origin = origin;
            this.direction = direction;
            this.damage = damage;
            this.level = level;
            this.world = world;
            this.worldId = world.provider.getDimension();
            this.bulletId = bulletId;
        }
    }

    @SubscribeEvent
    public void onBulletSpawn(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (!entity.getClass().getName().startsWith("com.mrcrayfish.guns.entity")) return;
        if (isExplosive(entity)) return;

        boolean isRemote = event.getWorld().isRemote;

        EntityPlayer shooter = findShooter(entity);
        if (shooter == null) return;

        ItemStack main = shooter.getHeldItemMainhand();
        if (!EnchantHelper.isGun(main)) {
            main = shooter.getHeldItemOffhand();
            if (!EnchantHelper.isGun(main)) return;
        }

        int collLevel = EnchantHelper.getLevel(main, ModEnchantments.COLLATERAL);
        if (collLevel <= 0) return;

        // 获取子弹伤害
        float baseDamage = 5.0f;
        try {
            baseDamage = (float) entity.getClass().getMethod("getDamage").invoke(entity);
        } catch (Exception ignored) {}

        // 计算方向（用子弹初速度方向）
        Vec3d dir = new Vec3d(entity.motionX, entity.motionY, entity.motionZ).normalize();
        if (dir.lengthVector() < 0.01) {
            // 没速度时用射手视线方向
            dir = shooter.getLookVec();
        }
        Vec3d origin = new Vec3d(entity.posX, entity.posY + shooter.getEyeHeight() * 0.5, entity.posZ);

        if (isRemote) {
            // 客户端粒子由 onWorldTick 统一处理
        } else {
            // 服务端：加入射线队列，下一 tick 执行
            pendingRays.add(new Rayan(shooter, origin, dir, baseDamage, collLevel, event.getWorld(), entity.getEntityId()));
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        World world = event.world;

        // 客户端：扫描 CGM 子弹实体喷粒子
        if (world.isRemote) {
            for (Entity e : (List<Entity>) world.loadedEntityList) {
                if (e.isDead) continue;
                String cn = e.getClass().getName();
                if (!cn.startsWith("com.mrcrayfish.guns.entity")) continue;
                if (cn.contains("missile") || cn.contains("grenade")) continue;
                // 喷大号白色烟雾粒子，更容易看见
                world.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    e.posX, e.posY + 0.3, e.posZ,
                    0, 0, 0, 0);
            }
            return;
        }

        // 服务端：处理射线
        if (pendingRays.isEmpty()) return;

        while (!pendingRays.isEmpty()) {
            Rayan ray = pendingRays.poll();
            if (ray.world.provider.getDimension() != ray.worldId) continue;

            // 射线前进，检测实体
            Set<EntityLivingBase> hitEntities = new HashSet<>();
            Vec3d current = ray.origin;

            for (double d = 0; d < MAX_RANGE; d += STEP) {
                current = ray.origin.addVector(ray.direction.x * d, ray.direction.y * d, ray.direction.z * d);
                BlockPos bp = new BlockPos(current);

                // 检测方块碰撞
                if (!world.isAirBlock(bp) && world.getBlockState(bp).isFullBlock()) break;

                // 检测实体
                AxisAlignedBB box = new AxisAlignedBB(current.x - 0.5, current.y - 0.5, current.z - 0.5,
                                                       current.x + 0.5, current.y + 0.5, current.z + 0.5);
                List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class, box);

                for (EntityLivingBase target : targets) {
                    if (hitEntities.contains(target)) continue;
                    if (target == ray.shooter) continue;
                    if (target.isDead) continue;

                    float bonus = 1.0f + 0.2f * ray.level;
                    target.attackEntityFrom(DamageSource.causeIndirectDamage(ray.shooter, ray.shooter), ray.damage * bonus);
                    hitEntities.add(target);
                }
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
}
