package org.papiricoh.create_nuclearindustry.accelerator.blockentity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorConfig;
import org.papiricoh.create_nuclearindustry.accelerator.physics.AntimatterModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.PenningTrapModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TargetIrradiationModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TargetStationModel;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterHolder;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterHolders;
import org.papiricoh.create_nuclearindustry.isotope.IsotopeConfig;
import org.papiricoh.create_nuclearindustry.isotope.Isotopes;
import org.papiricoh.create_nuclearindustry.isotope.TechnetiumGeneratorItem;
import org.papiricoh.create_nuclearindustry.processing.AllNuclearRecipeTypes;
import org.papiricoh.create_nuclearindustry.processing.recipe.TargetIrradiationRecipe;

import java.util.List;
import java.util.Locale;

/**
 * The Target Station: a beam dump with a dense target and a magnetic horn. It takes the whole beam
 * and gets activated like the dump. Above {@code accelerator.antimatter.thresholdGev} the horn also
 * collects antiprotons ({@link TargetStationModel}) and pushes them into the antimatter holders
 * beside it every tick (any side but the beam window); conduits pull from its small collector
 * buffer. The collector only gathers: antiprotons it cannot hand on within its buffer are lost
 * harmlessly in the target. Below the threshold the beam irradiates the target in its slot instead
 * ({@link TargetIrradiationRecipe}, {@link TargetIrradiationModel}): uranium becomes molybdenum-99
 * columns, for instance. Targets go in and products come out on any side.
 */
public class TargetStationBlockEntity extends BeamDumpBlockEntity implements AntimatterHolder {
    /** Antiprotons the collector keeps for conduits, ng. */
    public static final double COLLECTOR_BUFFER = 1.0;
    private static final int SYNC_INTERVAL = 20;

    private double beamEnergy;
    private double beamPower;
    private long beamTick = Long.MIN_VALUE;
    private double collected;
    /** Antiprotons made on the last tick with beam, ng. */
    private double lastProduction;
    private double syncedProduction;
    private final ItemStackHandler target = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** What irradiation made, waiting to be taken out. */
    private final ItemStackHandler products = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** Targets in through slot 0, products out of slots 1 to 4. */
    private final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1 + products.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? target.getStackInSlot(0) : products.getStackInSlot(slot - 1);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == 0 ? target.insertItem(0, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 0 ? ItemStack.EMPTY : products.extractItem(slot - 1, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? target.getSlotLimit(0) : products.getSlotLimit(slot - 1);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0;
        }
    };
    /** Beam energy put into the current target item, kJ. */
    private double irradiationKj;
    /** Whether the beam irradiated the target on the last tick, for the goggles. */
    private boolean irradiating;
    private boolean syncedIrradiating;

