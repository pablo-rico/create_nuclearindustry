package org.papiricoh.create_nuclearindustry.reactor.blockentity;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.reactor.block.TurbineOutputBlock;

import java.util.List;

public class TurbineOutputBlockEntity extends GeneratingKineticBlockEntity {
    private static final int TANK_CAPACITY = 16_000;
    private static final int MAX_STEAM_PER_TICK = 80;
    private static final float MAX_RPM = 64.0f;
    private static final float MAX_STRESS_CAPACITY = 512.0f;

    private final FluidTank steamTank = new FluidTank(TANK_CAPACITY, NuclearFluidHelper::isTurbineSteam) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank condensateTank = new FluidTank(TANK_CAPACITY, NuclearFluidHelper::isCoolant) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IFluidHandler fluidHandler = new TurbineFluidHandler();

    private boolean formed;
    private int validationCooldown;
    private int recentSteamUse;
    private float generatedSpeed;
    private float generatedCapacity;

    public TurbineOutputBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.TURBINE_OUTPUT.get(), pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }

        if (validationCooldown-- <= 0) {
            validationCooldown = 20;
            formed = validateStructure();
        }

        int consumed = formed ? processSteam() : 0;
        recentSteamUse = (recentSteamUse * 7 + consumed) / 8;

        float oldSpeed = generatedSpeed;
        float oldCapacity = generatedCapacity;
        float targetRatio = Math.min(1.0f, recentSteamUse / (float) MAX_STEAM_PER_TICK);
        generatedSpeed = getSignedSpeed(MAX_RPM * targetRatio);
        generatedCapacity = MAX_STRESS_CAPACITY * targetRatio;

        if (Math.abs(oldSpeed - generatedSpeed) > 0.25f || Math.abs(oldCapacity - generatedCapacity) > 1.0f) {
            updateGeneratedRotation();
            notifyStressCapacityChange(generatedCapacity);
        }
    }

    public IFluidHandler getFluidHandler(Direction direction) {
        return fluidHandler;
    }

    private boolean validateStructure() {
        Direction facing = getBlockState().getValue(TurbineOutputBlock.FACING);
        BlockPos rotorPos = getBlockPos().relative(facing.getOpposite());
        BlockPos casingPos = rotorPos.relative(facing.getOpposite());
        BlockState rotor = level.getBlockState(rotorPos);
        BlockState casing = level.getBlockState(casingPos);

        return rotor.is(AllNuclearBlocks.TURBINE_ROTOR.get())
                && rotor.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS) == facing.getAxis()
                && casing.is(AllNuclearBlocks.TURBINE_CASING.get());
    }

    private int processSteam() {
        if (steamTank.isEmpty() || condensateTank.getSpace() <= 0) {
            return 0;
        }

        FluidStack steam = steamTank.getFluid();
        boolean heavy = NuclearFluidHelper.isHeavySteam(steam);
        FluidStack condensate = new FluidStack(heavy ? AllNuclearFluids.HEAVY_WATER.get() : Fluids.WATER, MAX_STEAM_PER_TICK);
        int outputSpace = condensateTank.fill(condensate, IFluidHandler.FluidAction.SIMULATE);
        if (outputSpace <= 0) {
            return 0;
        }

        int amount = Math.min(MAX_STEAM_PER_TICK, outputSpace);
        FluidStack drained = steamTank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return 0;
        }

        condensateTank.fill(new FluidStack(condensate.getFluid(), drained.getAmount()), IFluidHandler.FluidAction.EXECUTE);
        return drained.getAmount();
    }

    private float getSignedSpeed(float speed) {
        Direction facing = getBlockState().getValue(TurbineOutputBlock.FACING);
        return facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? -speed : speed;
    }

    @Override
    public float getGeneratedSpeed() {
        return generatedSpeed;
    }

    @Override
    public float calculateAddedStressCapacity() {
        return generatedCapacity;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("Steam Turbine").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Structure: " + (formed ? "Formed" : "Incomplete"))
                .withStyle(formed ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.add(Component.literal("  Steam: " + steamTank.getFluidAmount() + "/" + steamTank.getCapacity() + " mB")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Condensate: " + condensateTank.getFluidAmount() + "/" + condensateTank.getCapacity() + " mB")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(String.format("  Generated: %.1f RPM", Math.abs(generatedSpeed)))
                .withStyle(ChatFormatting.GRAY));
        return true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("steam", steamTank.writeToNBT(registries, new CompoundTag()));
        tag.put("condensate", condensateTank.writeToNBT(registries, new CompoundTag()));
        tag.putBoolean("formed", formed);
        tag.putInt("recentSteamUse", recentSteamUse);
        tag.putFloat("generatedSpeed", generatedSpeed);
        tag.putFloat("generatedCapacity", generatedCapacity);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("steam")) {
            steamTank.readFromNBT(registries, tag.getCompound("steam"));
        }
        if (tag.contains("condensate")) {
            condensateTank.readFromNBT(registries, tag.getCompound("condensate"));
        }
        formed = tag.getBoolean("formed");
        recentSteamUse = tag.getInt("recentSteamUse");
        generatedSpeed = tag.getFloat("generatedSpeed");
        generatedCapacity = tag.getFloat("generatedCapacity");
    }

    private class TurbineFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? steamTank.getFluid() : condensateTank.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? steamTank.getCapacity() : condensateTank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && steamTank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return NuclearFluidHelper.isTurbineSteam(resource) ? steamTank.fill(resource, action) : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return NuclearFluidHelper.isCoolant(resource) ? condensateTank.drain(resource, action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return condensateTank.drain(maxDrain, action);
        }
    }
}
