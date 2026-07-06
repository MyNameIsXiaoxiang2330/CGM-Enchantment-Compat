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
    dependencies = "required-after:cgm"
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
