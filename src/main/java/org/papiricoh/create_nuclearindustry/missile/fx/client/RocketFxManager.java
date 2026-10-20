package org.papiricoh.create_nuclearindustry.missile.fx.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightFxEvent;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightFxState;
import org.papiricoh.create_nuclearindustry.missile.flight.RocketFxSource;
import org.papiricoh.create_nuclearindustry.missile.flight.RocketShape;
import org.papiricoh.create_nuclearindustry.missile.flight.client.ClientFlight;
import org.papiricoh.create_nuclearindustry.missile.flight.client.ClientFlights;
import org.papiricoh.create_nuclearindustry.missile.flight.client.FlightFxListener;
import org.papiricoh.create_nuclearindustry.missile.fx.FxBudget;
import org.papiricoh.create_nuclearindustry.missile.fx.FxCurves;
import org.papiricoh.create_nuclearindustry.missile.fx.PlumeModel;
import org.papiricoh.create_nuclearindustry.missile.fx.RocketAcoustics;
import org.papiricoh.create_nuclearindustry.missile.fx.RocketFxRegistry;
import org.papiricoh.create_nuclearindustry.missile.fx.TrailModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The rocket effects' brain on the client. It hears each flight's moments ({@link ClientFlights}), follows every flight the
 * server tells it of each tick, and keeps what outlives a moment: trails, flashes, the dust of a launch, the steam of a
 * countdown and the sounds. {@link RocketFxRender} draws what it holds. Spends no more than {@link FxBudget} allows.
 */
final class RocketFxManager implements FlightFxListener {
    static final RocketFxManager INSTANCE = new RocketFxManager();
    /** Rockets heard roaring at once, at most (the nearest). */
    private static final int MAX_ROARS = 6;
    /** Flights remembered at once, at most; the longest gone are forgotten first. */
    private static final int MAX_REMEMBERED = 96;
    /** Within this many blocks of the ground under it a rocket's exhaust blasts the ground. */
    private static final double BLAST_HEIGHT = 28;
    /** A falling stage this many ticks between puffs of smoke, near and further. */
    private static final int DEBRIS_PUFF_NEAR = 2, DEBRIS_PUFF_FAR = 5;

    final Map<UUID, FlightFx> flights = new LinkedHashMap<>();
    final List<Burst> bursts = new ArrayList<>();
    final SiloFx silos = new SiloFx();
    private final List<PendingLaunch> pending = new ArrayList<>();
    private final RandomSource random = RandomSource.create();
    @Nullable
    private ClientLevel level;
    /** Whether a falling stage was seen near last tick, so the frame knows to look for them. */
    boolean debrisNear;

    /** A lift-off heard before the flight itself: its first state comes a moment later. */
    private record PendingLaunch(Vec3 pos, long tick, @Nullable SiloFx.Mouth silo, double groundY) {
    }

    private RocketFxManager() {
    }

    // ------------------------------------------------------------------ bookkeeping

    void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private void clear() {
        for (FlightFx fx : flights.values()) stopSounds(fx);
        flights.clear();
        bursts.clear();
        pending.clear();
        silos.clear();
        RocketParticles.clear();
        level = null;
    }

    private boolean checkLevel() {
        ClientLevel current = Minecraft.getInstance().level;
        if (current != level) {
            clear();
            level = current;
        }
        return level != null;
    }

    /** The flight's effects, made the first time it is seen. */
    private FlightFx track(ClientFlight flight, long now) {
        FlightFx fx = flights.get(flight.id());
        if (fx == null) {
            fx = new FlightFx(flight.id());
            fx.update(flight);
            flights.put(flight.id(), fx);
            PendingLaunch launch = pendingNear(fx.position, now);
            if (launch != null) {
                pending.remove(launch);
                adopt(fx, launch);
            } else {
                fx.groundY = groundBelow(level, fx.position);
                fx.groundKnown = level.hasChunkAt(BlockPos.containing(fx.position));
                fx.launchY = fx.groundY;
            }
            forgetOldest();
        } else {
            fx.update(flight);
        }
        return fx;
    }

    @Nullable
    private PendingLaunch pendingNear(Vec3 pos, long now) {
        PendingLaunch best = null;
        double bestD = 48 * 48;
        for (PendingLaunch p : pending) {
            double d = p.pos.distanceToSqr(pos);
            if (now - p.tick <= 60 && d < bestD) {
                best = p;
                bestD = d;
            }
        }
        return best;
    }

    private void adopt(FlightFx fx, PendingLaunch launch) {
        fx.launchTick = launch.tick;
        fx.launchY = launch.pos.y;
        fx.groundY = launch.groundY;
        fx.groundKnown = launch.silo != null || level.hasChunkAt(BlockPos.containing(launch.pos));
        fx.silo = launch.silo;
    }

