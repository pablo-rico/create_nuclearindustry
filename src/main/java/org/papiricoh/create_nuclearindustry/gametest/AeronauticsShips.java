package org.papiricoh.create_nuclearindustry.gametest;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.neoforge.gametest.SableTestHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/** Ships for the game tests, through Sable's test helper. Only loaded with Sable installed. */
final class AeronauticsShips {
    private AeronauticsShips() {}

    /** A spawned ship and its container, removed by {@link #remove()}. */
    record Spawned(ServerSubLevelContainer container, ServerSubLevel subLevel) {
        /** The first block entity of a kind on the ship, in its plot. */
        <T extends BlockEntity> @Nullable T find(Class<T> type) {
            BoundingBox box = subLevel.getPlot().getBoundingBox().toMojang();
            for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                if (type.isInstance(subLevel.getLevel().getBlockEntity(pos))) {
                    return type.cast(subLevel.getLevel().getBlockEntity(pos));
                }
            }
            return null;
        }

        /** The lowest block of the ship in its plot. */
        @Nullable BlockPos firstBlock() {
            BoundingBox box = subLevel.getPlot().getBoundingBox().toMojang();
            for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                if (!subLevel.getLevel().getBlockState(pos).isAir()) {
                    return pos.immutable();
                }
            }
            return null;
        }

        boolean isRemoved() {
            return subLevel.isRemoved();
        }

        /** The ship's linear velocity, blocks per second. */
        Vec3 velocity() {
            org.joml.Vector3dc v = dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle.of(subLevel).getLinearVelocity();
            return new Vec3(v.x(), v.y(), v.z());
        }

        double mass() {
            return subLevel.getMassTracker() == null ? -1 : subLevel.getMassTracker().getMass();
        }

        void remove() {
            SableTestHelper.removeSubLevel(container, subLevel);
        }
    }

    /**
     * Assembles the blocks between two test positions into a ship that starts where they stood, as
     * Sable's own assembly tests do.
     */
    static @Nullable Spawned assemble(GameTestHelper helper, BlockPos from, BlockPos to) {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(helper.getLevel());
        if (container == null) {
            return null;
        }
        BlockPos min = helper.absolutePos(from);
        BlockPos max = helper.absolutePos(to);
        BlockPos lo = new BlockPos(Math.min(min.getX(), max.getX()), Math.min(min.getY(), max.getY()), Math.min(min.getZ(), max.getZ()));
        BlockPos hi = new BlockPos(Math.max(min.getX(), max.getX()), Math.max(min.getY(), max.getY()), Math.max(min.getZ(), max.getZ()));
        BoundingBox3i bounds = new BoundingBox3i(lo.getX(), lo.getY(), lo.getZ(), hi.getX(), hi.getY(), hi.getZ());
        ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(helper.getLevel(), lo, BlockPos.betweenClosed(lo, hi), bounds);
        if (subLevel == null) {
            return null;
        }
        container.physicsSystem().getPipeline().teleport(subLevel,
                new Vector3d(lo.getX() + (1 + hi.getX() - lo.getX()) / 2.0, lo.getY() + (1 + hi.getY() - lo.getY()) / 2.0,
                        lo.getZ() + (1 + hi.getZ() - lo.getZ()) / 2.0),
                new Quaterniond());
        return new Spawned(container, subLevel);
    }

    static @Nullable Spawned single(GameTestHelper helper, Vec3 worldPos, BlockState state) {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(helper.getLevel());
        if (container == null) {
            return null;
        }
        ServerSubLevel subLevel = SableTestHelper.spawnSingleBlockSubLevel(container, new Vector3d(worldPos.x, worldPos.y, worldPos.z), state);
        return subLevel == null ? null : new Spawned(container, subLevel);
    }
}
