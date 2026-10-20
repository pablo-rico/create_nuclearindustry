package org.papiricoh.create_nuclearindustry.missile.orbit;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.List;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.controlroom.link.LinkCode;

/**
 * A satellite ground station: talks to the satellites on its link code ({@code code}, like a radar's) while its dish turns fast
 * enough. Linked to a command table, it gives the table its network: its satellites, what they see and whom they relay.
 */
public class GroundStationBlockEntity extends KineticBlockEntity {
    private String code = "";
    private boolean online;
    private int satelliteCount;
    private long nextPass = -1;

    public int satelliteCount() { return satelliteCount; }
    /** Ticks until the next pass, or -1 if none within a day. */
    public long nextPass() { return nextPass; }
    public Component statusLine() { return Component.translatable("ground_station.create_nuclearindustry." + (operational() ? "online" : "offline")); }
    @Override public float calculateStressApplied() { return lastStressApplied = (float) MissileConfig.stressImpact; }
    @Override public void tick() {
        super.tick();
        if (!(level instanceof ServerLevel server) || level.getGameTime() % 20 != 0) return;
        online = operational();
        satelliteCount = 0; nextPass = -1;
        if (LinkCode.complete(code)) {
            long now = server.getGameTime();
            for (Satellite satellite : SatelliteTracker.get(server).ofCode(code)) {
                satelliteCount++;
                long pass = satellite.orbit().nextPass(worldPosition.getX() + .5, worldPosition.getZ() + .5, now, 24000, MissileConfig.radarEarthRadius, 20);
                if (pass >= 0 && (nextPass < 0 || pass - now < nextPass)) nextPass = pass - now;
            }
        }
        sendData();
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        super.addToGoggleTooltip(tooltip, sneaking);
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()).withStyle(ChatFormatting.GOLD));
        for (Component line : new GroundStationDisplaySource().read(this))
            tooltip.add(Component.literal("     ").append(line).withStyle(ChatFormatting.GRAY));
        return true;
    }

    public GroundStationBlockEntity(BlockPos pos, BlockState state) {
        super(OrbitRegistry.STATION_ENTITY.get(), pos, state);
    }

    public String code() {
        return code;
    }

    public void setCode(String code) {
        this.code = LinkCode.sanitize(code);
        setChanged();
        sendData();
    }

    /** Whether it works: stationary, turning fast enough, on a complete code. */
    public boolean operational() {
        if (level != null && level.isClientSide) return online;
        return !isRemoved() && !isVirtual() && level != null && level.hasChunkAt(worldPosition)
                && level.getBlockEntity(worldPosition) == this && !ShipSpace.onShip(level, worldPosition)
                && Math.abs(getSpeed()) >= MissileConfig.minimumRpm && !isOverStressed() && LinkCode.complete(code);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putString("Code", code);
        if (clientPacket) { tag.putBoolean("Online", online); tag.putInt("Satellites", satelliteCount); tag.putLong("NextPass", nextPass); }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        code = LinkCode.sanitize(tag.getString("Code"));
        if (clientPacket) { online = tag.getBoolean("Online"); satelliteCount = tag.getInt("Satellites"); nextPass = tag.getLong("NextPass"); }
    }
}