    private void forgetOldest() {
        if (flights.size() <= MAX_REMEMBERED) return;
        Iterator<FlightFx> it = flights.values().iterator();
        while (it.hasNext() && flights.size() > MAX_REMEMBERED) {
            FlightFx fx = it.next();
            if (fx.ended()) {
                stopSounds(fx);
                it.remove();
            }
        }
    }

    /** The ground's height under {@code pos}, or a guess if this client has not loaded it. */
    static double groundBelow(@Nullable ClientLevel level, Vec3 pos) {
        if (level != null && level.hasChunkAt(BlockPos.containing(pos))) {
            return Math.min(pos.y, level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(pos.x), (int) Math.floor(pos.z)));
        }
        return Math.min(pos.y, 64);
    }

    private void end(@Nullable FlightFx fx, long now, boolean nuclear) {
        if (fx == null || fx.ended()) return;
        fx.endedTick = now;
        fx.nuclearEnd = nuclear;
        fx.flight = null;
        fx.plume = Math.min(fx.plume, nuclear ? fx.plume : 0.5f);
    }

    // ------------------------------------------------------------------ moments

    @Override
    public void onEvent(@Nullable ClientFlight flight, FlightFxEvent event, int stage, Vec3 pos, Vec3 vel, int flags) {
        Minecraft mc = Minecraft.getInstance();
        if (!checkLevel() || !ClientConfig.rocketEffects) return;
        long now = level.getGameTime();
        Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
        double distance = eye.distanceTo(pos);
        FlightFx fx = flight == null ? null : track(flight, now);
        switch (event) {
            case LAUNCH -> launch(fx, pos, now, distance);
            case STAGE_SEPARATION -> separation(fx, stage, pos, vel, now, distance);
            case STAGE_IGNITION -> {
                if (fx != null) {
                    fx.ullageFrom = now;
                    fx.litAfter = now + FlightFx.ULLAGE_TICKS;
                }
                RocketSounds.oneShot(RocketFxRegistry.ROCKET_IGNITION.get(), pos, distance, 1800, 0.7f, 1.25f, true);
            }
            case BURNOUT -> {
                if (fx != null) {
                    fx.burnoutTick = now;
                    if (distance < FxBudget.MID) puffAtNozzles(fx, stage, 6, 0.8f);
                }
            }
            case PAYLOAD_SEPARATION -> payloadSeparation(fx, pos, vel, now, distance);
            case IMPACT -> impact(fx, pos, flags, now, distance);
            case DEBRIS_IMPACT -> {
                double ground = groundBelow(level, pos);
                boolean water = water(pos, ground);
                bursts.add(new Burst(Burst.Kind.DUST_RING, new Vec3(pos.x, ground, pos.z), null, now, 50, 10, water ? 0xE6ECF0 : 0x9C8E78, ground));
                if (distance < FxBudget.MID) dust(pos, ground, 18, 0.8, water);
                RocketSounds.oneShot(RocketFxRegistry.DEBRIS_IMPACT.get(), pos, distance, 900, 0.9f, 1.0f, true);
            }
            case ABORT -> end(fx, now, false);
            default -> {
            }
        }
    }

    private void launch(@Nullable FlightFx fx, Vec3 pos, long now, double distance) {
        SiloFx.Mouth mouth = silos.launchedFrom(pos);
        double ground = mouth != null ? mouth.y() : groundBelow(level, pos);
        PendingLaunch launch = new PendingLaunch(pos, now, mouth, ground);
        if (fx != null) {
            adopt(fx, launch);
        } else {
            pending.add(launch);
        }
        RocketSounds.oneShot(RocketFxRegistry.ROCKET_IGNITION.get(), pos, distance, 2600, 1.0f, 1.0f, true);
        if (mouth != null) {
            // Fire fills the shaft and bursts out of its mouth.
            bursts.add(new Burst(Burst.Kind.SHAFT, new Vec3(mouth.x(), mouth.floorY(), mouth.z()), null, now, 40, 1.5f, 0xFFD49A,
                    mouth.y() - mouth.floorY() + 2));
            bursts.add(new Burst(Burst.Kind.FLASH, mouth.centre().add(0, 1, 0), null, now, 16, 9, 0xFFE2B0, 0));
        } else {
            bursts.add(new Burst(Burst.Kind.FLASH, pos, null, now, 14, 6, 0xFFE2B0, 0));
        }
        bursts.add(new Burst(Burst.Kind.DUST_RING, new Vec3(pos.x, ground, pos.z), null, now, (int) (FxCurves.DUST_SECONDS * 1.6 * 20),
                (float) FxCurves.DUST_END, 0xA89C86, ground));
    }

    private void separation(@Nullable FlightFx fx, int dropped, Vec3 pos, Vec3 vel, long now, double distance) {
        RocketShape shape = fx != null ? fx.shape : RocketShape.LEGACY;
        Quaternionf attitude = fx != null ? fx.attitude : new Quaternionf();
        RocketShape.Stage stage = shape.stage(dropped);
        double joint = stage == null ? 1 : stage.top() + 1;
        Vec3 axis = rotate(attitude, 0, 1, 0);
        Vec3 at = pos.add(axis.scale(joint));
        float radius = shape.radius();
        bursts.add(new Burst(Burst.Kind.FLASH, at, axis, now, 9, radius * 7, 0xFFF0D8, 0));
        bursts.add(new Burst(Burst.Kind.RING, at, axis, now, 16, 7 + radius * 4, 0xFFD8A8, 0));
        if (distance < FxBudget.MID) ringOfSmoke(at, axis, vel, 14, radius + 0.4, 0.9f);
        RocketSounds.oneShot(RocketFxRegistry.STAGE_SEPARATION.get(), at, distance, 1400, 1.0f, 1.0f, true);
    }

    private void payloadSeparation(@Nullable FlightFx fx, Vec3 pos, Vec3 vel, long now, double distance) {
        RocketShape shape = fx != null ? fx.shape : RocketShape.LEGACY;
        Quaternionf attitude = fx != null ? fx.attitude : new Quaternionf();
        Vec3 axis = rotate(attitude, 0, 1, 0);
        Vec3 at = pos.add(axis.scale(shape.payloadBottom()));
        bursts.add(new Burst(Burst.Kind.FLASH, at, axis, now, 7, shape.radius() * 4, 0xFFF4E0, 0));
        bursts.add(new Burst(Burst.Kind.RING, at, axis, now, 12, 3 + shape.radius() * 3, 0xFFE0C0, 0));
        if (distance < FxBudget.MID) ringOfSmoke(at, axis, vel, 10, shape.radius() + 0.2, 0.6f);
        RocketSounds.oneShot(RocketFxRegistry.STAGE_SEPARATION.get(), at, distance, 1000, 0.6f, 1.4f, true);
    }

    private void impact(@Nullable FlightFx fx, Vec3 pos, int flags, long now, double distance) {
        if (fx != null && fx.passage.finish()) boom(pos, distance);
        if ((flags & FlightFxState.NUCLEAR_WARHEAD) != 0) {
            // The detonation's own effects take over: only let the warhead's glow go out.
            end(fx, now, true);
            return;
        }
        end(fx, now, false);
        double ground = Math.min(pos.y, groundBelow(level, pos));
        bursts.add(new Burst(Burst.Kind.FLASH, pos.add(0, 1, 0), null, now, 8, 5, 0xFFC890, 0));
        boolean water = water(pos, ground);
        bursts.add(new Burst(Burst.Kind.DUST_RING, new Vec3(pos.x, ground, pos.z), null, now, 70, 16, water ? 0xE6ECF0 : 0x9C8E78, ground));
        if (distance < FxBudget.MID) dust(pos, ground, 36, 1.4, water);
        RocketSounds.oneShot(RocketFxRegistry.DEBRIS_IMPACT.get(), pos, distance, 1400, 1.0f, 0.7f, true);
    }

    private void boom(Vec3 at, double distance) {
        RocketSounds.oneShot(RocketFxRegistry.SONIC_BOOM.get(), at, distance, 400, 1.0f, 1.0f, false);
    }

    // ------------------------------------------------------------------ each tick

    void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!checkLevel()) return;
        if (mc.isPaused()) return;
        if (!ClientConfig.rocketEffects) {
            if (!flights.isEmpty() || !bursts.isEmpty()) clear();
            level = mc.level;
            return;
        }
        long now = level.getGameTime();
        Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
        RocketParticles.prune();
        silos.tick(level, eye, now, random);
        pending.removeIf(p -> now - p.tick > 60);

        Set<UUID> present = new HashSet<>();
        for (ClientFlight flight : ClientFlights.all()) {
            present.add(flight.id());
            track(flight, now);
        }
        List<FlightFx> list = new ArrayList<>(flights.values());
        double[] distances = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            FlightFx fx = list.get(i);
            if (!present.contains(fx.id)) end(fx, now, fx.nuclearEnd);
            fx.distance = eye.distanceTo(fx.position);
            fx.lod = FxBudget.lod(fx.distance);
            // Flights with nothing left to draw of a trail do not take a place.
            distances[i] = fx.trail.isEmpty() && !fx.burning() ? Double.MAX_VALUE : fx.distance;
        }
        boolean[] trailed = FxBudget.trailed(distances);
        int wanting = 0;
        for (FlightFx fx : list) if (fx.lod.particles() && fx.plume > 0.05f) wanting++;
        for (int i = 0; i < list.size(); i++) {
            FlightFx fx = list.get(i);
            fx.trailed = trailed[i];
            step(fx, now, eye, wanting);
        }
        sounds(list, now);
        debris(eye, now);
        burstsTick(now);
        flights.values().removeIf(fx -> {
            boolean gone = fx.ended() && now - fx.endedTick > 40 && fx.trail.isEmpty() && fx.streak.isEmpty();
            if (gone) stopSounds(fx);
            return gone;
        });
        bursts.removeIf(b -> b.over(now));
    }

    private void step(FlightFx fx, long now, Vec3 eye, int wanting) {
        boolean lit = fx.burning() && now >= fx.litAfter;
        float target = lit ? 1 : 0;
        fx.plume += Math.max(-0.14f, Math.min(0.3f, target - fx.plume));
        fx.remember();
        if (fx.launchTick >= 0 && fx.clearedTick < 0 && !fx.rising()) fx.clearedTick = now;
        if (!fx.groundKnown && fx.launchTick >= 0 && level.hasChunkAt(BlockPos.containing(fx.position))) {
            // Its launch site has loaded here since: the ground it blasts is where it really is.
            fx.groundKnown = true;
            fx.groundY = Math.min(fx.launchY, groundBelow(level, fx.position));
        }

        fx.trail.expire(now, ClientConfig.rocketTrailSeconds * 20);
        Vec3 tail = null;
        if (!fx.ended() && fx.plume > 0.05f) {
            Vec3 nozzle = nozzleCentre(fx);
            PlumeModel.Plume plume = plumeOf(fx, nozzle.y, 1);
            Vec3 axis = fx.axis();
            tail = nozzle.subtract(axis.scale(Math.min(plume.length() * 0.55, Math.max(0.5, clipLength(fx, nozzle, axis)))));
            boolean inShaft = fx.silo != null && nozzle.y < fx.silo.y();
            if (fx.plume > 0.3f && !inShaft && fx.trail.due(now)) {
                fx.trail.add(new TrailModel.Point(tail.x, tail.y, tail.z, now, (float) Math.min(1, 0.25 + plume.smoke()),
                        (float) (plume.endRadius() * 1.3)));
            }
            if (fx.lod.particles()) {
                exhaust(fx, plume, nozzle, axis, tail, now, wanting);
                groundBlast(fx, nozzle, plume, wanting);
            }
        }
        if (fx.lod.particles() && !fx.ended() && now >= fx.ullageFrom && now < fx.litAfter) ullage(fx);
        fx.lastTail = tail;

        if (fx.descending() && !fx.ended()) {
            fx.pushStreak(fx.position.add(fx.axis().scale(fx.shape.height())));
        } else if (!fx.streak.isEmpty()) {
            fx.streak.removeFirst();
        }
        if (!fx.ended() && fx.passage.update(fx.distance, fx.velocity.length(), fx.descending())) boom(fx.position, fx.distance);
    }

    /** Where the burning stage's nozzles are, on average, at the end of this tick. */
    static Vec3 nozzleCentre(FlightFx fx) {
        RocketShape.Stage stage = fx.shape.stage(fx.stage);
        if (stage == null || stage.nozzles().isEmpty()) return fx.position;
        double x = 0, y = 0, z = 0;
        for (RocketShape.Nozzle n : stage.nozzles()) {
            x += n.x();
            y += n.y();
            z += n.z();
        }
        int count = stage.nozzles().size();
        Vec3 local = rotate(fx.attitude, (float) (x / count), (float) (y / count), (float) (z / count));
        return fx.position.add(local);
    }

    /** One nozzle's plume as burning now, at height {@code y}. */
    static PlumeModel.Plume plumeOf(FlightFx fx, double y, double throttle) {
        RocketShape.Stage stage = fx.shape.stage(fx.stage);
        boolean liquid = stage != null && stage.propellant() == RocketShape.Propellant.LIQUID;
        int nozzles = stage == null ? 1 : Math.max(1, stage.nozzles().size());
        double r = fx.shape.radius() * 0.8 / Math.sqrt(nozzles);
        return PlumeModel.of(liquid, r, y - fx.launchY, y, MissileConfig.atmosphereTop, throttle * fx.plume);
    }

    static boolean liquid(FlightFx fx) {
        RocketShape.Stage stage = fx.shape.stage(fx.stage);
        return stage != null && stage.propellant() == RocketShape.Propellant.LIQUID;
    }

    /** How far the plume reaches down {@code axis} before the ground (or the silo's floor) stops it. */
    static double clipLength(FlightFx fx, Vec3 nozzle, Vec3 axis) {
        double floor = fx.silo != null && nozzle.y < fx.silo.y() + 0.5 ? fx.silo.floorY() : fx.groundY;
        // The exhaust runs down -axis: where does that meet the floor's height?
        if (axis.y <= 0.05) return Double.MAX_VALUE;
        return Math.max(0, (nozzle.y - floor) / axis.y);
    }

    /** Smoke and embers thrown out of the nozzles, spread along the path flown this tick. */
    private void exhaust(FlightFx fx, PlumeModel.Plume plume, Vec3 nozzle, Vec3 axis, Vec3 tail, long now, int wanting) {
        boolean near = fx.lod == FxBudget.Lod.NEAR;
        boolean inShaft = fx.silo != null && nozzle.y < fx.silo.y();
        if (inShaft) return;
        Vec3 from = fx.lastTail != null ? fx.lastTail : tail;
        boolean liquid = liquid(fx);
        int wanted = (int) Math.round((near ? 4 : (now % 2 == 0 ? 1 : 0)) * ClientConfig.rocketDetail * (0.35 + plume.smoke()) * fx.plume);
        int quota = FxBudget.quota(wanted, RocketParticles.alive(), wanting);
        float grey = liquid ? 0.95f : 0.84f;
        for (int i = 0; i < quota; i++) {
            Vec3 at = from.lerp(tail, (i + random.nextDouble()) / Math.max(1, quota));
            double spread = plume.endRadius() * 0.5;
            Vec3 jitter = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).scale(spread);
            Vec3 v = axis.scale(-0.15 - 0.2 * random.nextDouble()).add(jitter.scale(0.03));
            float size = (float) (plume.endRadius() * (near ? 1.4 : 3.0));
            RocketParticles.smoke(level, !liquid && random.nextFloat() < 0.4f, at.x + jitter.x, at.y + jitter.y, at.z + jitter.z, v.x, v.y, v.z,
                    new RocketParticles.Puff(grey, grey - 0.01f, grey - 0.03f, liquid ? 0.3f : 0.62f, size, size * (near ? 4.5f : 3f),
                            (near ? 70 : 90) + random.nextInt(40), liquid ? 2 : 5, 0.04f));
        }
        if (near && !liquid && fx.plume > 0.5f) {
            int embers = FxBudget.quota((int) Math.round(2 * ClientConfig.rocketDetail), RocketParticles.alive(), wanting);
            for (int i = 0; i < embers; i++) {
                Vec3 at = nozzle.subtract(axis.scale(plume.coreLength() * random.nextDouble()));
                Vec3 v = axis.scale(-0.5 - 0.6 * random.nextDouble()).add(random.nextGaussian() * 0.12, random.nextGaussian() * 0.12,
                        random.nextGaussian() * 0.12);
                RocketParticles.ember(level, at.x, at.y, at.z, v.x, v.y, v.z, 0.1f + random.nextFloat() * 0.08f, 10 + random.nextInt(14));
            }
        }
    }

    /**
     * While a rocket is low, its exhaust hits the ground (or is funnelled up its silo's shaft) and billows out: thick smoke
     * rolling outward along the ground, or boiling out of the silo's mouth.
     */
    private void groundBlast(FlightFx fx, Vec3 nozzle, PlumeModel.Plume plume, int wanting) {
        if (fx.lod != FxBudget.Lod.NEAR && fx.lod != FxBudget.Lod.MID) return;
        boolean inShaft = fx.silo != null && nozzle.y < fx.silo.y() + 1;
        double height = nozzle.y - (fx.silo != null ? fx.silo.y() : fx.groundY);
        double strength = inShaft ? 1 : 1 - FxCurves.clamp01(height / BLAST_HEIGHT);
        if (strength <= 0) return;
        int wanted = (int) Math.round((fx.lod == FxBudget.Lod.NEAR ? 6 : 2) * ClientConfig.rocketDetail * strength * fx.plume);
        int quota = FxBudget.quota(wanted, RocketParticles.alive(), wanting);
        boolean silo = fx.silo != null;
        double cx = silo ? fx.silo.x() : fx.position.x, cz = silo ? fx.silo.z() : fx.position.z;
        double cy = silo ? fx.silo.y() - 0.2 : fx.groundY + 0.3;
        for (int i = 0; i < quota; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double out = silo ? 0.08 + 0.22 * random.nextDouble() : 0.3 + 0.35 * random.nextDouble();
            double up = silo ? 0.25 + 0.4 * random.nextDouble() * strength : 0.02 + 0.08 * random.nextDouble();
            double r = silo ? random.nextDouble() * 1.2 : 0.5 + random.nextDouble() * 1.5;
            float tone = 0.74f + random.nextFloat() * 0.1f;
            float size = 1.2f + random.nextFloat() * 0.8f;
            RocketParticles.smoke(level, true, cx + Math.cos(a) * r, cy, cz + Math.sin(a) * r, Math.cos(a) * out * strength, up,
                    Math.sin(a) * out * strength,
                    new RocketParticles.Puff(tone, tone - 0.02f, tone - 0.05f, 0.8f, size, size * (silo ? 4.5f : 5.5f), 90 + random.nextInt(80), 4, 0.03f));
        }
    }

    /** Small white puffs from the next stage's ullage motors before it lights. */
    private void ullage(FlightFx fx) {
        RocketShape.Stage stage = fx.shape.stage(fx.stage);
        if (stage == null) return;
        Vec3 axis = fx.axis();
        for (RocketShape.Nozzle n : stage.nozzles()) {
            Vec3 at = fx.position.add(rotate(fx.attitude, n.x(), n.y(), n.z()));
            for (int i = 0; i < 2; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                Vec3 side = rotate(fx.attitude, (float) Math.cos(a), 0, (float) Math.sin(a)).scale(fx.shape.radius() + 0.1);
                Vec3 v = axis.scale(-0.45).add(side.scale(0.12));
                RocketParticles.smoke(level, false, at.x + side.x, at.y + side.y + 0.3 * axis.y, at.z + side.z, v.x, v.y, v.z,
                        new RocketParticles.Puff(0.97f, 0.97f, 0.98f, 0.6f, 0.25f, 1.3f, 14 + random.nextInt(8), 0, 0));
            }
        }
    }

    private void puffAtNozzles(FlightFx fx, int stage, int count, float alpha) {
        RocketShape.Stage s = fx.shape.stage(stage);
        if (s == null) return;
        Vec3 axis = fx.axis();
        for (RocketShape.Nozzle n : s.nozzles()) {
            Vec3 at = fx.position.add(rotate(fx.attitude, n.x(), n.y(), n.z()));
            for (int i = 0; i < count; i++) {
                Vec3 v = axis.scale(-0.2 - 0.2 * random.nextDouble()).add(random.nextGaussian() * 0.05, random.nextGaussian() * 0.05,
                        random.nextGaussian() * 0.05);
                RocketParticles.smoke(level, false, at.x, at.y, at.z, v.x, v.y, v.z,
                        new RocketParticles.Puff(0.88f, 0.88f, 0.87f, alpha, 0.6f, 3.0f, 40 + random.nextInt(30), 3, 0.02f));
            }
        }
    }

    /** A ring of smoke thrown out across the axis at a separation. */
    private void ringOfSmoke(Vec3 at, Vec3 axis, Vec3 vel, int count, double radius, float alpha) {
        Vec3 u = perpendicular(axis);
        Vec3 w = axis.cross(u);
        for (int i = 0; i < count; i++) {
            double a = (i + random.nextDouble() * 0.5) * 2 * Math.PI / count;
            Vec3 out = u.scale(Math.cos(a)).add(w.scale(Math.sin(a)));
            Vec3 p = at.add(out.scale(radius));
            // Left behind by the rocket, moving out.
            Vec3 v = out.scale(0.25 + 0.1 * random.nextDouble()).add(vel.scale(0.15));
            RocketParticles.smoke(level, false, p.x, p.y, p.z, v.x, v.y, v.z,
                    new RocketParticles.Puff(0.95f, 0.95f, 0.94f, alpha, 0.5f, 2.6f, 35 + random.nextInt(20), 2, 0));
        }
    }

    /** Whether what lies at {@code ground} under {@code pos} is water (a splash, not dust). */
    private boolean water(Vec3 pos, double ground) {
        BlockPos at = BlockPos.containing(pos.x, ground - 0.5, pos.z);
        return level.hasChunkAt(at) && !level.getFluidState(at).isEmpty();
    }

    /** Dust and debris thrown up by something hitting the ground, or spray off water. */
    private void dust(Vec3 pos, double ground, int count, double strength, boolean water) {
        for (int i = 0; i < count; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double out = (0.15 + 0.35 * random.nextDouble()) * strength;
            double up = (0.15 + 0.5 * random.nextDouble()) * strength;
            float tone = water ? 0.9f + random.nextFloat() * 0.08f : 0.55f + random.nextFloat() * 0.12f;
            float size = 0.8f + random.nextFloat();
            RocketParticles.smoke(level, true, pos.x, ground + 0.5, pos.z, Math.cos(a) * out, up * (water ? 1.4 : 1), Math.sin(a) * out,
                    new RocketParticles.Puff(water ? tone : tone + 0.05f, tone, water ? tone + 0.03f : tone - 0.07f, water ? 0.7f : 0.85f, size,
                            size * 4, 60 + random.nextInt(60), 0, -0.2f));
        }
        for (int i = 0; i < (water ? 0 : count / 2); i++) {
            double a = random.nextDouble() * Math.PI * 2;
            RocketParticles.ember(level, pos.x, ground + 0.6, pos.z, Math.cos(a) * 0.3, 0.3 + random.nextDouble() * 0.4, Math.sin(a) * 0.3,
                    0.12f, 14 + random.nextInt(12));
        }
    }

    /** Dust kicked up along the front of a launch's dust ring while it runs. */
    private void burstsTick(long now) {
        for (Burst burst : bursts) {
            if (burst.kind() != Burst.Kind.DUST_RING) continue;
            double seconds = burst.seconds(now, 0);
            if (seconds > FxCurves.DUST_SECONDS) continue;
            Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            if (eye.distanceTo(burst.at()) > FxBudget.MID) continue;
            double radius = FxCurves.dustRadius(seconds) * burst.size() / FxCurves.DUST_END;
            int count = FxBudget.quota((int) Math.round(5 * ClientConfig.rocketDetail), RocketParticles.alive(), 1);
            for (int i = 0; i < count; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                float shade = 0.92f + random.nextFloat() * 0.12f;
                int c = burst.colour();
                RocketParticles.smoke(level, true, burst.at().x + Math.cos(a) * radius, burst.height() + 0.4, burst.at().z + Math.sin(a) * radius,
                        Math.cos(a) * 0.12, 0.03 + 0.05 * random.nextDouble(), Math.sin(a) * 0.12,
                        new RocketParticles.Puff(Math.min(1, ((c >> 16) & 0xFF) / 255f * shade), Math.min(1, ((c >> 8) & 0xFF) / 255f * shade),
                                Math.min(1, (c & 0xFF) / 255f * shade), 0.7f, 1.2f, 4.0f, 50 + random.nextInt(40), 0, 0.01f));
            }
        }
    }

    /** Smoke off spent stages tumbling down, and flame from any still burning. */
    private void debris(Vec3 eye, long now) {
        debrisNear = false;
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof RocketFxSource source)) continue;
            Vec3 pos = source.fxPosition(1);
            double distance = eye.distanceTo(pos);
            if (distance > FxBudget.MID) continue;
            debrisNear = true;
            int every = distance <= FxBudget.NEAR ? DEBRIS_PUFF_NEAR : DEBRIS_PUFF_FAR;
            if ((now + entity.getId()) % every != 0) continue;
            RocketShape.Stage stage = source.fxStage();
            Quaternionf attitude = source.fxAttitude(1);
            // Its open top, where it came away, still breathes out vapour.
            Vec3 top = pos.add(rotate(attitude, 0, stage.top() - stage.bottom() + 1, 0));
            float size = distance <= FxBudget.NEAR ? 0.8f : 2.0f;
            RocketParticles.smoke(level, false, top.x, top.y, top.z, 0, 0.01, 0,
                    new RocketParticles.Puff(0.86f, 0.86f, 0.86f, 0.5f, size, size * 3.5f, 60 + random.nextInt(30), 0, 0.01f));
            if (source.fxBurning()) {
                for (RocketShape.Nozzle n : stage.nozzles()) {
                    Vec3 at = pos.add(rotate(attitude, n.x(), n.y() - stage.bottom(), n.z()));
                    RocketParticles.smoke(level, false, at.x, at.y, at.z, 0, 0, 0,
                            new RocketParticles.Puff(0.8f, 0.79f, 0.77f, 0.6f, size, size * 4, 70 + random.nextInt(30), 5, 0.01f));
                }
            }
        }
    }

    // ------------------------------------------------------------------ sound

    private void sounds(List<FlightFx> list, long now) {
        List<FlightFx> roaring = new ArrayList<>();
        for (FlightFx fx : list) {
            fx.roarWanted = false;
            if (!fx.ended() && fx.distance < RocketAcoustics.FAR_REACH && fx.heard(RocketAcoustics.delayTicks(fx.distance)) > 0.02f) {
                roaring.add(fx);
            }
            double heat = FxCurves.reentryHeat(fx.velocity.length(), FxCurves.airDepth(fx.position.y, MissileConfig.atmosphereTop));
            fx.hissWanted = !fx.ended() && fx.descending() && heat > 0.05 && fx.distance < 700;
            if (fx.hissWanted && (fx.hiss == null || fx.hiss.done())) {
                fx.hiss = new RocketSounds.Loop(RocketFxRegistry.REENTRY_HISS.get(), new HissSource(fx));
                Minecraft.getInstance().getSoundManager().play(fx.hiss);
            }
        }
        roaring.sort((a, b) -> Double.compare(a.distance, b.distance));
        for (int i = 0; i < roaring.size() && i < MAX_ROARS; i++) {
            FlightFx fx = roaring.get(i);
            fx.roarWanted = true;
            if (fx.roarNear == null || fx.roarNear.done()) {
                fx.roarNear = new RocketSounds.Loop(RocketFxRegistry.ROCKET_ROAR.get(), new RoarSource(fx, true));
                Minecraft.getInstance().getSoundManager().play(fx.roarNear);
            }
            if (fx.roarFar == null || fx.roarFar.done()) {
                fx.roarFar = new RocketSounds.Loop(RocketFxRegistry.ROCKET_ROAR_DISTANT.get(), new RoarSource(fx, false));
                Minecraft.getInstance().getSoundManager().play(fx.roarFar);
            }
        }
    }

    private static void stopSounds(FlightFx fx) {
        fx.roarWanted = false;
        fx.hissWanted = false;
        for (RocketSounds.Loop loop : new RocketSounds.Loop[]{fx.roarNear, fx.roarFar, fx.hiss}) {
            if (loop != null) Minecraft.getInstance().getSoundManager().stop(loop);
        }
    }

    /** The roar of a rocket's engines, near or distant, as loud as its engines were when the sound left them. */
    private record RoarSource(FlightFx fx, boolean near) implements RocketSounds.Source {
        @Override
        public boolean wanted() {
            return fx.roarWanted && heardLevel() > 0.02f;
        }

        private float heardLevel() {
            return fx.heard(RocketAcoustics.delayTicks(fx.distance));
        }

        @Override
        public float volume() {
            double v = near ? RocketAcoustics.nearVolume(fx.distance) : 0.85 * RocketAcoustics.farVolume(fx.distance);
            return (float) (v * heardLevel());
        }

        @Override
        public float pitch() {
            return near ? 0.96f + 0.08f * fx.plume : 1.0f;
        }

        @Override
        public Vec3 position() {
            return fx.position;
        }
    }

    /** The hiss of a warhead tearing back through the air. */
    private record HissSource(FlightFx fx) implements RocketSounds.Source {
        @Override
        public boolean wanted() {
            return fx.hissWanted;
        }

        @Override
        public float volume() {
            double heat = FxCurves.reentryHeat(fx.velocity.length(), FxCurves.airDepth(fx.position.y, MissileConfig.atmosphereTop));
            double near = 1 - FxCurves.clamp01(fx.distance / 700);
            return (float) (heat * near * near);
        }

        @Override
        public float pitch() {
            return (float) (0.85 + 0.05 * Math.min(6, fx.velocity.length()));
        }

        @Override
        public Vec3 position() {
            return fx.position;
        }
    }

    // ------------------------------------------------------------------ the view shaking

    void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!ClientConfig.rocketShake || !ClientConfig.rocketEffects || flights.isEmpty() || level == null) return;
        Vec3 eye = event.getCamera().getPosition();
        double amount = 0;
        for (FlightFx fx : flights.values()) {
            if (fx.ended()) continue;
            double distance = eye.distanceTo(fx.position);
            if (distance > FxCurves.SHAKE_REACH * 1.5) continue;
            float heard = fx.heard(RocketAcoustics.delayTicks(distance));
            amount += FxCurves.shake(distance, fx.position.y - fx.launchY) * heard;
        }
        amount = Math.min(1, amount);
        if (amount < 0.01) return;
        double t = (level.getGameTime() + event.getPartialTick()) / 20.0;
        event.setYaw(event.getYaw() + (float) (amount * 0.8 * (Math.sin(t * 43) + 0.5 * Math.sin(t * 97 + 1.1))));
        event.setPitch(event.getPitch() + (float) (amount * 0.8 * (Math.sin(t * 57 + 0.7) + 0.5 * Math.sin(t * 89 + 2.3))));
        event.setRoll(event.getRoll() + (float) (amount * 0.45 * Math.sin(t * 31 + 2.1)));
    }

    // ------------------------------------------------------------------ helpers

    static Vec3 rotate(Quaternionf q, float x, float y, float z) {
        Vector3f v = q.transform(new Vector3f(x, y, z));
        return new Vec3(v.x(), v.y(), v.z());
    }

    /** Some unit vector at right angles to {@code axis}. */
    static Vec3 perpendicular(Vec3 axis) {
        Vec3 n = axis.lengthSqr() < 1.0e-9 ? new Vec3(0, 1, 0) : axis.normalize();
        Vec3 other = Math.abs(n.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        return n.cross(other).normalize();
    }
}
