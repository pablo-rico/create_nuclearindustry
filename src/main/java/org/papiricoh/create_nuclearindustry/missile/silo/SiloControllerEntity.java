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
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;
import org.papiricoh.create_nuclearindustry.missile.*;
import org.papiricoh.create_nuclearindustry.missile.flight.*;
import org.papiricoh.create_nuclearindustry.missile.item.TargetDesignatorItem;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileFlight;
import org.papiricoh.create_nuclearindustry.multiblock.*;
import java.util.*;

public class SiloControllerEntity extends KineticBlockEntity implements MultiblockController {
    public enum State { INCOMPLETE, UNPREPARED, READY, ARMED, COUNTDOWN, LAUNCHED }
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

    public SiloControllerEntity(BlockPos pos, BlockState state) { super(SiloRegistry.CONTROLLER_ENTITY.get(), pos, state); errorPos = pos; }
    public BlockPos floor() { return worldPosition.relative(getBlockState().getValue(SiloControllerBlock.FACING).getOpposite(), 2); }
    public State state() { return state; }
    /** Why it is not ready, as a lang key suffix ({@code silo.create_nuclearindustry.error.*}), or empty. */
    public String error() { return error; }
    public BlockPos target() { return target; }
    public int countdown() { return countdown; }
    public double flightTicks() { return flightTicks; }
    public Component status() {
        return error.isEmpty() ? Component.translatable("silo.create_nuclearindustry.state." + state.name().toLowerCase(Locale.ROOT))
                : Component.translatable("silo.create_nuclearindustry.error." + error, errorPos.getX(), errorPos.getY(), errorPos.getZ());
    }
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
    }
    /** The player answerable for its launches (permission and cooldown), as {@link #configure} or {@link #arm} set it. */
    public void setOperator(UUID operator) { this.operator = operator; setChanged(); }
    public UUID operator() { return operator; }
    public void disarm() {
        countdown = -1;
        if (state == State.ARMED || state == State.COUNTDOWN) state = State.UNPREPARED;
        setChanged();
    }
    @Override public void initialize() { super.initialize(); if (level != null && !level.isClientSide) MultiblockRegistry.register(this); dirty = true; }
    // setRemoved is final in SmartBlockEntity; invalidate runs on both a replacement and a chunk unload.
    @Override public void invalidate() { if (level != null && !level.isClientSide) MultiblockRegistry.unregister(this); super.invalidate(); }
    @Override public BoundingBox interestBounds() {
        BlockPos f = floor();
        return new BoundingBox(f.getX()-2, f.getY(), f.getZ()-2, f.getX()+2, level == null ? f.getY()+18 : level.getMaxBuildHeight()-1, f.getZ()+2);
    }
    @Override public boolean shouldReactToBlockChange(BlockChange change) { return !change.pos().equals(worldPosition); }
    @Override public void requestStructureRevalidation(Player player) { dirty = true; }
    private Vec3 aim() {
        Vec3 aim = Vec3.atCenterOf(target);
        return airburst && rocket != null && rocket.warhead().nuclear() ? aim.add(0, TargetDesignatorItem.airburstHeight(rocket.warhead().destructionRadius()), 0) : aim;
    }
    /** Why the server's limits forbid launching at the current target, or {@link LaunchRules.Refusal#NONE}. */
    public LaunchRules.Refusal checkLimits(ServerLevel server) {
        if (target == null) return LaunchRules.Refusal.NONE;
        if (!LaunchRules.dimensionAllowed(server.dimension().location().toString(), MissileConfig.allowedDimensions)) return LaunchRules.Refusal.DIMENSION;
        Vec3 start = Vec3.atBottomCenterOf(floor().above()), aim = aim();
        if (LaunchRules.outOfRange(start.x, start.z, aim.x, aim.z, MissileConfig.maxRange)) return LaunchRules.Refusal.OUT_OF_RANGE;
        BlockPos spawn = server.getSharedSpawnPos();
        if (server.dimension() == Level.OVERWORLD && LaunchRules.nearSpawn(aim.x, aim.z, spawn.getX()+.5, spawn.getZ()+.5, MissileConfig.protectedSpawnRadius)) return LaunchRules.Refusal.SPAWN_PROTECTED;
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
        if (!result.valid()) { rocket = null; fail(result.error(), result.position(), result.error().equals("fuel") ? State.UNPREPARED : State.INCOMPLETE); return; }
        AssembledRocket next = result.rocket();
        // A stale chunk restored after a launch is reconciled against the dimension's launch ledger.
        if (FlightTracker.get(server).hasLaunched(next.launchId())) {
            consume(next); rocket = null; pendingLaunch = null; state = State.LAUNCHED; error = ""; setChanged(); return;
        }
        SiloValidator.Result geometry = SiloValidator.validate(level, floor(), worldPosition, next.height());
        if (!geometry.valid()) { fail(geometry.error(), geometry.position(), State.INCOMPLETE); return; }
        boolean same = next.equals(rocket);
        rocket = next;
        if (target == null) { fail("target", worldPosition, State.UNPREPARED); return; }
        if (!powered()) { fail("speed", worldPosition, State.UNPREPARED); return; }
        LaunchRules.Refusal refusal = checkLimits(server);
        if (refusal != LaunchRules.Refusal.NONE) { fail(refusal.name().toLowerCase(Locale.ROOT), worldPosition, State.UNPREPARED); return; }
        var prediction = MissileFlight.predict(rocket.design(), Flight.v3(Vec3.atBottomCenterOf(rocket.base())), Flight.v3(aim()), Flight.v3(Vec3.ZERO), MissileConfig.flightParams());
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
        if (before != state || level.getGameTime() % 20 == 0) sendData();
    }
    private void launch(ServerLevel server) {
        AssembledRocket assembled = rocket;
        pendingLaunch = assembled.launchId(); setChanged();
        // Server ticks serialize both redstone activations. A deterministic id survives stale chunk saves.
        if (!FlightTracker.get(server).hasLaunched(pendingLaunch)) {
            FlightTracker.get(server).launchModular(server, pendingLaunch, assembled.design(), Vec3.atBottomCenterOf(assembled.base()), aim(), target,
                    assembled.warhead(), operator, assembled.visuals(), assembled.composition().stageStarts());
            if (operator != null) LaunchCooldowns.get(server.getServer()).record(operator, LaunchCooldowns.now(server.getServer()));
        }
        consume(assembled);
        pendingLaunch = null; rocket = null; countdown = -1; state = State.LAUNCHED; error = ""; dirty = false; setChanged();
        server.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_LAUNCH, net.minecraft.sounds.SoundSource.BLOCKS, 2, .5f);
    }
    private void consume(AssembledRocket assembled) {
        for (int i = 0; i < assembled.height(); i++) {
            BlockPos pos = assembled.base().above(i);
            if (level.getBlockEntity(pos) instanceof RocketSectionEntity section && section.identity().equals(assembled.identities().get(i))) {
                section.consume(); level.removeBlock(pos, false);
            }
        }
    }
    @Override protected void write(CompoundTag tag, HolderLookup.Provider provider, boolean clientPacket) {
        super.write(tag, provider, clientPacket);
        if (target != null) tag.putLong("Target", target.asLong());
        if (operator != null) tag.putUUID("Operator", operator);
        if (pendingLaunch != null) tag.putUUID("PendingLaunch", pendingLaunch);
        tag.putBoolean("Airburst", airburst);
        if (clientPacket) {
            tag.putString("State", state.name()); tag.putString("Error", error); tag.putLong("ErrorPos", errorPos.asLong());
            tag.putInt("Countdown", countdown); tag.putDouble("FlightTicks", flightTicks);
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
            error = tag.getString("Error"); errorPos = BlockPos.of(tag.getLong("ErrorPos")); countdown = tag.getInt("Countdown"); flightTicks = tag.getDouble("FlightTicks");
        } else { state = State.UNPREPARED; countdown = -1; initializedSignal = false; rocket = null; dirty = true; }
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) { super.addToGoggleTooltip(tooltip, sneaking); tooltip.add(status()); return true; }
}
