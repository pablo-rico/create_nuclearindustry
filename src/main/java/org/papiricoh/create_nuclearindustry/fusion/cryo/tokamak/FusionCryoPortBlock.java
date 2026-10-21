package org.papiricoh.create_nuclearindustry.fusion.cryo.tokamak;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoRegistry;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakShellBlock;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakShellRole;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakFormedLook;

/**
 * Cryogenic port in the tokamak shell. INPUT ({@link TokamakShellRole#CRYO_IN}) takes liquid
 * helium in for the coils; OUTPUT ({@link TokamakShellRole#CRYO_OUT}) gives the boiled-off warm
 * helium back. The wrench toggles the mode.
 */
public class FusionCryoPortBlock extends BaseEntityBlock implements TokamakShellBlock {
    public static final EnumProperty<ReactorFluidPortMode> MODE = EnumProperty.create("mode", ReactorFluidPortMode.class);
    public static final MapCodec<FusionCryoPortBlock> CODEC = simpleCodec(FusionCryoPortBlock::new);
    private static final TagKey<Item> WRENCHES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

    public FusionCryoPortBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TokamakFormedLook.FORMED, false).setValue(MODE, ReactorFluidPortMode.INPUT));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE, TokamakFormedLook.FORMED);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(WRENCHES)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            BlockState updated = state.setValue(MODE, state.getValue(MODE).next());
            level.setBlock(pos, updated, UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof FusionCryoPortBlockEntity port) {
                // The role changed but not the block, so the tokamak has to recount its ports.
                port.notifyLinkedReactorChanged(player);
            }
            player.displayClientMessage(Component.translatable("create_nuclearindustry.cryo_port.mode",
                    Component.translatable(FusionCryoPortBlockEntity.modeKey(updated.getValue(MODE)))), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FusionCryoPortBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == CryoRegistry.FUSION_CRYO_PORT_ENTITY.get()
                ? (lvl, pos, blockState, blockEntity) -> {
                    if (blockEntity instanceof FusionCryoPortBlockEntity port) {
                        port.tick();
                    }
                }
                : null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public TokamakShellRole tokamakRole(BlockState state) {
        return state.getValue(MODE) == ReactorFluidPortMode.INPUT ? TokamakShellRole.CRYO_IN : TokamakShellRole.CRYO_OUT;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return TokamakFormedLook.isShaped(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        TokamakFormedLook.releaseIfOrphaned(state, level, pos);
    }
}
