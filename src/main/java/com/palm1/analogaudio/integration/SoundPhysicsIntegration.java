package com.palm1.analogaudio.integration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

public class SoundPhysicsIntegration {
    private static Boolean loaded;

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = ModList.get().isLoaded("sound_physics_remastered");
        }
        return loaded;
    }

    public static double[] processSound(int sourceId, double x, double y, double z, String category, String namespace,
            String path, boolean auxOnly) {
        if (!isLoaded()) {
            return null;
        }
        try {
            Class<?> soundPhysClass = Class.forName("com.sonicether.soundphysics.SoundPhysics");
            java.lang.reflect.Field slotField = soundPhysClass.getDeclaredField("auxFXSlot0");
            slotField.setAccessible(true);
            int slot = slotField.getInt(null);
            if (slot == 0) {
                soundPhysClass.getMethod("init").invoke(null);
                slot = slotField.getInt(null);
                if (slot == 0)
                    return null;
            }

            SoundSource soundSource = SoundSource.valueOf(category.toUpperCase());
            ResourceLocation sound = ResourceLocation.fromNamespaceAndPath(namespace, path);
            soundPhysClass.getMethod("setLastSoundCategoryAndName", SoundSource.class, ResourceLocation.class)
                    .invoke(null, soundSource, sound);

            Object result = soundPhysClass
                    .getMethod("processSound", int.class, double.class, double.class, double.class, SoundSource.class,
                            ResourceLocation.class, boolean.class)
                    .invoke(null, sourceId, x, y, z, soundSource, sound, auxOnly);

            if (result != null) {
                Vec3 vec = (Vec3) result;
                return new double[] { vec.x, vec.y, vec.z };
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    public static void setLastSound(SoundSource category, ResourceLocation sound) {
        if (!isLoaded()) {
            return;
        }
        try {
            Class<?> soundPhysClass = Class.forName("com.sonicether.soundphysics.SoundPhysics");
            soundPhysClass.getMethod("setLastSoundCategoryAndName", SoundSource.class, ResourceLocation.class)
                    .invoke(null, category, sound);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
