package com.example.cgmenchant.handler;

import com.example.cgmenchant.enchant.ModEnchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

public class HighExplosiveHandler {

    // 追踪高爆弹火箭/榴弹 (子弹ID → (等级, 世界ID, 最后位置))
    private final Map<Integer, int[]> heRockets = new HashMap<>();
    private final Map<Integer, BlockPos> rocketPos = new HashMap<>();

    @SubscribeEvent
    public void onBulletSpawn(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (event.getWorld().isRemote) return;
        if (!entity.getClass().getName().startsWith("com.mrcrayfish.guns.entity")) return;

        // 只追踪爆炸物
        String cn = entity.getClass().getName().toLowerCase();
        if (!cn.contains("missile") && !cn.contains("grenade")) return;

        EntityPlayer shooter = findShooter(entity);
        if (shooter == null) return;

        ItemStack main = shooter.getHeldItemMainhand();
        if (!EnchantHelper.isGun(main)) {
            main = shooter.getHeldItemOffhand();
            if (!EnchantHelper.isGun(main)) return;
        }

        int heLevel = EnchantHelper.getLevel(main, ModEnchantments.HIGH_EXPLOSIVE);
        if (heLevel <= 0) return;

        int eid = entity.getEntityId();
        heRockets.put(eid, new int[]{heLevel, event.getWorld().provider.getDimension()});
        rocketPos.put(eid, new BlockPos(entity.posX, entity.posY, entity.posZ));
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (heRockets.isEmpty()) return;
        if (event.world.isRemote) return;

        Iterator<Map.Entry<Integer, int[]>> it = heRockets.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, int[]> e = it.next();
            int eid = e.getKey();
            int[] data = e.getValue();
            int level = data[0];
            int dimId = data[1];

            if (dimId != event.world.provider.getDimension()) continue;

            Entity bullet = event.world.getEntityByID(eid);
            if (bullet == null || bullet.isDead) {
                // 火箭消失 → 生成大爆炸
                BlockPos pos = rocketPos.getOrDefault(eid, new BlockPos(bullet != null ? bullet.posX : 0, 0, 0));
                if (bullet != null && !bullet.isDead) {
                    pos = new BlockPos(bullet.posX, bullet.posY, bullet.posZ);
                }
                float size = 2.0f + 1.0f * level;
                event.world.newExplosion(null, pos.getX(), pos.getY(), pos.getZ(), size, false, false);
                it.remove();
                rocketPos.remove(eid);
                continue;
            }

            // 更新位置
            rocketPos.put(eid, new BlockPos(bullet.posX, bullet.posY, bullet.posZ));
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        EntityLivingBase target = event.getEntityLiving();
        if (target.world.isRemote) return;

        EntityPlayer shooter = EnchantHelper.getShooterFromDamageSource(event.getSource());
        if (shooter == null) return;

        ItemStack gun = shooter.getHeldItemMainhand();
        if (!EnchantHelper.isGun(gun)) {
            gun = shooter.getHeldItemOffhand();
            if (!EnchantHelper.isGun(gun)) return;
        }

        int heLevel = EnchantHelper.getLevel(gun, ModEnchantments.HIGH_EXPLOSIVE);
        if (heLevel <= 0) return;

        String gunId = gun.getItem().getRegistryName().toString();
        boolean isShotgun = gunId.contains("shotgun");
        boolean isExplosiveLauncher = gunId.contains("grenade_launcher") || gunId.contains("bazooka");

        float bonusDamage = isExplosiveLauncher ? 5.0f * heLevel : 2.0f * heLevel;

        // 火箭/榴弹命中实体时额外范围伤害
        if (isExplosiveLauncher) {
            World world = target.world;
            float range = 3.0f + 1.0f * heLevel;
            for (EntityLivingBase nearby : world.getEntitiesWithinAABB(EntityLivingBase.class,
                    target.getEntityBoundingBox().grow(range, range, range))) {
                if (nearby == shooter || nearby == target) continue;
                nearby.attackEntityFrom(DamageSource.causeIndirectDamage(target, shooter), bonusDamage * 0.5f);
            }
        }

        // 普通子弹/霰弹：小幅爆炸粒子
        if (!isExplosiveLauncher) {
            float smallSize = isShotgun ? 0.5f : 0.8f;
            target.world.newExplosion(target, target.posX, target.posY, target.posZ, smallSize, false, false);
        }

        event.setAmount(event.getAmount() + bonusDamage);
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
