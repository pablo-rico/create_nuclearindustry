package org.papiricoh.create_nuclearindustry.fusion.cryo.plant;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoConfig;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoRegistry;
import org.papiricoh.create_nuclearindustry.fusion.cryo.physics.CryoplantModel;
import org.papiricoh.create_nuclearindustry.multiblock.BlockChange;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockController;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockMessenger;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockValidationResult;
import org.papiricoh.create_nuclearindustry.multiblock.StructureSnapshot;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Controller of a cryogenic plant. Its compressors drive {@link CryoplantModel}: warm helium from
 * the tokamak comes back as liquid helium, or, with a Liquid Air port, air from outside comes out
 * as liquid air. Four internal tanks sit behind the ports; fractional amounts carry over between
 * ticks so slow plants still deliver.
 */
public class CryoplantControllerBlockEntity extends SmartBlockEntity implements MultiblockController, IHaveGoggleInformation {
    public static final int TANK_CAPACITY = 8_000;
    public static final int NITROGEN_CAPACITY = 4_000;
    private static final int SYNC_INTERVAL = 10;
    private static final double AVERAGE = 0.1;
    /** Smoothed output above which the plant counts as running (lamp, frost, hum). */
    private static final double RUNNING_RATE = 0.05;

    private final FluidTank warmHeliumTank = tank(TANK_CAPACITY, NuclearFluidHelper::isHelium);
    private final FluidTank liquidHeliumTank = tank(TANK_CAPACITY, NuclearFluidHelper::isLiquidHelium);
    private final FluidTank nitrogenTank = tank(NITROGEN_CAPACITY, NuclearFluidHelper::isLiquidNitrogen);
    private final FluidTank liquidAirTank = tank(TANK_CAPACITY, NuclearFluidHelper::isLiquidAir);
    private final MultiblockMessenger messenger = new MultiblockMessenger("Cryogenic Plant");
    private Optional<CryoplantStructure> structure = Optional.empty();
    private StructureSnapshot snapshot;
    private MultiblockValidationResult<CryoplantStructure> lastValidation =
            MultiblockValidationResult.invalid(List.of(CryoplantStructureValidator.KEY + "not_formed"), List.of());
    private boolean pendingRevalidation = true;
    private int syncTimer;
    // Fractional mB still owed to or by the tanks.
    private double heliumDebt;
    private double nitrogenDebt;
    private double liquidCredit;
    private double airCredit;
    // Smoothed for display: liquid made (mB/t), capacity (mB/t), compressor speed share and nitrogen bonus in effect.
    private double rate;
    private double capacity;
    private double speed;
    private double precooling;
    private double totalHelium;
    private double totalAir;
    private double totalLost;
    // Client copy of the structure summary
    private boolean formed;
    private int height;
    private int compressors;
    private boolean airMode;
    private BlockPos bottom = BlockPos.ZERO;
    private String message = "";

    public CryoplantControllerBlockEntity(BlockPos pos, BlockState state) {
        super(CryoRegistry.CRYOPLANT_CONTROLLER_ENTITY.get(), pos, state);
    }

