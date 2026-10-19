package org.papiricoh.create_nuclearindustry.missile.radar;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import java.util.List;

public class RadarBlockEntity extends KineticBlockEntity {
    private boolean signal;
    private long sampledSecond = Long.MIN_VALUE;
    private List<RadarContacts.Contact> contacts = List.of();
    public RadarBlockEntity(BlockPos pos, BlockState state) { super(RadarRegistry.ENTITY.get(), pos, state); }
    public boolean stationary() { return !isVirtual() && level != null && level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this && !ShipSpace.onShip(level, worldPosition); }
    public boolean operational() { return !isRemoved() && stationary() && Math.abs(getSpeed()) >= MissileConfig.minimumRpm && !isOverStressed(); }
    public boolean alarm() { return operational() && signal; }
    public List<RadarContacts.Contact> contacts() { return operational() ? contacts : List.of(); }
    @Override public float calculateStressApplied() { return lastStressApplied = (float) MissileConfig.stressImpact; }
    @Override public void tick() {
        super.tick();
        if (!(level instanceof ServerLevel server)) return;
        boolean working = operational();
        if (!working) { contacts = List.of(); sampledSecond = Long.MIN_VALUE; }
        else if (sampledSecond != level.getGameTime() / 20) {
            contacts = RadarContacts.get(server); sampledSecond = level.getGameTime() / 20;
        }
        boolean next = working && !contacts.isEmpty();
        if (signal != next) { signal = next; level.updateNeighborsAt(worldPosition, getBlockState().getBlock()); }
    }
    @Override public boolean addToGoggleTooltip(List<Component> lines, boolean sneak) {
        super.addToGoggleTooltip(lines, sneak);
        lines.add(Component.translatable("radar.create_nuclearindustry." + (!stationary() ? "static_only" : operational() ? "online" : "offline")));
        lines.add(Component.translatable("radar.create_nuclearindustry.contacts", contacts().size()));
        return true;
    }
}
