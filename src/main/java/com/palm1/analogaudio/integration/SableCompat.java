package com.palm1.analogaudio.integration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.palm1.analogaudio.AnalogAudio;

public class SableCompat {
    private static boolean active = false;
    private static Object helperInstance;
    private static Method projectOutOfSubLevel;
    private static Method getVelocity;

    public static void init() {
        if (ModList.get().isLoaded("sable")) {
            try {
                Class<?> sableClass = Class.forName("dev.ryanhcode.sable.Sable");
                Field helperField = sableClass.getField("HELPER");
                helperInstance = helperField.get(null);

                Class<?> companionClass = Class.forName("dev.ryanhcode.sable.companion.SableCompanion");
                projectOutOfSubLevel = companionClass.getMethod("projectOutOfSubLevel", Level.class, Position.class);
                getVelocity = companionClass.getMethod("getVelocity", Level.class, Position.class);

                active = true;
                AnalogAudio.LOGGER.info("Successfully hooked into Sable!");
            } catch (Exception e) {
                AnalogAudio.LOGGER.warn("Failed to hook into Sable.", e);
            }
        }
    }

    public static Vec3 getGlobalPos(Level level, Vec3 pos) {
        if (!active || helperInstance == null)
            return pos;
        try {
            return (Vec3) projectOutOfSubLevel.invoke(helperInstance, level, (Position) pos);
        } catch (Exception e) {
            return pos;
        }
    }

    public static Vec3 getVelocity(Level level, Vec3 pos) {
        if (!active || helperInstance == null)
            return Vec3.ZERO;
        try {
            return (Vec3) getVelocity.invoke(helperInstance, level, (Position) pos);
        } catch (Exception e) {
            return Vec3.ZERO;
        }
    }

    public static boolean isInsideSubLevel(Level level, BlockPos pos) {
        return Math.abs(pos.getX()) > 1000000 || Math.abs(pos.getZ()) > 1000000;
    }
}
