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

package com.example.cgmenchant;

import com.example.cgmenchant.enchant.ModEnchantments;
import com.example.cgmenchant.handler.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = Reference.MOD_ID,
    name = Reference.MOD_NAME,
    version = Reference.VERSION,
    dependencies = "required-after:cgm;after:obfuscate"
)
public class CGMEnchantmentMod {

    private static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();

        ModEnchantments.register();
        logger.info("Registered {} gun enchantments", ModEnchantments.ENCHANTMENTS.size());

        // 初始化创造模式标签页
        TabCGMEnchant.INSTANCE.getTabIconItem();

        MinecraftForge.EVENT_BUS.register(new DamageHandler());
        MinecraftForge.EVENT_BUS.register(new BulletHandler());
        MinecraftForge.EVENT_BUS.register(new CollateralHandler());
        MinecraftForge.EVENT_BUS.register(new PlayerHandler());
        MinecraftForge.EVENT_BUS.register(new ArcLightHandler());
        MinecraftForge.EVENT_BUS.register(new HighExplosiveHandler());
        MinecraftForge.EVENT_BUS.register(new GunStateHandler());
        logger.info("Event handlers registered");
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("{} v{} loaded!", Reference.MOD_NAME, Reference.VERSION);
    }

    public static Logger getLogger() { return logger; }
}
