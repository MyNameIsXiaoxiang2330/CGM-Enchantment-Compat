/*
 * CGM Enchantment Addon — 凶弹追踪系统（Miss惩罚 + 射击记录）
 */
package com.example.cgmenchant.handler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.WorldServer;

import java.util.*;

public class FellbulletTracker {

    /** 射击记录：玩家 → (等级, 过期世界tick) */
    private static final Map<UUID, FellShot> activeShots = new HashMap<>();

    /** 记录一次开枪 */
    public static void onFired(UUID playerId, int level, long worldTime) {
        long graceTicks = level * 100L; // 等级×5秒 (20 ticks/sec × 5 = 100)
        activeShots.put(playerId, new FellShot(level, worldTime + graceTicks));
    }

    /** 记录一次击杀（取消惩罚） */
    public static void onKill(UUID playerId) {
        activeShots.remove(playerId);
    }

    /** 每 tick 检查过期射击（返回是否还有活跃记录） */
    public static boolean tick(WorldServer ws) {
        if (activeShots.isEmpty()) return false;
        long now = ws.getTotalWorldTime();

        Iterator<Map.Entry<UUID, FellShot>> it = activeShots.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, FellShot> e = it.next();
            FellShot shot = e.getValue();
            if (now >= shot.expiryWorldTick) {
                // 过期 → 施加虚弱
                EntityPlayer player = ws.getMinecraftServer().getPlayerList().getPlayerByUUID(e.getKey());
                if (player != null && player.isEntityAlive()) {
                    int amp = shot.level >= 3 ? 1 : 0; // 0=虚弱I, 1=虚弱II
                    player.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 600, amp)); // 30秒
                }
                it.remove();
            }
        }
        return !activeShots.isEmpty();
    }

    private static class FellShot {
        final int level;
        final long expiryWorldTick;

        FellShot(int level, long expiryWorldTick) {
            this.level = level;
            this.expiryWorldTick = expiryWorldTick;
        }
    }
}
