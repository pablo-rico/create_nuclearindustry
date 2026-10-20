package org.papiricoh.create_nuclearindustry.missile.radar;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoomDevice;
import org.papiricoh.create_nuclearindustry.controlroom.PanelCluster;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A radar scope: a phosphor screen in a control room showing a linked radar's picture. Scopes of one kind facing one way join into
 * one screen up to {@link #MAX_SIZE} blocks a side ({@link PanelCluster}); the bottom-left one (the origin) holds the link, takes a
 * compact picture of the radar once a second and draws the whole screen. Link it with a Control Room Linker.
 */
public class RadarScopeBlockEntity extends SmartBlockEntity implements ControlRoomDevice {
    public static final int MAX_SIZE = 3;
    /** Echoes a scope shows at most. */
    public static final int MAX_ECHOES = 24;

    /** One echo of the picture, relative to the radar. */
    public record Echo(UUID id, String designation, float dx, float dz, float vx, float vz, float ix, float iz, boolean threat, byte phase) {
        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", id);
            tag.putString("D", designation);
            tag.putFloat("X", dx);
            tag.putFloat("Z", dz);
            tag.putFloat("VX", vx);
            tag.putFloat("VZ", vz);
            tag.putFloat("IX", ix);
            tag.putFloat("IZ", iz);
            tag.putBoolean("T", threat);
            tag.putByte("P", phase);
            return tag;
        }

        static Echo load(CompoundTag tag) {
            return new Echo(tag.getUUID("Id"), tag.getString("D"), tag.getFloat("X"), tag.getFloat("Z"), tag.getFloat("VX"), tag.getFloat("VZ"),
                    tag.getFloat("IX"), tag.getFloat("IZ"), tag.getBoolean("T"), tag.getByte("P"));
        }
    }

    /**
     * What the screen draws: the radar's state, its reach and zone, and its echoes; {@code status} -1 for no radar linked, -2 for
     * one out of reach.
     */
    public record Picture(int status, float speed, float range, int zoneDx, int zoneDz, int zoneRadius, boolean unacked, long sample,
                          List<Echo> echoes) {
        public static final Picture NONE = new Picture(-1, 0, 4000, 0, 0, 0, false, 0, List.of());
    }

    @Nullable
    private GlobalPos target;
    @Nullable
    private BlockPos origin;
    private int width = 1, height = 1;
    private Picture picture = Picture.NONE;
    private int counter;

    public RadarScopeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? state.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
    }

    public Direction right() {
        return facing().getCounterClockWise();
    }

    public boolean isOrigin() {
        return origin == null || origin.equals(worldPosition);
    }

    public int screenWidth() { return width; }
    public int screenHeight() { return height; }
    public Picture picture() { return picture; }

    @Nullable
    private RadarScopeBlockEntity originEntity() {
        if (isOrigin()) return this;
        return level != null && level.getBlockEntity(origin) instanceof RadarScopeBlockEntity scope ? scope : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        if (++counter % 20 != 0) return;
        PanelCluster cluster = PanelCluster.find(level, worldPosition, facing(), state -> state.getBlock() == getBlockState().getBlock(), MAX_SIZE, MAX_SIZE);
        BlockPos nextOrigin = cluster.rectangle() ? cluster.origin() : worldPosition;
        int nextWidth = cluster.rectangle() ? cluster.width() : 1, nextHeight = cluster.rectangle() ? cluster.height() : 1;
        boolean changed = !nextOrigin.equals(origin == null ? worldPosition : origin) || nextWidth != width || nextHeight != height;
        origin = nextOrigin.equals(worldPosition) ? null : nextOrigin.immutable();
        width = nextWidth;
        height = nextHeight;
        if (isOrigin()) {
            if (target == null && cluster.rectangle()) adoptTarget(cluster);
            picture = sample();
            changed = true;
        }
        if (changed) sendData();
    }

    /** A screen grown from scopes keeps the link one of them had. */
    private void adoptTarget(PanelCluster cluster) {
        for (BlockPos member : cluster.members()) {
            if (!member.equals(worldPosition) && level.getBlockEntity(member) instanceof RadarScopeBlockEntity scope && scope.target != null) {
                target = scope.target;
                scope.target = null;
                scope.setChanged();
                setChanged();
                return;
            }
        }
    }

    /** The linked radar's picture now, if it is in this dimension, loaded and a radar. */
    private Picture sample() {
        if (target == null) return Picture.NONE;
        if (!target.dimension().equals(level.dimension()) || !level.isLoaded(target.pos())
                || !(level.getBlockEntity(target.pos()) instanceof RadarBlockEntity radar))
            return new Picture(-2, 0, 4000, 0, 0, 0, false, level.getGameTime(), List.of());
        BlockPos at = radar.getBlockPos();
        double cx = at.getX() + 0.5, cz = at.getZ() + 0.5;
        List<Echo> echoes = new ArrayList<>();
        for (RadarTrack track : radar.tracks()) {
            if (echoes.size() >= MAX_ECHOES) break;
            echoes.add(new Echo(track.id(), track.designation(), (float) (track.position().x() - cx), (float) (track.position().z() - cz),
                    (float) track.velocity().x(), (float) track.velocity().z(), (float) (track.impact().x() - cx), (float) (track.impact().z() - cz),
                    radar.threats().contains(track), (byte) track.phase().ordinal()));
        }
        double reach = radar.reach();
        float range = (float) (reach <= 0 ? 8000 : Math.min(32000, reach));
        return new Picture(radar.status(), radar.getSpeed(), range, radar.zoneX() - at.getX(), radar.zoneZ() - at.getZ(), radar.zoneRadius(),
                radar.alarmUnacked(), level.getGameTime(), List.copyOf(echoes));
    }

    // ------------------------------------------------------------------ linking

    @Override
    @Nullable
    public GlobalPos getTarget() {
        RadarScopeBlockEntity screen = originEntity();
        return screen == null ? null : screen.target;
    }

    @Override
    public void setTarget(@Nullable GlobalPos target) {
        RadarScopeBlockEntity screen = originEntity();
        if (screen != null && screen != this) {
            screen.setTarget(target);
            return;
        }
        this.target = target;
        counter = 19;
        setChanged();
        sendData();
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        ControlRoomDevice.writeTarget(tag, target);
        if (origin != null) tag.putLong("Origin", origin.asLong());
        tag.putInt("W", width);
        tag.putInt("H", height);
        if (clientPacket) {
            CompoundTag p = new CompoundTag();
            p.putInt("Status", picture.status());
            p.putFloat("Speed", picture.speed());
            p.putFloat("Range", picture.range());
            p.putInt("ZX", picture.zoneDx());
            p.putInt("ZZ", picture.zoneDz());
            p.putInt("ZR", picture.zoneRadius());
            p.putBoolean("Unacked", picture.unacked());
            p.putLong("Sample", picture.sample());
            ListTag echoes = new ListTag();
            picture.echoes().forEach(e -> echoes.add(e.save()));
            p.put("Echoes", echoes);
            tag.put("Picture", p);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        target = ControlRoomDevice.readTarget(tag);
        origin = tag.contains("Origin") ? BlockPos.of(tag.getLong("Origin")) : null;
        width = Math.max(1, Math.min(MAX_SIZE, tag.getInt("W")));
        height = Math.max(1, Math.min(MAX_SIZE, tag.getInt("H")));
        if (clientPacket && tag.contains("Picture")) {
            CompoundTag p = tag.getCompound("Picture");
            List<Echo> echoes = new ArrayList<>();
            for (Tag t : p.getList("Echoes", Tag.TAG_COMPOUND)) if (echoes.size() < MAX_ECHOES) echoes.add(Echo.load((CompoundTag) t));
            picture = new Picture(p.getInt("Status"), p.getFloat("Speed"), Math.max(100, p.getFloat("Range")), p.getInt("ZX"), p.getInt("ZZ"),
                    p.getInt("ZR"), p.getBoolean("Unacked"), p.getLong("Sample"), List.copyOf(echoes));
        }
    }
}
