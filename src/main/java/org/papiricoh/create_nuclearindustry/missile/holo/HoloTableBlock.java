package org.papiricoh.create_nuclearindustry.missile.holo;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoomLinkerItem;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

/**
 * A command table piece. Nine placed by hand in a 3×3 fuse into one holographic command table, its block entity in the centre;
 * breaking any piece turns the rest back into loose pieces. Using a formed table opens its screen.
 */
public class HoloTableBlock extends BaseEntityBlock {
    public static final EnumProperty<HoloPart> PART = EnumProperty.create("part", HoloPart.class);
    public static final MapCodec<HoloTableBlock> CODEC = simpleCodec(HoloTableBlock::new);
    /** The glass's top and the rim above it, on the outer edges of the whole table only. */
    private static final VoxelShape BODY = Block.box(0, 0, 0, 16, 13.6, 16);
    private static final VoxelShape RIM_N = Block.box(0, 0, 0, 16, 14.6, 2), RIM_S = Block.box(0, 0, 14, 16, 14.6, 16),
            RIM_W = Block.box(0, 0, 0, 2, 14.6, 16), RIM_E = Block.box(14, 0, 0, 16, 14.6, 16);
    private static final java.util.Map<HoloPart, VoxelShape> SHAPES = new java.util.EnumMap<>(HoloPart.class);

    static {
        for (HoloPart part : HoloPart.values()) {
            String name = part.name();
            boolean single = part == HoloPart.SINGLE;
            VoxelShape shape = BODY;
            if (single || name.startsWith("N")) shape = net.minecraft.world.phys.shapes.Shapes.or(shape, RIM_N);
            if (single || name.startsWith("S") && part != HoloPart.SINGLE) shape = net.minecraft.world.phys.shapes.Shapes.or(shape, RIM_S);
            if (single || name.endsWith("W")) shape = net.minecraft.world.phys.shapes.Shapes.or(shape, RIM_W);
            if (single || name.endsWith("E") && part != HoloPart.SINGLE) shape = net.minecraft.world.phys.shapes.Shapes.or(shape, RIM_E);
            SHAPES.put(part, shape);
        }
    }

    public HoloTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PART, HoloPart.SINGLE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(PART));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == HoloPart.CENTER ? new HoloTableBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == HoloRegistry.ENTITY.get() && !level.isClientSide ? (l, p, s, be) -> ((HoloTableBlockEntity) be).serverTick() : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) HoloTable.tryForm(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        super.onRemove(state, level, pos, next, moving);
        if (level.isClientSide || state.is(next.getBlock())) return;
        BlockPos centre = HoloTable.centre(pos, state);
        if (centre != null) HoloTable.unform(level, centre);
    }

    /** A linker used on any piece of a whole table links the table's heart, in the centre. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        BlockPos centre = HoloTable.centre(pos, state);
        boolean linker = stack.getItem() instanceof ControlRoomLinkerItem
                || stack.getItem() instanceof org.papiricoh.create_nuclearindustry.missile.link.TacticalLinkerItem;
        if (!linker || centre == null || !HoloTable.intact(level, centre))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        stack.useOn(new UseOnContext(level, player, hand, stack, hit.withPosition(centre)));
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos centre = HoloTable.centre(pos, state);
        if (centre == null) return InteractionResult.PASS;
        if (level.isClientSide || !(player instanceof ServerPlayer server) || !(level.getBlockEntity(centre) instanceof HoloTableBlockEntity table))
            return InteractionResult.sidedSuccess(level.isClientSide);
        HoloTableBlockEntity.Click click = table.click(server, hit.getLocation(), player.getEyePosition(), player.getViewVector(1), player.isShiftKeyDown());
        String key = "holo_table.create_nuclearindustry.";
        switch (click) {
            case OPEN -> {
                if (ServerPayloadGuard.check(player, centre)) {
                    table.syncTo(player);
                    player.openMenu(new SimpleMenuProvider((id, inv, p) -> new HoloTableMenu(id, inv, centre),
                            Component.translatable("block.create_nuclearindustry.holo_table")), buf -> buf.writeBlockPos(centre));
                }
            }
            case TOO_FAR -> player.displayClientMessage(Component.translatable(key + "designate.too_far").withStyle(ChatFormatting.RED), true);
            case ZOOM -> player.displayClientMessage(table.zoom() == 0 ? Component.translatable(key + "zoom.auto")
                    : Component.translatable(key + "zoom", HoloView.VIEWS[table.zoom() - 1] / 1024), true);
            case MARKED, CONFIRMED, REFUSED -> {
                HoloTableBlockEntity.Designation result = table.lastDesignation();
                if (result == null) break;
                boolean ok = result == HoloTableBlockEntity.Designation.OK;
                int x = table.lastX(), z = table.lastZ();
                Component text = click == HoloTableBlockEntity.Click.MARKED && ok ? Component.translatable(key + "marked", x, z)
                        : click == HoloTableBlockEntity.Click.CONFIRMED ? Component.translatable(key + "designate.ok", x, z)
                        : Component.translatable(key + "designate." + result.name().toLowerCase(Locale.ROOT), x, z);
                player.displayClientMessage(text.copy().withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED), true);
            }
            default -> {
            }
        }
        return InteractionResult.SUCCESS;
    }
}
