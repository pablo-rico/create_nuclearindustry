package org.papiricoh.create_nuclearindustry.antimatter.client;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapState;
import org.papiricoh.create_nuclearindustry.antimatter.injector.AntiprotonInjectorBlockEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * An antiproton injector's shots as the client sees them. A shot shows as the time since the synced
 * last shot falling back (it only ever grows between shots); from then on the renderer flares the chamber, lights the nozzle and draws the beam
 * for {@link #FLASH_TICKS}, and a puff of cyan sparks leaves the port on the outer face. Counting
 * client ticks rather than the level's game time also works in a Ponder scene, where the game time
 * stands still. Sparks too when its chamber quenches.
 */
public final class InjectorClientEffects {
    /** How long a shot's light lasts, ticks. */
    static final int FLASH_TICKS = 14;
    private static final DustParticleOptions CYAN = new DustParticleOptions(new Vector3f(0x3F / 255f, 0xD8 / 255f, 0xE8 / 255f), 0.9f);
    private static final Map<AntiprotonInjectorBlockEntity, State> STATES = new WeakHashMap<>();

    private static final class State {
        long since;
        int age = Integer.MAX_VALUE;
    }

    private InjectorClientEffects() {}

    public static void tick(AntiprotonInjectorBlockEntity injector) {
        Level level = injector.getLevel();
        if (level == null) {
            return;
        }
        long since = injector.ticksSinceShot();
        // A shot already past when the block came into view does not flash again.
        State state = STATES.computeIfAbsent(injector, i -> {
            State s = new State();
            s.since = since;
            return s;
        });
        if (since < state.since) {
            state.age = 0;
            burst(injector, level, level.random);
        } else if (state.age < Integer.MAX_VALUE) {
            state.age++;
        }
        state.since = since;
        if (injector.state() == TrapState.QUENCHING && level.random.nextInt(2) == 0) {
            spark(injector, level, level.random);
        }
    }

    /** How lit the shot still is, 1 the moment it fires and 0 after {@link #FLASH_TICKS}. */
    static float flash(AntiprotonInjectorBlockEntity injector, float partialTicks) {
        State state = STATES.get(injector);
        if (state == null || state.age >= FLASH_TICKS) {
            return 0;
        }
        float t = (state.age + partialTicks) / FLASH_TICKS;
        return Math.max(0, 1 - t) * Math.max(0, 1 - t);
    }

    /** The middle of the outer face, where the port and its chamber are. */
    private static Vec3 front(AntiprotonInjectorBlockEntity injector) {
        Direction out = injector.facing();
        return Vec3.atCenterOf(injector.getBlockPos()).add(out.getStepX() * 0.53, out.getStepY() * 0.53 + (out.getAxis().isVertical() ? 0 : 0.08),
                out.getStepZ() * 0.53);
    }

    /** A puff of sparks out of the port as the dose leaves. */
    private static void burst(AntiprotonInjectorBlockEntity injector, Level level, RandomSource random) {
        Direction out = injector.facing();
        Vec3 at = front(injector);
        for (int i = 0; i < 10; i++) {
            double vx = out.getStepX() * 0.08 + (random.nextDouble() - 0.5) * 0.12;
            double vy = out.getStepY() * 0.08 + (random.nextDouble() - 0.5) * 0.12;
            double vz = out.getStepZ() * 0.08 + (random.nextDouble() - 0.5) * 0.12;
            level.addParticle(CYAN, at.x, at.y, at.z, vx, vy, vz);
        }
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, out.getStepX() * 0.2, out.getStepY() * 0.2, out.getStepZ() * 0.2);
    }

    private static void spark(AntiprotonInjectorBlockEntity injector, Level level, RandomSource random) {
        Direction out = injector.facing();
        Vec3 at = front(injector).add((random.nextDouble() - 0.5) * 0.5, (random.nextDouble() - 0.5) * 0.5, (random.nextDouble() - 0.5) * 0.5);
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, out.getStepX() * 0.1 + (random.nextDouble() - 0.5) * 0.06,
                out.getStepY() * 0.1 + (random.nextDouble() - 0.3) * 0.06, out.getStepZ() * 0.1 + (random.nextDouble() - 0.5) * 0.06);
    }
}
