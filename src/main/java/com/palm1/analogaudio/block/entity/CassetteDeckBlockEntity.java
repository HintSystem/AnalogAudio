package com.palm1.analogaudio.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.sounds.SoundSource;

import org.jetbrains.annotations.Nullable;

import com.palm1.analogaudio.inventory.CassetteDeckMenu;
import com.palm1.analogaudio.registry.ModBlockEntities;
import com.palm1.analogaudio.registry.ModItems;
import com.palm1.analogaudio.registry.ModSounds;

public class CassetteDeckBlockEntity extends BlockEntity implements MenuProvider {
    public final net.minecraft.world.SimpleContainer inventory = new net.minecraft.world.SimpleContainer(1) {
        @Override
        public boolean canPlaceItem(int index, ItemStack stack) {
            return stack.is(ModItems.CASSETTE_TAPE.get());
        }
    };
    private boolean wasEmpty = true;
    private ItemStack lastCassette = ItemStack.EMPTY;
    private long insertTime = 0;
    private long removeTime = 0;

    public CassetteDeckBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CASSETTE_DECK.get(), pos, blockState);

        this.wasEmpty = inventory.isEmpty();

        inventory.addListener(container -> {
            ItemStack currentCassette = container.getItem(0);
            boolean isEmpty = currentCassette.isEmpty();
            boolean itemChanged = !ItemStack.matches(currentCassette, lastCassette);

            if (this.level != null && !this.level.isClientSide()) {
                if (itemChanged) {
                    if (this.wasEmpty && !isEmpty) {
                        this.insertTime = this.level.getGameTime();
                        this.level.playSound(null, this.worldPosition, ModSounds.CASSETTE_INSERT.get(),
                                SoundSource.BLOCKS,
                                1.0f, 1.0f);
                    } else if (!this.wasEmpty && isEmpty) {
                        this.removeTime = this.level.getGameTime();
                        this.level.playSound(null, this.worldPosition, ModSounds.CASSETTE_EJECT.get(),
                                SoundSource.BLOCKS,
                                1.0f, 1.0f);
                    }
                    setChanged();
                    updateAndSync();
                }
            }
            this.lastCassette = currentCassette.copy();
            this.wasEmpty = isEmpty;
        });
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.analogaudio.cassette_deck");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CassetteDeckMenu(containerId, playerInventory, this);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        net.minecraft.nbt.CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    private void updateAndSync() {
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.createTag(registries));
        tag.putLong("InsertTime", insertTime);
        tag.putLong("RemoveTime", removeTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.fromTag(tag.getList("Inventory", 10), registries);
        insertTime = tag.getLong("InsertTime");
        removeTime = tag.getLong("RemoveTime");
        this.wasEmpty = inventory.isEmpty();
    }

    public net.minecraft.world.item.ItemStack getCassette() {
        return inventory.getItem(0);
    }

    public long getInsertTime() {
        return insertTime;
    }

    public long getRemoveTime() {
        return removeTime;
    }
}
