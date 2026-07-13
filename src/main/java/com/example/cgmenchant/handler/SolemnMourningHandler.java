/*
 * CGM Enchantment Addon — 庄严哀悼 (Soleme_lament)
 *
 * 子弹拖尾为黑白交替烟尘（CLOUD / SMOKE_NORMAL），
 * 命中后施加「抗性削弱」DeBuff，每级 +20% 受伤，V 级 +100%。
 */
package com.example.cgmenchant.handler;

import com.example.cgmenchant.enchant.ModEnchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.play.server.SPacketParticles;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

public class SolemnMourningHandler {

    /** 防递归标记 */
    private static boolean processingHit = false;

    private static final String BULLET_PREFIX = "com.mrcrayfish.guns.entity";
    private static final int DIALOGUE_INTERVAL = 60;
    private final Map<UUID, Long> lastDialogueTick = new HashMap<>();

    /** 子弹追踪（仅用于粒子拖尾）: EID → (等级, 粒子色) */
    private final Map<Integer, BulletTrack> bulletTracks = new HashMap<>();
    private static final int MAX_BULLET_AGE = 200; // 10 秒


    private static class BulletTrack {
        final int level;
        final boolean white; // true=CLOUD, false=SMOKE_NORMAL
        int age;
        BulletTrack(int level, boolean white) { this.level = level; this.white = white; this.age = 0; }
    }

    // ===================================================================
    //  子弹生成：注册粒子追踪 + 音效 + 台词
    // ===================================================================

    @SubscribeEvent
    public void onBulletSpawn(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity.world.isRemote) return;
        if (!entity.getClass().getName().startsWith(BULLET_PREFIX)) return;

        EntityPlayer shooter = findShooter(entity);
        if (shooter == null) return;

        int level = 0;
        for (net.minecraft.item.ItemStack stack : Arrays.asList(
                shooter.getHeldItemMainhand(), shooter.getHeldItemOffhand())) {
            if (EnchantHelper.isGun(stack)) {
                int lvl = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(
                    ModEnchantments.SOLEMN_MOURNING, stack);
                if (lvl > 0) { level = lvl; break; }
            }
        }
        if (level <= 0) return;

        // 粒子色（每发子弹交替黑白）
        boolean white = (entity.getEntityId() % 2 == 0);
        bulletTracks.put(entity.getEntityId(), new BulletTrack(level, white));

        // 音效
        SoundEvent[] altSounds = { SoundEvents.BLOCK_ANVIL_LAND, SoundEvents.BLOCK_ANVIL_PLACE };
        entity.world.playSound(null, shooter.posX, shooter.posY, shooter.posZ,
            altSounds[entity.world.rand.nextInt(altSounds.length)],
            SoundCategory.PLAYERS, 1.0f, 1.0f);

