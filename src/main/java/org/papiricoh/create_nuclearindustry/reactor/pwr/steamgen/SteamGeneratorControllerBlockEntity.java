package org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.PlantReadable;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.multiblock.BlockChange;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockController;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockMessenger;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockValidationResult;
import org.papiricoh.create_nuclearindustry.multiblock.StructureSnapshot;
import org.papiricoh.create_nuclearindustry.multiblock.SyncThrottle;
import org.papiricoh.create_nuclearindustry.PerformanceConfig;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrConfig;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.physics.SteamGeneratorModel;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Controller of a steam generator: hot primary coolant from a pressurized reactor flows through its
 * tube bundles and boils feedwater into steam for the turbines, then leaves cold for the reactor's
 * cold leg. Its ports reach four tanks; {@link SteamGeneratorModel} decides each tick how much heat
 * crosses the tubes.
 *
 * <p>Clients are updated when something they show moved by a visible step (1% of a tank, a unit of
 * a reading), checked every {@link #SYNC_INTERVAL} ticks, and at least every
 * {@link PerformanceConfig#forcedSyncTicks} ticks; a steam generator at rest sends next to nothing.
 */
public class SteamGeneratorControllerBlockEntity extends SmartBlockEntity
        implements MultiblockController, IHaveGoggleInformation, PlantReadable {
    private static final int PRIMARY_TANK = 8_000;
    private static final int SECONDARY_TANK = 16_000;
    private static final int SYNC_INTERVAL = 10;
    /** Heat that boils one mB of feedwater: what a reactor boiling its water directly spends on it. */
    private static final double STEAM_HEAT_PER_MB = 0.225;
    /** Feedwater that keeps one tube bundle block under water, mB. */
    private static final double WET_INVENTORY_PER_TUBE = 500.0;
    /** Rough thermal megawatts per heat unit per tick, for monitors and goggles. */
    public static final double MW_PER_HEAT = 5.0;
    private static final double AVERAGE = 0.1;
    /**
     * Visible steps of what clients show, in the order of {@link #syncValues}: 1% of each tank, a
     * mB/t of steam, a tenth of a heat unit (half a MW), half a degree, half a bar and half a percent.
     */
    private static final double[] SYNC_STEPS = {PRIMARY_TANK / 100.0, PRIMARY_TANK / 100.0, SECONDARY_TANK / 100.0,
            SECONDARY_TANK / 100.0, 0, 0, 0, 0, 0.5, 0.1, 0.5, 0.5, 0.005};

    private final FluidTank hotTank = tank(PRIMARY_TANK, NuclearFluidHelper::isHotPrimary);
    private final FluidTank coldTank = tank(PRIMARY_TANK, NuclearFluidHelper::isPrimaryCoolant);
    private final FluidTank feedTank = tank(SECONDARY_TANK, NuclearFluidHelper::isCoolant);
    private final FluidTank steamTank = tank(SECONDARY_TANK, NuclearFluidHelper::isTurbineSteam);
    private final SteamGeneratorModel model = new SteamGeneratorModel();
    /** Lang key prefix of the steam generator's texts. */
    private static final String KEY = "create_nuclearindustry.steam_generator.";
    private final MultiblockMessenger messenger = new MultiblockMessenger("Steam Generator");
    private Optional<SteamGeneratorStructure> structure = Optional.empty();
    private StructureSnapshot snapshot;
    private MultiblockValidationResult<SteamGeneratorStructure> lastValidation =
            MultiblockValidationResult.invalid(List.of("create_nuclearindustry.steam_generator.error.not_formed"), List.of());
    private boolean pendingRevalidation = true;
    private int syncTimer;
    private final SyncThrottle throttle = new SyncThrottle(SYNC_STEPS, new double[SYNC_STEPS.length]);
    private int clientSyncs;
    private int receivedThisTick;
    // Smoothed for display: mB/t of steam made, primary cooled and primary received, heat units/t
    private double steamRate;
    private double cooledRate;
    private double receivedRate;
    private double heatRate;
    private double pressure;
    private double wetFraction;
    private long totalSteam;
    private long totalCooled;
    // Client copy of the structure summary
    private boolean formed;
    private int tubes;
    private int height;
    private String message = "";

    public SteamGeneratorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(PwrRegistry.STEAM_GENERATOR_CONTROLLER_ENTITY.get(), pos, state);
    }

    private FluidTank tank(int capacity, java.util.function.Predicate<FluidStack> valid) {
        return new FluidTank(capacity, valid) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (pendingRevalidation) {
            BoundingBox reach = interestBounds();
            if (level.hasChunksAt(reach.minX(), reach.minZ(), reach.maxX(), reach.maxZ())) {
                revalidate(false, null);
            }
        }
        SteamGeneratorModel.Result result = structure.map(this::exchange).orElse(SteamGeneratorModel.Result.IDLE);
        steamRate += (result.steamMade() - steamRate) * AVERAGE;
        cooledRate += (result.primaryCooled() - cooledRate) * AVERAGE;
        receivedRate += (receivedThisTick - receivedRate) * AVERAGE;
        heatRate += (result.heat() - heatRate) * AVERAGE;
        receivedThisTick = 0;
        pressure = result.pressure();
        wetFraction = result.wetFraction();
        if (++syncTimer >= SYNC_INTERVAL) {
            syncTimer = 0;
            if (throttle.due(level.getGameTime(), syncValues(), PerformanceConfig.forcedSyncTicks)) {
                sendData();
            }
        }
    }

    /** What clients show, as numbers for the {@link SyncThrottle}: tanks and their fluids, rates, pressure. */
    private double[] syncValues() {
        return new double[]{hotTank.getFluidAmount(), coldTank.getFluidAmount(), feedTank.getFluidAmount(),
                steamTank.getFluidAmount(), fluidId(hotTank), fluidId(coldTank), fluidId(feedTank), fluidId(steamTank),
                steamRate, heatRate, getPrimaryDrop(), pressure, wetFraction};
    }

    private static double fluidId(FluidTank tank) {
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid());
    }

    /** Every update sent goes through here: counted, and remembered by the throttle. */
    @Override
    public void sendData() {
        clientSyncs++;
        if (level != null && !level.isClientSide) {
            throttle.sent(level.getGameTime(), syncValues());
        }
        super.sendData();
    }

    /** Client updates this controller asked for, for tests. */
    public int clientSyncCount() {
        return clientSyncs;
    }

    private SteamGeneratorModel.Result exchange(SteamGeneratorStructure s) {
        FluidStack feed = feedTank.getFluid();
        FluidStack steamKind = new FluidStack(feed.isEmpty() || !NuclearFluidHelper.isHeavyWater(feed)
                ? AllNuclearFluids.STEAM.get() : AllNuclearFluids.HEAVY_STEAM.get(), 1);
        int steamSpace = steamTank.fill(steamKind.copyWithAmount(steamTank.getCapacity()), IFluidHandler.FluidAction.SIMULATE);
        int coldSpace = coldTank.fill(new FluidStack(AllNuclearFluids.PRIMARY_COOLANT.get(), coldTank.getCapacity()),
                IFluidHandler.FluidAction.SIMULATE);
        double steamFill = steamTank.getFluidAmount() / (double) steamTank.getCapacity();
        SteamGeneratorModel.Result result = model.tick(hotTank.getFluidAmount(), coldSpace, feedTank.getFluidAmount(), steamSpace,
                steamFill, s.tubes, params());
        if (result.primaryCooled() > 0) {
            hotTank.drain(result.primaryCooled(), IFluidHandler.FluidAction.EXECUTE);
            coldTank.fill(new FluidStack(AllNuclearFluids.PRIMARY_COOLANT.get(), result.primaryCooled()), IFluidHandler.FluidAction.EXECUTE);
            totalCooled += result.primaryCooled();
        }
        if (result.steamMade() > 0) {
            feedTank.drain(result.steamMade(), IFluidHandler.FluidAction.EXECUTE);
            steamTank.fill(steamKind.copyWithAmount(result.steamMade()), IFluidHandler.FluidAction.EXECUTE);
            totalSteam += result.steamMade();
        }
        return result;
    }

    private static SteamGeneratorModel.Params params() {
        return new SteamGeneratorModel.Params(PwrConfig.steamGeneratorUaPerTube, PwrConfig.hotLegTemperature(),
                PwrConfig.enthalpyPerMb, STEAM_HEAT_PER_MB, PwrConfig.steamGeneratorMinPressure,
                PwrConfig.steamGeneratorMaxPressure, WET_INVENTORY_PER_TUBE);
    }

    // ------------------------------------------------------------------ ports

    FluidTank tankFor(SteamGeneratorPortRole role) {
        return switch (role) {
            case PRIMARY_IN -> hotTank;
            case PRIMARY_OUT -> coldTank;
            case FEED_IN -> feedTank;
            case STEAM_OUT -> steamTank;
        };
    }

    boolean accepts(SteamGeneratorPortRole role, FluidStack stack) {
        return switch (role) {
            case PRIMARY_IN -> NuclearFluidHelper.isPrimaryLoopFluid(stack);
            case FEED_IN -> NuclearFluidHelper.isCoolant(stack);
            default -> false;
        };
    }

    /** Hot primary goes to the tubes; cold primary pumped in passes straight through to the outlet. */
    int fill(SteamGeneratorPortRole role, FluidStack resource, IFluidHandler.FluidAction action) {
        if (role == SteamGeneratorPortRole.PRIMARY_IN) {
            if (NuclearFluidHelper.isHotPrimary(resource)) {
                int filled = hotTank.fill(resource, action);
                if (action.execute()) {
                    receivedThisTick += filled;
                }
                return filled;
            }
            return NuclearFluidHelper.isPrimaryCoolant(resource) ? coldTank.fill(resource, action) : 0;
        }
        if (role == SteamGeneratorPortRole.FEED_IN && NuclearFluidHelper.isCoolant(resource)) {
            return feedTank.fill(resource, action);
        }
        return 0;
    }

    FluidStack drain(SteamGeneratorPortRole role, int maxDrain, IFluidHandler.FluidAction action) {
        return switch (role) {
            case PRIMARY_OUT -> coldTank.drain(maxDrain, action);
            case STEAM_OUT -> steamTank.drain(maxDrain, action);
            default -> FluidStack.EMPTY;
        };
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
            return structure.get().bounds();
        }
        BlockPos pos = getBlockPos();
        int reach = SteamGeneratorStructure.MAX_HEIGHT;
        return new BoundingBox(pos.getX() - 2, pos.getY() - reach, pos.getZ() - 2, pos.getX() + 2, pos.getY() + reach, pos.getZ() + 2);
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
        MultiblockValidationResult<SteamGeneratorStructure> result = SteamGeneratorStructureValidator.validate(level, getBlockPos());
        lastValidation = result;
        if (result.formed() && result.structure().isPresent()) {
            SteamGeneratorStructure next = result.structure().get();
            setStructure(Optional.of(next));
            if (playerInitiated || !wasFormed) {
                org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements.awardMachine(level, getBlockPos(), messenger.owner(level),
                        org.papiricoh.create_nuclearindustry.advancement.NuclearEvent.STEAM_GENERATOR_FORMED);
                messenger.status(level, playerInitiated ? player : null, Component.translatable(KEY + "message.formed",
                                Component.literal("[" + next.dimensionsText() + ", ")
                                        .append(Component.translatable(KEY + "message.tubes", next.tubes)).append("]")
                                        .withStyle(ChatFormatting.DARK_GRAY))
                        .withStyle(ChatFormatting.AQUA));
            }
        } else {
            setStructure(Optional.empty());
            if (playerInitiated || wasFormed) {
                messenger.status(level, playerInitiated ? player : null, Component.translatable(KEY + (wasFormed ? "message.broken" : "message.incomplete"),
                        SteamGeneratorStructureValidator.describe(result.primaryMessage())).withStyle(ChatFormatting.RED));
            }
        }
        sendData();
        setChanged();
    }

    private void setStructure(Optional<SteamGeneratorStructure> next) {
        boolean boundsChanged = !structure.map(SteamGeneratorStructure::bounds).equals(next.map(SteamGeneratorStructure::bounds));
        structure.ifPresent(s -> forEachPort(s, port -> port.unlink(getBlockPos())));
        structure = next;
        snapshot = next.map(s -> StructureSnapshot.capture(level, s.allPositions())).orElse(null);
        next.ifPresent(s -> forEachPort(s, port -> port.link(getBlockPos())));
        if (boundsChanged) {
            MultiblockRegistry.reindex(this);
        }
    }

    private void forEachPort(SteamGeneratorStructure s, Consumer<SteamGeneratorPortBlockEntity> action) {
        for (BlockPos pos : s.portPositions()) {
            if (level.getBlockEntity(pos) instanceof SteamGeneratorPortBlockEntity port) {
                action.accept(port);
            }
        }
    }

    // ------------------------------------------------------------------ accessors

    public Optional<SteamGeneratorStructure> getStructure() {
        return structure;
    }

    public boolean isFormed() {
        return level != null && level.isClientSide ? formed : structure.isPresent();
    }

    public FluidTank getHotTank() {
        return hotTank;
    }

    public FluidTank getColdTank() {
        return coldTank;
    }

    public FluidTank getFeedTank() {
        return feedTank;
    }

    public FluidTank getSteamTank() {
        return steamTank;
    }

    /** Steam made, mB/t, smoothed. */
    public double getSteamRate() {
        return steamRate;
    }

    /** Heat moved from the primary to the secondary, heat units per tick, smoothed. */
    public double getHeatRate() {
        return heatRate;
    }

    public long getTotalSteam() {
        return totalSteam;
    }

    public long getTotalCooled() {
        return totalCooled;
    }

    /** Secondary pressure, bar. */
    public double getPressure() {
        return pressure;
    }

    /** Share of the tubes under feedwater, 0-1. */
    public double getWetFraction() {
        return wetFraction;
    }

    /** Feedwater level, 0-1. */
    public float getWaterLevel() {
        return feedTank.getFluidAmount() / (float) feedTank.getCapacity();
    }

    /** How much the primary cools crossing the tubes, °C: full when every mB that arrives leaves cold. */
    public float getPrimaryDrop() {
        if (cooledRate < 0.05) {
            return 0.0f;
        }
        return (float) (PwrConfig.coreTemperatureRise * Math.min(1.0, cooledRate / Math.max(cooledRate, receivedRate)));
    }

    public boolean isDry() {
        return isFormed() && wetFraction <= 0.0 && hotTank.getFluidAmount() > 0;
    }

    // ------------------------------------------------------------------ control room

    @Override
    public TargetKind plantKind() {
        return TargetKind.STEAM_GENERATOR;
    }

    @Override
    public boolean plantFormed() {
        return structure.isPresent();
    }

    @Override
    public MonitorReading plantSample() {
        float[] v = new float[5];
        v[MonitorReading.S_LEVEL] = getWaterLevel();
        v[MonitorReading.S_STEAM] = (float) steamRate;
        v[MonitorReading.S_PRIMARY_DT] = getPrimaryDrop();
        v[MonitorReading.S_FORMED] = structure.isPresent() ? 1 : 0;
        v[MonitorReading.S_HEAT] = (float) (heatRate * MW_PER_HEAT);
        String status = structure.isEmpty() ? "create_nuclearindustry.control_room.status.unformed"
                : isDry() ? "create_nuclearindustry.steam_generator.status.dry"
                : steamRate >= 0.5 ? "create_nuclearindustry.control_room.status.running"
                : "create_nuclearindustry.control_room.status.idle";
        float steamSeries = (float) Math.min(1.0, steamRate / MonitorReading.SG_STEAM_MAX);
        return new MonitorReading(TargetKind.STEAM_GENERATOR, status, v, v[MonitorReading.S_LEVEL], steamSeries);
    }

    // ------------------------------------------------------------------ goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        String k = "create_nuclearindustry.steam_generator.goggles.";
        tooltip.add(Component.literal("    ").append(Component.translatable("block.create_nuclearindustry.steam_generator_controller"))
                .withStyle(ChatFormatting.GOLD));
        if (!formed) {
            tooltip.add(Component.literal("     ").append(SteamGeneratorStructureValidator.describe(message)).withStyle(ChatFormatting.RED));
            return true;
        }
        tooltip.add(line(Component.translatable(k + "size", height, tubes), ChatFormatting.GRAY));
        tooltip.add(line(Component.translatable(k + "steam", Math.round(steamRate), String.format(java.util.Locale.ROOT, "%.0f", pressure)),
                steamRate >= 0.5 ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        tooltip.add(line(Component.translatable(k + "heat", Math.round(heatRate * MW_PER_HEAT), Math.round(getPrimaryDrop())), ChatFormatting.GRAY));
        tooltip.add(line(Component.translatable(k + "primary", hotTank.getFluidAmount(), coldTank.getFluidAmount()), ChatFormatting.GRAY));
        tooltip.add(line(Component.translatable(k + "feed", feedTank.getFluidAmount(), feedTank.getCapacity(),
                steamTank.getFluidAmount()), ChatFormatting.GRAY));
        if (wetFraction < 1.0 && hotTank.getFluidAmount() > 0) {
            tooltip.add(line(Component.translatable(k + "drying", Math.round(wetFraction * 100)), ChatFormatting.RED));
        }
        return true;
    }

    private static Component line(Component text, ChatFormatting style) {
        return Component.literal("     ").append(text).withStyle(style);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Hot", hotTank.writeToNBT(registries, new CompoundTag()));
        tag.put("Cold", coldTank.writeToNBT(registries, new CompoundTag()));
        tag.put("Feed", feedTank.writeToNBT(registries, new CompoundTag()));
        tag.put("Steam", steamTank.writeToNBT(registries, new CompoundTag()));
        tag.putDouble("SteamRate", steamRate);
        tag.putDouble("HeatRate", heatRate);
        tag.putDouble("CooledRate", cooledRate);
        tag.putDouble("ReceivedRate", receivedRate);
        tag.putDouble("Pressure", pressure);
        tag.putDouble("Wet", wetFraction);
        tag.putDouble("PrimaryCarry", model.primaryCarry());
        tag.putDouble("SteamCarry", model.steamCarry());
        if (clientPacket) {
            tag.putBoolean("Formed", structure.isPresent());
            tag.putInt("Tubes", structure.map(s -> s.tubes).orElse(0));
            tag.putInt("Height", structure.map(s -> s.height).orElse(0));
            tag.putString("Message", lastValidation.formed() ? "" : lastValidation.primaryMessage());
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        hotTank.readFromNBT(registries, tag.getCompound("Hot"));
        coldTank.readFromNBT(registries, tag.getCompound("Cold"));
        feedTank.readFromNBT(registries, tag.getCompound("Feed"));
        steamTank.readFromNBT(registries, tag.getCompound("Steam"));
        steamRate = tag.getDouble("SteamRate");
        heatRate = tag.getDouble("HeatRate");
        cooledRate = tag.getDouble("CooledRate");
        receivedRate = tag.getDouble("ReceivedRate");
        pressure = tag.getDouble("Pressure");
        wetFraction = tag.getDouble("Wet");
        model.restore(tag.getDouble("PrimaryCarry"), tag.getDouble("SteamCarry"));
        if (clientPacket) {
            formed = tag.getBoolean("Formed");
            tubes = tag.getInt("Tubes");
            height = tag.getInt("Height");
            message = tag.getString("Message");
        } else {
            pendingRevalidation = true;
        }
    }
}
