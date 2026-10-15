package org.papiricoh.create_nuclearindustry.antimatter.conduit;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.accelerator.energy.FeBuffer;
import org.papiricoh.create_nuclearindustry.accelerator.physics.ConduitTransfer;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterConfig;

import java.util.List;
import java.util.Locale;

/**
 * One segment of Antimatter Conduit: its share of the network's energy buffer (a cable on any
 * segment feeds the whole network) and, for clients, whether the network is powered and whether
 * antimatter is flowing through it now, for the travelling glow.
 */
public class AntimatterConduitBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    /** Ticks of its own demand each segment's buffer holds. */
    public static final int BUFFER_TICKS = 100;

    private final FeBuffer energy;
    @Nullable
    private ConduitNetwork network;
    private boolean powered;
    private boolean flowing;

    public AntimatterConduitBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.energy = new FeBuffer(bufferCapacity(), this::setChanged);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    private static int bufferCapacity() {
        return Math.max(1, AntimatterConfig.conduit().fePerBlock() * BUFFER_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        int capacity = bufferCapacity();
        if (capacity != energy.getMaxEnergyStored()) {
            energy.resize(capacity);
        }
        if (network == null || !network.valid()) {
            network = ConduitNetwork.build(level, worldPosition);
        }
        network.tick(level);
    }

    void joinNetwork(ConduitNetwork network) {
        this.network = network;
    }

    /** This segment's network, built now if it has none (game tests). */
    public ConduitNetwork network() {
        if ((network == null || !network.valid()) && level != null && !level.isClientSide) {
            network = ConduitNetwork.build(level, worldPosition);
        }
        return network;
    }

    public void invalidateNetwork() {
        if (network != null) {
            network.invalidate();
        }
        network = null;
    }

    @Override
    public void invalidate() {
        invalidateNetwork();
        super.invalidate();
    }

    void setStatus(boolean powered, boolean flowing) {
        if (powered != this.powered || flowing != this.flowing) {
            this.powered = powered;
            this.flowing = flowing;
            sendData();
        }
    }

    /** The network had all its energy on its last tick (synced). */
    public boolean powered() {
        return powered;
    }

    /** Antimatter moved through the network on its last tick (synced): the renderer's travelling glow. */
    public boolean flowing() {
        return flowing;
    }

    public IEnergyStorage energy() {
        return energy;
    }

    public FeBuffer buffer() {
        return energy;
    }

    // ------------------------------------------------------------------ goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        String k = "create_nuclearindustry.antimatter_conduit.goggles.";
        ConduitTransfer.Params p = AntimatterConfig.conduit();
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("     ").append(Component.translatable(powered ? k + "powered" : k + "unpowered"))
                .withStyle(powered ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.add(Component.literal("     ").append(Component.translatable(flowing ? k + "flowing" : k + "idle",
                String.format(Locale.ROOT, "%.2f", p.ratePerTick() * 20))).withStyle(flowing ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        tooltip.add(Component.literal("     ").append(Component.translatable(k + "energy", energy.getEnergyStored(),
                energy.getMaxEnergyStored(), p.fePerBlock())).withStyle(ChatFormatting.GRAY));
        return true;
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putBoolean("Powered", powered);
        tag.putBoolean("Flowing", flowing);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        energy.set(tag.getInt("Energy"));
        powered = tag.getBoolean("Powered");
        flowing = tag.getBoolean("Flowing");
    }
}
