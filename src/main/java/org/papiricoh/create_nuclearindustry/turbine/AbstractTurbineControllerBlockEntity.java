package org.papiricoh.create_nuclearindustry.turbine;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.multiblock.BlockChange;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockController;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockMessenger;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockText;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockValidationResult;
import org.papiricoh.create_nuclearindustry.multiblock.StructureSnapshot;
import org.papiricoh.create_nuclearindustry.turbine.block.TurbineRotorFormed;

import java.util.List;
import java.util.Optional;

/**
 * Controller of a box turbine: the output block at the centre of an end face. Expands the
 * working fluid at a rate limited by rotors and ports, condenses it back, and drives the
 * Create network at 64 RPM with capacity proportional to the flow.
 */
public abstract class AbstractTurbineControllerBlockEntity extends GeneratingKineticBlockEntity implements MultiblockController {
    public static final float ACTIVE_RPM = 64.0f;
    /** Exhausting to a cooling tower (a cold condenser) doubles the work each mB of steam does. */
    public static final float EXHAUST_POWER_MULTIPLIER = 2.0f;
    private static final int VERSION = 2;
    private static final int SYNC_INTERVAL = 10;

    private final FluidTank steamTank;
    private final FluidTank condensateTank;
    /** Lang key prefix of the turbine's texts. */
    public static final String KEY = "create_nuclearindustry.turbine.";
    private final MultiblockMessenger messenger = new MultiblockMessenger("Turbine");
    private Optional<TurbineStructure> structure = Optional.empty();
    private StructureSnapshot snapshot;
    private MultiblockValidationResult<TurbineStructure> lastValidation =
            MultiblockValidationResult.invalid(List.of("Not formed"), List.of());
    private boolean pendingRevalidation = true;
    private boolean needsKineticRefresh;
    private boolean suspended;
    @Nullable
    private Direction lastFacing;
    /** The largest turbine this controller could form facing {@link #unformedBoundsFacing}, kept so asking costs nothing. */
    @Nullable
    private BoundingBox unformedBounds;
    @Nullable
    private Direction unformedBoundsFacing;
    private int currentUse;
    private float recentUse;
    private float generatedSpeed;
    private float generatedCapacity;
    private int syncTimer;
    private int lastSyncedSteam = -1;

    // Client
    private TurbineDisplay display = TurbineDisplay.EMPTY;
    public final LerpedFloat steamLevel = LerpedFloat.linear();

