package org.papiricoh.create_nuclearindustry.fusion.blockentity;

import com.mojang.logging.LogUtils;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionReactorControllerBlock;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakStructure;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakStructureValidator;
import org.papiricoh.create_nuclearindustry.fusion.physics.TokamakPhysicsSimulator;
import org.papiricoh.create_nuclearindustry.multiblock.BlockChange;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockController;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockMessenger;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockValidationResult;
import org.papiricoh.create_nuclearindustry.multiblock.StructureSnapshot;
import org.papiricoh.create_nuclearindustry.radiation.FalloutData;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Tokamak controller. Owns the plasma model, the fuel, coolant and plasma steam tanks, and
 * the structure found by {@link TokamakStructureValidator}. Ports and injectors are thin
 * proxies onto these tanks.
 */
public class FusionReactorBlockEntity extends BlockEntity implements IHaveGoggleInformation, MultiblockController {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int FUEL_TANK_CAPACITY = 8_000;
    public static final int TANK_CAPACITY = 16_000;
    /** Injectors in OUTPUT mode never drain the tritium tank below this. */
    public static final int TRITIUM_RESERVE = 1_000;
    private static final int PORT_THROUGHPUT = 160;
    private static final int LEGACY_PELLET_FLUID = 250;
    /** Released tritium contaminates the hall for a couple of in-game hours. */
    private static final int DISRUPTION_CONTAMINATION_TICKS = 6_000;
    private static final double DISRUPTION_RADIATION = 8.0;

    private Status status = Status.UNFORMED;
    private Optional<TokamakStructure> structure = Optional.empty();
    private StructureSnapshot snapshot;
    private MultiblockValidationResult<TokamakStructure> lastValidation =
            MultiblockValidationResult.invalid(List.of("Not formed"), List.of());
    private final TokamakPhysicsSimulator physics = new TokamakPhysicsSimulator();
    private final MultiblockMessenger messenger = new MultiblockMessenger("Tokamak");
    private boolean pendingRevalidation = true;
    private boolean fuelEnabled = true;
    private int syncCounter;
    // Tanks hold whole millibuckets; burn and breeding are fractional, so remainders carry over.
    private double deuteriumDebt;
    private double tritiumDebt;
    private double tritiumCredit;
    private double magnetDrive;
    private final List<ItemStack> legacyDrops = new ArrayList<>();

    // Structure summary synced to clients for goggles and the GUI.
    private int dispRadius;
    private int dispCoils;
    private int dispBlankets;
    private double dispCoverage;
    private String dispValidationMessage = "Not formed";

    private final FluidTank deuteriumTank = tank(FUEL_TANK_CAPACITY, NuclearFluidHelper::isDeuterium);
    private final FluidTank tritiumTank = tank(FUEL_TANK_CAPACITY, NuclearFluidHelper::isTritium);
    private final FluidTank coolantTank = tank(TANK_CAPACITY, NuclearFluidHelper::isCoolant);
    private final FluidTank plasmaSteamTank = tank(TANK_CAPACITY, NuclearFluidHelper::isPlasmaSteam);

    public enum Status { UNFORMED, FORMED, SUSPENDED }

