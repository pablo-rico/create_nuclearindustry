package org.papiricoh.create_nuclearindustry.antimatter.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import org.papiricoh.create_nuclearindustry.antimatter.generator.AnnihilationGeneratorBlockEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * An annihilation generator's life on the client, each tick: its drone ({@link GeneratorDroneSound}),
 * the core's heat easing towards the synced burn (so it warms up and dies down rather than snapping),
 * and now and then a spark at the porthole when it runs hard, or off the terminals when its cold
 * chamber is losing its field.
 */
public final class GeneratorClientEffects {
    private static final Map<AnnihilationGeneratorBlockEntity, State> STATES = new WeakHashMap<>();

    private static final class State {
        float heat, previousHeat;
        float output, previousOutput;
        @Nullable GeneratorDroneSound drone;
    }

    /**
     * How the core looks at one instant.
     *
     * @param heat    how lit the core is, 0 (cold) .. 1 (burning)
     * @param output  the output fraction it shows, 0..1
     * @param flicker brightness share this frame: 1, or dipping when the supply runs short
     */
    record Look(float heat, float output, float flicker) {}

    private GeneratorClientEffects() {}

    public static void tick(AnnihilationGeneratorBlockEntity generator) {
        Level level = generator.getLevel();
        if (level == null) {
            return;
        }
        State state = STATES.computeIfAbsent(generator, g -> {
            State s = new State();
            s.heat = s.previousHeat = g.burning() ? 1 : 0;
            s.output = s.previousOutput = (float) g.outputFraction();
            return s;
        });
        state.previousHeat = state.heat;
        state.previousOutput = state.output;
        state.heat += ((generator.burning() ? 1 : 0) - state.heat) * 0.15f;
        state.output += ((float) generator.outputFraction() - state.output) * 0.12f;

        Minecraft minecraft = Minecraft.getInstance();
        if (level == minecraft.level && GeneratorDroneSound.wants(generator) && !playing(state.drone)) {
            state.drone = new GeneratorDroneSound(generator);
            minecraft.getSoundManager().play(state.drone);
        }
        RandomSource random = level.random;
        if (generator.burning() && generator.outputFraction() > 0.5 && random.nextInt(14) == 0) {
            porthole(generator, level, random);
        }
        if (generator.fieldFailing() && random.nextInt(3) == 0) {
            terminalSpark(generator, level, random);
        }
    }

    static Look look(AnnihilationGeneratorBlockEntity generator, float partialTicks, float time) {
        State state = STATES.get(generator);
        float heat, output;
        if (state == null) {
            heat = generator.burning() ? 1 : 0;
            output = (float) generator.outputFraction();
        } else {
            heat = state.previousHeat + (state.heat - state.previousHeat) * partialTicks;
            output = state.previousOutput + (state.output - state.previousOutput) * partialTicks;
        }
        float flicker = 1;
        if (generator.shortSupply()) {
            flicker = TrapLook.noise((long) Math.floor(time * 2)) < 0.35f ? 0.45f : 0.85f + 0.15f * (float) Math.sin(time * 2.1);
        } else if (generator.burning()) {
            flicker = 0.93f + 0.07f * (float) Math.sin(time * (0.3 + 0.5 * output));
        }
        return new Look(Math.max(0, Math.min(1, heat)), Math.max(0, Math.min(1, output)), flicker);
    }

    private static boolean playing(@Nullable GeneratorDroneSound sound) {
        return sound != null && !sound.isStopped() && Minecraft.getInstance().getSoundManager().isActive(sound);
    }

    /** The generator's back, where the porthole is, in the world. */
    private static Direction back(AnnihilationGeneratorBlockEntity generator) {
        BlockState state = generator.getBlockState();
        return state.hasProperty(HorizontalKineticBlock.HORIZONTAL_FACING)
                ? state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING).getOpposite() : Direction.SOUTH;
    }

    /** A spark leaping off the porthole glass. */
    private static void porthole(AnnihilationGeneratorBlockEntity generator, Level level, RandomSource random) {
        Direction back = back(generator);
        Direction across = back.getClockWise();
        double sideways = (random.nextDouble() - 0.5) * 0.4;
        Vec3 at = Vec3.atCenterOf(generator.getBlockPos())
                .add(back.getStepX() * 0.52 + across.getStepX() * sideways, (random.nextDouble() - 0.4) * 0.35,
                        back.getStepZ() * 0.52 + across.getStepZ() * sideways);
        // Sparks only: a lingering mote would hang outside the glass like a stuck pixel.
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, back.getStepX() * 0.05 + (random.nextDouble() - 0.5) * 0.04,
                (random.nextDouble() - 0.3) * 0.06, back.getStepZ() * 0.05 + (random.nextDouble() - 0.5) * 0.04);
    }

    /** A spark off one of the FE terminals on the generator's sides. */
    private static void terminalSpark(AnnihilationGeneratorBlockEntity generator, Level level, RandomSource random) {
        Direction side = back(generator).getClockWise();
        if (random.nextBoolean()) {
            side = side.getOpposite();
        }
        Vec3 at = Vec3.atCenterOf(generator.getBlockPos()).add(side.getStepX() * 0.52, (random.nextDouble() - 0.5) * 0.3,
                side.getStepZ() * 0.52);
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, side.getStepX() * 0.08, (random.nextDouble() - 0.3) * 0.08,
                side.getStepZ() * 0.08);
    }
}
