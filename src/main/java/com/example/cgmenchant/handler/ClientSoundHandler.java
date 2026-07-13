/*
 * CGM Enchantment Addon — 客户端音效拦截
 * Copyright (C) 2026 CGM Enchantment Addon Team
 *
 * 当玩家持有带「庄严哀悼」的枪械时，静默 CGM 原版枪声。
 * 替换音效已在服务端通过 SolemnMourningHandler 播放。
 */
package com.example.cgmenchant.handler;

import com.example.cgmenchant.enchant.ModEnchantments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = "cgmenchant", value = Side.CLIENT)
public class ClientSoundHandler {

    /** CGM 模组的 namespace */
    private static final String CGM_NAMESPACE = "cgm";

    @SideOnly(Side.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlaySound(PlaySoundEvent event) {
        if (event.getSound() == null) return;

        ResourceLocation loc = event.getSound().getSoundLocation();
        if (loc == null) return;

        // 只拦截 CGM 命名空间的音效
        if (!CGM_NAMESPACE.equals(loc.getResourceDomain())) return;

        // 检查本地玩家是否持有带「庄严哀悼」的枪
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) return;

        ItemStack main = player.getHeldItemMainhand();
        ItemStack off = player.getHeldItemOffhand();

        boolean hasMourning = (EnchantHelper.isGun(main) &&
            net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(
                ModEnchantments.SOLEMN_MOURNING, main) > 0) ||
            (EnchantHelper.isGun(off) &&
            net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(
                ModEnchantments.SOLEMN_MOURNING, off) > 0);

        if (hasMourning) {
            // 静默原版枪声
            event.setResultSound(null);
        }
    }
}