        // 台词
        long now = shooter.world.getTotalWorldTime();
        if (now - lastDialogueTick.getOrDefault(shooter.getUniqueID(), 0L) >= DIALOGUE_INTERVAL) {
            lastDialogueTick.put(shooter.getUniqueID(), now);
            String line = DialogueManager.getRandom("Soleme_lament");
            if (!line.isEmpty()) {
                String c = shooter.world.rand.nextBoolean() ? "§f" : "§8";
                CommandCGMEnchant.sendDialogue(shooter, "§f庄严哀悼§7》 " + c + line);
            }
        }
    }

    // ===================================================================
    //  世界 tick：子弹粒子拖尾 + 抗性削弱掉落粒子
    // ===================================================================

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.world.isRemote) return;

        WorldServer ws = (WorldServer) event.world;

        // ---- 抗性削弱掉落粒子（每 tick） ----
        if (event.world.getTotalWorldTime() % 2 == 0) {
            net.minecraft.potion.Potion potion = net.minecraft.potion.Potion.REGISTRY.getObject(
                new net.minecraft.util.ResourceLocation("cgmenchant", "vulnerability"));
            if (potion != null) {
                for (net.minecraft.entity.Entity e : ws.loadedEntityList) {
                    if (e instanceof net.minecraft.entity.EntityLivingBase
                        && ((net.minecraft.entity.EntityLivingBase)e).isPotionActive(potion)) {
                        // 黑白粒子从身上往下掉
                        boolean white = ws.rand.nextBoolean();
                        SPacketParticles fall = new SPacketParticles(
                            white ? EnumParticleTypes.CLOUD : EnumParticleTypes.SMOKE_LARGE,
                            true,
                            (float)(e.posX + (ws.rand.nextDouble() - 0.5) * e.width),
                            (float)(e.posY + e.height * 0.2f),
                            (float)(e.posZ + (ws.rand.nextDouble() - 0.5) * e.width),
                            0.05f, 0.08f, 0.05f, 0.02f, 2);
                        for (net.minecraft.entity.player.EntityPlayerMP p :
                                ws.getMinecraftServer().getPlayerList().getPlayers()) {
                            if (p.dimension == ws.provider.getDimension()
                                && p.getDistanceSq(e.posX, e.posY, e.posZ) < 16384) {
                                p.connection.sendPacket(fall);
                            }
                        }
                    }
                }
            }
        }

        // ---- 子弹粒子拖尾 ----
        if (bulletTracks.isEmpty()) return;
        Iterator<Map.Entry<Integer, BulletTrack>> it = bulletTracks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, BulletTrack> e = it.next();
            int eid = e.getKey();
            BulletTrack t = e.getValue();

            Entity bullet = ws.getEntityByID(eid);
            if (bullet == null || bullet.isDead) { it.remove(); continue; }
            t.age++;
            if (t.age > MAX_BULLET_AGE) { it.remove(); continue; }

            // 粒子：交替 CLOUD / SMOKE_NORMAL，发包方式同 Collateral
            SPacketParticles p = new SPacketParticles(
                t.white ? EnumParticleTypes.CLOUD : EnumParticleTypes.SMOKE_NORMAL, true,
                (float)bullet.posX, (float)(bullet.posY + bullet.height * 0.5), (float)bullet.posZ,
                0.001f, 0.001f, 0.001f, 0.0f, 1);

            for (net.minecraft.entity.player.EntityPlayerMP player :
                    ws.getMinecraftServer().getPlayerList().getPlayers()) {
                if (player.dimension == ws.provider.getDimension()
                    && player.getDistanceSq(bullet.posX, bullet.posY, bullet.posZ) < 16384) {
                    player.connection.sendPacket(p);
                }
            }
        }
    }

    // ===================================================================
    //  命中：施加「抗性削弱」DeBuff
    // ===================================================================

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (processingHit) return;
        EntityLivingBase target = event.getEntityLiving();
        if (target.world.isRemote) return;

        DamageSource source = event.getSource();
        if (source == null) return;

        // CGM 子弹的 DamageSource 拿不到射手，需要从子弹实体反射
        EntityPlayer shooter = null;
        Entity immediate = source.getImmediateSource();
        if (immediate != null && immediate.getClass().getName().startsWith(BULLET_PREFIX)) {
            shooter = findShooter(immediate);
        }
        if (shooter == null) {
            shooter = EnchantHelper.getShooterFromDamageSource(source);
        }
        if (shooter == null) return;
        if (shooter == target) return; // 防自伤

        int level = 0;
        for (net.minecraft.item.ItemStack stack : Arrays.asList(
                shooter.getHeldItemMainhand(), shooter.getHeldItemOffhand())) {
            if (EnchantHelper.isGun(stack)) {
                int lvl = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(
                    ModEnchantments.SOLEMN_MOURNING, stack);
                if (lvl > 0) { level = lvl; break; }
            }
        }
        if (level <= 0) return;

        // ==== 必中效果：额外魔法伤害，绕过凋灵远程免疫 / 末影人闪避 ====
        processingHit = true;
        target.attackEntityFrom(net.minecraft.util.DamageSource.causeIndirectMagicDamage(shooter, shooter), level * 2.0f);
        processingHit = false;

        // ==== 增伤：每级 +0.5 倍，V 级 +2.5 倍（共 3.5 倍）====
        float bonus = level * 0.5f;
        event.setAmount(event.getAmount() * (1.0f + bonus));

        // 施加 DeBuff 药水（固定12秒，不可牛奶解除）
        net.minecraft.potion.Potion potion = net.minecraft.potion.Potion.REGISTRY.getObject(
            new net.minecraft.util.ResourceLocation("cgmenchant", "vulnerability"));
        if (potion != null) {
            target.addPotionEffect(
                new com.example.cgmenchant.potion.VulnerabilityEffect(potion, 240, 0));
        }

        // 命中黑烟粒子
        if (target.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) target.world;
            SPacketParticles hit = new SPacketParticles(
                EnumParticleTypes.SMOKE_NORMAL, true,
                (float)target.posX, (float)(target.posY + target.height * 0.5), (float)target.posZ,
                0.3f, 0.3f, 0.3f, 0.05f, 8);
            for (net.minecraft.entity.player.EntityPlayerMP p :
                    ws.getMinecraftServer().getPlayerList().getPlayers()) {
                if (p.dimension == ws.provider.getDimension()
                    && p.getDistanceSq(target.posX, target.posY, target.posZ) < 16384) {
                    p.connection.sendPacket(hit);
                }
            }
        }
    }

    // ===================================================================
    //  工具
    // ===================================================================

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
