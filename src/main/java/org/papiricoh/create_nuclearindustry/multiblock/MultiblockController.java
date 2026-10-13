package org.papiricoh.create_nuclearindustry.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

/**
 * A block entity that owns a multiblock structure. Controllers register themselves in
 * {@link MultiblockRegistry} so block changes and part lookups can find them by chunk.
 */
public interface MultiblockController {

    @Nullable
    Level getLevel();

    BlockPos getBlockPos();

    boolean isRemoved();

    /**
     * Positions whose changes may affect this controller: the formed structure, or the
     * largest structure it could form while unformed. Call {@link MultiblockRegistry#reindex}
     * whenever this changes.
     */
    BoundingBox interestBounds();

    /** Whether a change inside {@link #interestBounds()} warrants revalidation. */
    boolean shouldReactToBlockChange(BlockChange change);

    void requestStructureRevalidation(@Nullable Player player);
}
