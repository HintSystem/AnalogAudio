package com.palm1.analogaudio.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import com.palm1.analogaudio.block.entity.RadioBlockEntity;
import com.palm1.analogaudio.client.ClientHooks;
import com.palm1.analogaudio.item.CassetteData;
import com.palm1.analogaudio.registry.ModBlockEntities;
import com.palm1.analogaudio.registry.ModDataComponents;
import com.palm1.analogaudio.registry.ModItems;

public class RadioBlock extends BaseEntityBlock {
    public static final MapCodec<RadioBlock> CODEC = simpleCodec(RadioBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty HAS_RECORD = BlockStateProperties.HAS_RECORD;

    public RadioBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(HAS_RECORD, false));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, HAS_RECORD);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem().toString() == "create:mechanical_arm") {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.getBlockEntity(pos) instanceof RadioBlockEntity radio) {
            if (stack.is(ModItems.CASSETTE_TAPE.get()) && radio.getItem(0).isEmpty()) {
                if (!level.isClientSide) {
                    radio.setItem(0, stack.split(1));
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (stack.is(ModItems.CASSETTE_BAG.get()) && radio.getItem(1).isEmpty()) {
                if (!level.isClientSide) {
                    radio.setItem(1, stack.split(1));
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (player.getMainHandItem().getItem().toString() == "create:mechanical_arm" ||
                player.getOffhandItem().getItem().toString() == "create:mechanical_arm") {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof RadioBlockEntity radio) {
                player.openMenu(radio, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos,
            Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RadioBlockEntity radio) {
                radio.setPowered(level.hasNeighborSignal(pos));
            }
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RadioBlockEntity radio) {
                radio.setPowered(level.hasNeighborSignal(pos));
            }
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadioBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return createTickerHelper(blockEntityType, ModBlockEntities.RADIO.get(), (lvl, pos, st, entity) -> {
                ItemStack cassette = entity.getCassette();
                if (cassette != null && !cassette.isEmpty() && entity.getStartTime() != 0) {
                    CassetteData data = cassette.get(ModDataComponents.CASSETTE_DATA.get());
                    if (data != null) {
                        boolean shouldLoop = entity.isLooping() && !entity.isPlayingFromBag();
                        ClientHooks.tickRadio(pos, Vec3.atCenterOf(pos), data,
                                entity.getStartTime(), entity.getVolume(), shouldLoop);

                        if (lvl.getGameTime() % 10 == 0 && entity.isPlaying()) {
                            double x = pos.getX() + 0.5 + (lvl.random.nextDouble() - 0.5) * 0.4;
                            double y = pos.getY() + 0.8;
                            double z = pos.getZ() + 0.5 + (lvl.random.nextDouble() - 0.5) * 0.4;

                            int r = (data.color() >> 16) & 0xFF;
                            int g = (data.color() >> 8) & 0xFF;
                            int b = data.color() & 0xFF;
                            float[] hsb = java.awt.Color.RGBtoHSB(r, g, b, null);
                            float colorOffset = (0.33f - hsb[0] + 1.0f) % 1.0f;

                            lvl.addParticle(ParticleTypes.NOTE, x, y, z, colorOffset, 0, 0);
                        }
                        // Check for parrots to get their freak on.
                        AABB area = new AABB(pos).inflate(3.0D);
                        for (net.minecraft.world.entity.animal.Parrot parrot : lvl
                                .getEntitiesOfClass(net.minecraft.world.entity.animal.Parrot.class, area)) {
                            parrot.setRecordPlayingNearby(pos, true);
                        }
                        return;
                    }
                }
                ClientHooks.stopRadio(pos);
            });
        } else {
            return createTickerHelper(blockEntityType, ModBlockEntities.RADIO.get(), RadioBlockEntity::serverTick);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.isClientSide()) {
                // Tell parrots to stop schmooving and grooving when radio is broken.
                AABB area = new AABB(pos).inflate(3.46D);
                for (net.minecraft.world.entity.animal.Parrot parrot : level
                        .getEntitiesOfClass(net.minecraft.world.entity.animal.Parrot.class, area)) {
                    parrot.setRecordPlayingNearby(pos, false);
                }
                ClientHooks.stopRadio(pos);
            } else {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof RadioBlockEntity radio) {
                    Containers.dropContents(level, pos, radio.inventory);
                    level.updateNeighbourForOutputSignal(pos, this);
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof RadioBlockEntity radio) {
            if (!radio.isPlaying()) {
                return 0;
            }

            ItemStack cassette = radio.getCassette();
            if (!cassette.isEmpty()) {
                CassetteData data = cassette.get(ModDataComponents.CASSETTE_DATA.get());
                if (data != null && data.duration() > 0) {
                    long elapsedTicks = level.getGameTime() - radio.getStartTime();
                    long elapsedMs = elapsedTicks * 50;
                    float progress = (float) elapsedMs / (float) data.duration();
                    return Math.max(1, Math.min(15, (int) (progress * 15)));
                }
            }
            return 15;
        }
        return 0;
    }
}
