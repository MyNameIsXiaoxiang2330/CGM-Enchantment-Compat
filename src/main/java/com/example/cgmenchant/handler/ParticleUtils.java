/*
 * CGM Enchantment Addon — 粒子动画工具函数
 * Copyright (C) 2026 CGM Enchantment Addon Team
 */
package com.example.cgmenchant.handler;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketParticles;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

/**
 * 通用粒子工具类，提供可复用的动画函数。
 *
 * 所有函数自动发包给附近 128m 内的玩家。
 */
public class ParticleUtils {

    // ===================================================================
    //  2D 地面环 — 适用于地霰形
    // ===================================================================

    /** 画一个 2D 同心环（在地面 y 高度） */
    public static void drawRing2D(WorldServer ws, double cx, double y, double cz,
                                  float radius, int pts, EnumParticleTypes type) {
        for (int i = 0; i < pts; i++) {
            double a = 2.0 * Math.PI * i / pts;
            double px = cx + Math.cos(a) * radius;
            double pz = cz + Math.sin(a) * radius;
            spawnParticleFull(ws, type, (float)px, (float)y + 0.05f, (float)pz, 0, 0, 0, 0, 1);
        }
    }

    /** 画一组 2D 同心环 */
    public static void drawRings2D(WorldServer ws, double cx, double y, double cz,
                                   float[] radii, int ptsPerRing, EnumParticleTypes type) {
        for (float r : radii) {
            if (r < 0.05f) continue;
            drawRing2D(ws, cx, y, cz, r, ptsPerRing, type);
        }
    }

    /** 画 2D 准星线（水平+垂直） */
    public static void drawCrosshair2D(WorldServer ws, double cx, double y, double cz,
                                       float length, int pts, EnumParticleTypes type) {
        if (length < 0.15f) return;
        double step = length / pts;
        for (int i = -pts; i <= pts; i++) {
            double offset = i * step;
            if (Math.abs(offset) < 0.15) continue;
            // 水平
            spawnParticleFull(ws, type, (float)(cx + offset), (float)y + 0.05f, (float)cz, 0, 0, 0, 0, 1);
            // 垂直
            spawnParticleFull(ws, type, (float)cx, (float)y + 0.05f, (float)(cz + offset), 0, 0, 0, 0, 1);
        }
    }

    // ===================================================================
    //  3D 空间环 — 适用于贯霰形
    // ===================================================================

    /** 画一个 3D 空间环（垂直于 normal 方向） */
    public static void drawRing3D(WorldServer ws, Vec3d center, Vec3d normal,
                                  float radius, int pts, EnumParticleTypes type) {
        // 计算正交基
        Vec3d up = Math.abs(normal.y) < 0.9 ? new Vec3d(0, 1, 0) : new Vec3d(1, 0, 0);
        Vec3d right = normal.crossProduct(up).normalize();
        Vec3d forward = right.crossProduct(normal).normalize();

        for (int i = 0; i < pts; i++) {
            double a = 2.0 * Math.PI * i / pts;
            double px = center.x + right.x * Math.cos(a) * radius + forward.x * Math.sin(a) * radius;
            double py = center.y + right.y * Math.cos(a) * radius + forward.y * Math.sin(a) * radius;
            double pz = center.z + right.z * Math.cos(a) * radius + forward.z * Math.sin(a) * radius;
            spawnParticleFull(ws, type, (float)px, (float)py, (float)pz, 0, 0, 0, 0, 1);
        }
    }

    /** 画一组 3D 同心环 */
    public static void drawRings3D(WorldServer ws, Vec3d center, Vec3d normal,
                                   float[] radii, int ptsPerRing, EnumParticleTypes type) {
        for (float r : radii) {
            if (r < 0.05f) continue;
            drawRing3D(ws, center, normal, r, ptsPerRing, type);
        }
    }

    /** 画 3D 准星线（在环平面上，沿 right 和 forward 方向） */
    public static void drawCrosshair3D(WorldServer ws, Vec3d center, Vec3d right, Vec3d forward,
                                       float length, int pts, EnumParticleTypes type) {
        if (length < 0.15f) return;
        double step = length / pts;
        for (int i = -pts; i <= pts; i++) {
            double offset = i * step;
            if (Math.abs(offset) < 0.15) continue;
            // 水平方向（沿 right）
            spawnParticleFull(ws, type,
                (float)(center.x + right.x * offset),
                (float)(center.y + right.y * offset),
                (float)(center.z + right.z * offset),
                0, 0, 0, 0, 1);
            // 垂直方向（沿 forward）
            spawnParticleFull(ws, type,
                (float)(center.x + forward.x * offset),
                (float)(center.y + forward.y * offset),
                (float)(center.z + forward.z * offset),
                0, 0, 0, 0, 1);
        }
    }

    /** 计算 3D 环的正交基（从 normal 推导 right 和 forward） */
    public static Vec3d[] getBasis(Vec3d normal) {
        normal = normal.normalize();
        Vec3d up = Math.abs(normal.y) < 0.9 ? new Vec3d(0, 1, 0) : new Vec3d(1, 0, 0);
        Vec3d right = normal.crossProduct(up).normalize();
        Vec3d forward = right.crossProduct(normal).normalize();
        return new Vec3d[]{right, forward};
    }

    // ===================================================================
    //  爆发粒子
    // ===================================================================

    /** 在指定位置放一个爆发粒子效果 */
    public static void spawnBurst(WorldServer ws, double x, double y, double z,
                                  EnumParticleTypes type, float spread, int count, float speed) {
        spawnParticleFull(ws, type, (float)x, (float)y, (float)z, spread, spread, spread, speed, count);
    }

    // ===================================================================
    //  底层发包
    // ===================================================================

    /** 快速发射单颗粒子（无偏移） */
    public static void spawnParticleAt(WorldServer ws, EnumParticleTypes type, float x, float y, float z) {
        SPacketParticles packet = new SPacketParticles(type, false, x, y, z, 0, 0, 0, 0, 1);
        for (EntityPlayerMP player : ws.getMinecraftServer().getPlayerList().getPlayers()) {
            if (player.dimension == ws.provider.getDimension()
                && player.getDistanceSq(x, y, z) < 16384) {
                player.connection.sendPacket(packet);
            }
        }
    }

    /** 发送完整粒子包到附近 128m 内的所有玩家 */
    public static void spawnParticleFull(WorldServer ws, EnumParticleTypes type,
                                      float x, float y, float z,
                                      float ox, float oy, float oz, float speed, int count) {
        SPacketParticles packet = new SPacketParticles(type, false, x, y, z, ox, oy, oz, speed, count);
        for (EntityPlayerMP player : ws.getMinecraftServer().getPlayerList().getPlayers()) {
            if (player.dimension == ws.provider.getDimension()
                && player.getDistanceSq(x, y, z) < 16384) {
                player.connection.sendPacket(packet);
            }
        }
    }
}
