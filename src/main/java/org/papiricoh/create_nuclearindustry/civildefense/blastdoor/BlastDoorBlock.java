package org.papiricoh.create_nuclearindustry.civildefense.blastdoor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.radiation.RadiationRay;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * One leaf of a blast door, steel or lead. Shut, it is a solid block that shrugs off blasts (explosion
 * resistance 1500, so nuclear craters leave it standing) and stops radiation by its
 * {@code radiation_transmission} entry. While its controller slides the panel, it turns invisible
 * (the controller draws the panel) but stays solid; once the panel has left it, it has no collision
 * and lets blasts and radiation through, without being destroyed by them.
 */
public class BlastDoorBlock extends Block implements RadiationRay.Passable {
    public static final EnumProperty<LeafState> LEAF = EnumProperty.create("leaf", LeafState.class);
    /** How far around a loose leaf looks for the controller that slid it open. */
    private static final int CONTROLLER_SEARCH = 5;

    public BlastDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEAF, LeafState.CLOSED));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEAF);
    }

    public static boolean isOpen(BlockState state) {
        return state.getBlock() instanceof BlastDoorBlock && state.getValue(LEAF) == LeafState.OPEN;
    }

    @Override
    public boolean letsRadiationThrough(BlockState state) {
        return state.getValue(LEAF) == LeafState.OPEN;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(LEAF) == LeafState.OPEN ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    /** Only a shut leaf hides the faces of its neighbours; a moving or open one is see-through. */
    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(LEAF) == LeafState.CLOSED ? Shapes.block() : Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(LEAF) != LeafState.CLOSED;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(LEAF) == LeafState.CLOSED ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    /** An open doorway does not hold back a vanilla explosion; the retracted panel does not count. */
    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return state.getValue(LEAF) == LeafState.OPEN ? 0.0f : super.getExplosionResistance(state, level, pos, explosion);
    }

    /** The blast passes through an open doorway, but the panel retracted in the wall survives it. */
    @Override
    protected void onExplosionHit(BlockState state, Level level, BlockPos pos, Explosion explosion,
                                  BiConsumer<ItemStack, BlockPos> dropConsumer) {
        if (state.getValue(LEAF) != LeafState.OPEN) {
            super.onExplosionHit(state, level, pos, explosion, dropConsumer);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(LEAF) != LeafState.CLOSED;
    }

    /** A leaf left open with no controller to account for it (one broken without notice) shuts itself. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!hasControllerFor(level, pos)) {
            level.setBlock(pos, state.setValue(LEAF, LeafState.CLOSED), Block.UPDATE_ALL);
        }
    }

    /**
     * Whether a controller within {@value #CONTROLLER_SEARCH} blocks covers the leaf. For each loaded
     * chunk the box reaches it takes the cheaper of two lookups: walking the chunk's block entities
     * when it holds fewer of them than the box has positions in it, otherwise asking the chunk's block
     * entity map for each position. A chunk crowded with Create machines then costs no more than the
     * old per-position lookup, and an ordinary one costs a handful of checks.
     */
    public static boolean hasControllerFor(ServerLevel level, BlockPos pos) {
        int minX = pos.getX() - CONTROLLER_SEARCH, maxX = pos.getX() + CONTROLLER_SEARCH;
        int minY = pos.getY() - CONTROLLER_SEARCH, maxY = pos.getY() + CONTROLLER_SEARCH;
        int minZ = pos.getZ() - CONTROLLER_SEARCH, maxZ = pos.getZ() + CONTROLLER_SEARCH;
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (int cx = SectionPos.blockToSectionCoord(minX); cx <= SectionPos.blockToSectionCoord(maxX); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(minZ); cz <= SectionPos.blockToSectionCoord(maxZ); cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                // The part of the box inside this chunk.
                int x0 = Math.max(minX, SectionPos.sectionToBlockCoord(cx)), x1 = Math.min(maxX, SectionPos.sectionToBlockCoord(cx, 15));
                int z0 = Math.max(minZ, SectionPos.sectionToBlockCoord(cz)), z1 = Math.min(maxZ, SectionPos.sectionToBlockCoord(cz, 15));
                Map<BlockPos, BlockEntity> entities = chunk.getBlockEntities();
                int positions = (x1 - x0 + 1) * (maxY - minY + 1) * (z1 - z0 + 1);
                if (entities.size() <= positions) {
                    for (BlockEntity entity : entities.values()) {
                        BlockPos at = entity.getBlockPos();
                        if (at.getX() >= x0 && at.getX() <= x1 && at.getY() >= minY && at.getY() <= maxY
                                && at.getZ() >= z0 && at.getZ() <= z1 && covering(entity, pos)) {
                            return true;
                        }
                    }
                } else {
                    for (int x = x0; x <= x1; x++) {
                        for (int y = minY; y <= maxY; y++) {
                            for (int z = z0; z <= z1; z++) {
                                if (covering(entities.get(probe.set(x, y, z)), pos)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean covering(@Nullable BlockEntity entity, BlockPos leaf) {
        return entity instanceof BlastDoorControllerBlockEntity controller && !entity.isRemoved() && controller.covers(leaf);
    }
}
