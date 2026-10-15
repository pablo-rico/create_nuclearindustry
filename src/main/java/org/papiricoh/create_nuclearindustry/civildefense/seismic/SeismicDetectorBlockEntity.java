package org.papiricoh.create_nuclearindustry.civildefense.seismic;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseConfig;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonationEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Keeps the last {@link #CAPACITY} detonations felt in its dimension, newest first. The comparator
 * output follows the strongest one of the last {@code civilDefense.seismic.memorySeconds}, so it rises
 * with a blast and falls back to 0 on its own.
 */
public class SeismicDetectorBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final int CAPACITY = 8;
    private static final int CHECK_INTERVAL = 20;

    private final List<SeismicEvent> events = new ArrayList<>();
    private int lastSignal;
    private int timer;

    public SeismicDetectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void initialize() {
        super.initialize();
        SeismicNetwork.add(this);
    }

    @Override
    public void invalidate() {
        SeismicNetwork.remove(this);
        super.invalidate();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || ++timer < CHECK_INTERVAL) {
            return;
        }
        timer = 0;
        refreshSignal();
    }

    /** Notes a detonation in this dimension. */
    public void record(NuclearDetonationEvent event) {
        record(event.getCenter(), event.getPower(), event.isDirty());
    }

    /** Records ground motion from {@code source}: a detonation, or an earthquake read as a blast of {@code power}. */
    public void record(Vec3 source, float power, boolean dirty) {
        if (level == null) {
            return;
        }
        Vec3 here = Vec3.atCenterOf(worldPosition);
        double bearing = SeismicModel.bearing(source.x - here.x, source.z - here.z);
        events.add(0, new SeismicEvent(here.distanceTo(source), power, (float) bearing,
                level.getGameTime(), dirty));
        while (events.size() > CAPACITY) {
            events.remove(events.size() - 1);
        }
        setChanged();
        notifyUpdate();
        refreshSignal();
        org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements.awardNearby(level, worldPosition, org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements.WITNESS_RADIUS, org.papiricoh.create_nuclearindustry.advancement.NuclearEvent.SEISMIC_DETECTED);
    }

    public List<SeismicEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public Optional<SeismicEvent> latest() {
        return events.isEmpty() ? Optional.empty() : Optional.of(events.get(0));
    }

    /** 0 to 15 from the strongest ground motion felt within the memory window. */
    public int comparatorSignal() {
        if (level == null) {
            return 0;
        }
        long since = level.getGameTime() - CivilDefenseConfig.seismicMemorySeconds * 20L;
        double strongest = 0;
        for (SeismicEvent event : events) {
            if (event.gameTime() >= since) {
                strongest = Math.max(strongest, event.groundMotion(CivilDefenseConfig.seismicFalloff));
            }
        }
        return SeismicModel.comparatorSignal(strongest, CivilDefenseConfig.seismicFullScale);
    }

    private void refreshSignal() {
        int signal = comparatorSignal();
        if (signal != lastSignal && level != null) {
            lastSignal = signal;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable("block.create_nuclearindustry.seismic_detector"))
                .withStyle(ChatFormatting.GOLD));
        if (events.isEmpty()) {
            tooltip.add(indent(Component.translatable("create_nuclearindustry.seismic.quiet").withStyle(ChatFormatting.GRAY)));
            return true;
        }
        long now = level == null ? 0 : level.getGameTime();
        int shown = isPlayerSneaking ? events.size() : Math.min(3, events.size());
        for (int i = 0; i < shown; i++) {
            SeismicEvent event = events.get(i);
            long seconds = Math.max(0, now - event.gameTime()) / 20;
            tooltip.add(indent(event.describe().withStyle(i == 0 ? ChatFormatting.YELLOW : ChatFormatting.GRAY)));
            tooltip.add(indent(Component.literal("  ").append(Component.translatable(
                    event.dirty() ? "create_nuclearindustry.seismic.dirty_ago" : "create_nuclearindustry.seismic.ago", seconds))
                    .withStyle(ChatFormatting.DARK_GRAY)));
        }
        if (!isPlayerSneaking && events.size() > shown) {
            tooltip.add(indent(Component.translatable("create_nuclearindustry.seismic.more", events.size() - shown)
                    .withStyle(ChatFormatting.DARK_GRAY)));
        }
        return true;
    }

    private static Component indent(Component line) {
        return Component.literal("     ").append(line);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        ListTag list = new ListTag();
        events.forEach(event -> list.add(event.save()));
        tag.put("Events", list);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        events.clear();
        ListTag list = tag.getList("Events", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && i < CAPACITY; i++) {
            events.add(SeismicEvent.load(list.getCompound(i)));
        }
    }
}