    public FusionReactorBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.FUSION_CONTROLLER.get(), pos, state);
    }

    private FluidTank tank(int capacity, java.util.function.Predicate<FluidStack> validator) {
        return new FluidTank(capacity, validator) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
    }

    // ------------------------------------------------------------------ ticking

    public static void tick(Level level, BlockPos pos, BlockState state, FusionReactorBlockEntity entity) {
        if (level.isClientSide) {
            return;
        }
        entity.dropLegacyItems();
        if (entity.pendingRevalidation) {
            entity.revalidate(false, null);
        }
        if (entity.structure.isPresent()) {
            TokamakStructure tokamak = entity.structure.get();
            if (!isLoaded(level, tokamak.bounds())) {
                entity.status = Status.SUSPENDED;
            } else {
                entity.status = Status.FORMED;
                entity.runPlasma(tokamak);
            }
        } else if (entity.physics.getPlasmaTemperature() > TokamakPhysicsSimulator.AMBIENT_TEMP) {
            entity.physics.quench();
        }
        entity.updateBlockStateFlags();
        if (++entity.syncCounter >= 10) {
            entity.syncCounter = 0;
            entity.markDirtyAndSync();
        }
    }

    private static boolean isLoaded(Level level, BoundingBox box) {
        return level.hasChunksAt(box.minX(), box.minZ(), box.maxX(), box.maxZ());
    }

    private void runPlasma(TokamakStructure tokamak) {
        magnetDrive = readMagnetDrive(tokamak);
        int steamCapacity = Math.min(tokamak.coolantPortCount, tokamak.steamPortCount) * PORT_THROUGHPUT;
        TokamakPhysicsSimulator.Outputs out = physics.tick(new TokamakPhysicsSimulator.Inputs(
                tokamak.coilCoverage(Config.fusionCoilsPerSector),
                magnetDrive,
                true,
                tokamak.plasmaVolume(),
                tokamak.blanketFraction(),
                fuelEnabled && tokamak.fuelInjectorCount > 0,
                deuteriumTank.getFluidAmount() - deuteriumDebt,
                tritiumTank.getFluidAmount() - tritiumDebt,
                coolantTank.getFluidAmount(),
                Math.min(steamCapacity, plasmaSteamTank.getSpace())));

        deuteriumDebt += out.deuteriumUsed();
        tritiumDebt += out.tritiumUsed();
        tritiumCredit += out.tritiumBred();
        deuteriumDebt -= deuteriumTank.drain((int) Math.floor(deuteriumDebt), IFluidHandler.FluidAction.EXECUTE).getAmount();
        tritiumDebt -= tritiumTank.drain((int) Math.floor(tritiumDebt), IFluidHandler.FluidAction.EXECUTE).getAmount();
        int bred = (int) Math.floor(tritiumCredit);
        if (bred > 0) {
            // Tritium beyond the tank's capacity is vented.
            tritiumTank.fill(new FluidStack(AllNuclearFluids.TRITIUM.get(), bred), IFluidHandler.FluidAction.EXECUTE);
            tritiumCredit -= bred;
        }
        int steam = (int) Math.floor(out.steamProduced());
        if (steam > 0) {
            FluidStack coolant = coolantTank.drain(steam, IFluidHandler.FluidAction.EXECUTE);
            plasmaSteamTank.fill(new FluidStack(AllNuclearFluids.PLASMA_STEAM.get(), coolant.getAmount()), IFluidHandler.FluidAction.EXECUTE);
        }

        if (physics.isDisrupting()) {
            disrupt(tokamak);
        }
    }

    private double readMagnetDrive(TokamakStructure tokamak) {
        if (level == null) {
            return 0;
        }
        double drive = 0;
        for (BlockPos pos : tokamak.magnetInputs) {
            if (level.getBlockEntity(pos) instanceof FusionMagnetInputBlockEntity magnet) {
                drive += magnet.getConfinementContribution();
            }
        }
        int needed = Math.max(1, (int) Math.ceil(tokamak.majorRadius() / 4.0));
        return Math.min(1.0, drive / needed);
    }

    /** A disruption dumps the plasma energy into the vessel wall. */
    private void disrupt(TokamakStructure tokamak) {
        BlockPos pos = getBlockPos();
        LOGGER.warn("Tokamak disruption at {}", pos);
        physics.quench();
        // The tritium inventory escapes into the hall.
        if (!tritiumTank.isEmpty()) {
            FalloutData.contaminate(level, pos, DISRUPTION_RADIATION, DISRUPTION_CONTAMINATION_TICKS);
        }
        tritiumTank.setFluid(FluidStack.EMPTY);
        tritiumDebt = 0;
        tritiumCredit = 0;
        messenger.send(level, "§4Plasma disruption at " + pos.toShortString(), true);
        if (!Config.fusionDisruptionExplosions || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        TokamakGeometry geometry = tokamak.geometry;
        float power = (float) Math.min(12.0, 2.0 + 0.5 * tokamak.majorRadius());
        for (int i = 0; i < 3; i++) {
            int index = serverLevel.random.nextInt(geometry.shellCount());
            BlockPos at = tokamak.center.offset(geometry.shellX(index), geometry.shellY(index), geometry.shellZ(index));
            serverLevel.explode(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, power, Level.ExplosionInteraction.BLOCK);
        }
    }

    private void updateBlockStateFlags() {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.hasProperty(FusionReactorControllerBlock.ON)) {
            return;
        }
        boolean on = status == Status.FORMED && magnetDrive > 0;
        boolean ignited = physics.isBurning();
        if (state.getValue(FusionReactorControllerBlock.ON) != on || state.getValue(FusionReactorControllerBlock.IGNITED) != ignited) {
            level.setBlock(getBlockPos(), state.setValue(FusionReactorControllerBlock.ON, on)
                    .setValue(FusionReactorControllerBlock.IGNITED, ignited), Block.UPDATE_CLIENTS);
        }
    }

    // ------------------------------------------------------------------ structure

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            MultiblockRegistry.register(this);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            MultiblockRegistry.unregister(this);
        }
        super.setRemoved();
    }

    public void attemptFormation(Player player) {
        revalidate(true, player);
    }

    @Override
    public void requestStructureRevalidation(Player player) {
        messenger.setOwner(player);
        pendingRevalidation = true;
    }

    @Override
    public BoundingBox interestBounds() {
        if (structure.isPresent()) {
            return structure.get().bounds();
        }
        // The axis can be up to (maxRadius + 2) blocks away in any horizontal direction.
        int reach = 2 * (Math.max(TokamakGeometry.MIN_RADIUS, Config.fusionMaxMajorRadius) + 2);
        BlockPos pos = getBlockPos();
        return new BoundingBox(pos.getX() - reach, pos.getY() - TokamakGeometry.HALF_HEIGHT, pos.getZ() - reach,
                pos.getX() + reach, pos.getY() + TokamakGeometry.HALF_HEIGHT, pos.getZ() + reach);
    }

    @Override
    public boolean shouldReactToBlockChange(BlockChange change) {
        if (structure.isPresent()) {
            return snapshot != null && snapshot.isAffectedBy(change);
        }
        return change.removed() || change.isMultiblockPart();
    }

    private void revalidate(boolean playerInitiated, Player player) {
        messenger.setOwner(player);
        pendingRevalidation = false;
        if (level == null) {
            return;
        }
        boolean wasFormed = structure.isPresent();
        MultiblockValidationResult<TokamakStructure> result = TokamakStructureValidator.validate(level, getBlockPos());
        lastValidation = result;
        if (result.formed() && result.structure().isPresent()) {
            TokamakStructure next = result.structure().get();
            structure.ifPresent(this::unlinkParts);
            setStructure(Optional.of(next));
            linkParts(next);
            if (playerInitiated || !wasFormed) {
                String message = "§bTokamak formed §8[" + next.dimensionsText() + "]";
                if (!result.warnings().isEmpty()) {
                    message += " §e" + result.warnings().get(0);
                }
                messenger.send(level, message, playerInitiated);
            }
        } else {
            structure.ifPresent(this::unlinkParts);
            setStructure(Optional.empty());
            if (playerInitiated || wasFormed) {
                messenger.send(level, (wasFormed ? "§cTokamak broken: " : "§cTokamak incomplete: ") + describe(result), playerInitiated);
            }
        }
        markDirtyAndSync();
    }

    private static String describe(MultiblockValidationResult<TokamakStructure> result) {
        String message = result.primaryMessage();
        return TokamakStructureValidator.LEGACY_RING_ERROR.equals(message)
                ? "this is an old flat fusion ring. Rebuild it as a tokamak (see the Ponder scene)"
                : message;
    }

    private void setStructure(Optional<TokamakStructure> next) {
        boolean boundsChanged = !structure.map(TokamakStructure::bounds).equals(next.map(TokamakStructure::bounds));
        structure = next;
        snapshot = next.isPresent() && level != null && !level.isClientSide
                ? StructureSnapshot.capture(level, next.get().allPositions())
                : null;
        status = next.isPresent() ? Status.FORMED : Status.UNFORMED;
        if (boundsChanged) {
            MultiblockRegistry.reindex(this);
        }
    }

    private void linkParts(TokamakStructure tokamak) {
        forEachPart(tokamak, part -> part.link(getBlockPos()));
    }

    private void unlinkParts(TokamakStructure tokamak) {
        forEachPart(tokamak, part -> part.unlink(getBlockPos()));
    }

    private void forEachPart(TokamakStructure tokamak, java.util.function.Consumer<TokamakPart> action) {
        if (level == null) {
            return;
        }
        for (BlockPos pos : tokamak.fluidPorts) {
            if (level.getBlockEntity(pos) instanceof TokamakPart part) {
                action.accept(part);
            }
        }
        for (BlockPos pos : tokamak.injectors) {
            if (level.getBlockEntity(pos) instanceof TokamakPart part) {
                action.accept(part);
            }
        }
    }

    // ------------------------------------------------------------------ controls and accessors

    public boolean isFuelEnabled() {
        return fuelEnabled;
    }

    public void setFuelEnabled(boolean enabled) {
        if (fuelEnabled != enabled) {
            fuelEnabled = enabled;
            messenger.send(level, enabled ? "§bFuel injection on" : "§eFuel injection off: plasma will cool", true);
            markDirtyAndSync();
        }
    }

    public Status getStatus() {
        return status;
    }

    public Optional<TokamakStructure> getStructure() {
        return structure;
    }

    public TokamakPhysicsSimulator getPhysics() {
        return physics;
    }

    public double getMagnetDrive() {
        return magnetDrive;
    }

    public boolean isFormed() {
        return level != null && level.isClientSide ? dispRadius > 0 : structure.isPresent();
    }

    public int getMajorRadius() {
        return level != null && level.isClientSide ? dispRadius : structure.map(TokamakStructure::majorRadius).orElse(0);
    }

    public int getCoilCount() {
        return level != null && level.isClientSide ? dispCoils : structure.map(s -> s.coilCount).orElse(0);
    }

    public int getBlanketCount() {
        return level != null && level.isClientSide ? dispBlankets : structure.map(s -> s.blanketCount).orElse(0);
    }

    public double getCoilCoverage() {
        return level != null && level.isClientSide ? dispCoverage
                : structure.map(s -> s.coilCoverage(Config.fusionCoilsPerSector)).orElse(0.0);
    }

    public String getValidationMessage() {
        return level != null && level.isClientSide ? dispValidationMessage : describe(lastValidation);
    }

    public FluidTank getDeuteriumTank() {
        return deuteriumTank;
    }

    public FluidTank getTritiumTank() {
        return tritiumTank;
    }

    public FluidTank getCoolantTank() {
        return coolantTank;
    }

    public FluidTank getPlasmaSteamTank() {
        return plasmaSteamTank;
    }

    /** Routes deuterium and tritium into their own tanks. */
    public int fillFuel(FluidStack resource, IFluidHandler.FluidAction action) {
        if (NuclearFluidHelper.isDeuterium(resource)) {
            return deuteriumTank.fill(resource, action);
        }
        if (NuclearFluidHelper.isTritium(resource)) {
            return tritiumTank.fill(resource, action);
        }
        return 0;
    }

    public FluidStack drainSurplusTritium(int maxDrain, IFluidHandler.FluidAction action) {
        int surplus = Math.max(0, tritiumTank.getFluidAmount() - TRITIUM_RESERVE);
        return surplus <= 0 ? FluidStack.EMPTY : tritiumTank.drain(Math.min(maxDrain, surplus), action);
    }

    private void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_CLIENTS);
        }
    }

    // ------------------------------------------------------------------ goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("Tokamak").withStyle(ChatFormatting.AQUA));
        if (!isFormed()) {
            tooltip.add(Component.literal("  " + getValidationMessage()).withStyle(ChatFormatting.RED));
            return true;
        }
        tooltip.add(Component.literal("  Major radius: " + getMajorRadius()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(String.format("  Coils: %d (coverage %.0f%%)  Blankets: %d",
                getCoilCount(), getCoilCoverage() * 100, getBlanketCount())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(String.format("  Plasma: %.0f deg  %s", physics.getPlasmaTemperature(), physics.getMode()))
                .withStyle(physics.isBurning() ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        tooltip.add(Component.literal(String.format("  Confinement: %.0f%%  Instability: %.0f%%",
                physics.getConfinement() * 100, physics.getInstability()))
                .withStyle(physics.getInstability() > 50 ? ChatFormatting.RED : ChatFormatting.GRAY));
        tooltip.add(Component.literal(String.format("  Fusion power: %.0f MW  TBR: %.2f", physics.getFusionPower(), physics.getTritiumBreedingRatio()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  D: " + deuteriumTank.getFluidAmount() + " mB  T: " + tritiumTank.getFluidAmount() + " mB")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Coolant: " + coolantTank.getFluidAmount() + " mB  Plasma steam: " + plasmaSteamTank.getFluidAmount() + " mB")
                .withStyle(ChatFormatting.GRAY));
        return true;
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        writeCommon(tag, provider);
        tag.putDouble("deuteriumDebt", deuteriumDebt);
        tag.putDouble("tritiumDebt", tritiumDebt);
        tag.putDouble("tritiumCredit", tritiumCredit);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        readCommon(tag, provider);
        deuteriumDebt = tag.getDouble("deuteriumDebt");
        tritiumDebt = tag.getDouble("tritiumDebt");
        tritiumCredit = tag.getDouble("tritiumCredit");
        if (tag.contains("fuelInventory")) {
            migrateLegacyPellets(tag.getCompound("fuelInventory"), provider);
        }
        pendingRevalidation = true;
    }

    private void writeCommon(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putString("status", status.name());
        CompoundTag physicsTag = new CompoundTag();
        physics.serializeNBT(physicsTag);
        tag.put("physics", physicsTag);
        tag.put("deuterium", deuteriumTank.writeToNBT(provider, new CompoundTag()));
        tag.put("tritium", tritiumTank.writeToNBT(provider, new CompoundTag()));
        tag.put("coolant", coolantTank.writeToNBT(provider, new CompoundTag()));
        tag.put("plasmaSteam", plasmaSteamTank.writeToNBT(provider, new CompoundTag()));
        tag.putBoolean("fuelEnabled", fuelEnabled);
    }

    private void readCommon(CompoundTag tag, HolderLookup.Provider provider) {
        try {
            status = Status.valueOf(tag.getString("status"));
        } catch (IllegalArgumentException ignored) {
            status = Status.UNFORMED;
        }
        physics.deserializeNBT(tag.getCompound("physics"));
        deuteriumTank.readFromNBT(provider, tag.getCompound("deuterium"));
        tritiumTank.readFromNBT(provider, tag.getCompound("tritium"));
        coolantTank.readFromNBT(provider, tag.getCompound("coolant"));
        plasmaSteamTank.readFromNBT(provider, tag.getCompound("plasmaSteam"));
        fuelEnabled = !tag.contains("fuelEnabled") || tag.getBoolean("fuelEnabled");
    }

    /**
     * Controllers from the flat fusion ring kept D-T pellets. Each unused pellet becomes
     * 250 mB of deuterium and of tritium; spent pellets are dropped next to the controller.
     */
    private void migrateLegacyPellets(CompoundTag inventoryTag, HolderLookup.Provider provider) {
        ItemStackHandler legacy = new ItemStackHandler(2);
        legacy.deserializeNBT(provider, inventoryTag);
        ItemStack pellets = legacy.getStackInSlot(0);
        if (pellets.is(AllNuclearItems.DT_FUEL_PELLET.get())) {
            int amount = pellets.getCount() * LEGACY_PELLET_FLUID;
            deuteriumTank.fill(new FluidStack(AllNuclearFluids.DEUTERIUM.get(), amount), IFluidHandler.FluidAction.EXECUTE);
            tritiumTank.fill(new FluidStack(AllNuclearFluids.TRITIUM.get(), amount), IFluidHandler.FluidAction.EXECUTE);
        }
        ItemStack spent = legacy.getStackInSlot(1);
        if (!spent.isEmpty()) {
            legacyDrops.add(spent.copy());
        }
    }

    private void dropLegacyItems() {
        if (legacyDrops.isEmpty() || level == null) {
            return;
        }
        BlockPos pos = getBlockPos();
        for (ItemStack stack : legacyDrops) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stack);
        }
        legacyDrops.clear();
        setChanged();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        writeCommon(tag, provider);
        tag.putInt("dispRadius", structure.map(TokamakStructure::majorRadius).orElse(0));
        tag.putInt("dispCoils", structure.map(s -> s.coilCount).orElse(0));
        tag.putInt("dispBlankets", structure.map(s -> s.blanketCount).orElse(0));
        tag.putDouble("dispCoverage", structure.map(s -> s.coilCoverage(Config.fusionCoilsPerSector)).orElse(0.0));
        tag.putDouble("dispMagnetDrive", magnetDrive);
        tag.putString("dispValidationMessage", describe(lastValidation));
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider provider) {
        handleUpdateTag(pkt.getTag(), provider);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        readCommon(tag, provider);
        dispRadius = tag.getInt("dispRadius");
        dispCoils = tag.getInt("dispCoils");
        dispBlankets = tag.getInt("dispBlankets");
        dispCoverage = tag.getDouble("dispCoverage");
        magnetDrive = tag.getDouble("dispMagnetDrive");
        dispValidationMessage = tag.getString("dispValidationMessage");
    }

    /** A tokamak block that proxies onto the controller. */
    public interface TokamakPart {
        void link(BlockPos controllerPos);

        void unlink(BlockPos controllerPos);
    }
}
