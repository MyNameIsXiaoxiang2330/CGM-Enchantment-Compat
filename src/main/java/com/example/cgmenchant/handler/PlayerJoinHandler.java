/*
 * CGM Enchantment Addon — First join hint
 * Copyright (C) 2026 CGM Enchantment Addon Team
 */
package com.example.cgmenchant.handler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PlayerJoinHandler {

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.player;
        if (player.world.isRemote) return;

        NBTTagCompound data = player.getEntityData();
        if (!data.getBoolean("cgmenchant_intro")) {
            data.setBoolean("cgmenchant_intro", true);

            String name = com.example.cgmenchant.Reference.MOD_NAME;
            player.sendMessage(new TextComponentString("§6§l=== " + name + " ==="));
            player.sendMessage(new TextComponentString("§7部分附魔触发时会显示§c台词§7效果。"));
            player.sendMessage(new TextComponentString("§e/cgmen help <附魔名> §7查看附魔说明"));
            player.sendMessage(new TextComponentString("§e/cgmen text [on/off] §7开关台词"));
            player.sendMessage(new TextComponentString("§e/cgmen text ReadText §7预览台词"));
        }
    }
}
