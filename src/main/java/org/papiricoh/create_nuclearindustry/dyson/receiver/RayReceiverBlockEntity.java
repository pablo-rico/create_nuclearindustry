package org.papiricoh.create_nuclearindustry.dyson.receiver;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements;
import org.papiricoh.create_nuclearindustry.advancement.NuclearEvent;
import org.papiricoh.create_nuclearindustry.dyson.DysonConfig;
import org.papiricoh.create_nuclearindustry.dyson.DysonFxHooks;
import org.papiricoh.create_nuclearindustry.dyson.DysonRegistry;
import org.papiricoh.create_nuclearindustry.dyson.swarm.DysonSwarm;
import org.papiricoh.create_nuclearindustry.energy.FeSource;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;

import java.util.List;

/**
 * A ray receiver: a dish that takes the light the Dyson swarm beams down and turns it into Forge Energy, pushed into the blocks
 * around it (or pulled by cables). Once a second it asks the swarm for what it can take, up to {@link DysonConfig#receiverMaxFe}
 * FE per tick, and the swarm splits its power among all the receivers that asked ({@link DysonSwarm#share}). It needs the open sky
 * over it; the sun's height and the weather are the swarm's to weigh.
 */
public class RayReceiverBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private final FeSource energy;
    private ReceiverStatus status = ReceiverStatus.WAITING;
    /** FE per tick it takes this second, and how many receivers share the swarm with it. */
    private int share;
    private int sharing;
    private boolean skyClear = true;
    private boolean everReceived;

    public RayReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(DysonRegistry.RECEIVER_ENTITY.get(), pos, state);
        int capacity = (int) Math.min(Integer.MAX_VALUE, 20L * DysonConfig.receiverMaxFe * DysonConfig.receiverBufferSeconds);
        energy = new FeSource(capacity, capacity, this::setChanged);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public FeSource energy() {
        return energy;
    }

    public ReceiverStatus status() {
        return status;
    }

    /** FE per tick it takes from the swarm now. */
    public int share() {
        return share;
    }

    /** Receivers sharing the swarm's power, itself among them. */
    public int sharing() {
        return sharing;
    }

    /** How strongly it receives, 0 to 1 of the most a receiver may take: for its beam and glow. */
    public float intensity() {
        return Math.min(1f, share / (float) Math.max(1, DysonConfig.receiverMaxFe));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level instanceof ServerLevel server) || isVirtual()) return;
        int was = share;
        ReceiverStatus before = status;
        DysonSwarm swarm = DysonSwarm.underSwarm(server) ? DysonSwarm.get(server) : null;
        if (server.getGameTime() % 20 == 0) {
            skyClear = server.canSeeSky(ShipSpace.worldBlock(server, worldPosition).above());
            if (swarm != null && skyClear) swarm.request(worldPosition, demand());
        }
        share = swarm != null && skyClear ? swarm.share(worldPosition) : 0;
        sharing = swarm != null ? swarm.receivers() : 0;
        if (share > 0) energy.generate(share);
        energy.pushFairly(server, worldPosition);
        status = evaluate(server, swarm);
        if (share > 0 && !everReceived) {
            everReceived = true;
            NuclearAdvancements.awardNearby(server, worldPosition, NuclearAdvancements.WITNESS_RADIUS, NuclearEvent.DYSON_POWER);
        }
        if (status != before || was != share) sendData();
    }

    /** FE per tick it can take for the next second: what fits its store, up to its most. */
    private int demand() {
        long room = (long) energy.getMaxEnergyStored() - energy.getEnergyStored();
        return (int) Math.max(0, Math.min(DysonConfig.receiverMaxFe, Math.max(room / 20, room > 0 ? 1 : 0)));
    }

    private ReceiverStatus evaluate(ServerLevel server, DysonSwarm swarm) {
        if (swarm == null) return ReceiverStatus.WRONG_DIMENSION;
        if (!skyClear) return ReceiverStatus.NO_SKY;
        if (share > 0) return ReceiverStatus.RECEIVING;
        if (swarm.totalSails() == 0) return ReceiverStatus.NO_SWARM;
        if (DysonSwarm.sunFactorNow(server) <= 0) return ReceiverStatus.NO_SUN;
        if (energy.getEnergyStored() >= energy.getMaxEnergyStored()) return ReceiverStatus.FULL;
        return ReceiverStatus.WAITING;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()).withStyle(ChatFormatting.GOLD));
        for (Component line : statusLines())
            tooltip.add(Component.literal("     ").append(line).withStyle(ChatFormatting.GRAY));
        return true;
    }

    /** Its state in words: what it is doing, what it takes, how many share the swarm and what it holds. */
    public List<Component> statusLines() {
        return List.of(Component.translatable(status.key()),
                Component.translatable("create_nuclearindustry.receiver.share", share),
                Component.translatable("create_nuclearindustry.receiver.sharing", sharing),
                Component.translatable("create_nuclearindustry.receiver.stored", energy.getEnergyStored(), energy.getMaxEnergyStored()));
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putBoolean("EverReceived", everReceived);
        if (clientPacket) {
            tag.putInt("Status", status.ordinal());
            tag.putInt("Share", share);
            tag.putInt("Sharing", sharing);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        energy.set(tag.getInt("Energy"));
        everReceived = tag.getBoolean("EverReceived");
        if (clientPacket) {
            int was = share;
            status = ReceiverStatus.byOrdinal(tag.getInt("Status"));
            share = Math.max(0, tag.getInt("Share"));
            sharing = Math.max(0, tag.getInt("Sharing"));
            if (was == 0 && share > 0 && level != null && level.isClientSide) DysonFxHooks.receiverLit.accept(this);
        }
    }
}
