package org.papiricoh.create_nuclearindustry.missile.holo;

import io.netty.buffer.ByteBuf;
import org.papiricoh.create_nuclearindustry.controlroom.link.LinkCode;
import org.papiricoh.create_nuclearindustry.missile.orbit.*;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightFxBroadcaster;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightFxEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.flight.Flight;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightTracker;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloControllerEntity;

import org.papiricoh.create_nuclearindustry.missile.defense.*;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarConfigurePayload;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Commands are checked against the open table and its links. Ground-target missiles launch only on their controller's redstone.
 * The table orders interceptors and satellite launch vehicles directly. Actions and {@link Command}s are sent by number:
 * add new ones at the end.
 */
public record HoloCommandPayload(BlockPos centre, byte action, BlockPos silo, int x, int z, int arg,
                                 @Nullable UUID flight, List<BlockPos> silos) implements CustomPacketPayload {
    public static final byte SELECT = 0, DESIGNATE = 1, ARM = 2, DISARM = 3, SALVO = 4, ABORT = 5, UNLINK_SILO = 6, UNLINK_RADAR = 7;
    /**
     * Missile defence. {@code ENGAGE}: {@code flight} the missile, {@code arg} -1 for the best launcher, 0-7 a battery, 100 + i
     * silo i. {@code SET_AUTO}: {@code arg} 1 on, 0 off. {@code SET_DOCTRINE}: {@code arg} a {@code Doctrine} ordinal.
     * {@code SET_ZONE}: {@code x, z} the centre, {@code arg} the radius. {@code UNLINK_BATTERY}: {@code silo} the battery's place.
     * {@code CEASE}: automatic off and every order dropped.
     */
    public static final byte ENGAGE = 8, SET_AUTO = 9, SET_DOCTRINE = 10, SET_ZONE = 11, UNLINK_BATTERY = 12, CEASE = 13;
    /**
     * Satellites. {@code LAUNCH_SAT}: {@code silo} the launch controller topped by a satellite fairing, {@code x, z} the orbit's
     * anchor (absolute), {@code arg} bit 0 the {@code OrbitType} (0 low, 1 geostationary) and {@code arg >> 1} a low orbit's heading
     * in whole degrees as a flight's heading (0 towards +Z, south; 90 towards +X, east). {@code DEORBIT}: {@code flight} one of
     * the table's own satellites. {@code ENGAGE_SAT}: {@code flight} a foreign satellite the table knows of, {@code arg} -1 for the
     * best launcher, 100 + i silo i. {@code UNLINK_STATION}: {@code silo} the ground station's place.
     */
    public static final byte LAUNCH_SAT = 14, DEORBIT = 15, ENGAGE_SAT = 16, UNLINK_STATION = 17;
    public enum Command { OK, TOO_FAR, BROKEN, NOT_LINKED, UNLOADED, REFUSED, NO_TARGET, TOO_LONG, NOT_OWN, GONE,
        NO_SHOOTER, NOT_TRACKED, NO_SOLUTION, FRIENDLY, NO_AMMO, ALREADY_ENGAGED,
        NO_STATION, NO_SATELLITE, NO_ORBIT, ORBIT_FULL, OUT_OF_BOUNDS }
    public static final Type<HoloCommandPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "holo_command"));
    public static final StreamCodec<ByteBuf, HoloCommandPayload> CODEC = StreamCodec.of(HoloCommandPayload::write, HoloCommandPayload::read);

    public HoloCommandPayload { silos = List.copyOf(silos); }

    private static void write(ByteBuf b, HoloCommandPayload p) {
        if (p.silos.size() > 16) throw new IllegalArgumentException("Too many salvo silos");
        BlockPos.STREAM_CODEC.encode(b, p.centre); b.writeByte(p.action); BlockPos.STREAM_CODEC.encode(b, p.silo);
        b.writeInt(p.x); b.writeInt(p.z); b.writeInt(p.arg); b.writeBoolean(p.flight != null);
        if (p.flight != null) { b.writeLong(p.flight.getMostSignificantBits()); b.writeLong(p.flight.getLeastSignificantBits()); }
        b.writeByte(p.silos.size());
        for (BlockPos pos : p.silos) BlockPos.STREAM_CODEC.encode(b, pos);
    }

    private static HoloCommandPayload read(ByteBuf b) {
        BlockPos centre = BlockPos.STREAM_CODEC.decode(b); byte action = b.readByte(); BlockPos silo = BlockPos.STREAM_CODEC.decode(b);
        int x = b.readInt(), z = b.readInt(), arg = b.readInt();
        UUID flight = b.readBoolean() ? new UUID(b.readLong(), b.readLong()) : null;
        int count = b.readUnsignedByte();
        if (count > 16) throw new IllegalArgumentException("Too many salvo silos");
        List<BlockPos> silos = new ArrayList<>(count);
        for (int i = 0; i < count; i++) silos.add(BlockPos.STREAM_CODEC.decode(b));
        return new HoloCommandPayload(centre, action, silo, x, z, arg, flight, silos);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ServerPlayer player, HoloCommandPayload payload) {
        Command result = execute(player, payload);
        player.displayClientMessage(Component.translatable("holo_table.create_nuclearindustry.command." + result.name().toLowerCase(Locale.ROOT))
                .withStyle(result == Command.OK ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        if (player.level().getBlockEntity(payload.centre) instanceof HoloTableBlockEntity table) table.syncTo(player);
    }

    public static Command execute(ServerPlayer player, HoloCommandPayload p) {
        if (!(player.containerMenu instanceof HoloTableMenu menu) || !menu.centre.equals(p.centre)) return Command.REFUSED;
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(p.centre) || !(level.getBlockEntity(p.centre) instanceof HoloTableBlockEntity table)) return Command.BROKEN;
        if (!table.near(player)) return Command.TOO_FAR;
        if (!HoloTable.intact(level, p.centre)) return Command.BROKEN;
        if (!menu.stillValid(player)) return Command.REFUSED;
        if (p.action == ABORT) return abort(level, table, p.flight);
        if (p.action == SALVO) return salvo(player, table, p.silos, p.arg);
        switch (p.action) {
            case DEORBIT -> {
                SatelliteTracker tracker = SatelliteTracker.get(level);
                Satellite satellite = p.flight == null ? null : tracker.get(p.flight);
                if (satellite == null) return Command.NO_SATELLITE;
                if (!LinkCode.complete(table.network()) || !satellite.code().equals(table.network())) return Command.NOT_OWN;
                tracker.remove(level, satellite.id(), SatelliteTracker.Removal.DEORBITED);
                long now = level.getGameTime();
                FlightFxBroadcaster.event(level, satellite.id(), FlightFxEvent.SATELLITE_REENTRY, 0,
                        Flight.vec(satellite.orbit().position(now)), Flight.vec(satellite.orbit().velocity(now)));
                return Command.OK;
            }
            case ENGAGE_SAT -> { return engageSatellite(level, table, p.flight, p.arg); }
            case ENGAGE -> {
                if (p.flight == null || table.tracks().stream().noneMatch(t -> t.id().equals(p.flight))) return Command.NOT_TRACKED;
                Flight target = FlightTracker.get(level).get(p.flight);
                if (target != null && table.own(target)) return Command.FRIENDLY;
                EngageResult result = DefenseCommand.manualEngage(level, table.tracks(), table.shooters(p.arg), p.flight, table.defense().doctrine(), table::own);
                return result == EngageResult.QUEUED ? Command.OK : Command.valueOf(result.name());
            }
            case SET_AUTO -> {
                if (p.arg != 0 && p.arg != 1) return Command.REFUSED;
                table.setDefense(table.defense().withAuto(p.arg == 1)); return Command.OK;
            }
            case SET_DOCTRINE -> {
                table.setDefense(table.defense().withDoctrine(Doctrine.byOrdinal(p.arg))); return Command.OK;
            }
            case SET_ZONE -> {
                int limit = RadarConfigurePayload.MAX_ZONE_OFFSET;
                int x = Math.max(p.centre.getX() - limit, Math.min(p.centre.getX() + limit, p.x));
                int z = Math.max(p.centre.getZ() - limit, Math.min(p.centre.getZ() + limit, p.z));
                table.setDefense(table.defense().withZone(x, z, p.arg)); return Command.OK;
            }
            case CEASE -> { table.cease(); return Command.OK; }
        }
        GlobalPos link = GlobalPos.of(level.dimension(), p.silo);
        if (p.action == UNLINK_STATION) return table.stations().contains(link) && table.unlink(link) ? Command.OK : Command.NOT_LINKED;
        if (p.action == LAUNCH_SAT && !LinkCode.complete(table.network())) return Command.NO_STATION;
        if (p.action == UNLINK_BATTERY) return table.batteries().contains(link) && table.unlink(link) ? Command.OK : Command.NOT_LINKED;
        if (p.action == UNLINK_RADAR) return table.radars().contains(link) && table.unlink(link) ? Command.OK : Command.NOT_LINKED;
        if (!table.silos().contains(link)) return Command.NOT_LINKED;
        // Unlink also works for a destroyed or unloaded device.
        if (p.action == UNLINK_SILO) return table.unlink(link) ? Command.OK : Command.NOT_LINKED;
        Command problem = controllerProblem(level, table, p.silo);
        if (problem != Command.OK) return problem;
        SiloControllerEntity controller = (SiloControllerEntity) level.getBlockEntity(p.silo);
        switch (p.action) {
            case LAUNCH_SAT -> {
                if (!controller.satelliteMode()) return Command.REFUSED;
                Orbit.Bounds bounds = OrbitConfig.bounds(level.getWorldBorder());
                if (!bounds.contains(p.x, p.z)) return Command.OUT_OF_BOUNDS;
                long now = level.getGameTime();
                Orbit orbit = (p.arg & 1) == 0 ? OrbitConfig.leo(p.x, p.z, Math.toRadians(p.arg >> 1), now, bounds)
                        : OrbitConfig.geo(p.x, p.z, now, bounds);
                return switch (controller.launchSatellite(player.getUUID(), orbit, table.network())) {
                    case OK -> Command.OK;
                    case NOT_READY -> Command.REFUSED;
                    case NO_SATELLITE -> Command.NO_SATELLITE;
                    case NO_ORBIT -> Command.NO_ORBIT;
                    case FULL -> Command.ORBIT_FULL;
                };
            }
            case SELECT -> table.selectSilo(p.silo);
            case DESIGNATE -> {
                table.selectSilo(p.silo);
                HoloTableBlockEntity.Designation result = table.designate(player, p.x, p.z, (p.arg & 1) != 0);
                if (result != HoloTableBlockEntity.Designation.OK) return Command.REFUSED;
            }
            case ARM -> {
                if (controller.target() == null) return Command.NO_TARGET;
                controller.arm(player);
                if (controller.state() != SiloControllerEntity.State.ARMED) return Command.REFUSED;
            }
            case DISARM -> { controller.disarm(); controller.sendData(); }
            default -> { return Command.REFUSED; }
        }
        return Command.OK;
    }

    private static Command controllerProblem(ServerLevel level, HoloTableBlockEntity table, BlockPos pos) {
        if (!table.silos().contains(GlobalPos.of(level.dimension(), pos))) return Command.NOT_LINKED;
        if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof SiloControllerEntity)) return Command.UNLOADED;
        return Command.OK;
    }

    private static Command engageSatellite(ServerLevel level, HoloTableBlockEntity table, @Nullable UUID id, int choice) {
        String code = table.network();
        SatelliteTracker tracker = SatelliteTracker.get(level);
        if (id == null || !LinkCode.complete(code) || !tracker.known(id, code)) return Command.NOT_TRACKED;
        Satellite target = tracker.get(id);
        if (target.code().equals(code)) return Command.FRIENDLY;
        if (target.orbit().geostationary() || target.orbit().altitude() > OrbitConfig.asatCeiling) return Command.NO_SOLUTION;
        if (FlightTracker.get(level).interceptorsToward(id) > 0 || EngagementBoard.reserved(level, id) > 0) return Command.ALREADY_ENGAGED;
        if (!DefenseConfig.enabled) return Command.NO_SHOOTER;
        List<SiloControllerEntity> shooters = table.shooters(choice).stream().filter(s -> s instanceof SiloControllerEntity)
                .map(s -> (SiloControllerEntity) s).toList();
        if (shooters.stream().noneMatch(SiloControllerEntity::operational)) return Command.NO_SHOOTER;
        if (shooters.stream().noneMatch(s -> s.operational() && s.readyRounds() > 0)) return Command.NO_AMMO;
        SiloControllerEntity best = null;
        double earliest = Double.POSITIVE_INFINITY;
        for (SiloControllerEntity shooter : shooters) {
            if (!shooter.operational() || shooter.readyRounds() <= 0) continue;
            int delay = shooter.reactionTicks(target.orbit().position(level.getGameTime()).sub(shooter.launchPoint()).normalize());
            var solution = AsatCommand.solve(level, target, shooter, delay);
            if (solution.isPresent() && solution.get().arrivalTicks() + delay < earliest) {
                best = shooter;
                earliest = solution.get().arrivalTicks() + delay;
            }
        }
        if (best == null) return Command.NO_SOLUTION;
        best.setNetwork(code);
        FireResult result = best.order(level, id, EngagementSource.MANUAL_TABLE);
        if (result == FireResult.QUEUED) EngagementBoard.reserve(level, id, best.shooterPos(), level.getGameTime() + DefenseConfig.orderExpireTicks);
        return result == FireResult.QUEUED || result == FireResult.LAUNCHED ? Command.OK : Command.REFUSED;
    }

    private static Command salvo(ServerPlayer player, HoloTableBlockEntity table, List<BlockPos> positions, int spacing) {
        if (positions.isEmpty() || positions.size() > 16 || new HashSet<>(positions).size() != positions.size()) return Command.REFUSED;
        if (spacing < 0 || spacing > MissileConfig.salvoMaxSpacingSeconds * 20) return Command.REFUSED;
        List<SiloControllerEntity> controllers = new ArrayList<>();
        for (BlockPos pos : positions) {
            Command problem = controllerProblem(player.serverLevel(), table, pos);
            if (problem != Command.OK) return problem;
            controllers.add((SiloControllerEntity) player.level().getBlockEntity(pos));
        }
        // Arm every participant before computing the plan, using fresh predictions. Roll back every touched controller on failure.
        for (SiloControllerEntity controller : controllers) {
            controller.arm(player);
            if (controller.state() != SiloControllerEntity.State.ARMED) {
                table.selectSilo(controller.getBlockPos());
                controllers.forEach(SiloControllerEntity::disarm);
                return controller.target() == null ? Command.NO_TARGET : Command.REFUSED;
            }
        }
        int[] delays = SalvoPlan.delays(controllers.stream().mapToDouble(SiloControllerEntity::flightTicks).toArray(), spacing,
                MissileConfig.maxLaunchDelaySeconds * 20);
        if (delays == null) { controllers.forEach(SiloControllerEntity::disarm); return Command.TOO_LONG; }
        for (int i = 0; i < controllers.size(); i++) controllers.get(i).setLaunchDelay(delays[i]);
        return Command.OK;
    }

    private static Command abort(ServerLevel level, HoloTableBlockEntity table, @Nullable UUID id) {
        FlightTracker tracker = FlightTracker.getIfPresent(level);
        Flight flight = tracker == null || id == null ? null : tracker.get(id);
        if (flight == null) return Command.GONE;
        if (table.ownInterceptor(flight)) return tracker.abort(level, id) ? Command.OK : Command.GONE;
        if (flight.kind() != Flight.Kind.MISSILE) return Command.NOT_OWN;
        for (GlobalPos link : table.silos()) {
            if (controllerProblem(level, table, link.pos()) == Command.OK
                    && level.getBlockEntity(link.pos()) instanceof SiloControllerEntity controller
                    && controller.floor().above().equals(flight.origin())) {
                return tracker.abort(level, id) ? Command.OK : Command.GONE;
            }
        }
        return Command.NOT_OWN;
    }
}
