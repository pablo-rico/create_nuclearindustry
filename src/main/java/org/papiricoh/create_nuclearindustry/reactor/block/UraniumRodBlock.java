package org.papiricoh.create_nuclearindustry.reactor.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;

/**
 * A fuel channel of the fission reactor. Each channel holds one of the controller's fuel
 * assemblies; the controller shows on the block what the channel holds ({@link #FUEL}), how hard
 * the core is running ({@link #GLOW}) and whether coolant fills it ({@link #COOLANT}). These are
 * looks only: the fuel itself stays in the controller.
 */
public class UraniumRodBlock extends Block {
    /** What the channel shows: no assembly, or one fresh, half burnt or nearly spent. */
    public static final EnumProperty<Fuel> FUEL = EnumProperty.create("fuel", Fuel.class);
    /** The core's power, in four steps, as Cherenkov glow and light. */
    public static final IntegerProperty GLOW = IntegerProperty.create("glow", 0, 3);
    /** Whether coolant fills the channel. */
    public static final BooleanProperty COOLANT = BooleanProperty.create("coolant");

    // Model parts: end plates, the channel body and the four corner posts.
    private static final VoxelShape BOTTOM = Block.box(0, 0, 0, 16, 1, 16);
    private static final VoxelShape TOP    = Block.box(0, 15, 0, 16, 16, 16);
    private static final VoxelShape CORE   = Block.box(1, 1, 1, 15, 15, 15);

    private static final VoxelShape PILLAR_NW = Block.box(0, 1, 0, 1, 15, 1);
    private static final VoxelShape PILLAR_NE = Block.box(15, 1, 0, 16, 15, 1);
    private static final VoxelShape PILLAR_SW = Block.box(0, 1, 15, 1, 15, 16);
    private static final VoxelShape PILLAR_SE = Block.box(15, 1, 15, 16, 15, 16);

    private static final VoxelShape SHAPE = Shapes.or(
        BOTTOM, TOP, CORE,
        PILLAR_NW, PILLAR_NE, PILLAR_SW, PILLAR_SE
    ).optimize();

    public UraniumRodBlock(Properties props) {
        // Not a full cube: don't cull the neighbours' faces.
        super(props.noOcclusion());
        registerDefaultState(defaultBlockState().setValue(FUEL, Fuel.EMPTY).setValue(GLOW, 0).setValue(COOLANT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FUEL, GLOW, COOLANT);
    }

    /** Light the channel gives off at each glow step. */
    public static int lightLevel(BlockState state) {
        return switch (state.getValue(GLOW)) {
            case 1 -> 7;
            case 2 -> 11;
            case 3 -> 15;
            default -> 0;
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    /** What a channel's fuel looks like. */
    public enum Fuel implements StringRepresentable {
        EMPTY,
        FRESH,
        HALF,
        SPENT;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
