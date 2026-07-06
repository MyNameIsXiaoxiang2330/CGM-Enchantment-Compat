package com.example.cgmenchant.handler;

import com.example.cgmenchant.enchant.ModEnchantments;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class GunStateHandler {

    private static final String AMMO_KEY = "AmmoCount";
    private static final String PREV_AMMO_KEY = "cgm_prev_ammo";
    private static final String QH_TICK_KEY = "qh_tick";

    @SubscribeEvent(priority = net.minecraftforge.fml.common.eventhandler.EventPriority.HIGHEST)
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;
        ItemStack main = player.getHeldItemMainhand();
        if (!EnchantHelper.isGun(main)) return;

        NBTTagCompound tag = main.getTagCompound();
        if (tag == null) {
            main.setTagCompound(new NBTTagCompound());
            tag = main.getTagCompound();
        }
        if (!tag.hasKey(AMMO_KEY)) return;

        // 初始化 MaxAmmo NBT（给 CoreMod ASM 用）
        if (!tag.hasKey("MaxAmmo")) {
            tag.setInteger("MaxAmmo", getGunMaxAmmo(main));
        }

        // ========== Phase.START: 在 CGM 运行前设置好 Gun.general.maxAmmo ==========
        if (event.phase == TickEvent.Phase.START) {
            int ocLevel = EnchantHelper.getLevel(main, ModEnchantments.OVER_CAPACITY);
            if (ocLevel > 0 && tag.hasKey("oc_base")) {
                int base = tag.getInteger("oc_base");
                int boosted = base + (int)(base * 0.5 * ocLevel);
                setGunMaxAmmo(main, boosted);
                tag.setInteger("MaxAmmo", boosted);
            } else if (tag.hasKey("oc_base")) {
                // 非 OC 枪但 Gun 被改过 → 恢复
                int base = tag.getInteger("oc_base");
                setGunMaxAmmo(main, base);
                tag.setInteger("MaxAmmo", base);
                tag.removeTag("oc_base");
            } else {
                // 普通枪：确保 Gun.general 是原始值
                int curMax = getGunMaxAmmo(main);
                if (curMax != tag.getInteger("MaxAmmo")) {
                    setGunMaxAmmo(main, tag.getInteger("MaxAmmo"));
                }
            }
            return;
        }

        // ========== Phase.END: 超容量/弹药回收/熟练手逻辑 ==========
        if (event.phase != TickEvent.Phase.END) return;

        // ==== 超容量 (Over Capacity) — 记录 oc_base ====
        int ocLevel = EnchantHelper.getLevel(main, ModEnchantments.OVER_CAPACITY);
        if (ocLevel > 0) {
            if (!tag.hasKey("oc_base")) {
                tag.setInteger("oc_base", getGunMaxAmmo(main));
            }
            // START phase 已经设好了 Gun.general，这里只处理弹药上限
            int curAmmo = tag.getInteger(AMMO_KEY);
            int boosted = tag.getInteger("MaxAmmo");
            if (curAmmo > boosted) tag.setInteger(AMMO_KEY, boosted);
        } else if (tag.hasKey("oc_base")) {
            // START phase 已经恢复了
        }

        // ==== 弹药回收 (Reclaimed) ====
        int rcLevel = EnchantHelper.getLevel(main, ModEnchantments.RECLAIMED);
        if (rcLevel > 0) {
            int prev = tag.getInteger(PREV_AMMO_KEY);
            int cur = tag.getInteger(AMMO_KEY);
            if (prev > 0 && cur < prev) {
                float chance;
                if (rcLevel == 1) chance = 1.0f / 3.0f;
                else if (rcLevel == 2) chance = 0.5f;
                else chance = 7.0f / 8.0f;
                if (player.getRNG().nextFloat() < chance) {
                    int maxAmmo = tag.getInteger("MaxAmmo");
                    if (cur < maxAmmo) tag.setInteger(AMMO_KEY, Math.min(maxAmmo, cur + 1));
                }
            }
            tag.setInteger(PREV_AMMO_KEY, cur);
        } else {
            tag.removeTag(PREV_AMMO_KEY);
        }

        // ==== 熟练手 (Quick Hands) ====
        int qhLevel = EnchantHelper.getLevel(main, ModEnchantments.QUICK_HANDS);
        if (qhLevel > 0) {
            if (isReloading(player)) {
                int cur = tag.getInteger(AMMO_KEY);
                int max = tag.getInteger("MaxAmmo");
                if (cur < max) {
                    int interval = Math.max(1, 10 - qhLevel * 3);
                    int lastTick = tag.getInteger(QH_TICK_KEY);
                    int now = player.ticksExisted;
                    if (lastTick == 0) tag.setInteger(QH_TICK_KEY, now);
                    else if (now - lastTick >= interval) {
                        tag.setInteger(AMMO_KEY, Math.min(max, cur + 1));
                        tag.setInteger(QH_TICK_KEY, now);
                    }
                }
            } else {
                tag.removeTag(QH_TICK_KEY);
            }
        } else {
            tag.removeTag(QH_TICK_KEY);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean isReloading(EntityPlayer player) {
        try {
            Class<?> cls = Class.forName("com.mrcrayfish.guns.event.CommonEvents");
            Object f = cls.getDeclaredField("RELOADING").get(null);
            if (f instanceof net.minecraft.network.datasync.DataParameter) {
                net.minecraft.network.datasync.DataParameter<Boolean> param =
                    (net.minecraft.network.datasync.DataParameter<Boolean>) f;
                return player.getDataManager().get(param);
            }
        } catch (Exception ignored) {}
        return false;
    }

    private int getGunMaxAmmo(ItemStack stack) {
        try {
            Object gun = stack.getItem().getClass().getMethod("getGun").invoke(stack.getItem());
            Object general = gun.getClass().getField("general").get(gun);
            return general.getClass().getField("maxAmmo").getInt(general);
        } catch (Exception e) { return 30; }
    }

    private void setGunMaxAmmo(ItemStack stack, int value) {
        try {
            Object gun = stack.getItem().getClass().getMethod("getGun").invoke(stack.getItem());
            Object general = gun.getClass().getField("general").get(gun);
            general.getClass().getField("maxAmmo").setInt(general, value);
        } catch (Exception ignored) {}
    }
}