    private FluidTank tank(int capacity, Predicate<FluidStack> valid) {
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
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            CryoplantClientHooks.clientTick.accept(this);
            return;
        }
        if (pendingRevalidation) {
            BoundingBox reach = interestBounds();
            if (level.hasChunksAt(reach.minX(), reach.minZ(), reach.maxX(), reach.maxZ())) {
                revalidate(false, null);
            }
        }
        CryoplantModel.Result result = structure.map(this::liquefy).orElse(CryoplantModel.Result.IDLE);
        rate += (result.made() - rate) * AVERAGE;
        capacity += (result.capacity() - capacity) * AVERAGE;
        precooling = result.precooling();
        updateRunningState();
        if (++syncTimer >= SYNC_INTERVAL) {
            syncTimer = 0;
            sendData();
        }
    }

    private CryoplantModel.Result liquefy(CryoplantStructure s) {
        speed = compressorSpeed(s);
        CryoplantModel.Result result = CryoplantModel.tick(new CryoplantModel.Inputs(s.mode(), s.height, speed,
                Math.max(0, warmHeliumTank.getFluidAmount() - heliumDebt),
                Math.max(0, liquidHeliumTank.getSpace() - liquidCredit),
                Math.max(0, nitrogenTank.getFluidAmount() - nitrogenDebt),
                Math.max(0, liquidAirTank.getSpace() - airCredit)), CryoConfig.plantParams());
        heliumDebt += result.heliumTaken();
        nitrogenDebt += result.nitrogenUsed();
        liquidCredit += result.liquidHeliumMade();
        airCredit += result.liquidAirMade();
        heliumDebt -= warmHeliumTank.drain((int) Math.floor(heliumDebt), IFluidHandler.FluidAction.EXECUTE).getAmount();
        nitrogenDebt -= nitrogenTank.drain((int) Math.floor(nitrogenDebt), IFluidHandler.FluidAction.EXECUTE).getAmount();
        int liquid = (int) Math.floor(liquidCredit);
        if (liquid > 0) {
            liquidCredit -= liquidHeliumTank.fill(new FluidStack(AllNuclearFluids.LIQUID_HELIUM.get(), liquid), IFluidHandler.FluidAction.EXECUTE);
        }
        int air = (int) Math.floor(airCredit);
        if (air > 0) {
            airCredit -= liquidAirTank.fill(new FluidStack(AllNuclearFluids.LIQUID_AIR.get(), air), IFluidHandler.FluidAction.EXECUTE);
        }
        totalHelium += result.liquidHeliumMade();
        totalAir += result.liquidAirMade();
        totalLost += result.heliumLost();
        return result;
    }

    /** The compressors' average share of rated speed: an idle compressor drags the plant down. */
    private double compressorSpeed(CryoplantStructure s) {
        double sum = 0;
        int count = 0;
        for (BlockPos pos : s.compressorPositions()) {
            count++;
            if (level.getBlockEntity(pos) instanceof CryoplantCompressorBlockEntity compressor) {
                sum += compressor.speedFactor();
            }
        }
        return count == 0 ? 0 : sum / count;
    }

    private void updateRunningState() {
        BlockState state = getBlockState();
        if (!state.hasProperty(CryoplantControllerBlock.RUNNING)) {
            return;
        }
        boolean running = structure.isPresent() && rate >= RUNNING_RATE;
        if (state.getValue(CryoplantControllerBlock.RUNNING) != running) {
            level.setBlock(getBlockPos(), state.setValue(CryoplantControllerBlock.RUNNING, running), Block.UPDATE_CLIENTS);
        }
    }

    // ------------------------------------------------------------------ ports

    FluidTank tankFor(CryoplantPortMode mode) {
        return switch (mode) {
            case HELIUM_IN -> warmHeliumTank;
            case LIQUID_HELIUM_OUT -> liquidHeliumTank;
            case NITROGEN_IN -> nitrogenTank;
            case LIQUID_AIR_OUT -> liquidAirTank;
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
        int reach = CryoplantStructure.MAX_HEIGHT;
        return new BoundingBox(pos.getX() - 2, pos.getY() - reach, pos.getZ() - 2, pos.getX() + 2, pos.getY() + reach, pos.getZ() + 2);
    }

    @Override
    public boolean shouldReactToBlockChange(BlockChange change) {
        if (change.pos().equals(getBlockPos())) {
            // The running lamp is a state change of this very block; only a replacement matters.
            return change.removed() || !change.state().is(CryoRegistry.CRYOPLANT_CONTROLLER.get());
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
        MultiblockValidationResult<CryoplantStructure> result = CryoplantStructureValidator.validate(level, getBlockPos());
        lastValidation = result;
        String k = "create_nuclearindustry.cryoplant.";
        if (result.formed() && result.structure().isPresent()) {
            CryoplantStructure next = result.structure().get();
            setStructure(Optional.of(next));
            if (playerInitiated || !wasFormed) {
                org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements.awardMachine(level, getBlockPos(), messenger.owner(level), org.papiricoh.create_nuclearindustry.advancement.NuclearEvent.CRYOPLANT_FORMED);
                messenger.status(level, playerInitiated ? player : null, Component.translatable(k + "formed", next.height, next.compressorPositions().size())
                        .withStyle(ChatFormatting.AQUA));
            }
        } else {
            setStructure(Optional.empty());
            if (playerInitiated || wasFormed) {
                messenger.status(level, playerInitiated ? player : null, Component.translatable(wasFormed ? k + "broken" : k + "incomplete",
                        CryoplantStructureValidator.describe(result.primaryMessage())).withStyle(ChatFormatting.RED));
            }
        }
        sendData();
        setChanged();
    }

    private void setStructure(Optional<CryoplantStructure> next) {
        boolean boundsChanged = !structure.map(CryoplantStructure::bounds).equals(next.map(CryoplantStructure::bounds));
        structure.ifPresent(this::unlinkParts);
        structure = next;
        snapshot = next.map(s -> StructureSnapshot.capture(level, s.allPositions())).orElse(null);
        next.ifPresent(this::linkParts);
        if (boundsChanged) {
            MultiblockRegistry.reindex(this);
        }
    }

    private void linkParts(CryoplantStructure s) {
        double share = s.height / (double) Math.max(1, s.compressorPositions().size());
        for (BlockPos pos : s.portPositions()) {
            if (level.getBlockEntity(pos) instanceof CryoplantPortBlockEntity port) {
                port.link(getBlockPos());
            }
        }
        for (BlockPos pos : s.compressorPositions()) {
            if (level.getBlockEntity(pos) instanceof CryoplantCompressorBlockEntity compressor) {
                compressor.link(getBlockPos(), share);
            }
        }
    }

    private void unlinkParts(CryoplantStructure s) {
        for (BlockPos pos : s.portPositions()) {
            if (level.getBlockEntity(pos) instanceof CryoplantPortBlockEntity port) {
                port.unlink(getBlockPos());
            }
        }
        for (BlockPos pos : s.compressorPositions()) {
            if (level.getBlockEntity(pos) instanceof CryoplantCompressorBlockEntity compressor) {
                compressor.unlink(getBlockPos());
            }
        }
    }

    // ------------------------------------------------------------------ accessors

    public Optional<CryoplantStructure> getStructure() {
        return structure;
    }

    /** Why the plant last failed to form (a translation key with its arguments), or empty when formed. Server side. */
    public String getValidationMessage() {
        return lastValidation.formed() ? "" : lastValidation.primaryMessage();
    }

    public boolean isFormed() {
        return level != null && level.isClientSide ? formed : structure.isPresent();
    }

    /** Running: formed and making liquid. Works on both sides. */
    public boolean isRunning() {
        return isFormed() && rate >= RUNNING_RATE;
    }

    /** What it liquefies: air when it has a Liquid Air port. Works on both sides. */
    public boolean isAirMode() {
        return level != null && level.isClientSide ? airMode
                : structure.map(s -> s.mode() == CryoplantModel.Mode.AIR).orElse(false);
    }

    /** Plant height in layers, 0 when not formed. Works on both sides. */
    public int getHeight() {
        return level != null && level.isClientSide ? height : structure.map(s -> s.height).orElse(0);
    }

    /** The centre of the bottom layer, or this block when not formed. Works on both sides. */
    public BlockPos getBottomCenter() {
        if (level != null && level.isClientSide) {
            return formed ? bottom : getBlockPos();
        }
        return structure.map(s -> s.bottomCenter).orElse(getBlockPos());
    }

    public FluidTank getWarmHeliumTank() {
        return warmHeliumTank;
    }

    public FluidTank getLiquidHeliumTank() {
        return liquidHeliumTank;
    }

    public FluidTank getNitrogenTank() {
        return nitrogenTank;
    }

    public FluidTank getLiquidAirTank() {
        return liquidAirTank;
    }

    /** Liquid made, mB/t, smoothed. */
    public double getRate() {
        return rate;
    }

    /** The compressors' average share of rated speed, 0..1. */
    public double getSpeed() {
        return speed;
    }

    /** Liquid helium made since the plant was placed, mB. */
    public double getTotalHelium() {
        return totalHelium;
    }

    /** Liquid air made since the plant was placed, mB. */
    public double getTotalAir() {
        return totalAir;
    }

    /** Helium that leaked out of the loop, mB. */
    public double getTotalLost() {
        return totalLost;
    }

    // ------------------------------------------------------------------ goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        String k = "create_nuclearindustry.cryoplant.goggles.";
        tooltip.add(Component.literal("    ").append(Component.translatable("block.create_nuclearindustry.cryoplant_controller"))
                .withStyle(ChatFormatting.GOLD));
        if (!isFormed()) {
            tooltip.add(line(CryoplantStructureValidator.describe(message), ChatFormatting.RED));
            return true;
        }
        tooltip.add(line(Component.translatable(k + "size", height, compressors), ChatFormatting.GRAY));
        tooltip.add(line(Component.translatable(k + "compressors", Math.round(speed * 100)),
                speed > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        if (airMode) {
            tooltip.add(line(Component.translatable(k + "air_mode"), ChatFormatting.AQUA));
            tooltip.add(line(Component.translatable(k + "making_air", one(rate), one(capacity)),
                    rate >= RUNNING_RATE ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
            tooltip.add(line(Component.translatable(k + "air_tank", liquidAirTank.getFluidAmount(), liquidAirTank.getCapacity()),
                    ChatFormatting.GRAY));
            return true;
        }
        tooltip.add(line(Component.translatable(k + "helium_mode"), ChatFormatting.AQUA));
        tooltip.add(line(Component.translatable(k + "making_helium", one(rate), one(capacity)),
                rate >= RUNNING_RATE ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        tooltip.add(line(Component.translatable(k + "helium_tanks", warmHeliumTank.getFluidAmount(),
                liquidHeliumTank.getFluidAmount(), liquidHeliumTank.getCapacity()), ChatFormatting.GRAY));
        if (nitrogenTank.getFluidAmount() > 0 || precooling > 0) {
            tooltip.add(line(Component.translatable(k + "nitrogen", nitrogenTank.getFluidAmount(),
                    Math.round(precooling * CryoConfig.nitrogenBonus * 100)), ChatFormatting.AQUA));
        }
        if (speed > 0 && warmHeliumTank.isEmpty()) {
            tooltip.add(line(Component.translatable(k + "no_helium"), ChatFormatting.YELLOW));
        } else if (speed > 0 && liquidHeliumTank.getSpace() == 0) {
            tooltip.add(line(Component.translatable(k + "full"), ChatFormatting.YELLOW));
        }
        return true;
    }

    private static String one(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static Component line(Component text, ChatFormatting style) {
        return Component.literal("     ").append(text).withStyle(style);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("WarmHelium", warmHeliumTank.writeToNBT(registries, new CompoundTag()));
        tag.put("LiquidHelium", liquidHeliumTank.writeToNBT(registries, new CompoundTag()));
        tag.put("Nitrogen", nitrogenTank.writeToNBT(registries, new CompoundTag()));
        tag.put("LiquidAir", liquidAirTank.writeToNBT(registries, new CompoundTag()));
        tag.putDouble("Rate", rate);
        tag.putDouble("Capacity", capacity);
        tag.putDouble("Speed", speed);
        tag.putDouble("Precooling", precooling);
        tag.putDouble("TotalHelium", totalHelium);
        tag.putDouble("TotalAir", totalAir);
        tag.putDouble("TotalLost", totalLost);
        tag.putDouble("HeliumDebt", heliumDebt);
        tag.putDouble("NitrogenDebt", nitrogenDebt);
        tag.putDouble("LiquidCredit", liquidCredit);
        tag.putDouble("AirCredit", airCredit);
        if (clientPacket) {
            tag.putBoolean("Formed", structure.isPresent());
            tag.putInt("Height", structure.map(s -> s.height).orElse(0));
            tag.putInt("Compressors", structure.map(s -> s.compressorPositions().size()).orElse(0));
            tag.putBoolean("AirMode", structure.map(s -> s.mode() == CryoplantModel.Mode.AIR).orElse(false));
            tag.putLong("Bottom", structure.map(s -> s.bottomCenter).orElse(getBlockPos()).asLong());
            tag.putString("Message", lastValidation.formed() ? "" : lastValidation.primaryMessage());
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        warmHeliumTank.readFromNBT(registries, tag.getCompound("WarmHelium"));
        liquidHeliumTank.readFromNBT(registries, tag.getCompound("LiquidHelium"));
        nitrogenTank.readFromNBT(registries, tag.getCompound("Nitrogen"));
        liquidAirTank.readFromNBT(registries, tag.getCompound("LiquidAir"));
        rate = tag.getDouble("Rate");
        capacity = tag.getDouble("Capacity");
        speed = tag.getDouble("Speed");
        precooling = tag.getDouble("Precooling");
        totalHelium = tag.getDouble("TotalHelium");
        totalAir = tag.getDouble("TotalAir");
        totalLost = tag.getDouble("TotalLost");
        heliumDebt = tag.getDouble("HeliumDebt");
        nitrogenDebt = tag.getDouble("NitrogenDebt");
        liquidCredit = tag.getDouble("LiquidCredit");
        airCredit = tag.getDouble("AirCredit");
        if (clientPacket) {
            formed = tag.getBoolean("Formed");
            height = tag.getInt("Height");
            compressors = tag.getInt("Compressors");
            airMode = tag.getBoolean("AirMode");
            bottom = BlockPos.of(tag.getLong("Bottom"));
            message = tag.getString("Message");
        } else {
            pendingRevalidation = true;
        }
    }
}
