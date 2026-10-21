package org.papiricoh.create_nuclearindustry.fusion.heating.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearSounds;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakReadings;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakShellRole;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakShellRoles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Client tick of every tokamak controller (installed on {@link org.papiricoh.create_nuclearindustry.fusion.api.TokamakClientHooks}):
 * the magnets' hum and the plasma's roar, the crack of breakdown, frost on a cold cryostat and
 * steam at the plasma steam ports. Also smooths the synced readings for {@link PlasmaTorusRenderer},
 * which arrive only every half second.
 */
public final class TokamakClientEffects {
    /** Sounds and particles only near the machine, in blocks from the nearest point of the ring. */
    private static final double EFFECT_RANGE = 64.0;
    /** Coils colder than this frost the cryostat, in kelvin. */
    static final double FROST_TEMPERATURE = 30.0;
    /** Steam port vents are looked for again this often, in ticks. */
    private static final int VENT_RESCAN = 100;

    private static final Map<FusionReactorBlockEntity, State> STATES = new WeakHashMap<>();

    private TokamakClientEffects() {}

    /** What the client remembers about one controller between ticks. */
    static final class State {
        boolean seen;
        TokamakPhase lastPhase = TokamakPhase.OFFLINE;
        @Nullable TokamakLoopSound hum;
        @Nullable TokamakLoopSound rumble;
        double temperature;
        double density;
        double instability;
        double previousTemperature;
        double previousDensity;
        double previousInstability;
        List<Vent> vents = List.of();
        long ventsScannedAt = Long.MIN_VALUE;
        BlockPos ventsCenter = BlockPos.ZERO;
        int ventsRadius;

        double temperature(float partial) {
            return previousTemperature + (temperature - previousTemperature) * partial;
        }

        double density(float partial) {
            return previousDensity + (density - previousDensity) * partial;
        }

        double instability(float partial) {
            return previousInstability + (instability - previousInstability) * partial;
        }
    }

    /** An open face of a plasma steam port, where steam puffs out. */
    record Vent(Vec3 pos, Direction side) {}

    @Nullable
    static State state(FusionReactorBlockEntity controller) {
        return STATES.get(controller);
    }

    public static void tick(FusionReactorBlockEntity controller) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = controller.getLevel();
        // Ponder scenes run in their own client level; keep them quiet.
        if (level == null || level != minecraft.level || minecraft.player == null) {
            return;
        }
        State state = STATES.computeIfAbsent(controller, c -> new State());
        TokamakReadings readings = controller.readings();
        smooth(state, readings);
        int radius = readings.majorRadius();
        BlockPos center = controller.getCenter();
        boolean formed = readings.formed() && radius >= TokamakGeometry.MIN_RADIUS;
        // On a ship the ring is laid out in its plot: see the player from there.
        Vec3 player = org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace.localView(level, center, minecraft.player.position());
        Vec3 nearest = formed ? nearestRingPoint(center, radius, player) : controller.getBlockPos().getCenter();
        boolean near = nearest.distanceToSqr(player) < EFFECT_RANGE * EFFECT_RANGE;

