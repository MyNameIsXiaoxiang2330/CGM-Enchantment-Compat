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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

public class GunStateHandler {

    private static final String AMMO_KEY = "AmmoCount";
    private static final String QH_TICK_KEY = "qh_tick";

    /**
     * 每种枪型的原始弹容量缓存。
     *
     * 为什么需要这个？
     * CGM 的 Gun.general.maxAmmo 是共享可变字段——同一枪型的所有实例共享一个 Gun 对象。
     * 超容量附魔会把共享字段改成 boosted 值，导致普通枪也读到被污染的值。
     * 此缓存在第一次遇到某枪型时快照原始值，之后所有"原始值"查询都走缓存，
     * 不再信任被污染的共享 Gun 字段。
     */
    private static final Map<Item, Integer> BASE_MAX_AMMO = new HashMap<>();
    private static final Map<Item, Integer> BASE_RATE = new HashMap<>();

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

        int baseMaxAmmo = getBaseMaxAmmo(main);

        // ========== Phase.START: 在 CGM 运行前设置好 Gun.general.maxAmmo ==========
        if (event.phase == TickEvent.Phase.START) {
            int ocLevel = EnchantHelper.getLevel(main, ModEnchantments.OVER_CAPACITY);

            if (ocLevel > 0) {
                // 第一次拿到带 OC 的枪：记录 oc_base
                if (!tag.hasKey("oc_base")) {
                    tag.setInteger("oc_base", baseMaxAmmo);
                }
                int base = tag.getInteger("oc_base");
                int boosted = base + (int)(base * 0.5 * ocLevel);
                setGunMaxAmmo(main, boosted);
                tag.setInteger("MaxAmmo", boosted);
            } else {
                // 无 OC：恢复到缓存中的原始值（不信任共享 Gun 字段）
                if (tag.hasKey("oc_base")) {
                    tag.removeTag("oc_base");
                }
                setGunMaxAmmo(main, baseMaxAmmo);
                tag.setInteger("MaxAmmo", baseMaxAmmo);
            }

            // ========== 快速扳机 (Trigger Finger): 每级减 1 tick 射速 ==========
            int tfLevel = EnchantHelper.getLevel(main, ModEnchantments.TRIGGER_FINGER);
            if (tfLevel > 0) {
                int baseRate = getBaseRate(main);
                int reduced = Math.max(1, baseRate - tfLevel * 4);
                setGunRate(main, reduced);
            } else {
                setGunRate(main, getBaseRate(main));
            }

            return;
        }

        // ========== Phase.END: 弹药回收/熟练手逻辑 ==========
        if (event.phase != TickEvent.Phase.END) return;

        int curAmmo = tag.getInteger(AMMO_KEY);
        int maxAmmo = tag.getInteger("MaxAmmo");

        // ==== 勤俭节约 — 逻辑已移至 DamageHandler，命中目标时触发 ====

        // ==== 熟练手 (Quick Hands) ====
        int qhLevel = EnchantHelper.getLevel(main, ModEnchantments.QUICK_HANDS);
        if (qhLevel > 0) {
            if (isReloading(player)) {
                if (curAmmo < maxAmmo) {
                    int interval = Math.max(1, 10 - qhLevel * 3);
                    int lastTick = tag.hasKey(QH_TICK_KEY) ? tag.getInteger(QH_TICK_KEY) : 0;
                    int now = player.ticksExisted;
                    if (lastTick == 0) {
                        tag.setInteger(QH_TICK_KEY, now);
                    } else if (now - lastTick >= interval) {
                        tag.setInteger(AMMO_KEY, Math.min(maxAmmo, curAmmo + 1));
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

    /**
     * 获取枪型的原始弹容量。首次调用时从共享 Gun 对象快照，之后走缓存。
     * 这绕过了共享 Gun 字段被超容量附魔污染的问题。
     */
    private int getBaseMaxAmmo(ItemStack stack) {
        Item item = stack.getItem();
        Integer cached = BASE_MAX_AMMO.get(item);
        if (cached != null) return cached;

        // 首次快照：此时应该还没有被污染
        int base = getGunMaxAmmoRaw(item);
        BASE_MAX_AMMO.put(item, base);
        return base;
    }

    /**
     * 直接从共享 Gun 对象读取 maxAmmo（可能已被污染，仅用于首次快照）。
     */
    private int getGunMaxAmmoRaw(Item item) {
        try {
            Object gun = item.getClass().getMethod("getGun").invoke(item);
            Object general = gun.getClass().getField("general").get(gun);
            return general.getClass().getField("maxAmmo").getInt(general);
        } catch (Exception e) { return 30; }
    }

    /**
     * 写入共享 Gun 对象的 maxAmmo（只能通过这个入口修改，避免引入多个污染源）。
     */
    private void setGunMaxAmmo(ItemStack stack, int value) {
        try {
            Object gun = stack.getItem().getClass().getMethod("getGun").invoke(stack.getItem());
            Object general = gun.getClass().getField("general").get(gun);
            general.getClass().getField("maxAmmo").setInt(general, value);
        } catch (Exception ignored) {}
    }

    /** 获取枪型原始射速 */
    private int getBaseRate(ItemStack stack) {
        Item item = stack.getItem();
        Integer cached = BASE_RATE.get(item);
        if (cached != null) return cached;
        int base = readGunRate(item);
        BASE_RATE.put(item, base);
        return base;
    }

    /** 从 Gun 对象读取 rate */
    private int readGunRate(Item item) {
        try {
            Object gun = item.getClass().getMethod("getGun").invoke(item);
            Object general = gun.getClass().getField("general").get(gun);
            return general.getClass().getField("rate").getInt(general);
        } catch (Exception e) { return 3; }
    }

    /** 写入 Gun 对象的 rate */
    private void setGunRate(ItemStack stack, int value) {
        try {
            Object gun = stack.getItem().getClass().getMethod("getGun").invoke(stack.getItem());
            Object general = gun.getClass().getField("general").get(gun);
            general.getClass().getField("rate").setInt(general, value);
        } catch (Exception ignored) {}
    }

}