    public TargetStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void receiveBeam(double energyMev, double current, double powerKw) {
        super.receiveBeam(energyMev, current, powerKw);
        if (level != null) {
            beamEnergy = Math.max(0, energyMev);
            beamPower = Math.max(0, powerKw);
            beamTick = level.getGameTime();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        boolean beam = level.getGameTime() - beamTick <= 1;
        lastProduction = beam ? TargetStationModel.perTick(AcceleratorConfig.targetStation(), AcceleratorConfig.antimatter(),
                beamEnergy / 1000.0, beamPower) : 0;
        if (lastProduction > 0) {
            collected = Math.min(COLLECTOR_BUFFER, collected + lastProduction);
        }
        if (collected > PenningTrapModel.EMPTY) {
            pushToNeighbours();
        }
        irradiating = beam && irradiate();
        if (level.getGameTime() % SYNC_INTERVAL == 0
                && (Math.abs(lastProduction - syncedProduction) > 1.0e-6 || irradiating != syncedIrradiating)) {
            syncedProduction = lastProduction;
            syncedIrradiating = irradiating;
            sendData();
        }
    }

    /**
     * One tick of irradiating the target with the beam, below the antiproton threshold. Returns
     * whether the target took the beam.
     */
    private boolean irradiate() {
        ItemStack input = target.getStackInSlot(0);
        TargetIrradiationRecipe recipe = input.isEmpty() ? null : recipeFor(input);
        if (recipe == null || recipe.getRollableResults().isEmpty()) {
            return false;
        }
        double gev = beamEnergy / 1000.0;
        if (!TargetIrradiationModel.irradiates(AcceleratorConfig.antimatter(), gev, IsotopeConfig.targetMinEnergyGev)) {
            return false;
        }
        ItemStack result = recipe.getRollableResults().get(0).getStack();
        if (!ItemHandlerHelper.insertItemStacked(products, result.copy(), true).isEmpty()) {
            return false;
        }
        TargetIrradiationModel.Result step = TargetIrradiationModel.tick(AcceleratorConfig.antimatter(), irradiationKj, gev, beamPower,
                IsotopeConfig.targetMinEnergyGev, recipe.nominalKj(), 1);
        irradiationKj = step.progress();
        if (step.made() > 0) {
            target.extractItem(0, 1, false);
            ItemHandlerHelper.insertItemStacked(products, dated(result.copy()), false);
            irradiationKj = 0;
        }
        setChanged();
        return true;
    }

    /** Products start their decay clocks as they leave the target. */
    private ItemStack dated(ItemStack product) {
        long now = level.getGameTime();
        if (product.getItem() instanceof TechnetiumGeneratorItem) {
            TechnetiumGeneratorItem.refresh(product, now);
        } else if (Isotopes.isotopeOf(product) != null) {
            Isotopes.stamp(product, now, 1.0);
        }
        return product;
    }

    @Nullable
    private TargetIrradiationRecipe recipeFor(ItemStack input) {
        return level.getRecipeManager().getRecipeFor(AllNuclearRecipeTypes.TARGET_IRRADIATION.<SingleRecipeInput, TargetIrradiationRecipe>getType(),
                new SingleRecipeInput(input), level).map(RecipeHolder::value).orElse(null);
    }

    /** Share of the current target item done, 0..1, for the goggles. */
    public double irradiationProgress() {
        ItemStack input = target.getStackInSlot(0);
        TargetIrradiationRecipe recipe = input.isEmpty() || level == null ? null : recipeFor(input);
        return recipe == null ? 0 : TargetIrradiationModel.fraction(irradiationKj, recipe.nominalKj());
    }

    public IItemHandler products() {
        return products;
    }

    /** Hands the collector's antiprotons to the holders beside it that take them now, beam window excepted. */
    private void pushToNeighbours() {
        Direction window = beamWindow();
        for (Direction side : Direction.values()) {
            if (side == window || collected <= PenningTrapModel.EMPTY) {
                continue;
            }
            BlockPos next = worldPosition.relative(side);
            AntimatterHolder holder = AntimatterHolders.at(level, next);
            if (holder == null || holder == this || !holder.acceptsAntimatter()) {
                continue;
            }
            double taken = holder.insert(collected, false);
            collected = Math.max(0, collected - taken);
            setChanged();
        }
    }

    /** The face the beam comes in through. */
    public Direction beamWindow() {
        BlockState state = getBlockState();
        return state.hasProperty(HorizontalDirectionalBlock.FACING) ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
    }

    /** Antiprotons this station makes per minute with its beam now, ng. */
    public double productionPerMinute() {
        return lastProduction * 1200;
    }

    /** Antiprotons per tick as clients last heard it (the renderer's collector glow), ng. */
    public double displayedProduction() {
        return Math.max(lastProduction, syncedProduction);
    }

    /** Whether the beam it gets is above the antiproton threshold. */
    public boolean aboveThreshold() {
        return TargetStationModel.produces(AcceleratorConfig.antimatter(), beamEnergy / 1000.0) && incomingPower() > 0;
    }

    public IItemHandler targetSlot() {
        return target;
    }

    /** What pipes and funnels see: the target slot to fill, the products to empty. */
    public IItemHandler automation() {
        return automation;
    }

    // ------------------------------------------------------------------ the collector

    @Override
    public double stored() {
        return collected;
    }

    @Override
    public double capacity() {
        return COLLECTOR_BUFFER;
    }

    @Override
    public double insert(double ng, boolean simulate) {
        return 0;
    }

    @Override
    public double extract(double ng, boolean simulate) {
        double given = Math.max(0, Math.min(ng, collected));
        if (!simulate && given > 0) {
            collected -= given;
            setChanged();
        }
        return given;
    }

    @Override
    public boolean powered() {
        return true;
    }

    @Override
    public boolean acceptsAntimatter() {
        return false;
    }

    @Override
    public void destroy() {
        if (org.papiricoh.create_nuclearindustry.integration.sable.ShipAssembly.isMoving()) {
            // Carried to or from a ship: the copy at the new place keeps everything.
            super.destroy();
            return;
        }
        super.destroy();
        if (level != null && !level.isClientSide) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    target.getStackInSlot(0));
            for (int slot = 0; slot < products.getSlots(); slot++) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                        products.getStackInSlot(slot));
            }
        }
    }

    // ------------------------------------------------------------------ goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        String k = "create_nuclearindustry.accelerator.target.goggles.";
        AntimatterModel.Params am = AcceleratorConfig.antimatter();
        if (lastProduction > 0 || syncedProduction > 0) {
            tooltip.add(line(Component.translatable(k + "antiprotons", fmt3(Math.max(lastProduction, syncedProduction) * 1200)),
                    ChatFormatting.GREEN));
        } else {
            tooltip.add(line(Component.translatable(k + "threshold", fmt1(am.thresholdGev())), ChatFormatting.GRAY));
        }
        ItemStack input = target.getStackInSlot(0);
        if (!input.isEmpty()) {
            tooltip.add(line(Component.translatable(irradiating ? k + "irradiating" : k + "target", input.getHoverName(),
                    Math.round(irradiationProgress() * 100)), irradiating ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        }
        return true;
    }

    private static Component line(Component text, ChatFormatting style) {
        return Component.literal("     ").append(text).withStyle(style);
    }

    private static String fmt1(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String fmt3(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("Collected", collected);
        tag.put("Target", target.serializeNBT(registries));
        tag.put("Products", products.serializeNBT(registries));
        tag.putDouble("IrradiationKj", irradiationKj);
        if (clientPacket) {
            tag.putDouble("Production", lastProduction);
            tag.putBoolean("Irradiating", irradiating);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        collected = tag.getDouble("Collected");
        target.deserializeNBT(registries, tag.getCompound("Target"));
        if (tag.contains("Products")) {
            products.deserializeNBT(registries, tag.getCompound("Products"));
        }
        irradiationKj = tag.getDouble("IrradiationKj");
        if (clientPacket) {
            syncedProduction = tag.getDouble("Production");
            irradiating = tag.getBoolean("Irradiating");
        }
    }
}
