package org.papiricoh.create_nuclearindustry.wastetransport.cask;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
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

import java.util.ArrayList;
import java.util.List;

/**
 * One block of a Shielded Waste Cask, ported from Create's Item Vault (whose classes are tied to
 * Create's own block and block entity types). Blocks along a horizontal axis connect into one cask
 * up to 3x3 across and three times as long; each block keeps its own slots and the controller (the
 * lowest corner) presents them as one inventory, keeps the breach and is the only radiation source.
 */
public class WasteCaskBlockEntity extends SmartBlockEntity
        implements IMultiBlockEntityContainer.Inventory, Clearable, WasteContainer, RadiationEmitter, IHaveGoggleInformation {
    private static final int UPDATE_INTERVAL = 20;

    protected final WasteCaskInventory inventory = new WasteCaskInventory(WasteTransportConfig.caskSlotsPerBlock,
            () -> level == null ? -1L : level.getGameTime(), this::onContentsChanged);
    protected BlockPos controller;
    protected BlockPos lastKnownPos;
    protected boolean updateConnectivity;
    protected int radius = 1;
    protected int length = 1;

    @Nullable
    private IItemHandler handler;
    /** Breach damage of the whole cask, on its controller. */
    private float breach;
    /** Breach a contraption handed back to this block, passed on to the controller on the next tick. */
    private float pendingBreach;
    /** Bare activity of the whole cask, worked out on the controller once a second (and synced for goggles). */
    private double activity;
    private boolean contentsChanged = true;

    public WasteCaskBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(UPDATE_INTERVAL);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    private void onContentsChanged() {
        if (level != null) {
            level.blockEntityChanged(worldPosition);
            WasteCaskBlockEntity controllerBE = controllerCask();
            if (controllerBE != null) {
                controllerBE.contentsChanged = true;
            }
        }
    }

    // ------------------------------------------------------------------ connectivity (from Create's vault)

    protected void updateConnectivity() {
        updateConnectivity = false;
        if (level != null && !level.isClientSide() && isController()) {
            ConnectivityHandler.formMulti(this);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (lastKnownPos == null) {
            lastKnownPos = getBlockPos();
        } else if (!lastKnownPos.equals(worldPosition) && worldPosition != null) {
            onPositionChanged();
            return;
        }
        if (updateConnectivity) {
            updateConnectivity();
        }
        if (pendingBreach > 0 && level != null && !level.isClientSide) {
            WasteCaskBlockEntity controllerBE = controllerCask();
            if (controllerBE != null) {
                controllerBE.breach = Math.max(controllerBE.breach, pendingBreach);
                pendingBreach = 0;
                controllerBE.notifyUpdate();
            }
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (!(level instanceof ServerLevel serverLevel) || !isController()) {
            return;
        }
        double before = activity;
        refreshActivity();
        if (Math.abs(before - activity) > Math.max(0.01, before * 0.01)) {
            sendData();
        }
        if (breach > 0) {
            BreachEffects.leak(serverLevel, worldPosition, activity);
        }
    }

    /** Adds up the activity of every block's slots; on the controller. Public so tests need not wait a second. */
    public void refreshActivity() {
        contentsChanged = false;
        double total = 0;
        for (WasteCaskBlockEntity part : parts()) {
            total += WasteCargo.activity(part.inventory);
        }
        activity = total;
    }

    @Override
    public BlockPos getLastKnownPos() {
        return lastKnownPos;
    }

    @Override
    public boolean isController() {
        return controller == null || worldPosition.getX() == controller.getX() && worldPosition.getY() == controller.getY()
                && worldPosition.getZ() == controller.getZ();
    }

    private void onPositionChanged() {
        removeController(true);
        lastKnownPos = worldPosition;
    }

    @SuppressWarnings("unchecked")
    @Override
    public WasteCaskBlockEntity getControllerBE() {
        if (isController()) {
            return this;
        }
        BlockEntity blockEntity = level == null ? null : level.getBlockEntity(controller);
        return blockEntity instanceof WasteCaskBlockEntity cask ? cask : null;
    }

    @Nullable
    public WasteCaskBlockEntity controllerCask() {
        return getControllerBE();
    }

    @Override
    public void removeController(boolean keepContents) {
        if (level == null || level.isClientSide()) {
            return;
        }
        // Split off a breached cask, every piece stays breached.
        if (controller != null && !isController() && level.getBlockEntity(controller) instanceof WasteCaskBlockEntity old) {
            breach = Math.max(breach, old.breach);
        }
        updateConnectivity = true;
        controller = null;
        radius = 1;
        length = 1;
        handler = null;
        contentsChanged = true;
        invalidateCapabilities();
        setChanged();
        sendData();
    }

    @Override
    public void setController(BlockPos controller) {
        if (level == null || level.isClientSide && !isVirtual() || controller.equals(this.controller)) {
            return;
        }
        if (!level.isClientSide && breach > 0 && !controller.equals(worldPosition)
                && level.getBlockEntity(controller) instanceof WasteCaskBlockEntity target && target != this) {
            target.breach = Math.max(target.breach, breach);
            breach = 0;
            target.notifyUpdate();
        }
        this.controller = controller;
        handler = null;
        invalidateCapabilities();
        setChanged();
        sendData();
    }

    @Override
    public BlockPos getController() {
        return isController() ? worldPosition : controller;
    }

    @Override
    public void preventConnectivityUpdate() {
        updateConnectivity = false;
    }

    @Override
    public void notifyMultiUpdated() {
        handler = null;
        contentsChanged = true;
        invalidateCapabilities();
        setChanged();
        sendData();
    }

    @Override
    public Direction.Axis getMainConnectionAxis() {
        return getMainAxisOf(this);
    }

    @Override
    public int getMaxLength(Direction.Axis longAxis, int width) {
        return longAxis == Direction.Axis.Y ? getMaxWidth() : width * 3;
    }

    @Override
    public int getMaxWidth() {
        return 3;
    }

    @Override
    public int getHeight() {
        return length;
    }

    @Override
    public void setHeight(int height) {
        length = height;
    }

    @Override
    public int getWidth() {
        return radius;
    }

    @Override
    public void setWidth(int width) {
        radius = width;
    }

    @Override
    public boolean hasInventory() {
        return true;
    }

    /** Every block of the cask, from its controller, in slot order; just this block if not connected. */
    public List<WasteCaskBlockEntity> parts() {
        WasteCaskBlockEntity controllerBE = controllerCask();
        List<WasteCaskBlockEntity> parts = new ArrayList<>();
        if (controllerBE == null || level == null) {
            parts.add(this);
            return parts;
        }
        boolean alongZ = WasteCaskBlock.getCaskAxis(controllerBE.getBlockState()) == Direction.Axis.Z;
        for (int along = 0; along < controllerBE.length; along++) {
            for (int a = 0; a < controllerBE.radius; a++) {
                for (int b = 0; b < controllerBE.radius; b++) {
                    BlockPos pos = alongZ ? controllerBE.worldPosition.offset(a, b, along) : controllerBE.worldPosition.offset(along, a, b);
                    WasteCaskBlockEntity part = ConnectivityHandler.partAt(getType(), level, pos);
                    if (part != null) {
                        parts.add(part);
                    }
                }
            }
        }
        return parts;
    }

    // ------------------------------------------------------------------ inventory

    /** The inventory of the whole cask, for capabilities: its blocks' slots one after another. */
    @Nullable
    public IItemHandler itemHandler() {
        if (!isController()) {
            WasteCaskBlockEntity controllerBE = controllerCask();
            return controllerBE == null ? null : controllerBE.itemHandler();
        }
        if (handler == null) {
            List<IItemHandlerModifiable> inventories = new ArrayList<>();
            for (WasteCaskBlockEntity part : parts()) {
                inventories.add(part.inventory);
            }
            handler = new CombinedInvWrapper(inventories.toArray(IItemHandlerModifiable[]::new));
        }
        return handler;
    }

    /** This block's own slots. */
    public WasteCaskInventory getInventoryOfBlock() {
        return inventory;
    }

    /** Puts a contraption's cargo back into this block, and its breach towards the controller. */
    public void applyMounted(IItemHandler cargo, float mountedBreach) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            inventory.setStackInSlot(slot, slot < cargo.getSlots() ? cargo.getStackInSlot(slot).copy() : ItemStack.EMPTY);
        }
        if (isController()) {
            breach = Math.max(0, mountedBreach);
        } else {
            pendingBreach = Math.max(pendingBreach, mountedBreach);
        }
        setChanged();
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    // ------------------------------------------------------------------ waste container

    @Override
    public double bareActivity() {
        WasteCaskBlockEntity controllerBE = controllerCask();
        if (controllerBE == null) {
            return WasteCargo.activity(inventory);
        }
        if (controllerBE.contentsChanged && level != null && !level.isClientSide) {
            controllerBE.refreshActivity();
        }
        return controllerBE.activity;
    }

    @Override
    public float breachDamage() {
        WasteCaskBlockEntity controllerBE = controllerCask();
        return controllerBE == null ? breach : controllerBE.breach;
    }

    @Override
    public boolean markBreached() {
        WasteCaskBlockEntity controllerBE = controllerCask();
        if (controllerBE == null) {
            return false;
        }
        boolean wasSealed = controllerBE.breach <= 0;
        controllerBE.breach = 1;
        controllerBE.notifyUpdate();
        return wasSealed;
    }

    @Override
    public MaintenanceKitItem.Outcome repair(double amount) {
        WasteCaskBlockEntity controllerBE = controllerCask();
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
    public Vec3 radiationOrigin() {
        Vec3 corner = Vec3.atLowerCornerOf(worldPosition);
        if (!isController()) {
            return ShipSpace.toWorld(level, corner.add(0.5, 0.5, 0.5));
        }
        boolean alongZ = WasteCaskBlock.getCaskAxis(getBlockState()) == Direction.Axis.Z;
        Vec3 centre = alongZ ? corner.add(radius / 2.0, radius / 2.0, length / 2.0) : corner.add(length / 2.0, radius / 2.0, radius / 2.0);
        return ShipSpace.toWorld(level, centre);
    }

    @Override
    public double radiationStrength() {
        if (!isController()) {
            return 0;
        }
        if (contentsChanged && level != null && !level.isClientSide) {
            refreshActivity();
        }
        return BreachModel.emission(activity, breach > 0, WasteTransportConfig.shieldedTransmission,
                WasteTransportConfig.breachedTransmission, WasteTransportConfig.maxEmission);
    }

    @Nullable
    @Override
    public BoundingBox radiationIgnoreBox() {
        if (!isController()) {
            return null;
        }
        boolean alongZ = WasteCaskBlock.getCaskAxis(getBlockState()) == Direction.Axis.Z;
        BlockPos far = alongZ ? worldPosition.offset(radius - 1, radius - 1, length - 1) : worldPosition.offset(length - 1, radius - 1, radius - 1);
        return BoundingBox.fromCorners(worldPosition, far);
    }

    // ------------------------------------------------------------------ goggles and saving

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        WasteCaskBlockEntity controllerBE = controllerCask();
        if (controllerBE == null) {
            return false;
        }
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()).withStyle(ChatFormatting.GOLD));
        int blocks = controllerBE.radius * controllerBE.radius * controllerBE.length;
        tooltip.add(Component.literal("     ").append(Component.translatable(WasteTransportText.PREFIX + "goggles.cask_size",
                blocks, blocks * inventory.getSlots())).withStyle(ChatFormatting.GRAY));
        double emission = BreachModel.emission(controllerBE.activity, controllerBE.breach > 0, WasteTransportConfig.shieldedTransmission,
                WasteTransportConfig.breachedTransmission, WasteTransportConfig.maxEmission);
        WasteTransportText.addStatus(tooltip, controllerBE.breach, controllerBE.activity, emission);
        return true;
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        BlockPos controllerBefore = controller;
        int prevSize = radius;
        int prevLength = length;
        updateConnectivity = compound.contains("Uninitialized");
        lastKnownPos = compound.contains("LastKnownPos") ? NBTHelper.readBlockPos(compound, "LastKnownPos") : null;
        controller = compound.contains("Controller") ? NBTHelper.readBlockPos(compound, "Controller") : null;
        if (isController()) {
            radius = Math.max(1, compound.getInt("Size"));
            length = Math.max(1, compound.getInt("Length"));
        }
        breach = compound.getFloat("Breach");
        if (!clientPacket) {
            inventory.deserializeNBT(registries, compound.getCompound("Inventory"));
            pendingBreach = compound.getFloat("PendingBreach");
            contentsChanged = true;
        } else {
            activity = compound.getDouble("Activity");
            boolean changeOfController = controllerBefore == null ? controller != null : !controllerBefore.equals(controller);
            if (hasLevel() && (changeOfController || prevSize != radius || prevLength != length)) {
                level.setBlocksDirty(getBlockPos(), Blocks.AIR.defaultBlockState(), getBlockState());
            }
        }
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        if (updateConnectivity) {
            compound.putBoolean("Uninitialized", true);
        }
        if (lastKnownPos != null) {
            compound.put("LastKnownPos", NbtUtils.writeBlockPos(lastKnownPos));
        }
        if (!isController()) {
            compound.put("Controller", NbtUtils.writeBlockPos(controller));
        } else {
            compound.putInt("Size", radius);
            compound.putInt("Length", length);
        }
        if (breach > 0) {
            compound.putFloat("Breach", breach);
        }
        super.write(compound, registries, clientPacket);
        if (!clientPacket) {
            compound.put("Inventory", inventory.serializeNBT(registries));
            if (pendingBreach > 0) {
                compound.putFloat("PendingBreach", pendingBreach);
            }
        } else if (isController()) {
            compound.putDouble("Activity", activity);
        }
    }
}
