package com.palm1.analogaudio.network.packet;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.block.entity.SpeakerBlockEntity;
import com.palm1.analogaudio.registry.ModDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetFrequencyC2SPacket(InteractionHand hand, BlockPos blockPos, int frequency, boolean isItem)
        implements CustomPacketPayload {

    public static final Type<SetFrequencyC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID, "set_frequency"));

    public static final StreamCodec<FriendlyByteBuf, SetFrequencyC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(
            SetFrequencyC2SPacket::write,
            SetFrequencyC2SPacket::new);

    public SetFrequencyC2SPacket(InteractionHand hand, int frequency) {
        this(hand, BlockPos.ZERO, frequency, true);
    }

    public SetFrequencyC2SPacket(BlockPos blockPos, int frequency) {
        this(InteractionHand.MAIN_HAND, blockPos, frequency, false);
    }

    public SetFrequencyC2SPacket(FriendlyByteBuf buf) {
        this(buf.readEnum(InteractionHand.class), buf.readBlockPos(), buf.readInt(), buf.readBoolean());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
        buf.writeBlockPos(blockPos);
        buf.writeInt(frequency);
        buf.writeBoolean(isItem);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final SetFrequencyC2SPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (data.isItem) {
                ItemStack stack = player.getItemInHand(data.hand);
                stack.set(ModDataComponents.FREQUENCY.get(), data.frequency);
            } else {
                BlockEntity blockEntity = player.level().getBlockEntity(data.blockPos);
                if (blockEntity instanceof SpeakerBlockEntity speaker) {
                    speaker.setFrequency(data.frequency);
                }
            }
        });
    }
}
