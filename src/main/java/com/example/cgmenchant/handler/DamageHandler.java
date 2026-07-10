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
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class DamageHandler {

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        EntityLivingBase target = event.getEntityLiving();
        if (target.world.isRemote) return;

        EntityPlayer shooter = EnchantHelper.getShooterFromDamageSource(event.getSource());
        if (shooter == null) return;

        if (!EnchantHelper.isGun(shooter.getHeldItemMainhand()) &&
            !EnchantHelper.isGun(shooter.getHeldItemOffhand())) return;

        float damage = event.getAmount();

        // 加速器 (Accelerator): 伤害 +10%/级
        int accelLevel = EnchantHelper.getLevel(shooter, ModEnchantments.ACCELERATOR);
        if (accelLevel > 0) {
            damage += damage * 0.1f * accelLevel;
        }

        // 穿甲弹 (Puncturing): 忽视护甲
        int punctLevel = EnchantHelper.getLevel(shooter, ModEnchantments.PUNCTURING);
        if (punctLevel > 0) {
            float armorIgnore = 0.25f * punctLevel;
            float armorReduction = getArmorReduction(target);
            damage += damage * armorReduction * armorIgnore;
        }

        // 纵火者 (Fire Starter): 点燃（方块火焰由 BulletHandler 在子弹消失时放置）
        int fireLevel = EnchantHelper.getLevel(shooter, ModEnchantments.FIRE_STARTER);
        if (fireLevel > 0) {
            target.setFire(5);
        }

        // 弧光引导 (ArcLight): 纯视觉闪电 + 充能苦力怕 + 魔法伤害
        int arcLevel = EnchantHelper.getLevel(shooter, ModEnchantments.ARC_LIGHT);
        if (arcLevel > 0) {
            BlockPos pos = target.getPosition();
            EntityLightningBolt bolt = new EntityLightningBolt(target.world, pos.getX(), pos.getY(), pos.getZ(), true);
            target.world.addWeatherEffect(bolt);

            double r = 4.0;
            for (Entity e : target.world.getEntitiesWithinAABB(Entity.class,
                    target.getEntityBoundingBox().grow(r, r, r))) {
                if (e instanceof EntityCreeper && !e.isDead) {
                    ((EntityCreeper) e).onStruckByLightning(bolt);
                }
            }

            if (target.world.getBlockState(pos).getBlock() == Blocks.FIRE)
                target.world.setBlockToAir(pos);

            damage += 5.0f * arcLevel;
        }

        event.setAmount(damage);

        // ==== 凶弹基础增伤：每级+25%枪械基础伤害 ====
        int fellLevel = EnchantHelper.getLevel(shooter, ModEnchantments.FELLBULLET);
        int piercerLevel = EnchantHelper.getLevel(shooter, ModEnchantments.FELLBULLET_PIERCER);
        int fellLvl = Math.max(fellLevel, piercerLevel);
        if (fellLvl > 0) {
            ItemStack gun = shooter.getHeldItemMainhand();
            if (!EnchantHelper.isGun(gun)) {
                gun = shooter.getHeldItemOffhand();
            }
            if (EnchantHelper.isGun(gun)) {
                float gunBase = getCGMProjectileDamage(gun);
                int projCount = getCGMProjectileCount(gun);
                if (projCount <= 1) {
                    // 普通枪：每级+10%基础伤害
                    damage += gunBase * fellLvl * 0.1f;
                } else {
                    // 霰弹枪：每发 = 基础 + 基础×等级×0.5 + (1 + 等级×0.25)
                    float perPellet = gunBase + gunBase * fellLvl * 0.5f + (1 + fellLvl * 0.25f);
                    damage = perPellet * projCount;
                }
                event.setAmount(damage);
            }
        }

        // ==== 勤俭节约：命中目标后概率回复弹药 ====
        int thriftyLevel = EnchantHelper.getLevel(shooter, ModEnchantments.RECLAIMED);
        if (thriftyLevel > 0) {
            ItemStack gun = shooter.getHeldItemMainhand();
            if (!EnchantHelper.isGun(gun)) {
                gun = shooter.getHeldItemOffhand();
            }
            if (EnchantHelper.isGun(gun) && gun.getTagCompound() != null) {
                NBTTagCompound tag = gun.getTagCompound();
                int curAmmo = tag.getInteger("AmmoCount");
                int maxAmmo = tag.getInteger("MaxAmmo");
                if (maxAmmo <= 0) maxAmmo = 30;

                float chance;
                if (thriftyLevel == 1) chance = 1.0f / 3.0f;    // 33%
                else if (thriftyLevel == 2) chance = 0.50f;     // 50%
                else chance = 0.875f;                            // Ⅲ级 87.5%

                if (curAmmo < maxAmmo && shooter.getRNG().nextFloat() < chance) {
                    tag.setInteger("AmmoCount", curAmmo + 1);
                }
            }
        }
    }

    /** 从 CGM Gun 对象反射读取字段（尝试多个名称） */
    private Object getGunField(Object obj, String... names) throws Exception {
        for (String n : names) {
            try {
                java.lang.reflect.Field f = obj.getClass().getDeclaredField(n);
                f.setAccessible(true);
                return f.get(obj);
            } catch (NoSuchFieldException ignored) {}
            try {
                java.lang.reflect.Field f = obj.getClass().getField(n);
                f.setAccessible(true);
                return f.get(obj);
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException();
    }

    /** 反射读取 CGM 枪械的基础伤害 */
    private float getCGMProjectileDamage(ItemStack gun) {
        try {
            Object gunObj = gun.getItem().getClass().getMethod("getGun").invoke(gun.getItem());
            // 先试试字段，再试试内部类
            Object proj = null;
            try { proj = getGunField(gunObj, "projectile", "Projectile"); } catch (Exception ignored) {}
            if (proj == null) {
                for (Class<?> inner : gunObj.getClass().getDeclaredClasses()) {
                    if (inner.getSimpleName().equals("Projectile")) {
                        proj = inner.cast(inner.getMethod("getProjectile").invoke(gunObj));
                        break;
                    }
                }
            }
            if (proj == null) return 5.0f;
            Object dmg = getGunField(proj, "damage", "Damage");
            return ((Number) dmg).floatValue();
        } catch (Exception e) {
            return 5.0f;
        }
    }

    /** 反射读取 CGM 枪械的弹丸数（霰弹用） */
    private int getCGMProjectileCount(ItemStack gun) {
        try {
            Object gunObj = gun.getItem().getClass().getMethod("getGun").invoke(gun.getItem());
            Object general = getGunField(gunObj, "general", "General");
            Object cnt = getGunField(general, "projectileCount", "ProjectileCount");
            return ((Number) cnt).intValue();
        } catch (Exception e) {
            return 1;
        }
    }

    private float getArmorReduction(EntityLivingBase entity) {
        int armor = entity.getTotalArmorValue();
        if (armor <= 0) return 0f;
        return Math.min(1.0f, armor * 0.04f);
    }
}
