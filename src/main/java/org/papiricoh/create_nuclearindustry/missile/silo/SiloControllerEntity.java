package org.papiricoh.create_nuclearindustry.missile.silo;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.minecraft.ChatFormatting;
import org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements;
import org.papiricoh.create_nuclearindustry.advancement.NuclearEvent;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;
import org.papiricoh.create_nuclearindustry.missile.*;
import org.papiricoh.create_nuclearindustry.missile.flight.*;
import org.papiricoh.create_nuclearindustry.missile.item.TargetDesignatorItem;
import org.papiricoh.create_nuclearindustry.missile.physics.AccuracyModel;
import org.papiricoh.create_nuclearindustry.missile.physics.GuidanceTier;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileFlight;
import org.papiricoh.create_nuclearindustry.missile.physics.RocketAttitude;
import org.papiricoh.create_nuclearindustry.missile.rocket.RocketContraption;
import org.papiricoh.create_nuclearindustry.missile.rocket.RocketScan;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;
import org.papiricoh.create_nuclearindustry.multiblock.*;
import java.util.*;

public class SiloControllerEntity extends KineticBlockEntity implements MultiblockController {
    public enum State { INCOMPLETE, UNPREPARED, READY, ARMED, COUNTDOWN, LAUNCHED }
    /** The tallest rocket a silo takes, in sections. */
    public static final int MAX_ROCKET = 17;
    private State state = State.INCOMPLETE;
    private BlockPos target;
    private boolean airburst;
    private UUID operator;
    private UUID pendingLaunch;
    private AssembledRocket rocket;
    private boolean dirty = true, lastSignal, initializedSignal;
    private int countdown = -1;
    private String error = "height";
    private BlockPos errorPos;
    private double flightTicks;
    /** Sections in the rocket standing in it, 0 if none is complete; for the client's countdown and launch effects. */
    private int rocketHeight;
    private GuidanceTier syncedGuidance = GuidanceTier.BASIC;
    /** The dry run of assembling the rocket standing in it, from its last validation. */
    private RocketScan scan;
    /** Create's reason the rocket cannot be assembled, for the {@code assembly} error. */
    private Component assemblyMessage;
    private double syncedCep;
    private final MultiblockMessenger messenger = new MultiblockMessenger("silo");

