package org.papiricoh.create_nuclearindustry.coolingtower.blockentity;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.coolingtower.CoolingTowerClientHooks;
import org.papiricoh.create_nuclearindustry.coolingtower.CoolingTowerDisplay;
import org.papiricoh.create_nuclearindustry.coolingtower.CoolingTowerGeometry;
import org.papiricoh.create_nuclearindustry.coolingtower.CoolingTowerStructure;
import org.papiricoh.create_nuclearindustry.coolingtower.CoolingTowerStructureValidator;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.multiblock.BlockChange;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockController;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockMessenger;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockValidationResult;
import org.papiricoh.create_nuclearindustry.multiblock.StructureSnapshot;

import java.util.List;
import java.util.Optional;

/**
 * Controller of a hyperboloid cooling tower: condenses steam (and heavy steam) back into water
 * at a rate set by the tower's size, boosted by fans. Produces no power; a plume rises from
 * the top while it works.
 */
public class CoolingTowerControllerBlockEntity extends SmartBlockEntity implements MultiblockController, IHaveGoggleInformation {
    private static final int SYNC_INTERVAL = 10;
    private static final int FAN_SCAN_INTERVAL = 20;
    private static final int BASE_TANK = 16000;

    private final FluidTank steamTank = new FluidTank(BASE_TANK, NuclearFluidHelper::isCondensable) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank waterTank = new FluidTank(BASE_TANK, NuclearFluidHelper::isCoolant) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final MultiblockMessenger messenger = new MultiblockMessenger("Cooling Tower");
    private Optional<CoolingTowerStructure> structure = Optional.empty();
    private StructureSnapshot snapshot;
    private MultiblockValidationResult<CoolingTowerStructure> lastValidation =
            MultiblockValidationResult.invalid(List.of("Not formed"), List.of());
    private boolean pendingRevalidation = true;
    private int currentUse;
    private float recentUse;
    private double fanBoost;
    private int fanScanTimer;
    private int syncTimer;
    private CoolingTowerDisplay display = CoolingTowerDisplay.EMPTY;

