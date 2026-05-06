package com.palm1.analogaudio.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;

import org.jetbrains.annotations.Nullable;

import com.palm1.analogaudio.inventory.RadioMenu;
import com.palm1.analogaudio.block.RadioBlock;
import com.palm1.analogaudio.registry.ModBlockEntities;
import com.palm1.analogaudio.registry.ModDataComponents;
import com.palm1.analogaudio.registry.ModSounds;
import com.palm1.analogaudio.registry.ModItems;
import com.palm1.analogaudio.item.CassetteData;

import java.util.ArrayList;
import java.util.List;

public class RadioBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {

    private ItemStack cassetteStack = ItemStack.EMPTY;
    private ItemStack bagStack = ItemStack.EMPTY;
    private int lastSignal = 0;

    public final Container inventory = this;

    public static void serverTick(Level level, BlockPos pos, BlockState state,
            RadioBlockEntity entity) {
        if (level.getGameTime() % 20 == 0) {
            int currentSignal = state.getAnalogOutputSignal(level, pos);
            if (currentSignal != entity.lastSignal) {
                entity.lastSignal = currentSignal;
                level.updateNeighbourForOutputSignal(pos, state.getBlock());
            }
        }
    }

    public void updateComparator() {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.updateNeighbourForOutputSignal(this.worldPosition, this.getBlockState().getBlock());
        }
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return cassetteStack.isEmpty() && bagStack.isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        if (index == 0)
            return cassetteStack;
        if (index == 1)
            return bagStack;
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack stack = getItem(index);
        if (stack.isEmpty())
            return ItemStack.EMPTY;
        ItemStack result = stack.split(count);
        if (stack.isEmpty())
            setItem(index, ItemStack.EMPTY);
        else
            setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack stack = getItem(index);
        if (stack.isEmpty())
            return ItemStack.EMPTY;
        setItem(index, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index == 0)
            cassetteStack = stack;
        else if (index == 1)
            bagStack = stack;
        setChanged();
        onInventoryChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        if (index == 0)
            return stack.is(ModItems.CASSETTE_TAPE.get());
        if (index == 1)
            return stack.is(ModItems.CASSETTE_BAG.get());
        return false;
    }

    @Override
    public void clearContent() {
        cassetteStack = ItemStack.EMPTY;
        bagStack = ItemStack.EMPTY;
        setChanged();
        onInventoryChanged();
    }

    private long startTime = 0;
    private float volume = 0.75f;
    private boolean looping = false;
    private boolean shuffle = false;
    private int playlistIndex = 0;
    private List<Integer> playedIndices = new ArrayList<>();

    private boolean wasPowered = false;
    private boolean playing = false;
    private long pausedOffset = 0;
    private boolean wasEmpty = true;
    private ItemStack lastCassette = ItemStack.EMPTY;
    private ItemStack lastBag = ItemStack.EMPTY;
    private long insertTime = 0;
    private long removeTime = 0;

    public RadioBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.RADIO.get(), pos, blockState);
        this.wasEmpty = true;
    }

    private void onInventoryChanged() {
        ItemStack currentCassette = cassetteStack;
        ItemStack currentBag = bagStack;
        boolean isEmpty = currentCassette.isEmpty() && currentBag.isEmpty();
        boolean itemChanged = !ItemStack.matches(currentCassette, lastCassette)
                || !ItemStack.matches(currentBag, lastBag);

        if (this.level != null && !this.level.isClientSide()) {
            if (itemChanged) {
                if (!ItemStack.matches(currentCassette, lastCassette)) {
                    if (lastCassette.isEmpty() && !currentCassette.isEmpty()) {
                        this.insertTime = this.level.getGameTime();
                        this.level.playSound(null, this.worldPosition, ModSounds.CASSETTE_INSERT.get(),
                                SoundSource.BLOCKS, 1.0f, 1.0f);
                    } else if (!lastCassette.isEmpty() && currentCassette.isEmpty()) {
                        this.removeTime = this.level.getGameTime();
                        this.level.playSound(null, this.worldPosition, ModSounds.CASSETTE_EJECT.get(),
                                SoundSource.BLOCKS, 1.0f, 1.0f);
                    }
                }

                if (!ItemStack.matches(currentBag, lastBag)) {
                    if (lastBag.isEmpty() && !currentBag.isEmpty()) {
                        this.level.playSound(null, this.worldPosition, ModSounds.BAG_OPEN.get(), SoundSource.BLOCKS,
                                0.5f, 1.0f);

                        if (this.shuffle) {
                            List<ItemStack> contents = currentBag.get(ModDataComponents.BAG_CONTENTS.get());
                            if (contents != null) {
                                List<Integer> validIndices = new ArrayList<>();
                                for (int i = 0; i < contents.size(); i++) {
                                    if (contents.get(i).has(ModDataComponents.CASSETTE_DATA.get())) {
                                        validIndices.add(i);
                                    }
                                }
                                if (!validIndices.isEmpty()) {
                                    this.playlistIndex = validIndices.get(level.random.nextInt(validIndices.size()));
                                    this.playedIndices.add(this.playlistIndex);
                                } else {
                                    this.playlistIndex = 0;
                                }
                            } else {
                                this.playlistIndex = 0;
                            }
                        } else {
                            this.playlistIndex = 0;
                        }
                    } else if (!lastBag.isEmpty() && currentBag.isEmpty()) {
                        this.level.playSound(null, this.worldPosition, ModSounds.BAG_CLOSE.get(), SoundSource.BLOCKS,
                                0.5f, 1.0f);
                        this.playlistIndex = 0;
                    }
                    this.playedIndices.clear();
                    if (this.shuffle && !currentBag.isEmpty()) {
                        this.playedIndices.add(this.playlistIndex);
                    }
                }
            }

            boolean hasData = false;
            if (!currentBag.isEmpty()) {
                List<ItemStack> contents = currentBag.get(ModDataComponents.BAG_CONTENTS.get());
                if (contents != null && !contents.isEmpty()) {
                    for (ItemStack s : contents) {
                        if (s.has(ModDataComponents.CASSETTE_DATA.get())) {
                            hasData = true;
                            break;
                        }
                    }
                }
            } else if (!currentCassette.isEmpty()) {
                hasData = currentCassette.has(ModDataComponents.CASSETTE_DATA.get());
            }

            if (hasData) {
                loadVolumeFromCassette();
                if (!playing || itemChanged) {
                    this.playing = true;
                    this.startTime = this.level.getGameTime();
                    this.pausedOffset = 0;
                }
            } else {
                this.playing = false;
                this.startTime = 0;
                this.pausedOffset = 0;
            }
            updatePowerState();
            updateComparator();
            updateAndSync();
        } else if (this.level != null && this.level.isClientSide()) {
            loadVolumeFromCassette();
        }

        this.lastCassette = currentCassette.copy();
        this.lastBag = currentBag.copy();
        this.wasEmpty = isEmpty;
    }

    public void setSettings(float volume, boolean looping, boolean playing, boolean shuffle) {
        boolean loopStarted = !this.looping && looping;
        if (this.level != null && this.playing != playing) {
            if (playing) {
                this.startTime = this.level.getGameTime() - this.pausedOffset;
            } else {
                this.pausedOffset = this.level.getGameTime() - this.startTime;
            }
        }
        this.playing = playing;

        if (loopStarted && !this.playing) {
            this.playing = true;
            if (this.level != null) {
                this.startTime = this.level.getGameTime();
            }
            this.pausedOffset = 0;
        }

        this.volume = volume;
        this.looping = looping;
        if (this.shuffle != shuffle) {
            this.playedIndices.clear();
            if (shuffle && isPlayingFromBag()) {
                this.playedIndices.add(this.playlistIndex);
            }
        }
        this.shuffle = shuffle;
        saveVolumeToCassette();
        updatePowerState();
        updateComparator();
        updateAndSync();
    }

    public void skipToNextTrack() {
        if (level == null || level.isClientSide())
            return;

        if (bagStack.isEmpty() || !bagStack.is(ModItems.CASSETTE_BAG.get())) {
            if (!cassetteStack.isEmpty()) {
                if (!looping) {
                    this.playing = false;
                    this.startTime = 0;
                    this.pausedOffset = 0;
                    updatePowerState();
                    updateComparator();
                    updateAndSync();
                } else {
                    this.startTime = level.getGameTime();
                    this.pausedOffset = 0;
                    updateAndSync();
                }
            }
            return;
        }

        List<ItemStack> contents = bagStack.get(ModDataComponents.BAG_CONTENTS.get());
        if (contents == null || contents.isEmpty())
            return;

        List<Integer> validIndices = new ArrayList<>();
        for (int i = 0; i < contents.size(); i++) {
            if (contents.get(i).has(ModDataComponents.CASSETTE_DATA.get()))
                validIndices.add(i);
        }

        if (validIndices.isEmpty())
            return;

        if (shuffle) {
            List<Integer> available = new ArrayList<>();
            for (int idx : validIndices)
                if (!playedIndices.contains(idx))
                    available.add(idx);

            if (available.isEmpty()) {
                if (!looping) {
                    this.playing = false;
                    this.startTime = 0;
                    this.pausedOffset = 0;
                    updatePowerState();
                    updateAndSync();
                    return;
                }
                playedIndices.clear();
                available.addAll(validIndices);
            }
            if (!available.isEmpty()) {
                playlistIndex = available.get(level.random.nextInt(available.size()));
                playedIndices.add(playlistIndex);
            }
        } else {
            int nextIdx = -1;
            for (int idx : validIndices)
                if (idx > playlistIndex) {
                    nextIdx = idx;
                    break;
                }

            if (nextIdx == -1) {
                if (!looping) {
                    this.playing = false;
                    this.startTime = 0;
                    this.pausedOffset = 0;
                    updatePowerState();
                    updateAndSync();
                    return;
                }
                nextIdx = validIndices.get(0);
            }
            playlistIndex = nextIdx;
        }

        this.startTime = level.getGameTime();
        this.pausedOffset = 0;
        loadVolumeFromCassette();
        updateAndSync();
    }

    public void setPowered(boolean powered) {
        if (powered && !wasPowered && this.level != null) {
            if (this.playing) {
                this.pausedOffset = this.level.getGameTime() - this.startTime;
                this.playing = false;
            } else {
                boolean hasData = false;
                ItemStack currentCassette = cassetteStack;
                ItemStack currentBag = bagStack;
                if (!currentBag.isEmpty()) {
                    List<ItemStack> contents = currentBag.get(ModDataComponents.BAG_CONTENTS.get());
                    if (contents != null && !contents.isEmpty()) {
                        for (ItemStack s : contents) {
                            if (s.has(ModDataComponents.CASSETTE_DATA.get())) {
                                hasData = true;
                                break;
                            }
                        }
                    }
                } else if (!currentCassette.isEmpty()) {
                    hasData = currentCassette.has(ModDataComponents.CASSETTE_DATA.get());
                }

                if (hasData) {
                    this.startTime = this.level.getGameTime() - this.pausedOffset;
                    this.playing = true;
                }
            }
            updatePowerState();
            updateAndSync();
        }
        this.wasPowered = powered;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) {
            return new int[] { 0 };
        } else if (side == Direction.DOWN) {
            return new int[] { 0, 1 };
        } else {
            return new int[] { 1 };
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        if (index == 0) {
            return direction == Direction.UP && stack.is(ModItems.CASSETTE_TAPE.get());
        }
        if (index == 1) {
            return direction != Direction.UP && direction != Direction.DOWN && stack.is(ModItems.CASSETTE_BAG.get());
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        if (direction == Direction.DOWN) {
            return !playing && pausedOffset == 0 && !looping;
        }
        return false;
    }

    private void updateAndSync() {
        if (level != null)
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        setChanged();
    }

    private void updatePowerState() {
        if (this.level == null || this.level.isClientSide())
            return;
        BlockState state = getBlockState();
        if (state.hasProperty(RadioBlock.POWERED)) {
            boolean wasPowered = state.getValue(RadioBlock.POWERED);
            if (wasPowered != playing) {
                this.level.setBlock(getBlockPos(), state.setValue(RadioBlock.POWERED, playing), 3);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!cassetteStack.isEmpty())
            tag.put("Cassette", cassetteStack.save(registries));
        if (!bagStack.isEmpty())
            tag.put("Bag", bagStack.save(registries));

        tag.putLong("StartTime", startTime);
        tag.putFloat("Volume", volume);
        tag.putBoolean("Looping", looping);
        tag.putBoolean("Shuffle", shuffle);
        tag.putInt("PlaylistIndex", playlistIndex);

        ListTag playedTag = new ListTag();
        for (int idx : playedIndices)
            playedTag.add(IntTag.valueOf(idx));
        tag.put("PlayedIndices", playedTag);

        tag.putBoolean("WasPowered", wasPowered);
        tag.putBoolean("Playing", playing);
        tag.putLong("PausedOffset", pausedOffset);
        tag.putLong("InsertTime", insertTime);
        tag.putLong("RemoveTime", removeTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Cassette"))
            cassetteStack = ItemStack.parse(registries, tag.getCompound("Cassette")).orElse(ItemStack.EMPTY);
        else
            cassetteStack = ItemStack.EMPTY;

        if (tag.contains("Bag"))
            bagStack = ItemStack.parse(registries, tag.getCompound("Bag")).orElse(ItemStack.EMPTY);
        else
            bagStack = ItemStack.EMPTY;

        startTime = tag.getLong("StartTime");
        volume = tag.contains("Volume") ? tag.getFloat("Volume") : 0.75f;
        looping = tag.getBoolean("Looping");
        shuffle = tag.getBoolean("Shuffle");
        playlistIndex = tag.getInt("PlaylistIndex");

        playedIndices.clear();
        ListTag playedTag = tag.getList("PlayedIndices", 3);
        for (int i = 0; i < playedTag.size(); i++)
            playedIndices.add(playedTag.getInt(i));

        wasPowered = tag.getBoolean("WasPowered");
        playing = tag.getBoolean("Playing");
        pausedOffset = tag.getLong("PausedOffset");
        insertTime = tag.getLong("InsertTime");
        removeTime = tag.getLong("RemoveTime");
        this.wasEmpty = cassetteStack.isEmpty() && bagStack.isEmpty();
    }

    public ItemStack getCassette() {
        if (!bagStack.isEmpty() && bagStack.is(ModItems.CASSETTE_BAG.get())) {
            List<ItemStack> contents = bagStack.get(ModDataComponents.BAG_CONTENTS.get());
            if (contents != null && playlistIndex >= 0 && playlistIndex < contents.size()) {
                ItemStack cassette = contents.get(playlistIndex);
                if (cassette.has(ModDataComponents.CASSETTE_DATA.get()))
                    return cassette;
            }
        }
        return cassetteStack;
    }

    public long getStartTime() {
        return playing ? startTime : 0;
    }

    public float getVolume() {
        return volume;
    }

    public boolean isLooping() {
        return looping;
    }

    public boolean isShuffle() {
        return shuffle;
    }

    public boolean isPlaying() {
        return playing;
    }

    public int getPlaylistIndex() {
        return playlistIndex;
    }

    public boolean isPlayingFromBag() {
        return !bagStack.isEmpty() && bagStack.is(ModItems.CASSETTE_BAG.get());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.analogaudio.radio");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new RadioMenu(id, inv, worldPosition);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public long getInsertTime() {
        return insertTime;
    }

    public long getRemoveTime() {
        return removeTime;
    }

    private void loadVolumeFromCassette() {
        ItemStack cassette = getCassette();
        if (!cassette.isEmpty() && cassette.has(ModDataComponents.CASSETTE_DATA.get())) {
            CassetteData data = cassette.get(ModDataComponents.CASSETTE_DATA.get());
            if (data != null) {
                this.volume = data.volume();
            }
        }
    }

    private void saveVolumeToCassette() {
        if (this.level == null || this.level.isClientSide())
            return;

        ItemStack playingStack = getCassette();
        if (!playingStack.isEmpty() && playingStack.has(ModDataComponents.CASSETTE_DATA.get())) {
            CassetteData oldData = playingStack.get(ModDataComponents.CASSETTE_DATA.get());
            if (oldData != null) {
                playingStack.set(ModDataComponents.CASSETTE_DATA.get(),
                        new CassetteData(oldData.uuid(), oldData.url(), oldData.name(), oldData.color(), this.volume,
                                oldData.duration()));

                if (isPlayingFromBag()) {
                    List<ItemStack> contents = bagStack.get(ModDataComponents.BAG_CONTENTS.get());
                    if (contents != null) {
                        List<ItemStack> newContents = new ArrayList<>(contents);
                        newContents.set(playlistIndex, playingStack);
                        bagStack.set(ModDataComponents.BAG_CONTENTS.get(), newContents);
                    }
                }
                setChanged();
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }
}
