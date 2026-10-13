package org.papiricoh.create_nuclearindustry.fusion.cryo.well;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoConfig;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoRegistry;
import org.papiricoh.create_nuclearindustry.fusion.cryo.physics.CryoplantModel;
import org.papiricoh.create_nuclearindustry.fusion.cryo.physics.HeliumWellModel;
import org.papiricoh.create_nuclearindustry.multiblock.NeighbourFluidHandlers;

import java.util.List;
import java.util.Locale;

/**
 * A helium well: a stress consumer that trickles helium gas into its own small tank while its shaft
 * turns fast enough, and pushes it every tick into the tanks and machines beside it (every side but
 * the shaft's). Pipes may also drain it. The fractional output is carried in {@link #balance}; with
 * the tank full the well stops drawing (see {@link HeliumWellModel}).
 */
public class HeliumWellBlockEntity extends KineticBlockEntity {
    public static final int CAPACITY = 1000;
    /** Ticks between client syncs while the tank level changes. */
    private static final int SYNC_INTERVAL = 10;

    private final FluidTank tank = new FluidTank(CAPACITY, NuclearFluidHelper::isHelium) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IFluidHandler drainOnly = new DrainOnly();
    private final NeighbourFluidHandlers neighbours = new NeighbourFluidHandlers(this, null);
    /** Fractional helium owed to the tank, mB (0..1). */
    private double balance;
    private int syncedAmount = -1;
    private long lastSync;

    public HeliumWellBlockEntity(BlockPos pos, BlockState state) {
        super(CryoRegistry.HELIUM_WELL_ENTITY.get(), pos, state);
    }

    /** Share of rated output the shaft gives, 0..1: nothing below the minimum speed or while overstressed. */
    public double speedFactor() {
        if (isOverStressed()) {
            return 0.0;
        }
        return CryoplantModel.speedFactor(getSpeed(), CryoConfig.heliumWellMinSpeed, CryoConfig.heliumWellRatedSpeed);
    }

    /** Helium drawn per tick at the current speed, mB/t, whether or not there is room for it. */
    public double outputPerTick() {
        return HeliumWellModel.output(CryoConfig.heliumWellRate, speedFactor());
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        HeliumWellModel.Step step = HeliumWellModel.tick(balance, outputPerTick(), tank.getSpace());
        if (step.balance() != balance) {
            balance = step.balance();
            setChanged();
        }
        if (step.released() > 0) {
            tank.fill(new FluidStack(AllNuclearFluids.HELIUM.get(), step.released()), IFluidHandler.FluidAction.EXECUTE);
        }
        pushToNeighbours();
        int amount = tank.getFluidAmount();
        if (amount != syncedAmount && level.getGameTime() - lastSync >= SYNC_INTERVAL) {
            syncedAmount = amount;
            lastSync = level.getGameTime();
            sendData();
        }
    }

    /** Offers the stored helium to every neighbour but the one behind the shaft, up to the transfer limit in all. */
    private void pushToNeighbours() {
        int budget = CryoConfig.heliumWellTransfer;
        if (tank.isEmpty() || budget <= 0) {
            return;
        }
        Direction shaft = HeliumWellBlock.shaftSide(getBlockState());
        for (Direction side : Direction.values()) {
            if (side == shaft) {
                continue;
            }
            IFluidHandler neighbour = neighbours.get(side);
            if (neighbour == null) {
                continue;
            }
            budget -= FluidUtil.tryFluidTransfer(neighbour, drainOnly, budget, true).getAmount();
            if (budget <= 0 || tank.isEmpty()) {
                return;
            }
        }
    }

    /** The handler pipes and neighbours see: helium can be drained from every side but the shaft's, never filled. */
    @Nullable
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        return side != null && side == HeliumWellBlock.shaftSide(getBlockState()) ? null : drainOnly;
    }

    /** The well's own tank (game tests). */
    public FluidTank tank() {
        return tank;
    }

    /** Fractional helium owed to the tank, mB (game tests). */
    public double balance() {
        return balance;
    }

    @Override
    public float calculateStressApplied() {
        float impact = (float) CryoConfig.heliumWellStress;
        lastStressApplied = impact;
        return impact;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putDouble("Balance", balance);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        tank.readFromNBT(registries, tag.getCompound("Tank"));
        double read = tag.getDouble("Balance");
        balance = Double.isFinite(read) ? Math.max(0.0, Math.min(1.0, read)) : 0.0;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        String k = "create_nuclearindustry.helium_well.";
        tooltip.add(Component.literal("    ").append(Component.translatable("block.create_nuclearindustry.helium_well"))
                .withStyle(ChatFormatting.GOLD));
        double factor = speedFactor();
        if (factor <= 0) {
            tooltip.add(line(Component.translatable(k + "too_slow", Math.round(CryoConfig.heliumWellMinSpeed)), ChatFormatting.RED));
        } else if (tank.getSpace() <= 0) {
            tooltip.add(line(Component.translatable(k + "full"), ChatFormatting.RED));
        } else {
            tooltip.add(line(Component.translatable(k + "drawing",
                    String.format(Locale.ROOT, "%.1f", HeliumWellModel.perMinute(outputPerTick())), Math.round(factor * 100)),
                    ChatFormatting.GREEN));
        }
        tooltip.add(line(Component.translatable(k + "tank", tank.getFluidAmount(), CAPACITY), ChatFormatting.AQUA));
        return true;
    }

    private static Component line(Component text, ChatFormatting style) {
        return Component.literal("     ").append(text).withStyle(style);
    }

    /** Lets helium out of the tank and nothing in. */
    private final class DrainOnly implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            return tank.getFluid();
        }

        @Override
        public int getTankCapacity(int index) {
            return CAPACITY;
        }

        @Override
        public boolean isFluidValid(int index, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return tank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return tank.drain(maxDrain, action);
        }
    }
}
