package org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen;

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
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;

/** A fluid port in a steam generator's casing; its {@link #ROLE} says what it carries and the wrench cycles it. */
public class SteamGeneratorPortBlock extends BaseEntityBlock {
    public static final EnumProperty<SteamGeneratorPortRole> ROLE = EnumProperty.create("role", SteamGeneratorPortRole.class);
    public static final MapCodec<SteamGeneratorPortBlock> CODEC = simpleCodec(SteamGeneratorPortBlock::new);
    private static final TagKey<Item> WRENCHES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

    public SteamGeneratorPortBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ROLE, SteamGeneratorPortRole.PRIMARY_IN));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROLE);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(WRENCHES)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            SteamGeneratorPortRole role = state.getValue(ROLE).next();
            level.setBlock(pos, state.setValue(ROLE, role), UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof SteamGeneratorPortBlockEntity port) {
                port.refreshLink();
            }
            player.displayClientMessage(Component.translatable("create_nuclearindustry.steam_generator.port_role",
                    Component.translatable(role.translationKey())), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamGeneratorPortBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == PwrRegistry.STEAM_GENERATOR_PORT_ENTITY.get()
                ? (lvl, pos, blockState, blockEntity) -> {
                    if (blockEntity instanceof SteamGeneratorPortBlockEntity port) {
                        port.tick();
                    }
                }
                : null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
