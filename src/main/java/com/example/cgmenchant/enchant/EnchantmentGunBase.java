package com.example.cgmenchant.enchant;

import com.example.cgmenchant.Reference;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public abstract class EnchantmentGunBase extends Enchantment {

    private static Class<?> itemGunClass;
    static {
        try {
            itemGunClass = Class.forName(Reference.CGM_ITEM_GUN_CLASS);
        } catch (ClassNotFoundException e) {}
    }

    protected final EnchantmentType type;
    private final int maxLevel;

    public EnchantmentGunBase(Rarity rarity, EnchantmentType type, int maxLevel) {
        super(rarity, EnumEnchantmentType.WEAPON,
              new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND, EntityEquipmentSlot.OFFHAND});
        this.type = type;
        this.maxLevel = maxLevel;
        setRegistryName(new ResourceLocation(Reference.MOD_ID, getEnchantmentName()));
        setName(Reference.MOD_ID + "." + getEnchantmentName());
    }

    @Override
    public int getMaxLevel() { return maxLevel; }

    public abstract String getEnchantmentName();
    public abstract String getDisplayName();

    @Override
    public boolean canApply(ItemStack stack) {
        return itemGunClass != null && itemGunClass.isInstance(stack.getItem());
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return canApply(stack);
    }

    @Override
    protected boolean canApplyTogether(Enchantment other) {
        if (other instanceof EnchantmentGunBase) {
            return ((EnchantmentGunBase) other).type != this.type;
        }
        return super.canApplyTogether(other);
    }

    @Override
    public String getTranslatedName(int level) {
        String name = getDisplayName();
        if (getMaxLevel() > 1) {
            name += " " + getRomanNumeral(level);
        }
        return name;
    }

    private static String getRomanNumeral(int n) {
        switch (n) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            default: return "" + n;
        }
    }

    @Override
    public int getMinEnchantability(int level) { return 10 + level * 5; }

    @Override
    public int getMaxEnchantability(int level) { return getMinEnchantability(level) + 20; }

    public enum EnchantmentType {
        WEAPON, AMMO, PROJECTILE
    }
}
