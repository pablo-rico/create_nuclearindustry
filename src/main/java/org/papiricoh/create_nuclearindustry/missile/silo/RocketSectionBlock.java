package org.papiricoh.create_nuclearindustry.missile.silo;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

import java.util.List;
import java.util.Locale;

/**
 * One section of a modular rocket. Its {@link #TOP} and {@link #BOTTOM} say what it meets, so stacked sections draw one fuselage:
 * joints only where two kinds of section meet, a bare end where nothing does. What it holds shows too
 * ({@link RocketTankBlock}, {@link RocketWarheadBlock}): its entity schedules a tick when its contents change, and the tick sets
 * the state from them ({@link #look}), so a rocket keeps its looks when it flies as a contraption.
 */
public class RocketSectionBlock extends BaseEntityBlock {
    public static final EnumProperty<Joint> TOP = EnumProperty.create("top", Joint.class);
    public static final EnumProperty<Joint> BOTTOM = EnumProperty.create("bottom", Joint.class);

    /** What a section meets above or below: nothing that is a section, the same kind of section, or another kind. */
    public enum Joint implements StringRepresentable {
        NONE, SAME, OTHER;

        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    public final RocketComposition.Section section;
    public RocketSectionBlock(Properties properties, RocketComposition.Section section) {
        super(properties);
        this.section = section;
        registerDefaultState(defaultBlockState().setValue(TOP, Joint.NONE).setValue(BOTTOM, Joint.NONE));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(p -> new RocketSectionBlock(p, section)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(TOP, BOTTOM); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return joined(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }
    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (direction == Direction.UP) return state.setValue(TOP, joint(neighbour));
        if (direction == Direction.DOWN) return state.setValue(BOTTOM, joint(neighbour));
        return state;
    }
    /** {@code state} with its joints set from the blocks above and below {@code pos}. */
    public BlockState joined(BlockState state, BlockGetter level, BlockPos pos) {
        return state.setValue(TOP, joint(level.getBlockState(pos.above()))).setValue(BOTTOM, joint(level.getBlockState(pos.below())));
    }
    private Joint joint(BlockState neighbour) {
        return !(neighbour.getBlock() instanceof RocketSectionBlock) ? Joint.NONE : neighbour.is(this) ? Joint.SAME : Joint.OTHER;
    }
    /** {@code state} showing what {@code part} holds; sections that show nothing return it unchanged. */
    public BlockState look(BlockState state, RocketSectionEntity part) { return state; }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof RocketSectionEntity part) {
            BlockState look = look(state, part);
            if (look != state) level.setBlock(pos, look, Block.UPDATE_CLIENTS);
        }
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RocketSectionEntity(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.create_nuclearindustry.rocket_section." + section.name().toLowerCase(java.util.Locale.ROOT)));
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (ServerPayloadGuard.check(player, pos) && level.getBlockEntity(pos) instanceof RocketSectionEntity part) {
            if (player.isShiftKeyDown()) part.unload(player);
            else part.insert(player, hand);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && ServerPayloadGuard.check(player, pos) && level.getBlockEntity(pos) instanceof RocketSectionEntity part) {
            if (player.isShiftKeyDown()) part.unload(player);
            else player.displayClientMessage(part.description(), true);
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        // A contraption moving the section takes its contents with it; only a section broken in place spills them.
        if (!moving && !state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof RocketSectionEntity part) part.dropContents();
        super.onRemove(state, level, pos, next, moving);
    }
}
