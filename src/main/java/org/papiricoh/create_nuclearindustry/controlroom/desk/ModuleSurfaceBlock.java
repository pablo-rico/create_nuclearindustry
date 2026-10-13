package org.papiricoh.create_nuclearindustry.controlroom.desk;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoomBlocks;

/**
 * A block that holds up to four control modules: a desk, a table or a wall panel (see
 * {@link ModuleSurface}). FACING is the side the operator stands on. Blocks of the same kind side by
 * side join (LEFT and RIGHT drop the end panels), and every module surface touching another side by
 * side, or a wall panel above or below another on the same wall, belongs to one console sharing a
 * link. Fit a module by clicking a slot with it; wrench a module to set it up, sneak-wrench to take
 * it out; click it with an empty hand to work it.
 */
public abstract class ModuleSurfaceBlock extends HorizontalDirectionalBlock implements IBE<ControlDeskBlockEntity> {
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> WRENCHES = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

    protected ModuleSurfaceBlock(Properties properties) {
        super(properties);
    }

    public abstract ModuleSurface surface();

    /** The state with its joins to the neighbours worked out. */
    public abstract BlockState connect(BlockState state, BlockGetter level, BlockPos pos);

    /** Works the joins of any module surface out again, for tests and builders that place blocks without updates. */
    public static BlockState refresh(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getBlock() instanceof ModuleSurfaceBlock block ? block.connect(state, level, pos) : state;
    }

    /**
     * Whether two blocks next to each other towards {@code direction} belong to one console: any two
     * module surfaces side by side, and wall panels above one another on the same wall.
     */
    public static boolean sameConsole(BlockState a, BlockState b, Direction direction) {
        if (!(a.getBlock() instanceof ModuleSurfaceBlock first) || !(b.getBlock() instanceof ModuleSurfaceBlock second)) {
            return false;
        }
        if (direction.getAxis().isHorizontal()) {
            return true;
        }
        return first.surface() == ModuleSurface.WALL && second.surface() == ModuleSurface.WALL
                && a.getValue(FACING) == b.getValue(FACING);
    }

    /** Whether the neighbour at the {@code left} (or right) end carries this block on: the same block, operated from the same side. */
    protected static boolean sameRow(BlockState state, BlockGetter level, BlockPos pos, boolean left) {
        Direction side = left ? ModuleSurface.leftOf(state) : ModuleSurface.rightOf(state);
        BlockState neighbor = level.getBlockState(pos.relative(side));
        return neighbor.getBlock() == state.getBlock() && neighbor.getValue(FACING) == state.getValue(FACING);
    }

    /** Turns a shape a number of quarter turns clockwise seen from above, like a blockstate y rotation. */
    protected static VoxelShape rotateClockwise(VoxelShape shape, int quarterTurns) {
        VoxelShape turned = shape;
        for (int i = 0; i < quarterTurns; i++) {
            VoxelShape[] built = {Shapes.empty()};
            turned.forAllBoxes((x0, y0, z0, x1, y1, z1) ->
                    built[0] = Shapes.or(built[0], Shapes.box(1 - z1, y0, x0, 1 - z0, y1, x1)));
            turned = built[0];
        }
        return turned;
    }

    // ------------------------------------------------------------------ slots and interaction

    /** The slot a click lands on, 0-3, or -1 off the module surface. */
    public static int slotAt(BlockState state, BlockPos pos, Vec3 hit) {
        ModuleSurface surface = ModuleSurface.of(state);
        return surface == null ? -1 : surface.slotAt(state, pos, hit);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        ItemInteractionResult linker = ControlRoomBlocks.linkerFirst(stack);
        if (linker == ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION) {
            return linker;
        }
        int slot = slotAt(state, pos, hitResult.getLocation());
        if (slot < 0 || !(level.getBlockEntity(pos) instanceof ControlDeskBlockEntity desk)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        ControlModule module = desk.module(slot);
        if (stack.getItem() instanceof ControlModuleItem moduleItem) {
            if (module != null) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide && desk.insert(slot, moduleItem.type())) {
                stack.consume(1, player);
                level.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 0.6f, 1.4f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(WRENCHES) && module != null) {
            if (!level.isClientSide) {
                if (player.isShiftKeyDown()) {
                    ModuleType removed = desk.remove(slot);
                    if (removed != null) {
                        player.getInventory().placeItemBackInInventory(ControlModuleItem.stackOf(removed));
                        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4f, 1.0f);
                    }
                } else if (player instanceof ServerPlayer serverPlayer) {
                    // Any module of an LED display sets up the whole display, which shows its head's setup.
                    LedGroups.Cell head = LedGroups.headOf(level, pos, slot);
                    ControlModuleMenu.open(serverPlayer, head.pos(), head.slot());
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        int slot = slotAt(state, pos, hitResult.getLocation());
        if (slot < 0 || !(level.getBlockEntity(pos) instanceof ControlDeskBlockEntity desk)) {
            return InteractionResult.PASS;
        }
        ControlModule module = desk.module(slot);
        if (module == null || !module.type().interactive()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (desk.press(slot, player.isShiftKeyDown())) {
                level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4f, 1.2f);
                Component feedback = desk.takeFeedback(slot);
                if (feedback != null) {
                    player.displayClientMessage(feedback, true);
                }
            } else {
                player.displayClientMessage(Component.translatable("create_nuclearindustry.control_module.inactive")
                        .withStyle(ChatFormatting.RED), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moved) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, moved);
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, ControlDeskBlockEntity::requestClusterUpdate);
        }
    }

    // ------------------------------------------------------------------ redstone

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /** Out of the bottom and the back, for modules set to put their state out below. */
    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (direction != Direction.UP && direction != state.getValue(FACING)) {
            return 0;
        }
        return level.getBlockEntity(pos) instanceof ControlDeskBlockEntity desk ? desk.belowSignal() : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ControlDeskBlockEntity desk) {
            for (ItemStack drop : desk.drops()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, drop);
            }
        }
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<ControlDeskBlockEntity> getBlockEntityClass() {
        return ControlDeskBlockEntity.class;
    }
}
