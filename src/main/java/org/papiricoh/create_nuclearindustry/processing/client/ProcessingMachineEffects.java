package org.papiricoh.create_nuclearindustry.processing.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.chemistry.electrolyzer.ElectrolyzerBlockEntity;
import org.papiricoh.create_nuclearindustry.enrichment.block.CentrifugeBlock;
import org.papiricoh.create_nuclearindustry.enrichment.blockentity.CentrifugeBlockEntity;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;
import org.papiricoh.create_nuclearindustry.processing.KineticProcessingBlock;
import org.papiricoh.create_nuclearindustry.processing.KineticProcessingBlockEntity;

/**
 * Small working effects of the centrifuge and the electrolyzer while they process: gas bubbling up
 * the electrolyzer's sight glass and venting at its top, and a faint wisp at a centrifuge's header
 * joints. Positions are pixels on the north-facing model, turned to the machine's facing.
 */
public final class ProcessingMachineEffects {
    /** Effects only this close to the machine, in blocks. */
    private static final double EFFECT_RANGE = 24.0;

    /** The bottom of the sight glasses on either flank of the gearbox plate; they run 4 px up. */
    private static final Vec3 GLASS_WEST = new Vec3(-0.2, 9, 14);
    private static final Vec3 GLASS_EAST = new Vec3(16.2, 9, 14);
    private static final Vec3 TOP_VENT = new Vec3(8, 16.2, 6);
    private static final Vec3 HEADER_WEST = new Vec3(0, 13.5, 10);
    private static final Vec3 HEADER_EAST = new Vec3(16, 13.5, 10);

    private ProcessingMachineEffects() {}

    public static void tick(BlockEntity be) {
        if (!(be instanceof KineticProcessingBlockEntity machine) || !machine.looksBusy()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Level level = machine.getLevel();
        if (level == null || level != minecraft.level || minecraft.player == null || minecraft.isPaused()) {
            return;
        }
        if (ShipSpace.toWorld(level, machine.getBlockPos()).distanceToSqr(minecraft.player.position()) > EFFECT_RANGE * EFFECT_RANGE) {
            return;
        }
        RandomSource random = level.random;
        BlockState state = machine.getBlockState();
        if (machine instanceof ElectrolyzerBlockEntity) {
            if (random.nextInt(4) == 0) {
                Vec3 glass = random.nextBoolean() ? GLASS_WEST : GLASS_EAST;
                spawn(level, machine, state, glass.add(0, random.nextFloat() * 4, (random.nextFloat() - 0.5)),
                        ParticleTypes.BUBBLE_POP, 0.01);
            }
            if (random.nextInt(25) == 0) {
                spawn(level, machine, state, TOP_VENT, ParticleTypes.WHITE_SMOKE, 0.02);
            }
        } else if (machine instanceof CentrifugeBlockEntity && random.nextInt(60) == 0) {
            boolean west = random.nextBoolean();
            if (state.getValue(west ? CentrifugeBlock.LEFT : CentrifugeBlock.RIGHT)) {
                spawn(level, machine, state, west ? HEADER_WEST : HEADER_EAST, ParticleTypes.WHITE_SMOKE, 0.01);
            }
        }
    }

    private static void spawn(Level level, KineticProcessingBlockEntity machine, BlockState state, Vec3 pixel,
                              ParticleOptions particle, double rise) {
        Vec3 local = turn(pixel.scale(1 / 16.0), state.getValue(KineticProcessingBlock.FACING));
        Vec3 at = ShipSpace.toWorld(level, Vec3.atLowerCornerOf(machine.getBlockPos()).add(local));
        level.addParticle(particle, at.x, at.y, at.z, 0, rise, 0);
    }

    /** A point on the north-facing model (in blocks) turned about the block's centre to {@code facing}. */
    private static Vec3 turn(Vec3 point, Direction facing) {
        double x = point.x - 0.5, z = point.z - 0.5;
        return switch (facing) {
            case EAST -> new Vec3(0.5 - z, point.y, 0.5 + x);
            case SOUTH -> new Vec3(0.5 - x, point.y, 0.5 - z);
            case WEST -> new Vec3(0.5 + z, point.y, 0.5 - x);
            default -> point;
        };
    }
}
