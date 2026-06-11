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
    private static final int STEAM_PER_PORT_PER_TICK = 80;
    private static final float ACTIVE_RPM = 32.0f;
    private static final float STRESS_CAPACITY_PER_PORT = 512.0f;

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

    private boolean formed;
    private int validationCooldown;
    private int currentSteamUse;
    private int recentSteamUse;
    private float generatedSpeed;
    private float generatedCapacity;
    private int steamInputPortCount;
    private int condensateOutputPortCount;

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
        currentSteamUse = consumed;
        recentSteamUse = (recentSteamUse * 7 + consumed) / 8;

        float oldSpeed = generatedSpeed;
        float oldCapacity = generatedCapacity;
        generatedSpeed = consumed > 0 ? getSignedSpeed(ACTIVE_RPM) : 0.0f;
        generatedCapacity = STRESS_CAPACITY_PER_PORT * recentSteamUse / (float) STEAM_PER_PORT_PER_TICK;

        if (Math.abs(oldSpeed - generatedSpeed) > 0.25f || Math.abs(oldCapacity - generatedCapacity) > 1.0f) {
            updateGeneratedRotation();
            if (hasNetwork()) {
                notifyStressCapacityChange(generatedCapacity);
            }
        }
    }

    private boolean validateStructure() {
        Direction facing = getBlockState().getValue(TurbineOutputBlock.FACING);
        BlockPos rotorPos = getBlockPos().relative(facing.getOpposite());
        BlockPos casingPos = rotorPos.relative(facing.getOpposite());
        BlockState rotor = level.getBlockState(rotorPos);
        BlockState casing = level.getBlockState(casingPos);

        boolean baseFormed = rotor.is(AllNuclearBlocks.TURBINE_ROTOR.get())
                && rotor.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS) == facing.getAxis()
                && casing.is(AllNuclearBlocks.TURBINE_CASING.get());
        countLinkedPorts();
        return baseFormed && steamInputPortCount > 0 && condensateOutputPortCount > 0;
    }

    private void countLinkedPorts() {
        steamInputPortCount = 0;
        condensateOutputPortCount = 0;
        if (level == null) {
            return;
        }
        for (BlockPos turbineBlock : getStructureBlocks()) {
            for (Direction direction : Direction.values()) {
                BlockPos portPos = turbineBlock.relative(direction);
                BlockState portState = level.getBlockState(portPos);
                if (!portState.is(AllNuclearBlocks.TURBINE_FLUID_PORT.get())) {
                    continue;
                }
                if (portState.getValue(org.papiricoh.create_nuclearindustry.reactor.block.TurbineFluidPortBlock.MODE)
                        == org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode.INPUT) {
                    steamInputPortCount++;
                } else {
                    condensateOutputPortCount++;
                }
            }
        }
    }

    private List<BlockPos> getStructureBlocks() {
        Direction facing = getBlockState().getValue(TurbineOutputBlock.FACING);
        BlockPos rotorPos = getBlockPos().relative(facing.getOpposite());
        BlockPos casingPos = rotorPos.relative(facing.getOpposite());
        return List.of(getBlockPos(), rotorPos, casingPos);
    }

    public boolean isPortAttached(BlockPos portPos) {
        if (level == null) {
            return false;
        }
        for (BlockPos turbineBlock : getStructureBlocks()) {
            if (portPos.distManhattan(turbineBlock) == 1) {
                return true;
            }
        }
        return false;
    }

    private int processSteam() {
        if (steamTank.isEmpty() || condensateTank.getSpace() <= 0) {
            return 0;
        }

        int throughput = getSteamThroughputLimit();
        if (throughput <= 0) {
            return 0;
        }

        FluidStack steam = steamTank.getFluid();
        boolean heavy = NuclearFluidHelper.isHeavySteam(steam);
        FluidStack condensate = new FluidStack(heavy ? AllNuclearFluids.HEAVY_WATER.get() : Fluids.WATER, throughput);
        int outputSpace = condensateTank.fill(condensate, IFluidHandler.FluidAction.SIMULATE);
        if (outputSpace <= 0) {
            return 0;
        }

        int amount = Math.min(throughput, outputSpace);
        FluidStack drained = steamTank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return 0;
        }

        condensateTank.fill(new FluidStack(condensate.getFluid(), drained.getAmount()), IFluidHandler.FluidAction.EXECUTE);
        return drained.getAmount();
    }

    private int getSteamThroughputLimit() {
        return Math.min(steamInputPortCount, condensateOutputPortCount) * STEAM_PER_PORT_PER_TICK;
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
        tooltip.add(Component.literal("  Steam input ports: " + steamInputPortCount)
                .withStyle(steamInputPortCount > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.add(Component.literal("  Condensate output ports: " + condensateOutputPortCount)
                .withStyle(condensateOutputPortCount > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.add(Component.literal("  Steam: " + steamTank.getFluidAmount() + "/" + steamTank.getCapacity() + " mB")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Condensate: " + condensateTank.getFluidAmount() + "/" + condensateTank.getCapacity() + " mB")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(String.format("  Generated: %.1f RPM", Math.abs(generatedSpeed)))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Steam use: " + currentSteamUse + "/" + getSteamThroughputLimit() + " mB/t")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(String.format("  Stress capacity: %.0f SU", generatedCapacity))
                .withStyle(ChatFormatting.GRAY));
        return true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("steam", steamTank.writeToNBT(registries, new CompoundTag()));
        tag.put("condensate", condensateTank.writeToNBT(registries, new CompoundTag()));
        tag.putBoolean("formed", formed);
        tag.putInt("currentSteamUse", currentSteamUse);
        tag.putInt("recentSteamUse", recentSteamUse);
        tag.putFloat("generatedSpeed", generatedSpeed);
        tag.putFloat("generatedCapacity", generatedCapacity);
        tag.putInt("steamInputPortCount", steamInputPortCount);
        tag.putInt("condensateOutputPortCount", condensateOutputPortCount);
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
        currentSteamUse = tag.getInt("currentSteamUse");
        recentSteamUse = tag.getInt("recentSteamUse");
        generatedSpeed = tag.getFloat("generatedSpeed");
        generatedCapacity = tag.getFloat("generatedCapacity");
        steamInputPortCount = tag.getInt("steamInputPortCount");
        condensateOutputPortCount = tag.getInt("condensateOutputPortCount");
    }

    public int getTankCapacity() {
        return TANK_CAPACITY;
    }

    public int getSteamAmount() {
        return steamTank.getFluidAmount();
    }

    public int getCondensateAmount() {
        return condensateTank.getFluidAmount();
    }

    public FluidStack getSteamFluid() {
        return steamTank.getFluid().copy();
    }

    public FluidStack getCondensateFluid() {
        return condensateTank.getFluid().copy();
    }

    public int fillSteam(FluidStack resource, IFluidHandler.FluidAction action) {
        return NuclearFluidHelper.isTurbineSteam(resource) ? steamTank.fill(resource, action) : 0;
    }

    public FluidStack drainCondensate(FluidStack resource, IFluidHandler.FluidAction action) {
        return NuclearFluidHelper.isCoolant(resource) ? condensateTank.drain(resource, action) : FluidStack.EMPTY;
    }

    public FluidStack drainCondensate(int maxDrain, IFluidHandler.FluidAction action) {
        return condensateTank.drain(maxDrain, action);
    }
}