        if (near) {
            sounds(controller, state, readings, nearest);
        }
        if (formed && near && !minecraft.isPaused()) {
            RandomSource random = level.random;
            frost(level, center, radius, readings, random);
            steam(level, controller, state, center, radius, readings, random);
        }
        state.lastPhase = readings.phase();
        state.seen = true;
    }

    private static void smooth(State state, TokamakReadings readings) {
        state.previousTemperature = state.temperature;
        state.previousDensity = state.density;
        state.previousInstability = state.instability;
        if (!state.seen) {
            state.temperature = readings.temperature();
            state.density = readings.density();
            state.instability = readings.instability();
            state.previousTemperature = state.temperature;
            state.previousDensity = state.density;
            state.previousInstability = state.instability;
            return;
        }
        state.temperature += (readings.temperature() - state.temperature) * 0.25;
        state.density += (readings.density() - state.density) * 0.25;
        state.instability += (readings.instability() - state.instability) * 0.25;
    }

    // ------------------------------------------------------------------ sound

    private static void sounds(FusionReactorBlockEntity controller, State state, TokamakReadings readings, Vec3 nearest) {
        Minecraft minecraft = Minecraft.getInstance();
        if (TokamakLoopSound.Kind.HUM.wants(readings) && !playing(state.hum)) {
            state.hum = new TokamakLoopSound(TokamakLoopSound.Kind.HUM, controller);
            minecraft.getSoundManager().play(state.hum);
        }
        if (TokamakLoopSound.Kind.RUMBLE.wants(readings) && !playing(state.rumble)) {
            state.rumble = new TokamakLoopSound(TokamakLoopSound.Kind.RUMBLE, controller);
            minecraft.getSoundManager().play(state.rumble);
        }
        if (state.seen && readings.phase() == TokamakPhase.BREAKDOWN && state.lastPhase != TokamakPhase.BREAKDOWN) {
            minecraft.getSoundManager().play(new SimpleSoundInstance(AllNuclearSounds.PLASMA_BREAKDOWN.get(), SoundSource.BLOCKS,
                    2.0f, 0.9f + 0.2f * controller.getLevel().random.nextFloat(), SoundInstance.createUnseededRandom(),
                    nearest.x, nearest.y, nearest.z));
        }
    }

    private static boolean playing(@Nullable TokamakLoopSound sound) {
        return sound != null && !sound.isStopped() && Minecraft.getInstance().getSoundManager().isActive(sound);
    }

    /** The point on the plasma's centre line closest to {@code from}: where the machine sounds from. */
    static Vec3 nearestRingPoint(BlockPos center, int radius, Vec3 from) {
        double cx = center.getX() + 0.5;
        double cy = center.getY() + 0.5;
        double cz = center.getZ() + 0.5;
        double dx = from.x - cx;
        double dz = from.z - cz;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 1.0e-3) {
            return new Vec3(cx + radius, cy, cz);
        }
        return new Vec3(cx + dx / distance * radius, cy, cz + dz / distance * radius);
    }

    // ------------------------------------------------------------------ particles

    /** Snowflakes drifting off the outside of the shell while the coils are superconducting-cold. */
    private static void frost(Level level, BlockPos center, int radius, TokamakReadings readings, RandomSource random) {
        double temperature = readings.magnetTemperature();
        if (!(temperature < FROST_TEMPERATURE)) {
            return;
        }
        double coldness = Math.min(1.0, (FROST_TEMPERATURE - temperature) / (FROST_TEMPERATURE - 5.0));
        double expected = coldness * 0.5 * radius / TokamakGeometry.MIN_RADIUS;
        int count = (int) expected + (random.nextDouble() < expected - (int) expected ? 1 : 0);
        TokamakGeometry geometry = TokamakGeometry.of(radius);
        for (int n = 0; n < count; n++) {
            for (int attempt = 0; attempt < 4; attempt++) {
                int i = random.nextInt(geometry.shellCount());
                Direction side = Direction.values()[random.nextInt(6)];
                int dx = geometry.shellX(i);
                int dy = geometry.shellY(i);
                int dz = geometry.shellZ(i);
                if (!opensOutside(level, geometry, center, dx, dy, dz, side)) {
                    continue;
                }
                Vec3 face = faceSpot(center.offset(dx, dy, dz), side, random);
                level.addParticle(ParticleTypes.SNOWFLAKE, face.x, face.y, face.z,
                        side.getStepX() * 0.01, -0.02 + side.getStepY() * 0.01, side.getStepZ() * 0.01);
                break;
            }
        }
    }

    /** Puffs of steam at the plasma steam ports while the plasma makes power. */
    private static void steam(Level level, FusionReactorBlockEntity controller, State state, BlockPos center, int radius,
                              TokamakReadings readings, RandomSource random) {
        if (!(readings.fusionPower() > 1.0)) {
            return;
        }
        long now = level.getGameTime();
        if (now - state.ventsScannedAt >= VENT_RESCAN || !state.ventsCenter.equals(center) || state.ventsRadius != radius) {
            state.vents = findVents(level, center, radius);
            state.ventsScannedAt = now;
            state.ventsCenter = center;
            state.ventsRadius = radius;
        }
        double chance = Math.min(0.6, 0.08 + readings.fusionPower() / 500.0);
        for (Vent vent : state.vents) {
            if (random.nextDouble() >= chance) {
                continue;
            }
            Direction side = vent.side();
            double speed = 0.05 + 0.05 * random.nextDouble();
            level.addParticle(ParticleTypes.CLOUD, vent.pos().x, vent.pos().y, vent.pos().z,
                    side.getStepX() * speed, 0.03 + side.getStepY() * speed, side.getStepZ() * speed);
        }
    }

    static List<Vent> findVents(Level level, BlockPos center, int radius) {
        TokamakGeometry geometry = TokamakGeometry.of(radius);
        List<Vent> vents = new ArrayList<>();
        for (int i = 0; i < geometry.shellCount(); i++) {
            int dx = geometry.shellX(i);
            int dy = geometry.shellY(i);
            int dz = geometry.shellZ(i);
            BlockPos pos = center.offset(dx, dy, dz);
            if (TokamakShellRoles.roleOf(level.getBlockState(pos)) != TokamakShellRole.STEAM_PORT) {
                continue;
            }
            for (Direction side : Direction.values()) {
                if (opensOutside(level, geometry, center, dx, dy, dz, side)) {
                    vents.add(new Vent(Vec3.atCenterOf(pos).add(side.getStepX() * 0.6, side.getStepY() * 0.6, side.getStepZ() * 0.6), side));
                }
            }
        }
        return vents;
    }

    /** Whether the shell cell's face on {@code side} looks out of the torus into open air. */
    private static boolean opensOutside(Level level, TokamakGeometry geometry, BlockPos center, int dx, int dy, int dz, Direction side) {
        int nx = dx + side.getStepX();
        int ny = dy + side.getStepY();
        int nz = dz + side.getStepZ();
        TokamakGeometry.Cell cell = geometry.classify(nx, ny, nz);
        if (cell == TokamakGeometry.Cell.SHELL || cell == TokamakGeometry.Cell.PLASMA) {
            return false;
        }
        return level.getBlockState(center.offset(nx, ny, nz)).isAir();
    }

    private static Vec3 faceSpot(BlockPos pos, Direction side, RandomSource random) {
        double u = random.nextDouble() - 0.5;
        double v = random.nextDouble() - 0.5;
        double x = pos.getX() + 0.5 + side.getStepX() * 0.56 + (side.getAxis() == Direction.Axis.X ? 0 : u * 0.9);
        double y = pos.getY() + 0.5 + side.getStepY() * 0.56 + (side.getAxis() == Direction.Axis.Y ? 0 : v * 0.9);
        double z = pos.getZ() + 0.5 + side.getStepZ() * 0.56
                + (side.getAxis() == Direction.Axis.Z ? 0 : (side.getAxis() == Direction.Axis.X ? u : v) * 0.9);
        return new Vec3(x, y, z);
    }
}
