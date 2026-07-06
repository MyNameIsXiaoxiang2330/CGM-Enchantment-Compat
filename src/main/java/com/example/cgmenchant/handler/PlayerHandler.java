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
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.UUID;

public class PlayerHandler {

    private static final UUID SPEED_ID = UUID.fromString("a12b3c4d-5e6f-7890-abcd-ef1234567890");

    @SubscribeEvent
    public void onPlayerTick(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntity() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntity();

        boolean holdingGun = EnchantHelper.isGun(player.getHeldItem(EnumHand.MAIN_HAND)) ||
                             EnchantHelper.isGun(player.getHeldItem(EnumHand.OFF_HAND));

        // 移动速度
        IAttributeInstance movement = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (movement == null) return;
        AttributeModifier existing = movement.getModifier(SPEED_ID);

        if (holdingGun && player.isHandActive() && !player.isSprinting()) {
            int level = EnchantHelper.getLevel(player, ModEnchantments.LIGHTWEIGHT);
            if (level > 0) {
                // 移速 +20%/级
                double bonus = 0.20 * level;
                if (existing == null) {
                    movement.applyModifier(new AttributeModifier(SPEED_ID, "lightweight", bonus, 2));
                } else if (existing.getAmount() != bonus) {
                    movement.removeModifier(existing);
                    movement.applyModifier(new AttributeModifier(SPEED_ID, "lightweight", bonus, 2));
                }

                // 开镜散布减小
                reduceSpread(player, level);
                return;
            }
        }
        if (existing != null) movement.removeModifier(existing);
    }

    /** 开镜时减小散布：通过反射修改 SpreadTracker 内部值 */
    private void reduceSpread(EntityPlayer player, int level) {
        try {
            // 获取 SpreadTracker
            Class<?> spreadHandler = Class.forName("com.mrcrayfish.guns.common.SpreadHandler");
            Object tracker = spreadHandler.getMethod("getSpreadTracker", UUID.class)
                .invoke(null, player.getUniqueID());
            if (tracker == null) return;

            // 获取当前主手 ItemGun
            Object gun = player.getHeldItemMainhand().getItem();
            if (!gun.getClass().getName().equals("com.mrcrayfish.guns.item.ItemGun")) return;

            // 反射访问 SPREAD_TRACKER_MAP
            java.lang.reflect.Field mapField = tracker.getClass().getDeclaredField("SPREAD_TRACKER_MAP");
            mapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<Object, Object> map = (java.util.Map<Object, Object>) mapField.get(tracker);

            Object entry = map.get((Object) gun);
            if (entry == null) return;

            // Pair<MutableLong, MutableInt> → getRight() 是散布值
            Object mutableInt = entry.getClass().getMethod("getRight").invoke(entry);
            if (mutableInt == null) return;

            int current = (int) mutableInt.getClass().getMethod("intValue").invoke(mutableInt);
            int reduced = Math.max(0, current - 5 * level);
            mutableInt.getClass().getMethod("setValue", int.class).invoke(mutableInt, reduced);

        } catch (Exception ignored) {}
    }
}
