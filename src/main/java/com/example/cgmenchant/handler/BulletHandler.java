package com.example.cgmenchant.handler;

import com.example.cgmenchant.enchant.ModEnchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

public class BulletHandler {

    private static final String BULLET_PREFIX = "com.mrcrayfish.guns.entity";

    // 纵火狂子弹追踪: 子弹ID → 最后位置 + 运动方向
    private final Map<Integer, BlockPos> fireTrail = new HashMap<>();
    private final Map<Integer, Vec3d> fireMotion = new HashMap<>();
    private final Set<Integer> fireAlive = new HashSet<>();
    private final Map<Integer, Boolean> fireExplosive = new HashMap<>();

    @SubscribeEvent
    public void onBulletSpawn(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        World world = entity.world;
        if (world.isRemote) return;

        if (!entity.getClass().getName().startsWith(BULLET_PREFIX)) return;

        EntityPlayer shooter = findShooter(entity);
        if (shooter == null) return;

        ItemStack gun = shooter.getHeldItemMainhand();
        if (!EnchantHelper.isGun(gun)) {
            gun = shooter.getHeldItemOffhand();
            if (!EnchantHelper.isGun(gun)) return;
        }

        int eid = entity.getEntityId();

        // 加速器 (Accelerator): 提高弹速
        int accelLevel = EnchantHelper.getLevel(gun, ModEnchantments.ACCELERATOR);
        if (accelLevel > 0) {
            double mul = 1.0 + 0.3 * accelLevel;
            entity.motionX *= mul;
            entity.motionY *= mul;
            entity.motionZ *= mul;
        }

        // 高爆弹 (HE): 霰弹射程加成
        int heLevel = EnchantHelper.getLevel(gun, ModEnchantments.HIGH_EXPLOSIVE);
        if (heLevel > 0 && isShotgun(gun)) {
            double rangeMul = 1.0 + 0.2 * heLevel;
            entity.motionX *= rangeMul;
            entity.motionY *= rangeMul;
            entity.motionZ *= rangeMul;
        }

        // 纵火者 (Fire Starter): 追踪子弹（含爆炸物）
        int fireLevel = EnchantHelper.getLevel(gun, ModEnchantments.FIRE_STARTER);
        if (fireLevel > 0) {
            entity.setFire(100);
            entity.getEntityData().setBoolean("cgmenchant_fire", true);
            BlockPos pos = new BlockPos(entity.posX, entity.posY, entity.posZ);
            Vec3d motion = new Vec3d(entity.motionX, entity.motionY, entity.motionZ);
            fireTrail.put(eid, pos);
            fireMotion.put(eid, motion);
            fireAlive.add(eid);
            // 标记是否为爆炸物
            String cn = entity.getClass().getName().toLowerCase();
            fireExplosive.put(eid, cn.contains("missile") || cn.contains("grenade"));
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (fireTrail.isEmpty()) return;
        if (event.world.isRemote) return;

        Iterator<Map.Entry<Integer, BlockPos>> it = fireTrail.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, BlockPos> entry = it.next();
            int eid = entry.getKey();
            BlockPos lastPos = entry.getValue();
            Vec3d lastMotion = fireMotion.containsKey(eid) ? fireMotion.get(eid) : new Vec3d(0, 0, 0);
            boolean isExplosive = fireExplosive.getOrDefault(eid, false);

            Entity bullet = event.world.getEntityByID(eid);
            if (bullet == null || bullet.isDead) {
                if (fireAlive.contains(eid)) {
                    // 用运动方向推算子弹消失位置：最后位置 + 运动方向×2格
                    Vec3d dir = lastMotion.normalize();
                    BlockPos impactPos = lastPos.add(
                        (int)(dir.x * 2), (int)(dir.y * 2), (int)(dir.z * 2));

                    if (isExplosive) {
                        // 纵火狂爆炸：大范围火焰扩散
                        event.world.newExplosion(null,
                            impactPos.getX(), impactPos.getY(), impactPos.getZ(),
                            4.0f, true, false);
                    } else {
                        if (event.world.isAirBlock(impactPos))
                            event.world.setBlockState(impactPos, Blocks.FIRE.getDefaultState(), 3);
                        BlockPos above = impactPos.up();
                        if (event.world.isAirBlock(above))
                            event.world.setBlockState(above, Blocks.FIRE.getDefaultState(), 3);
                    }
                }
                it.remove();
                fireAlive.remove(eid);
                fireExplosive.remove(eid);
                fireMotion.remove(eid);
                continue;
            }

            // 更新位置和运动
            entry.setValue(new BlockPos(bullet.posX, bullet.posY, bullet.posZ));
            fireMotion.put(eid, new Vec3d(bullet.motionX, bullet.motionY, bullet.motionZ));
        }
    }

    private boolean isShotgun(ItemStack gun) {
        String id = gun.getItem().getRegistryName().toString();
        return id.contains("shotgun");
    }

    private EntityPlayer findShooter(Entity bullet) {
        // 方法1: CGM 标准 getShooter()
        try {
            Object s = bullet.getClass().getMethod("getShooter").invoke(bullet);
            if (s instanceof EntityPlayer) return (EntityPlayer) s;
        } catch (Exception ignored) {}
        // 方法2: getOwner()
        try {
            Object s = bullet.getClass().getMethod("getOwner").invoke(bullet);
            if (s instanceof EntityPlayer) return (EntityPlayer) s;
        } catch (Exception ignored) {}
        // 方法3: 遍历玩家找最近的可能射手
        try {
            double bestDist = Double.MAX_VALUE;
            EntityPlayer best = null;
            for (EntityPlayer player : (java.util.List<EntityPlayer>)bullet.world.playerEntities) {
                double dist = bullet.getDistance(player.posX, player.posY, player.posZ);
                if (dist < bestDist && dist < 10) {
                    bestDist = dist;
                    best = player;
                }
            }
            if (best != null) return best;
        } catch (Exception ignored) {}
        return null;
    }
}
