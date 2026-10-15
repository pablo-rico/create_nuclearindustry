package org.papiricoh.create_nuclearindustry.wastetransport.cistern;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;
import org.papiricoh.create_nuclearindustry.maintenance.MaintenanceKitItem;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitter;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitters;
import org.papiricoh.create_nuclearindustry.wastetransport.WasteCargo;
import org.papiricoh.create_nuclearindustry.wastetransport.WasteTransportConfig;
import org.papiricoh.create_nuclearindustry.wastetransport.WasteTransportText;
import org.papiricoh.create_nuclearindustry.wastetransport.breach.BreachEffects;
import org.papiricoh.create_nuclearindustry.wastetransport.breach.BreachModel;
import org.papiricoh.create_nuclearindustry.wastetransport.breach.WasteContainer;

import java.util.List;

/**
 * A Create fluid tank behind a lead and concrete jacket. It only takes the fluids in
 * {@code radioactive_waste_fluids}, has no window (and so is never a boiler), and irradiates its
 * surroundings through its shielding, much more once breached. The controller keeps the fluid,
 * the breach and the single radiation source of the whole multiblock.
 */
public class WasteCisternBlockEntity extends FluidTankBlockEntity implements WasteContainer, RadiationEmitter, Clearable {
    private static final int UPDATE_INTERVAL = 20;

    /** Breach damage of the multiblock, on its controller: 0 sealed, 1 freshly holed. */
    private float breach;
    /** Fluid and breach a contraption handed back to this block while it was not the controller. */
    @Nullable
    private FluidStack pendingFluid;
    private float pendingBreach;

