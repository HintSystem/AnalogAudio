package com.palm1.analogaudio.mixin;

import com.palm1.analogaudio.block.RadioBlock;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Parrot.class)
public abstract class ParrotMixin {

    @Unique
    private static final TagKey<Block> ANALOGAUDIO$JUKEBOXES = TagKey.create(Registries.BLOCK,
            ResourceLocation.withDefaultNamespace("jukeboxes"));

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private boolean analogaudio$allowRadioDancingBlock(BlockState instance, Block block) {
        if (block == Blocks.JUKEBOX) {
            if (instance.is(Blocks.JUKEBOX))
                return true;
            return instance.getBlock() instanceof RadioBlock && instance.getValue(RadioBlock.HAS_RECORD);
        }
        return instance.is(block);
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean analogaudio$allowRadioDancingTag(BlockState instance, TagKey<Block> tag) {
        if (tag.equals(ANALOGAUDIO$JUKEBOXES)) {
            if (instance.is(ANALOGAUDIO$JUKEBOXES))
                return true;
            return instance.getBlock() instanceof RadioBlock && instance.getValue(RadioBlock.HAS_RECORD);
        }
        return instance.is(tag);
    }
}