    protected AbstractTurbineControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        TurbineTier tier = tier();
        steamTank = new FluidTank(tier.baseTank, tier::isWorkingFluid) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        condensateTank = new FluidTank(tier.baseTank, stack -> NuclearFluidHelper.isCoolant(stack) || NuclearFluidHelper.isExhaustSteam(stack)) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
    }

    public abstract TurbineTier tier();

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

        Direction facing = facing();
        if (facing != lastFacing) {
            lastFacing = facing;
            MultiblockRegistry.reindex(this);
            pendingRevalidation = true;
        }
        if (pendingRevalidation) {
            BoundingBox reach = interestBounds();
            if (level.hasChunksAt(reach.minX(), reach.minZ(), reach.maxX(), reach.maxZ())) {
                revalidate(false, null);
            }
        }
        if (needsKineticRefresh) {
            needsKineticRefresh = false;
            updateGeneratedRotation();
        }

        suspended = structure.isPresent() && !isLoaded(structure.get().bounds());
        int used = structure.isPresent() && !suspended ? expand(structure.get()) : 0;
        currentUse = used;
        recentUse = (recentUse * 7 + used) / 8;
        if (recentUse < 0.5f) {
            recentUse = 0;
        }

        float speed = recentUse > 0 ? signedRpm() : 0;
        float capacity = tier().capacityFor(recentUse)
                * (structure.map(TurbineStructure::exhausting).orElse(false) ? EXHAUST_POWER_MULTIPLIER : 1.0f);
        if (Math.abs(speed - generatedSpeed) > 0.25f || Math.abs(capacity - generatedCapacity) > 1.0f) {
            generatedSpeed = speed;
            generatedCapacity = capacity;
            updateGeneratedRotation();
            if (hasNetwork()) {
                notifyStressCapacityChange(generatedCapacity);
            }
        }

        if (++syncTimer >= SYNC_INTERVAL) {
            syncTimer = 0;
            if (Math.abs(steamTank.getFluidAmount() - lastSyncedSteam) >= Math.max(1, steamTank.getCapacity() / 100)
                    || display.currentUse() != currentUse) {
                lastSyncedSteam = steamTank.getFluidAmount();
                refreshDisplay();
                sendData();
            }
        }
    }

    protected void clientTick() {
        float target = steamTank.getCapacity() <= 0 ? 0 : steamTank.getFluidAmount() / (float) steamTank.getCapacity();
        steamLevel.chase(target, 0.25, LerpedFloat.Chaser.EXP);
        steamLevel.tickChaser();
        TurbineClientHooks.tick(this);
    }

    private boolean isLoaded(BoundingBox box) {
        return level.hasChunksAt(box.minX(), box.minZ(), box.maxX(), box.maxZ());
    }

    private int expand(TurbineStructure s) {
        FluidStack working = steamTank.getFluid();
        if (working.isEmpty()) {
            return 0;
        }
        // Through an exhaust port the steam leaves as exhaust for a cooling tower; otherwise it condenses inside.
        FluidStack condensate = new FluidStack(s.exhausting() ? tier().exhaust(working) : tier().condensate(working), Integer.MAX_VALUE);
        int space = condensateTank.fill(condensate.copyWithAmount(condensateTank.getCapacity()), IFluidHandler.FluidAction.SIMULATE);
        int amount = Math.min(s.throughputLimit(), Math.min(working.getAmount(), space));
        if (amount <= 0) {
            return 0;
        }
        steamTank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        condensateTank.fill(condensate.copyWithAmount(amount), IFluidHandler.FluidAction.EXECUTE);
        return amount;
    }

    private float signedRpm() {
        return facing().getAxisDirection() == Direction.AxisDirection.NEGATIVE ? -ACTIVE_RPM : ACTIVE_RPM;
    }

    @Override
    public float getGeneratedSpeed() {
        return generatedSpeed;
    }

    @Override
    public float calculateAddedStressCapacity() {
        lastCapacityProvided = generatedCapacity;
        return generatedCapacity;
    }

    protected Direction facing() {
        return getBlockState().getValue(BlockStateProperties.FACING);
    }

    // ------------------------------------------------------------------ structure

    @Override
    public void initialize() {
        super.initialize();
        if (level != null && !level.isClientSide) {
            MultiblockRegistry.register(this);
        }
    }

    /** SmartBlockEntity calls this on both removal and chunk unload. */
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
            return structure.get().bounds();
        }
        Direction facing = facing();
        if (unformedBounds == null || unformedBoundsFacing != facing) {
            unformedBoundsFacing = facing;
            unformedBounds = new TurbineStructure(tier(), getBlockPos(), facing,
                    TurbineStructureValidator.MAX_HALF, TurbineStructureValidator.MAX_HALF, TurbineStructureValidator.MAX_LENGTH).bounds();
        }
        return unformedBounds;
    }

    @Override
    public boolean shouldReactToBlockChange(BlockChange change) {
        if (change.pos().equals(getBlockPos())) {
            return true;
        }
        if (structure.isPresent()) {
            return snapshot != null && snapshot.isAffectedBy(change);
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
        MultiblockValidationResult<TurbineStructure> result = TurbineStructureValidator.validate(level, getBlockPos(), tier());
        lastValidation = result;
        if (result.formed() && result.structure().isPresent()) {
            TurbineStructure next = result.structure().get();
            setStructure(Optional.of(next));
            if (playerInitiated || !wasFormed) {
                org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements.awardMachine(level, getBlockPos(), messenger.owner(level),
                        org.papiricoh.create_nuclearindustry.advancement.NuclearEvent.TURBINE_FORMED);
                messenger.status(level, playerInitiated ? player : null, Component.translatable(KEY + "message.formed." + tierId(),
                                Component.translatable(KEY + "message.details", next.dimensionsText(), next.rotorCount(), next.throughputLimit())
                                        .withStyle(ChatFormatting.DARK_GRAY))
                        .withStyle(ChatFormatting.AQUA));
            }
        } else {
            setStructure(Optional.empty());
            if (playerInitiated || wasFormed) {
                messenger.status(level, playerInitiated ? player : null, Component.translatable(KEY + (wasFormed ? "message.broken" : "message.incomplete"),
                        describe(result.primaryMessage())).withStyle(ChatFormatting.RED));
            }
        }
        refreshDisplay();
        sendData();
        setChanged();
    }

    /** The validation message as kept and synced: an old line turbine's says how to rebuild it. */
    private static String storedMessage(String message) {
        return TurbineStructureValidator.LEGACY_LINE_ERROR.equals(message) ? KEY + "legacy_line" : message;
    }

    /** A validation message for players, in their language. */
    private static Component describe(String message) {
        return MultiblockText.text(storedMessage(message));
    }

    /** {@code steam} or {@code plasma}, for the tier's lang keys. */
    private String tierId() {
        return tier().name().toLowerCase(java.util.Locale.ROOT);
    }

    private static Component line(Component text, ChatFormatting style) {
        return Component.literal("     ").append(text).withStyle(style);
    }

    private void setStructure(Optional<TurbineStructure> next) {
        Optional<TurbineStructure> previous = structure;
        boolean boundsChanged = !previous.map(TurbineStructure::bounds).equals(next.map(TurbineStructure::bounds));
        previous.ifPresent(s -> forEachPort(s, port -> port.unlink(getBlockPos())));
        previous.ifPresent(s -> TurbineRotorFormed.apply(level, s, false));
        structure = next;
        snapshot = next.map(s -> StructureSnapshot.capture(level, s.allPositions())).orElse(null);
        next.ifPresent(s -> {
            TurbineRotorFormed.apply(level, s, true);
            forEachPort(s, port -> port.link(getBlockPos()));
            int capacity = s.tankCapacity();
            steamTank.setCapacity(Math.max(capacity, steamTank.getFluidAmount()));
            condensateTank.setCapacity(Math.max(capacity, condensateTank.getFluidAmount()));
        });
        if (boundsChanged) {
            MultiblockRegistry.reindex(this);
        }
    }

    /** Clears the formed look of the rotors when the controller block goes away. */
    public void onControllerRemoved() {
        structure.ifPresent(s -> TurbineRotorFormed.apply(level, s, false));
    }

    private void forEachPort(TurbineStructure s, java.util.function.Consumer<TurbinePortBlockEntity> action) {
        for (long packed : s.ports) {
            if (level.getBlockEntity(BlockPos.of(packed)) instanceof TurbinePortBlockEntity port) {
                action.accept(port);
            }
        }
    }

    private void refreshDisplay() {
        int capacity = steamTank.getCapacity();
        display = structure.map(s -> TurbineDisplay.of(s, currentUse, capacity))
                .orElseGet(() -> TurbineDisplay.unformed(storedMessage(lastValidation.primaryMessage()), capacity));
    }

    // ------------------------------------------------------------------ accessors

    public Optional<TurbineStructure> getStructure() {
        return structure;
    }

    public TurbineDisplay getDisplay() {
        return display;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public int getCurrentUse() {
        return currentUse;
    }

    public FluidTank getSteamTank() {
        return steamTank;
    }

    public FluidTank getCondensateTank() {
        return condensateTank;
    }

    public String getValidationMessage() {
        return display.formed() ? "" : display.message();
    }

    // ------------------------------------------------------------------ goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable(KEY + "title." + tierId())).withStyle(ChatFormatting.GOLD));
        if (!display.formed()) {
            tooltip.add(line(describe(display.message()), ChatFormatting.RED));
        } else {
            tooltip.add(line(Component.translatable(KEY + "goggles.size", display.dimensionsText(facing().getAxis() != Direction.Axis.Y),
                    display.rotors(), display.inputPorts(), display.outputPorts()), ChatFormatting.GRAY));
            tooltip.add(line(Component.translatable(KEY + "goggles.expanding", display.currentUse(), display.limit()),
                    display.currentUse() > 0 ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
            if (display.exhausting()) {
                tooltip.add(line(Component.translatable(KEY + "goggles.exhausting"), ChatFormatting.AQUA));
            }
            if (display.currentUse() == 0 && condensateTank.getSpace() == 0) {
                tooltip.add(line(Component.translatable(KEY + (display.exhausting() ? "goggles.exhaust_full" : "goggles.condensate_full")),
                        ChatFormatting.RED));
            }
        }
        tooltip.add(line(Component.translatable(KEY + "goggles.tank." + tierId(), steamTank.getFluidAmount(), steamTank.getCapacity()),
                ChatFormatting.GRAY));
        tooltip.add(line(Component.translatable(KEY + (NuclearFluidHelper.isExhaustSteam(condensateTank.getFluid()) ? "goggles.exhaust" : "goggles.condensate"),
                condensateTank.getFluidAmount(), condensateTank.getCapacity()), ChatFormatting.GRAY));
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        return true;
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("TurbineVersion", VERSION);
        tag.putInt("TankCapacity", steamTank.getCapacity());
        tag.put("Steam", steamTank.writeToNBT(registries, new CompoundTag()));
        tag.put("Condensate", condensateTank.writeToNBT(registries, new CompoundTag()));
        tag.putFloat("RecentUse", recentUse);
        tag.put("Turbine", display.write());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (!tag.contains("TurbineVersion")) {
            readLegacy(tag, registries);
            return;
        }
        int capacity = Math.max(tier().baseTank, tag.getInt("TankCapacity"));
        steamTank.setCapacity(capacity);
        condensateTank.setCapacity(capacity);
        steamTank.readFromNBT(registries, tag.getCompound("Steam"));
        condensateTank.readFromNBT(registries, tag.getCompound("Condensate"));
        recentUse = tag.getFloat("RecentUse");
        TurbineDisplay previous = display;
        display = TurbineDisplay.read(tag.getCompound("Turbine"));
        if (clientPacket && !display.equals(previous)) {
            invalidateRenderBoundingBox();
        }
        if (!clientPacket) {
            pendingRevalidation = true;
        }
    }

    /**
     * Turbines from before box turbines kept their working fluid under "steam" (or
     * "plasmaSteam") and their condensate under "condensate". Keep the fluids; the controller
     * revalidates and reports that the old line must be rebuilt.
     */
    private void readLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        String steamKey = tag.contains("plasmaSteam") ? "plasmaSteam" : "steam";
        steamTank.readFromNBT(registries, tag.getCompound(steamKey));
        condensateTank.readFromNBT(registries, tag.getCompound("condensate"));
        steamTank.setCapacity(Math.max(tier().baseTank, steamTank.getFluidAmount()));
        condensateTank.setCapacity(Math.max(tier().baseTank, condensateTank.getFluidAmount()));
        recentUse = 0;
        pendingRevalidation = true;
        needsKineticRefresh = true;
    }

    // ------------------------------------------------------------------ rendering bounds

    @Override
    protected AABB createRenderBoundingBox() {
        if (!display.formed()) {
            return super.createRenderBoundingBox();
        }
        TurbineStructure shape = new TurbineStructure(tier(), getBlockPos(), facing(), display.hu(), display.hv(), display.length());
        return AABB.of(shape.bounds()).inflate(1);
    }

    /** Block-entity-only access for the ports. */
    public int fillSteam(FluidStack resource, IFluidHandler.FluidAction action) {
        return tier().isWorkingFluid(resource) ? steamTank.fill(resource, action) : 0;
    }

    public FluidStack drainCondensate(int maxDrain, IFluidHandler.FluidAction action) {
        return condensateTank.drain(maxDrain, action);
    }

}