    public WasteCisternBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        window = false;
        setLazyTickRate(UPDATE_INTERVAL);
    }

    @Override
    protected SmartFluidTank createInventory() {
        // Runs from the superclass's field initialiser: nothing of this class exists yet.
        SmartFluidTank tank = new SmartFluidTank(getCapacityMultiplier(), this::onFluidStackChanged);
        tank.setValidator(WasteCargo::acceptsFluid);
        return tank;
    }

    /** The fluid handler the whole multiblock shares, for capabilities: its controller's tank. */
    @Nullable
    public IFluidHandler fluidHandler() {
        FluidTankBlockEntity controllerBE = getControllerBE();
        return controllerBE == null ? null : controllerBE.getTankInventory();
    }

    @Nullable
    public WasteCisternBlockEntity controllerCistern() {
        return getControllerBE() instanceof WasteCisternBlockEntity cistern ? cistern : null;
    }

    // ------------------------------------------------------------------ no windows, no boiler

    @Override
    public void toggleWindows() {}

    @Override
    public void setWindows(boolean window) {
        super.setWindows(false);
    }

    @Override
    public void updateBoilerState() {}

    @Override
    public void setExtraData(@Nullable Object data) {}

    @Nullable
    @Override
    public Object getExtraData() {
        return false;
    }

    @Override
    public Object modifyExtraData(Object data) {
        return false;
    }

    // ------------------------------------------------------------------ multiblock bookkeeping

    @Override
    public void setController(BlockPos newController) {
        // A cistern merged into a bigger one hands its breach to the new controller.
        if (level != null && !level.isClientSide && breach > 0 && !newController.equals(worldPosition)
                && level.getBlockEntity(newController) instanceof WasteCisternBlockEntity target && target != this) {
            target.breach = Math.max(target.breach, breach);
            breach = 0;
            target.notifyUpdate();
        }
        super.setController(newController);
    }

    @Override
    public void removeController(boolean keepFluids) {
        // Split off a breached cistern, every piece stays breached.
        if (level != null && !level.isClientSide && controller != null && !isController()
                && level.getBlockEntity(controller) instanceof WasteCisternBlockEntity old) {
            breach = Math.max(breach, old.breach);
        }
        super.removeController(keepFluids);
    }

    /**
     * What a contraption hands back when it puts this block down. After a turn the old controller may
     * no longer be the controller: then the fluid waits here for the new one.
     */
    public void applyMounted(FluidStack fluid, float mountedBreach) {
        if (isController()) {
            tankInventory.setFluid(fluid.copy());
            breach = Math.max(0, mountedBreach);
            notifyUpdate();
        } else {
            pendingFluid = fluid.copy();
            pendingBreach = mountedBreach;
        }
    }

    /** Empties the tank (structure placement and test arenas clear block entities this way). */
    @Override
    public void clearContent() {
        tankInventory.setFluid(FluidStack.EMPTY);
        pendingFluid = null;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void initialize() {
        super.initialize();
        if (level != null && !level.isClientSide) {
            RadiationEmitters.register(this);
        }
    }

    @Override
    public void invalidate() {
        if (level != null && !level.isClientSide) {
            RadiationEmitters.unregister(this);
        }
        super.invalidate();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || pendingFluid == null) {
            return;
        }
        WasteCisternBlockEntity controllerBE = controllerCistern();
        if (controllerBE != null && controllerBE != this) {
            controllerBE.tankInventory.setFluid(pendingFluid);
            controllerBE.breach = Math.max(controllerBE.breach, pendingBreach);
            controllerBE.notifyUpdate();
            pendingFluid = null;
            pendingBreach = 0;
        } else if (isController()) {
            applyMounted(pendingFluid, pendingBreach);
            pendingFluid = null;
            pendingBreach = 0;
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (!(level instanceof ServerLevel serverLevel) || !isController() || breach <= 0) {
            return;
        }
        // A breached cistern drips: it loses fluid and the ground under it takes the leak.
        BreachEffects.leak(serverLevel, worldPosition, bareActivity());
        int lost = BreachModel.fluidLeakPerSecond(tankInventory.getFluidAmount(), WasteTransportConfig.breachLeakFactor);
        if (lost > 0) {
            tankInventory.drain(lost, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    // ------------------------------------------------------------------ waste container

    @Override
    public double bareActivity() {
        WasteCisternBlockEntity controllerBE = controllerCistern();
        return controllerBE == null ? 0 : WasteCargo.activity(controllerBE.tankInventory.getFluid());
    }

    @Override
    public float breachDamage() {
        WasteCisternBlockEntity controllerBE = controllerCistern();
        return controllerBE == null ? 0 : controllerBE.breach;
    }

    @Override
    public boolean markBreached() {
        WasteCisternBlockEntity controllerBE = controllerCistern();
        if (controllerBE == null) {
            return false;
        }
        boolean wasSealed = controllerBE.breach <= 0;
        controllerBE.breach = 1;
        controllerBE.notifyUpdate();
        return wasSealed;
    }

    @Override
    public void loseSpilledContents() {
        WasteCisternBlockEntity controllerBE = controllerCistern();
        if (controllerBE != null) {
            int spilled = BreachModel.fluidSpill(controllerBE.tankInventory.getFluidAmount(), WasteTransportConfig.spillFluidFraction);
            if (spilled > 0) {
                controllerBE.tankInventory.drain(spilled, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    @Override
    public MaintenanceKitItem.Outcome repair(double amount) {
        WasteCisternBlockEntity controllerBE = controllerCistern();
        if (controllerBE == null) {
            return MaintenanceKitItem.Outcome.NO_MACHINE;
        }
        if (controllerBE.breach <= 0) {
            return MaintenanceKitItem.Outcome.NOTHING_TO_DO;
        }
        controllerBE.breach = BreachModel.repair(controllerBE.breach, amount);
        controllerBE.notifyUpdate();
        return MaintenanceKitItem.Outcome.MENDED;
    }

    // ------------------------------------------------------------------ radiation

    @Override
    public Vec3 radiationOrigin() {
        Vec3 corner = Vec3.atLowerCornerOf(worldPosition);
        Vec3 centre = isController() ? corner.add(width / 2.0, height / 2.0, width / 2.0) : corner.add(0.5, 0.5, 0.5);
        return ShipSpace.toWorld(level, centre);
    }

    @Override
    public double radiationStrength() {
        if (!isController()) {
            return 0;
        }
        return BreachModel.emission(WasteCargo.activity(tankInventory.getFluid()), breach > 0, WasteTransportConfig.shieldedTransmission,
                WasteTransportConfig.breachedTransmission, WasteTransportConfig.maxEmission);
    }

    @Nullable
    @Override
    public BoundingBox radiationIgnoreBox() {
        if (!isController()) {
            return null;
        }
        BlockPos far = worldPosition.offset(width - 1, height - 1, width - 1);
        return BoundingBox.fromCorners(worldPosition, far);
    }

    // ------------------------------------------------------------------ goggles and saving

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        WasteCisternBlockEntity controllerBE = controllerCistern();
        if (controllerBE == null) {
            return false;
        }
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()).withStyle(ChatFormatting.GOLD));
        WasteTransportText.addStatus(tooltip, controllerBE.breach, controllerBE.bareActivity(), controllerBE.radiationStrength());
        containedFluidTooltip(tooltip, isPlayerSneaking, controllerBE.getTankInventory());
        return true;
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        if (breach > 0) {
            compound.putFloat("Breach", breach);
        }
        if (pendingFluid != null && !clientPacket) {
            compound.put("PendingFluid", pendingFluid.saveOptional(registries));
            compound.putFloat("PendingBreach", pendingBreach);
        }
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        window = false;
        breach = compound.getFloat("Breach");
        if (!clientPacket) {
            pendingFluid = compound.contains("PendingFluid") ? FluidStack.parseOptional(registries, compound.getCompound("PendingFluid")) : null;
            pendingBreach = compound.getFloat("PendingBreach");
        }
    }
}
