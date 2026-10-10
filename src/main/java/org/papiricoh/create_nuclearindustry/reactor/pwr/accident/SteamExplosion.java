package org.papiricoh.create_nuclearindustry.reactor.pwr.accident;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrConfig;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentMonitorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentRegistry;

/**
 * Water meeting molten fuel flashes to steam faster than it can get away: a steam explosion.
 * It happens when water is pumped back into a core hotter than
 * {@link PwrConfig#steamExplosionTemperature} and when corium falls into water. The blast is a
 * bounded vanilla explosion, which breaks blocks only with {@link PwrConfig#blockDestruction}; the
 * steam goes into the containment around it.
 */
public final class SteamExplosion {
    /** Water pumped onto a molten core. */
    public static final float REFLOOD_POWER = 4.0f;
    /** Corium falling into water. */
    public static final float CORIUM_POWER = 2.5f;
    /** Steam a corium quench boils off, mol. */
    private static final double QUENCH_STEAM = 600.0;
    private static final double STEAM_TEMPERATURE = 600.0;

    private SteamExplosion() {}

    /** A bounded explosion at {@code at}; breaks blocks only when the config allows it. It sounds the sirens around. */
    public static void explode(Level level, Vec3 at, float power) {
        PlantAlarm.raise(level, BlockPos.containing(at));
        level.explode(null, at.x, at.y, at.z, Math.min(6.0f, power),
                PwrConfig.blockDestruction ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
    }

    /**
     * Corium at {@code pos} touches water: the water around it boils away and, with steam
     * explosions on, it blows up. The boiled water ends the event, so it happens once per contact.
     */
    public static void quenchCorium(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    cursor.setWithOffset(pos, dx, dy, dz);
                    BlockState state = level.getBlockState(cursor);
                    if (state.getFluidState().isEmpty() || state.getFluidState().is(FluidTags.LAVA)) {
                        continue;
                    }
                    if (state.getBlock() instanceof LiquidBlock) {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    } else if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
                        level.setBlock(cursor, state.setValue(BlockStateProperties.WATERLOGGED, false),
                                Block.UPDATE_ALL);
                    }
                }
            }
        }
        Vec3 center = Vec3.atCenterOf(pos);
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.5, center.z, 30, 1.0, 0.8, 1.0, 0.1);
        ContainmentMonitorBlockEntity containment = ContainmentRegistry.at(level, pos);
        if (containment != null) {
            containment.addSteam(QUENCH_STEAM, STEAM_TEMPERATURE);
        }
        if (PwrConfig.steamExplosions) {
            explode(level, center, CORIUM_POWER);
        }
    }
}