    public SiloControllerEntity(BlockPos pos, BlockState state) { super(SiloRegistry.CONTROLLER_ENTITY.get(), pos, state); errorPos = pos; }
    public BlockPos floor() { return worldPosition.relative(getBlockState().getValue(SiloControllerBlock.FACING).getOpposite(), 2); }
    public State state() { return state; }
    /** Why it is not ready, as a lang key suffix ({@code silo.create_nuclearindustry.error.*}), or empty. */
    public String error() { return error; }
    public BlockPos target() { return target; }
    public int countdown() { return countdown; }
    public double flightTicks() { return flightTicks; }
    public int rocketHeight() { return rocketHeight; }
    public Component status() {
        if (error.equals("assembly") && assemblyMessage != null) return Component.translatable("silo.create_nuclearindustry.error.assembly_reason", assemblyMessage);
        return error.isEmpty() ? Component.translatable("silo.create_nuclearindustry.state." + state.name().toLowerCase(Locale.ROOT))
                : MultiblockText.text(errorMessage());
    }
    /** Why it is not ready, with where, as a {@link MultiblockText} message; empty when nothing is wrong. */
    public String errorMessage() {
        return error.isEmpty() ? "" : MultiblockText.message("silo.create_nuclearindustry.error." + error, errorPos.getX(), errorPos.getY(), errorPos.getZ());
    }
    /** Where the reason it is not ready lies (the controller when it is not a place). */
    public BlockPos errorPos() { return errorPos; }
    public boolean powered() { return !isOverStressed() && Math.abs(getSpeed()) >= MissileConfig.minimumRpm; }
    @Override public float calculateStressApplied() { return lastStressApplied = (float) MissileConfig.stressImpact; }
    public void configure(Player player, BlockPos target, boolean airburst) {
        if (!(level instanceof ServerLevel) || target == null || !level.getWorldBorder().isWithinBounds(target)
                || target.getY() < level.getMinBuildHeight() || target.getY() >= level.getMaxBuildHeight()) return;
        this.target = target.immutable(); this.airburst = airburst; this.operator = player.getUUID();
        disarm(); dirty = true; setChanged(); sendData();
    }
    public void arm(Player player) {
        if (!(level instanceof ServerLevel)) return;
        operator = player.getUUID();
        disarm(); validate();
        if (state == State.READY) state = State.ARMED;
        // Arming with a high input cannot count as an edge.
        lastSignal = level.hasNeighborSignal(worldPosition); initializedSignal = true;
        setChanged(); sendData();
        messenger.status(level, player, status());
    }
    /** The player answerable for its launches (permission and cooldown), as {@link #configure} or {@link #arm} set it. */
    public void setOperator(UUID operator) { this.operator = operator; setChanged(); }
    public UUID operator() { return operator; }
    public void disarm() {
        countdown = -1;
        if (state == State.ARMED || state == State.COUNTDOWN) state = State.UNPREPARED;
        setChanged();
    }
    @Override public void initialize() { super.initialize(); if (level != null && !level.isClientSide) { MultiblockRegistry.register(this); invalidatePorts(); } dirty = true; }
    // setRemoved is final in SmartBlockEntity; invalidate runs on both a replacement and a chunk unload.
    @Override public void invalidate() { if (level != null && !level.isClientSide) { MultiblockRegistry.unregister(this); invalidatePorts(); } super.invalidate(); }
    /** Whether {@code pos} is in its floor or walls (where a fuel port may sit), up to the tallest rocket a silo takes. */
    public boolean isWall(BlockPos pos) {
        BlockPos f = floor();
        int dx = pos.getX() - f.getX(), dy = pos.getY() - f.getY(), dz = pos.getZ() - f.getZ();
        return Math.abs(dx) <= 2 && Math.abs(dz) <= 2 && dy >= 0 && dy <= MAX_ROCKET + 1 && (dy == 0 || Math.abs(dx) == 2 || Math.abs(dz) == 2);
    }
    /** The tanks of the column standing on its floor, bottom to top, complete rocket or not; nothing loads chunks. */
    public List<RocketSectionEntity> columnTanks() {
        List<RocketSectionEntity> tanks = new ArrayList<>();
        if (level == null) return tanks;
        BlockPos base = floor().above();
        for (int i = 0; i < MAX_ROCKET; i++) {
            BlockPos p = base.above(i);
            if (!level.isLoaded(p) || !(level.getBlockEntity(p) instanceof RocketSectionEntity part)) break;
            if (part.section() == RocketComposition.Section.TANK) tanks.add(part);
        }
        return tanks;
    }
    /** Whether its fuel ports move propellant: not once armed, nor on a ship, nor while it is gone. */
    public boolean acceptsPropellant() {
        return !isRemoved() && state != State.ARMED && state != State.COUNTDOWN && (level == null || ShipSpace.shipAt(level, worldPosition) == null);
    }
    /** Pipes cache what a fuel port exposes; tells them to look again (the silo came, went or changed state). */
    private void invalidatePorts() {
        if (level == null) return;
        BlockPos f = floor();
        for (int y = 0; y <= MAX_ROCKET + 1; y++) for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            if (y != 0 && Math.abs(x) != 2 && Math.abs(z) != 2) continue;
            BlockPos p = f.offset(x, y, z);
            if (level.isLoaded(p) && level.getBlockState(p).is(SiloRegistry.FUEL_PORT.get())) level.invalidateCapabilities(p);
        }
    }
    @Override public BoundingBox interestBounds() {
        BlockPos f = floor();
        return new BoundingBox(f.getX()-2, f.getY(), f.getZ()-2, f.getX()+2, level == null ? f.getY()+18 : level.getMaxBuildHeight()-1, f.getZ()+2);
    }
    @Override public boolean shouldReactToBlockChange(BlockChange change) { return !change.pos().equals(worldPosition); }
    @Override public void requestStructureRevalidation(Player player) { dirty = true; }
    /** Where it is sent: the target, or above it for an airburst. */
    private Vec3 designatedAim() {
        Vec3 aim = Vec3.atCenterOf(target);
        return airburst && rocket != null && rocket.warhead().nuclear() ? aim.add(0, TargetDesignatorItem.airburstHeight(rocket.warhead().destructionRadius()), 0) : aim;
    }
    /** Where it really comes down: off the designated aim by its guidance's miss, drawn from the rocket itself. */
    private Vec3 aim() {
        Vec3 aim = designatedAim();
        if (rocket == null) return aim;
        V3 miss = AccuracyModel.miss(cep(), rocket.missSeed());
        return aim.add(miss.x(), 0, miss.z());
    }
    /** The circular error probable, blocks, of the rocket standing in it at its target; 0 without either. */
    public double cep() {
        if (level != null && level.isClientSide) return syncedCep;
        if (rocket == null || target == null) return 0;
        Vec3 start = Vec3.atBottomCenterOf(floor().above()), aim = designatedAim();
        return rocket.guidance().cep(Math.hypot(aim.x - start.x, aim.z - start.z)) * MissileConfig.cepScale;
    }
    /** Its guidance and circular error probable, for screens and goggles. */
    public Component guidanceLine() {
        return Component.translatable("silo.create_nuclearindustry.guidance",
                Component.translatable("silo.create_nuclearindustry.guidance." + guidance().name().toLowerCase(Locale.ROOT)), Math.round(cep()));
    }
    /** What the rocket standing in it steers by. */
    public GuidanceTier guidance() { return level != null && level.isClientSide ? syncedGuidance : rocket == null ? GuidanceTier.BASIC : rocket.guidance(); }
    /** Why the server's limits forbid launching at the current target, or {@link LaunchRules.Refusal#NONE}. */
    public LaunchRules.Refusal checkLimits(ServerLevel server) {
        if (target == null) return LaunchRules.Refusal.NONE;
        if (!LaunchRules.dimensionAllowed(server.dimension().location().toString(), MissileConfig.allowedDimensions)) return LaunchRules.Refusal.DIMENSION;
        Vec3 start = Vec3.atBottomCenterOf(floor().above()), aim = designatedAim(), landing = aim();
        if (LaunchRules.outOfRange(start.x, start.z, aim.x, aim.z, MissileConfig.maxRange)) return LaunchRules.Refusal.OUT_OF_RANGE;
        BlockPos spawn = server.getSharedSpawnPos();
        // Both where it is sent and where its guidance really puts it must keep clear of the spawn.
        if (server.dimension() == Level.OVERWORLD && (LaunchRules.nearSpawn(aim.x, aim.z, spawn.getX()+.5, spawn.getZ()+.5, MissileConfig.protectedSpawnRadius)
                || LaunchRules.nearSpawn(landing.x, landing.z, spawn.getX()+.5, spawn.getZ()+.5, MissileConfig.protectedSpawnRadius))) return LaunchRules.Refusal.SPAWN_PROTECTED;
        var online = operator == null ? null : server.getServer().getPlayerList().getPlayer(operator);
        GameProfile profile = online == null ? new GameProfile(operator == null ? new UUID(0,0) : operator, "") : online.getGameProfile();
        if (server.getServer().getProfilePermissions(profile) < MissileConfig.launchPermissionLevel) return LaunchRules.Refusal.NO_PERMISSION;
        if (operator != null && LaunchCooldowns.get(server.getServer()).ticksLeft(server.getServer(), operator) > 0) return LaunchRules.Refusal.COOLDOWN;
        return LaunchRules.Refusal.NONE;
    }
    private void fail(String reason, BlockPos pos, State state) {
        error = reason; errorPos = pos; this.state = state; countdown = -1;
    }
    public void validate() {
        dirty = false;
        if (!(level instanceof ServerLevel server)) return;
        if (ShipSpace.shipAt(level, worldPosition) != null) { fail("static", worldPosition, State.INCOMPLETE); return; }
        RocketValidator.Result result = RocketValidator.validate(level, floor().above());
        if (!result.valid()) { rocket = null; rocketHeight = 0; fail(result.error(), result.position(), result.error().equals("fuel") || result.error().equals("mixed_propellant") ? State.UNPREPARED : State.INCOMPLETE); return; }
        AssembledRocket next = result.rocket();
        // A stale chunk restored after a launch is reconciled against the dimension's launch ledger.
        if (FlightTracker.get(server).hasLaunched(next.launchId())) {
            consume(next); rocket = null; pendingLaunch = null; state = State.LAUNCHED; error = ""; setChanged(); return;
        }
        RocketScan dryRun = RocketScan.scan(level, floor(), next);
        SiloValidator.Result geometry = SiloValidator.validate(level, floor(), worldPosition, next.height(), dryRun.valid() ? dryRun.captured() : Set.of());
        // A missing wall explains more than anything the rocket captures through the gap, so it is reported first.
        boolean structural = !geometry.valid() && (geometry.error().equals("wall") || geometry.error().equals("unloaded"));
        if (!structural && !dryRun.valid()) { rocket = null; scan = null; assemblyMessage = dryRun.message(); fail(dryRun.error(), dryRun.errorPos(), State.INCOMPLETE); return; }
        if (!geometry.valid()) { fail(geometry.error(), geometry.position(), State.INCOMPLETE); return; }
        scan = dryRun;
        boolean same = next.equals(rocket);
        rocket = next;
        rocketHeight = next.height();
        if (target == null) { fail("target", worldPosition, State.UNPREPARED); return; }
        if (!powered()) { fail("speed", worldPosition, State.UNPREPARED); return; }
        LaunchRules.Refusal refusal = checkLimits(server);
        if (refusal != LaunchRules.Refusal.NONE) { fail(refusal.name().toLowerCase(Locale.ROOT), worldPosition, State.UNPREPARED); return; }
        var prediction = MissileFlight.predict(design(), Flight.v3(Vec3.atBottomCenterOf(rocket.base())), Flight.v3(aim()), Flight.v3(Vec3.ZERO), MissileConfig.flightParams());
        flightTicks = prediction.ticks();
        if (!prediction.onTarget()) { fail("range", worldPosition, State.UNPREPARED); return; }
        error = "";
        if (!same || state != State.ARMED && state != State.COUNTDOWN) { countdown = -1; state = State.READY; }
    }
    @Override public void tick() {
        super.tick();
        if (!(level instanceof ServerLevel server)) return;
        boolean signal = level.hasNeighborSignal(worldPosition);
        boolean rising = initializedSignal && signal && !lastSignal;
        initializedSignal = true; lastSignal = signal;
        State before = state;
        if (!powered() && (state == State.ARMED || state == State.COUNTDOWN)) { fail("speed", worldPosition, State.UNPREPARED); dirty = true; }
        // Events invalidate immediately; a slow audit also catches chunk unloads and raw world edits.
        if (dirty || level.getGameTime() % 20 == 0) validate();
        if (state == State.ARMED && rising) { countdown = MissileConfig.launchCountdownTicks(); state = State.COUNTDOWN; setChanged(); }
        else if (state == State.COUNTDOWN && --countdown <= 0) {
            validate(); // Full final server-side validation before consuming anything.
            if (state == State.COUNTDOWN) launch(server);
        }
        if (before != state) invalidatePorts();
        if (before != state || level.getGameTime() % 20 == 0) sendData();
    }
    private void launch(ServerLevel server) {
        AssembledRocket assembled = rocket;
        pendingLaunch = assembled.launchId(); setChanged();
        // Server ticks serialize both redstone activations. A deterministic id survives stale chunk saves.
        if (!FlightTracker.get(server).hasLaunched(pendingLaunch)) {
            Vec3 start = Vec3.atBottomCenterOf(assembled.base()), aim = aim();
            MissileDesign design = design();
            // Assembled for real, emptied (the flight carries the warhead and propellant), then lifted out of the world.
            RocketScan live = RocketScan.scan(server, floor(), assembled);
            RocketContraption contraption = live.valid() ? live.contraption() : null;
            RocketShape shape = live.valid() ? live.shape(assembled) : RocketShape.column(assembled.height(), assembled.composition().stageStarts());
            emptySections(assembled);
            CompoundTag cargo = null;
            if (contraption != null) {
                contraption.sanitize();
                contraption.removeBlocksFromWorld(server, BlockPos.ZERO);
                cargo = contraption.writeNBT(server.registryAccess(), false);
            }
            FlightTracker.get(server).launchModular(server, pendingLaunch, design, start, aim, target, assembled.warhead(), operator, shape, cargo,
                    (float) RocketAttitude.heading(aim.x - start.x, aim.z - start.z));
            if (operator != null) LaunchCooldowns.get(server.getServer()).record(operator, LaunchCooldowns.now(server.getServer()));
            NuclearAdvancements.awardMachine(server, worldPosition, operator == null ? null : server.getServer().getPlayerList().getPlayer(operator),
                    NuclearEvent.MISSILE_LAUNCHED);
        }
        consume(assembled);
        pendingLaunch = null; rocket = null; rocketHeight = 0; countdown = -1; state = State.LAUNCHED; error = ""; dirty = false; setChanged();
    }
    /** Takes the rocket (and what it captures) out of the silo without dropping anything: it has flown already. */
    private void consume(AssembledRocket assembled) {
        RocketScan live = RocketScan.scan(level, floor(), assembled);
        emptySections(assembled);
        if (live.valid() && live.contraption() != null) {
            live.contraption().removeBlocksFromWorld(level, BlockPos.ZERO);
            return;
        }
        for (int i = 0; i < assembled.height(); i++) {
            BlockPos pos = assembled.base().above(i);
            if (level.getBlockEntity(pos) instanceof RocketSectionEntity section && section.identity().equals(assembled.identities().get(i))) level.removeBlock(pos, false);
        }
    }
    private void emptySections(AssembledRocket assembled) {
        for (int i = 0; i < assembled.height(); i++) {
            BlockPos pos = assembled.base().above(i);
            if (level.getBlockEntity(pos) instanceof RocketSectionEntity section && section.identity().equals(assembled.identities().get(i))) section.consume();
        }
    }
    /** The dry run of assembling its rocket from its last validation, or null; for tests and screens. */
    public RocketScan lastScan() { return scan; }
    /** The rocket standing in it as it would fly: its stages and payload, what it captures and its guidance's spread. */
    private MissileDesign design() {
        MissileDesign design = rocket.design(cep());
        return scan != null ? scan.apply(design) : design;
    }
    @Override protected void write(CompoundTag tag, HolderLookup.Provider provider, boolean clientPacket) {
        super.write(tag, provider, clientPacket);
        if (target != null) tag.putLong("Target", target.asLong());
        if (operator != null) tag.putUUID("Operator", operator);
        if (pendingLaunch != null) tag.putUUID("PendingLaunch", pendingLaunch);
        tag.putBoolean("Airburst", airburst);
        if (clientPacket) {
            tag.putString("State", state.name()); tag.putString("Error", error); tag.putLong("ErrorPos", errorPos.asLong());
            tag.putInt("Countdown", countdown); tag.putDouble("FlightTicks", flightTicks); tag.putInt("RocketHeight", rocketHeight);
            tag.putInt("Guidance", guidance().ordinal()); tag.putDouble("Cep", cep());
            if (error.equals("assembly") && assemblyMessage != null) tag.putString("AssemblyText", Component.Serializer.toJson(assemblyMessage, provider));
        }
    }
    @Override protected void read(CompoundTag tag, HolderLookup.Provider provider, boolean clientPacket) {
        super.read(tag, provider, clientPacket);
        target = tag.contains("Target") ? BlockPos.of(tag.getLong("Target")) : null;
        operator = tag.hasUUID("Operator") ? tag.getUUID("Operator") : null;
        pendingLaunch = tag.hasUUID("PendingLaunch") ? tag.getUUID("PendingLaunch") : null;
        airburst = tag.getBoolean("Airburst");
        if (clientPacket) {
            try { state = State.valueOf(tag.getString("State")); } catch (IllegalArgumentException ignored) { state = State.INCOMPLETE; }
            error = tag.getString("Error"); errorPos = BlockPos.of(tag.getLong("ErrorPos")); countdown = tag.getInt("Countdown"); flightTicks = tag.getDouble("FlightTicks"); rocketHeight = tag.getInt("RocketHeight");
            syncedGuidance = GuidanceTier.byOrdinal(tag.getInt("Guidance")); syncedCep = tag.getDouble("Cep");
            assemblyMessage = tag.contains("AssemblyText") ? Component.Serializer.fromJson(tag.getString("AssemblyText"), provider) : null;
        } else { state = State.UNPREPARED; countdown = -1; initializedSignal = false; rocket = null; dirty = true; }
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        super.addToGoggleTooltip(tooltip, sneaking);
        tooltip.add(Component.literal("    ").append(Component.translatable("block.create_nuclearindustry.silo_controller")).withStyle(ChatFormatting.GOLD));
        ChatFormatting colour = !error.isEmpty() ? (state == State.INCOMPLETE ? ChatFormatting.RED : ChatFormatting.YELLOW)
                : state == State.LAUNCHED ? ChatFormatting.GRAY : ChatFormatting.GREEN;
        tooltip.add(Component.literal("     ").append(status()).withStyle(colour));
        if (state == State.COUNTDOWN) tooltip.add(Component.literal("     ").append(Component.translatable("silo.create_nuclearindustry.goggles.countdown", (countdown + 19) / 20)).withStyle(ChatFormatting.RED));
        if (target != null) tooltip.add(Component.literal("     ").append(Component.translatable("silo.create_nuclearindustry.goggles.target", target.getX(), target.getY(), target.getZ())).withStyle(ChatFormatting.GRAY));
        if (rocketHeight > 0) tooltip.add(Component.literal("     ").append(guidanceLine()).withStyle(ChatFormatting.GRAY));
        return true;
    }
}
