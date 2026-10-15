package org.papiricoh.create_nuclearindustry.antimatter.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapState;
import org.papiricoh.create_nuclearindustry.antimatter.trap.PenningTrapBlockEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * A Penning trap's life on the client, each tick: the field's hum ({@link TrapHumSound}), the
 * medium trap's gauge needle easing towards the synced field, and sparks leaping off the magnet
 * heads while it quenches (now and then too when it runs short of energy).
 */
public final class TrapClientEffects {
    private static final Map<PenningTrapBlockEntity, State> STATES = new WeakHashMap<>();
    private static final float PX = 1 / 16f;

    private static final class State {
        float needle;
        float previousNeedle;
        @Nullable TrapHumSound hum;
    }

    private TrapClientEffects() {}

    public static void tick(PenningTrapBlockEntity trap) {
        Level level = trap.getLevel();
        if (level == null) {
            return;
        }
        State state = STATES.computeIfAbsent(trap, t -> {
            State s = new State();
            s.needle = s.previousNeedle = t.fieldStrength();
            return s;
        });
        state.previousNeedle = state.needle;
        state.needle += (trap.fieldStrength() - state.needle) * 0.25f;

        Minecraft minecraft = Minecraft.getInstance();
        if (level == minecraft.level && TrapHumSound.wants(trap) && !playing(state.hum)) {
            state.hum = new TrapHumSound(trap);
            minecraft.getSoundManager().play(state.hum);
        }
        TrapState trapState = trap.state();
        RandomSource random = level.random;
        if (trapState == TrapState.QUENCHING) {
            int sparks = 1 + random.nextInt(3);
            for (int i = 0; i < sparks; i++) {
                spark(trap, level, random, true);
            }
        } else if (trapState == TrapState.LOW_POWER && trap.stored() > 0 && random.nextInt(12) == 0) {
            spark(trap, level, random, false);
        }
    }

    /** The needle's field share between the last two ticks. */
    static float needle(PenningTrapBlockEntity trap, float partialTicks) {
        State state = STATES.get(trap);
        if (state == null) {
            return trap.fieldStrength();
        }
        return state.previousNeedle + (state.needle - state.previousNeedle) * partialTicks;
    }

    private static boolean playing(@Nullable TrapHumSound sound) {
        return sound != null && !sound.isStopped() && Minecraft.getInstance().getSoundManager().isActive(sound);
    }

    /** A spark off the edge of one of the field bands, flying outwards. */
    private static void spark(PenningTrapBlockEntity trap, Level level, RandomSource random, boolean fierce) {
        TrapShape shape = TrapShape.of(trap.tier());
        float[] band = random.nextBoolean() ? shape.bandLow() : shape.bandHigh();
        float half = shape.bandHalf() + 0.5f;
        // A point on the square band's rim.
        float along = (random.nextFloat() * 2 - 1) * half;
        float x, z, nx, nz;
        switch (random.nextInt(4)) {
            case 0 -> { x = along; z = -half; nx = 0; nz = -1; }
            case 1 -> { x = along; z = half; nx = 0; nz = 1; }
            case 2 -> { x = -half; z = along; nx = -1; nz = 0; }
            default -> { x = half; z = along; nx = 1; nz = 0; }
        }
        Vec3 at = Vec3.atLowerCornerOf(trap.getBlockPos()).add((shape.cx() + x) * PX, (band[0] + band[1]) * 0.5 * PX, (shape.cz() + z) * PX);
        double speed = fierce ? 0.06 + random.nextDouble() * 0.08 : 0.03;
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, nx * speed + (random.nextDouble() - 0.5) * 0.04,
                (random.nextDouble() - 0.3) * 0.08, nz * speed + (random.nextDouble() - 0.5) * 0.04);
        if (fierce && random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, at.x, at.y, at.z, nx * 0.01, 0.01, nz * 0.01);
        }
    }
}
