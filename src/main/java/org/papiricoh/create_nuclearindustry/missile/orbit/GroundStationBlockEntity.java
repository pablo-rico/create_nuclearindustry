package org.papiricoh.create_nuclearindustry.missile.orbit;

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
        return false;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putString("Code", code);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        code = LinkCode.sanitize(tag.getString("Code"));
    }
}
