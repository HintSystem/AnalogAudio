package com.palm1.analogaudio.integration.create;

import com.palm1.analogaudio.client.audio.ClientAudioEngine;
import com.palm1.analogaudio.item.CassetteData;
import com.palm1.analogaudio.registry.ModDataComponents;
import com.palm1.analogaudio.registry.ModItems;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class RadioMovementBehaviour implements MovementBehaviour {

    @Override
    @OnlyIn(Dist.CLIENT)
    public void tick(MovementContext context) {
        if (!context.world.isClientSide)
            return;

        CompoundTag beData = context.blockEntityData;
        if (beData == null || !beData.contains("Cassette"))
            return;

        HolderLookup.Provider registries = context.world.registryAccess();
        ItemStack cassette = ItemStack.parseOptional(registries, beData.getCompound("Cassette"));

        boolean hasCassette = !cassette.isEmpty() && cassette.is(ModItems.CASSETTE_TAPE.get());
        Object identity = getIdentity(context);

        if (hasCassette) {
            CassetteData data = cassette.get(ModDataComponents.CASSETTE_DATA.get());
            long startTime = beData.getLong("StartTime");
            float volume = beData.contains("Volume") ? beData.getFloat("Volume") : 1.0f;
            boolean looping = beData.getBoolean("Looping");

            if (data != null && startTime != 0) {
                Vec3 pos = context.position != null ? context.position : Vec3.ZERO;
                ClientAudioEngine.tickRadio(identity, pos, data, startTime, volume, looping);
            } else {
                ClientAudioEngine.stopRadio(identity);
            }
        } else {
            ClientAudioEngine.stopRadio(identity);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void stopMoving(MovementContext context) {
        if (context.world.isClientSide) {
            Object identity = getIdentity(context);
            ClientAudioEngine.stopRadio(identity);
            ClientAudioEngine.stopRadio(context.localPos);
        }
    }

    private Object getIdentity(MovementContext context) {
        return "radio_contraption_" + context.contraption.entity.getUUID() + "_" + context.localPos.asLong();
    }
}