    public CoolingTowerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.COOLING_TOWER_CONTROLLER.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            clientTick();
            return;
        }
        if (pendingRevalidation) {
            BoundingBox reach = interestBounds();
            if (level.hasChunksAt(reach.minX(), reach.minZ(), reach.maxX(), reach.maxZ())) {
                revalidate(false, null);
            }
        }
        if (structure.isPresent() && ++fanScanTimer >= FAN_SCAN_INTERVAL) {
            fanScanTimer = 0;
            fanBoost = scanFans(structure.get());
        }

        int used = structure.isPresent() ? condense(getCapacity()) : 0;
        currentUse = used;
        recentUse = (recentUse * 7 + used) / 8;
        if (recentUse < 0.5f) {
            recentUse = 0;
        }

        if (++syncTimer >= SYNC_INTERVAL) {
            syncTimer = 0;
            CoolingTowerDisplay next = buildDisplay();
            if (!next.equals(display)) {
                display = next;
                sendData();
            }
        }
    }

    private void clientTick() {
        if (!display.formed() || display.currentUse() <= 0) {
            return;
        }
        // Plume: a few slow clouds rising from inside the top ring, more under heavier load.
        CoolingTowerGeometry geometry = CoolingTowerGeometry.of(display.baseRadius(), display.height());
        int top = display.height() - 1;
        double radius = Math.max(1, geometry.radius(top) - 0.5);
        int count = 1 + Math.round(display.load() * 6);
        for (int i = 0; i < count; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double distance = Math.sqrt(level.random.nextDouble()) * radius;
            double x = display.center().getX() + 0.5 + Math.cos(angle) * distance;
            double z = display.center().getZ() + 0.5 + Math.sin(angle) * distance;
            double y = display.center().getY() + top + 0.5;
            level.addAlwaysVisibleParticle(i % 2 == 0 ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.CLOUD, true,
                    x, y, z, 0, 0.08 + level.random.nextDouble() * 0.17, 0);
        }
        CoolingTowerClientHooks.tick(this);
    }

    private int condense(int capacity) {
        FluidStack steam = steamTank.getFluid();
        if (steam.isEmpty() || capacity <= 0) {
            return 0;
        }
        FluidStack water = new FluidStack(NuclearFluidHelper.isHeavyVapour(steam)
                ? AllNuclearFluids.HEAVY_WATER.get() : net.minecraft.world.level.material.Fluids.WATER, 1);
        int space = waterTank.fill(water.copyWithAmount(waterTank.getCapacity()), IFluidHandler.FluidAction.SIMULATE);
        int amount = Math.min(capacity, Math.min(steam.getAmount(), space));
        if (amount <= 0) {
            return 0;
        }
        steamTank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        waterTank.fill(water.copyWithAmount(amount), IFluidHandler.FluidAction.EXECUTE);
        return amount;
    }

    private double scanFans(CoolingTowerStructure s) {
        double boost = 0;
        for (BlockPos pos : s.fans) {
            if (level.getBlockEntity(pos) instanceof CoolingTowerFanBlockEntity fan) {
                boost += Config.coolingTowerFanBoost * fan.boostContribution();
            }
        }
        return Math.min(Config.coolingTowerMaxFanBoost, boost);
    }

    /** mB/t of steam the tower condenses right now: passive by size, plus the fans' boost. */
    public int getCapacity() {
        return structure.map(s -> (int) Math.round(s.geometry.passiveCapacity(Config.coolingTowerPassivePerBlock) * (1 + fanBoost)))
                .orElse(0);
    }

    /** Rescans the fans now instead of on the next interval. */
    public void refreshFans() {
        structure.ifPresent(s -> fanBoost = scanFans(s));
    }

    // ------------------------------------------------------------------ structure

    @Override
    public void initialize() {
        super.initialize();
        if (level != null && !level.isClientSide) {
            MultiblockRegistry.register(this);
        }
    }

    @Override
    public void invalidate() {
        if (level != null && !level.isClientSide) {
            MultiblockRegistry.unregister(this);
        }
        super.invalidate();
    }

    public void attemptFormation(@Nullable Player player) {
        revalidate(true, player);
    }

    @Override
    public void requestStructureRevalidation(@Nullable Player player) {
        messenger.setOwner(player);
        pendingRevalidation = true;
    }

    @Override
    public BoundingBox interestBounds() {
        if (structure.isPresent()) {
            BoundingBox box = structure.get().bounds();
            return new BoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY() + 2, box.maxZ());
        }
        int reach = 2 * CoolingTowerGeometry.MAX_BASE_RADIUS + 1;
        BlockPos pos = getBlockPos();
        return new BoundingBox(pos.getX() - reach, pos.getY() - 1, pos.getZ() - reach,
                pos.getX() + reach, pos.getY() + CoolingTowerGeometry.ABSOLUTE_MAX_HEIGHT, pos.getZ() + reach);
    }

    @Override
    public boolean shouldReactToBlockChange(BlockChange change) {
        if (change.pos().equals(getBlockPos())) {
            return true;
        }
        if (structure.isPresent()) {
            if (snapshot != null && snapshot.isAffectedBy(change)) {
                return true;
            }
            // Something placed on top of an open tower changes its measured height.
            BoundingBox box = structure.get().bounds();
            return change.pos().getY() > box.maxY() && box.isInside(change.pos().atY(box.maxY()));
        }
        return change.removed() || change.isMultiblockPart();
    }

    private void revalidate(boolean playerInitiated, @Nullable Player player) {
        messenger.setOwner(player);
        pendingRevalidation = false;
        if (level == null) {
            return;
        }
        boolean wasFormed = structure.isPresent();
        MultiblockValidationResult<CoolingTowerStructure> result = CoolingTowerStructureValidator.validate(level, getBlockPos());
        lastValidation = result;
        if (result.formed() && result.structure().isPresent()) {
            CoolingTowerStructure next = result.structure().get();
            setStructure(Optional.of(next));
            if (playerInitiated || !wasFormed) {
                org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements.awardMachine(level, getBlockPos(), messenger.owner(level),
                        org.papiricoh.create_nuclearindustry.advancement.NuclearEvent.COOLING_TOWER_FORMED);
                messenger.send(level, "§bCooling tower formed §8[" + next.dimensionsText() + ", " + getCapacity() + " mB/t]",
                        playerInitiated);
            }
        } else {
            setStructure(Optional.empty());
            if (playerInitiated || wasFormed) {
                messenger.send(level, (wasFormed ? "§cCooling tower broken: " : "§cCooling tower incomplete: ")
                        + result.primaryMessage(), playerInitiated);
            }
        }
        display = buildDisplay();
        sendData();
        setChanged();
    }

    private void setStructure(Optional<CoolingTowerStructure> next) {
        Optional<CoolingTowerStructure> previous = structure;
        boolean boundsChanged = !previous.map(CoolingTowerStructure::bounds).equals(next.map(CoolingTowerStructure::bounds));
        previous.ifPresent(s -> forEachPort(s, port -> port.unlink(getBlockPos())));
        structure = next;
        snapshot = next.map(s -> StructureSnapshot.capture(level, s.allPositions())).orElse(null);
        fanBoost = next.map(this::scanFans).orElse(0.0);
        next.ifPresent(s -> {
            forEachPort(s, port -> port.link(getBlockPos()));
            int capacity = s.tankCapacity();
            steamTank.setCapacity(Math.max(capacity, steamTank.getFluidAmount()));
            waterTank.setCapacity(Math.max(capacity, waterTank.getFluidAmount()));
        });
        if (boundsChanged) {
            MultiblockRegistry.reindex(this);
        }
    }

    private void forEachPort(CoolingTowerStructure s, java.util.function.Consumer<CoolingTowerPortBlockEntity> action) {
        for (long packed : s.ports) {
            if (level.getBlockEntity(BlockPos.of(packed)) instanceof CoolingTowerPortBlockEntity port) {
                action.accept(port);
            }
        }
    }

    private CoolingTowerDisplay buildDisplay() {
        return structure.map(s -> new CoolingTowerDisplay(true, s.center, s.baseRadius(), s.height(), currentUse, getCapacity(),
                        s.fans.size(), ""))
                .orElseGet(() -> new CoolingTowerDisplay(false, BlockPos.ZERO, 0, 0, 0, 0, 0, lastValidation.primaryMessage()));
    }

    // ------------------------------------------------------------------ accessors

    public Optional<CoolingTowerStructure> getStructure() {
        return structure;
    }

    public CoolingTowerDisplay getDisplay() {
        return display;
    }

    public int getCurrentUse() {
        return currentUse;
    }

    public float getRecentUse() {
        return recentUse;
    }

    public FluidTank getSteamTank() {
        return steamTank;
    }

    public FluidTank getWaterTank() {
        return waterTank;
    }

    public String getValidationMessage() {
        return lastValidation.formed() ? "" : lastValidation.primaryMessage();
    }

    public int fillSteam(FluidStack resource, IFluidHandler.FluidAction action) {
        return NuclearFluidHelper.isCondensable(resource) ? steamTank.fill(resource, action) : 0;
    }

    public FluidStack drainWater(int maxDrain, IFluidHandler.FluidAction action) {
        return waterTank.drain(maxDrain, action);
    }

    // ------------------------------------------------------------------ goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    Cooling Tower").withStyle(ChatFormatting.GOLD));
        if (!display.formed()) {
            tooltip.add(Component.literal("     " + display.message()).withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.literal("     Base radius " + display.baseRadius() + ", " + display.height() + " high, "
                    + display.fans() + " fans").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("     Condensing " + display.currentUse() + " / " + display.capacity() + " mB/t")
                    .withStyle(display.currentUse() > 0 ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        }
        tooltip.add(Component.literal("     Steam: " + steamTank.getFluidAmount() + " / " + steamTank.getCapacity() + " mB")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("     Water: " + waterTank.getFluidAmount() + " / " + waterTank.getCapacity() + " mB")
                .withStyle(ChatFormatting.GRAY));
        return true;
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("TankCapacity", steamTank.getCapacity());
        tag.put("Steam", steamTank.writeToNBT(registries, new CompoundTag()));
        tag.put("Water", waterTank.writeToNBT(registries, new CompoundTag()));
        tag.putFloat("RecentUse", recentUse);
        tag.put("Tower", display.write());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        int capacity = Math.max(BASE_TANK, tag.getInt("TankCapacity"));
        steamTank.setCapacity(capacity);
        waterTank.setCapacity(capacity);
        steamTank.readFromNBT(registries, tag.getCompound("Steam"));
        waterTank.readFromNBT(registries, tag.getCompound("Water"));
        recentUse = tag.getFloat("RecentUse");
        CoolingTowerDisplay previous = display;
        display = CoolingTowerDisplay.read(tag.getCompound("Tower"));
        if (clientPacket && !display.equals(previous)) {
            invalidateRenderBoundingBox();
        }
        if (!clientPacket) {
            pendingRevalidation = true;
        }
    }

    @Override
    protected AABB createRenderBoundingBox() {
        if (!display.formed()) {
            return super.createRenderBoundingBox();
        }
        int r = display.baseRadius();
        BlockPos c = display.center();
        return new AABB(c.getX() - r, c.getY(), c.getZ() - r, c.getX() + r + 1, c.getY() + display.height() + 1, c.getZ() + r + 1);
    }
}
