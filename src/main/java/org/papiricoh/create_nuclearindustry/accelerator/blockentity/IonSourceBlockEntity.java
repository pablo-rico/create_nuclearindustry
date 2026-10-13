package org.papiricoh.create_nuclearindustry.accelerator.blockentity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorConfig;

import java.util.List;

/**
 * The ion source: a hydrogen tank, filled by pipe, and the drive (shaft or Forge Energy) that strips
 * the gas into protons. The controller draws the hydrogen while it makes beam.
 */
public class IonSourceBlockEntity extends AcceleratorMachineBlockEntity {
    public static final int CAPACITY = 4000;
    /** Hydrogen from any mod: the {@code c:hydrogen} fluid tag. */
    public static final TagKey<Fluid> HYDROGEN = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", "hydrogen"));

    private final FluidTank tank = new FluidTank(CAPACITY, stack -> stack.is(HYDROGEN)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    /** Fractional hydrogen owed to the tank, mB. */
    private double balance;

    public IonSourceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, null);
    }

    public IFluidHandler fluidHandler() {
        return tank;
    }

    public FluidTank tank() {
        return tank;
    }

    /** Whether there is hydrogen for this tick's draw. */
    public boolean hasGas(double millibuckets) {
        return tank.getFluidAmount() + balance >= millibuckets - 1.0e-9;
    }

    /**
     * Draws {@code millibuckets} of hydrogen and returns the share of it there was, 0..1. Whole
     * millibuckets leave the tank; the fraction is carried over.
     */
    public double draw(double millibuckets) {
        if (millibuckets <= 0) {
            return 1;
        }
        double available = tank.getFluidAmount() + balance;
        double drawn = Math.min(millibuckets, Math.max(0, available));
        balance -= drawn;
        if (balance <= -1) {
            int whole = (int) Math.floor(-balance);
            balance += tank.drain(whole, IFluidHandler.FluidAction.EXECUTE).getAmount();
        }
        return drawn / millibuckets;
    }

    /** Fills the tank (game tests). */
    public void fill(FluidStack stack) {
        tank.fill(stack, IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    protected boolean wantsPower() {
        return linked() && phase().hasBeam();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(line(Component.translatable("create_nuclearindustry.accelerator.ion_source.goggles.hydrogen",
                tank.getFluidAmount(), CAPACITY, fmt1(AcceleratorConfig.hydrogenPerTick * 20)),
                tank.isEmpty() ? ChatFormatting.RED : ChatFormatting.AQUA));
        return true;
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
        balance = tag.getDouble("Balance");
    }
}
