package org.papiricoh.create_nuclearindustry.explosive.conventional;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.integration.claims.ClaimGuards;

/**
 * Blocks a ground burst throws out of its crater: a few of the surface blocks over the bowl fly up and out and fall back as
 * falling blocks round the rim, as clods of earth and rubble do. Never a block entity, a blast-proof block or one in a claim;
 * at most {@code missile.conventional.debrisMax} per blast.
 */
public final class DebrisThrower {
    private DebrisThrower() {}

    public static void throwOut(ServerLevel level, Vec3 centre, ConventionalProfile profile, int max, double blastProof) {
        double crater = profile.craterRadius();
        if (crater < 1.5 || max <= 0) {
            return;
        }
        RandomSource random = level.random;
        int count = (int) Math.min(max, Math.ceil(crater * crater * 0.25));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double r = crater * Math.sqrt(random.nextDouble()) * 0.9;
            int x = (int) Math.floor(centre.x + Math.cos(angle) * r);
            int z = (int) Math.floor(centre.z + Math.sin(angle) * r);
            pos.set(x, level.getMinBuildHeight(), z);
            if (!level.isLoaded(pos) || ClaimGuards.protectsFromExplosions(level, x >> 4, z >> 4)) {
                continue;
            }
            pos.setY(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.hasBlockEntity() || state.getDestroySpeed(level, pos) < 0 || state.is(Blocks.BEDROCK)
                    || state.getBlock().getExplosionResistance() > blastProof || !state.getFluidState().isEmpty()) {
                continue;
            }
            // Thrown out and up, the faster the closer to the middle of the bowl.
            double speed = (0.5 + 0.7 * (1 - r / crater)) * Math.sqrt(crater / 10.0);
            double out = 0.35 + 0.25 * random.nextDouble();
            Vec3 velocity = new Vec3(Math.cos(angle) * speed * out, speed * (0.8 + 0.4 * random.nextDouble()), Math.sin(angle) * speed * out);
            FallingBlockEntity falling = FallingBlockEntity.fall(level, pos.immutable(), state);
            falling.setDeltaMovement(velocity);
            falling.hurtMarked = true;
        }
    }
}
