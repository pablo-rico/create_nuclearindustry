package org.papiricoh.create_nuclearindustry.processing;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Clearable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.processing.recipe.NuclearProcessingRecipe;
import org.papiricoh.create_nuclearindustry.processing.recipe.ProcessingInput;

import java.util.List;
import java.util.Optional;

/**
 * A kinetic machine with one input slot, two output slots, one input tank and two output
 * tanks (product and tails), driven by {@link NuclearProcessingRecipe}s.
 *
 * <p>Sides, relative to the block's facing: the back takes the shaft; the front drains the
 * product tank; the bottom drains the tails tank and extracts items; every other side
 * accepts input items and fluid.
 *
 * <p>A finished recipe only consumes its inputs once every output fits, so a full output
 * stalls the machine instead of destroying anything.
 */
public abstract class KineticProcessingBlockEntity extends KineticBlockEntity implements Clearable {
    public static final int INPUT_SLOT = 0;
    public static final int FIRST_OUTPUT_SLOT = 1;
    public static final int OUTPUT_SLOTS = 2;
    public static final int TANK_CAPACITY = 4000;
    private static final int SYNC_INTERVAL = 10;

    protected final ItemStackHandler items = new ItemStackHandler(1 + OUTPUT_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot != INPUT_SLOT || acceptsInputItem(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot == INPUT_SLOT) {
                recipeDirty = true;
            }
            setChanged();
        }
    };
    protected final FluidTank inputTank = new FluidTank(TANK_CAPACITY, this::acceptsInputFluid) {
        @Override
        protected void onContentsChanged() {
            recipeDirty = true;
            setChanged();
        }
    };
    protected final FluidTank productTank = tank();
    protected final FluidTank tailsTank = tank();

    private final IItemHandler inputItems = new SlotRangeHandler(items, INPUT_SLOT, 1, true, false);
    private final IItemHandler outputItems = new SlotRangeHandler(items, FIRST_OUTPUT_SLOT, OUTPUT_SLOTS, false, true);
    private final IFluidHandler inputFluids = new TankAccess(inputTank, true, false);
    private final IFluidHandler productFluids = new TankAccess(productTank, false, true);
    private final IFluidHandler tailsFluids = new TankAccess(tailsTank, false, true);

    @Nullable
    private RecipeHolder<? extends NuclearProcessingRecipe> currentRecipe;
    private boolean recipeDirty = true;
    private boolean outputBlocked;
    private float progress;
    private int syncTimer;

    protected KineticProcessingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract AllNuclearRecipeTypes recipeType();

    /** RPM below which the machine stalls. Faster rotation speeds processing up linearly. */
    protected abstract float minimumSpeed();

    /** Stress units drawn per RPM. */
    protected abstract float stressImpact();

    protected abstract Component machineName();

    @Override
    public float calculateStressApplied() {
        float impact = stressImpact();
        lastStressApplied = impact;
        return impact;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            org.papiricoh.create_nuclearindustry.reactor.visual.ReactorVisualHooks.machineClientTick.accept(this);
            return;
        }

        if (recipeDirty) {
            recipeDirty = false;
            RecipeHolder<? extends NuclearProcessingRecipe> found = findRecipe().orElse(null);
            if (found == null || currentRecipe == null || !found.id().equals(currentRecipe.id())) {
                progress = 0;
            }
            currentRecipe = found;
            outputBlocked = false;
        }

        boolean changed = false;
        if (currentRecipe != null && isFastEnough()) {
            NuclearProcessingRecipe recipe = currentRecipe.value();
            int duration = Math.max(1, recipe.getProcessingDuration());
            if (progress < duration) {
                progress = Math.min(duration, progress + Math.abs(getSpeed()) / minimumSpeed());
                changed = true;
            }
            if (progress >= duration && !outputBlocked) {
                changed |= tryFinish(recipe);
            }
        }

        if (changed && ++syncTimer >= SYNC_INTERVAL) {
            syncTimer = 0;
            sendData();
        }
        if (changed) {
            setChanged();
        }
    }

    private boolean tryFinish(NuclearProcessingRecipe recipe) {
        if (!recipe.matches(currentInput(), level)) {
            recipeDirty = true;
            return false;
        }
        List<ItemStack> rolled = recipe.rollResults(level.random);
        FluidStack product = recipe.productFluid();
        FluidStack tails = recipe.tailsFluid();
        if (!fitsItems(rolled) || !fits(productTank, product) || !fits(tailsTank, tails)) {
            // Re-checked whenever an output is emptied (see TankAccess / SlotRangeHandler).
            outputBlocked = true;
            sendData();
            return false;
        }

        items.extractItem(INPUT_SLOT, recipe.itemCost(), false);
        if (recipe.fluidCost() > 0) {
            inputTank.drain(recipe.fluidCost(), IFluidHandler.FluidAction.EXECUTE);
        }
        for (ItemStack stack : rolled) {
            insertOutput(stack);
        }
        if (!product.isEmpty()) {
            productTank.fill(product.copy(), IFluidHandler.FluidAction.EXECUTE);
        }
        if (!tails.isEmpty()) {
            tailsTank.fill(tails.copy(), IFluidHandler.FluidAction.EXECUTE);
        }
        progress = 0;
        recipeDirty = true;
        onRecipeFinished(recipe);
        return true;
    }

    protected void onRecipeFinished(NuclearProcessingRecipe recipe) {}

    private Optional<RecipeHolder<? extends NuclearProcessingRecipe>> findRecipe() {
        ProcessingInput input = currentInput();
        if (input.isEmpty()) {
            return Optional.empty();
        }
        for (RecipeHolder<? extends NuclearProcessingRecipe> holder : recipeType().all(level)) {
            if (holder.value().matches(input, level)) {
                return Optional.of(holder);
            }
        }
        return Optional.empty();
    }

    private ProcessingInput currentInput() {
        return new ProcessingInput(items.getStackInSlot(INPUT_SLOT), inputTank.getFluid());
    }

    protected boolean acceptsInputItem(ItemStack stack) {
        if (level == null) {
            return false;
        }
        for (RecipeHolder<? extends NuclearProcessingRecipe> holder : recipeType().all(level)) {
            if (holder.value().acceptsItem(stack)) {
                return true;
            }
        }
        return false;
    }

    protected boolean acceptsInputFluid(FluidStack fluid) {
        if (level == null) {
            return false;
        }
        for (RecipeHolder<? extends NuclearProcessingRecipe> holder : recipeType().all(level)) {
            if (holder.value().acceptsFluid(fluid)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFastEnough() {
        return Math.abs(getSpeed()) >= minimumSpeed() && !isOverStressed();
    }

    private boolean fitsItems(List<ItemStack> stacks) {
        ItemStackHandler simulated = new ItemStackHandler(OUTPUT_SLOTS);
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            simulated.setStackInSlot(i, items.getStackInSlot(FIRST_OUTPUT_SLOT + i).copy());
        }
        for (ItemStack stack : stacks) {
            if (!ItemHandlerHelper.insertItemStacked(simulated, stack.copy(), false).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void insertOutput(ItemStack stack) {
        ItemStack remainder = stack.copy();
        for (int slot = FIRST_OUTPUT_SLOT; slot < FIRST_OUTPUT_SLOT + OUTPUT_SLOTS && !remainder.isEmpty(); slot++) {
            if (!items.getStackInSlot(slot).isEmpty()) {
                remainder = forceInsert(slot, remainder);
            }
        }
        for (int slot = FIRST_OUTPUT_SLOT; slot < FIRST_OUTPUT_SLOT + OUTPUT_SLOTS && !remainder.isEmpty(); slot++) {
            remainder = forceInsert(slot, remainder);
        }
    }

    private ItemStack forceInsert(int slot, ItemStack stack) {
        return items.insertItem(slot, stack, false);
    }

    private static boolean fits(FluidTank tank, FluidStack stack) {
        return stack.isEmpty() || tank.fill(stack.copy(), IFluidHandler.FluidAction.SIMULATE) == stack.getAmount();
    }

    private FluidTank tank() {
        return new FluidTank(TANK_CAPACITY) {
            @Override
            protected void onContentsChanged() {
                outputBlocked = false;
                setChanged();
            }
        };
    }

    // ------------------------------------------------------------------ capabilities

    protected Direction facing() {
        return getBlockState().getValue(KineticProcessingBlock.FACING);
    }

    @Nullable
    public IItemHandler getItemHandler(@Nullable Direction side) {
        if (side == null) {
            return items;
        }
        if (side == facing().getOpposite()) {
            return null;
        }
        return side == Direction.DOWN ? outputItems : inputItems;
    }

    @Nullable
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        if (side == null) {
            return inputFluids;
        }
        Direction facing = facing();
        if (side == facing.getOpposite()) {
            return null;
        }
        if (side == facing) {
            return productFluids;
        }
        return side == Direction.DOWN ? tailsFluids : inputFluids;
    }

    // ------------------------------------------------------------------ interaction

    public ItemInteractionResult use(Player player, InteractionHand hand, ItemStack held, Direction clickedFace) {
        if (level == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        IFluidHandler fluidSide = getFluidHandler(clickedFace);
        if (!held.isEmpty() && fluidSide != null && FluidUtil.getFluidHandler(held).isPresent()) {
            if (FluidUtil.interactWithFluidHandler(player, hand, fluidSide)) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!held.isEmpty()) {
            if (!acceptsInputItem(held)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide) {
                ItemStack remainder = items.insertItem(INPUT_SLOT, held.copy(), false);
                if (!player.getAbilities().instabuild) {
                    player.setItemInHand(hand, remainder);
                }
                sendData();
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide) {
            ItemStack taken = ItemStack.EMPTY;
            for (int slot = FIRST_OUTPUT_SLOT; slot < FIRST_OUTPUT_SLOT + OUTPUT_SLOTS && taken.isEmpty(); slot++) {
                taken = items.extractItem(slot, 64, false);
            }
            if (taken.isEmpty() && player.isShiftKeyDown()) {
                taken = items.extractItem(INPUT_SLOT, 64, false);
            }
            if (taken.isEmpty()) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            player.setItemInHand(hand, taken);
            sendData();
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    // ------------------------------------------------------------------ goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("    ").append(machineName()).withStyle(ChatFormatting.GOLD));
        tooltip.add(status().withStyle(statusColor()));
        if (progressFraction() > 0) {
            int percent = Math.round(progressFraction() * 100);
            tooltip.add(Component.literal("     " + progressBar(percent) + " " + percent + "%").withStyle(ChatFormatting.GRAY));
        }
        addStack(tooltip, "gui.create_nuclearindustry.machine.input", items.getStackInSlot(INPUT_SLOT));
        for (int slot = FIRST_OUTPUT_SLOT; slot < FIRST_OUTPUT_SLOT + OUTPUT_SLOTS; slot++) {
            addStack(tooltip, "gui.create_nuclearindustry.machine.output", items.getStackInSlot(slot));
        }
        addTank(tooltip, "gui.create_nuclearindustry.machine.feed", inputTank);
        addTank(tooltip, "gui.create_nuclearindustry.machine.product", productTank);
        addTank(tooltip, "gui.create_nuclearindustry.machine.tails", tailsTank);
        return true;
    }

    private net.minecraft.network.chat.MutableComponent status() {
        String key;
        if (Math.abs(getSpeed()) < minimumSpeed()) {
            return Component.translatable("gui.create_nuclearindustry.machine.status.too_slow", (int) minimumSpeed());
        } else if (isOverStressed()) {
            key = "gui.create_nuclearindustry.machine.status.overstressed";
        } else if (outputBlocked) {
            key = "gui.create_nuclearindustry.machine.status.output_full";
        } else if (currentRecipe == null) {
            key = "gui.create_nuclearindustry.machine.status.idle";
        } else {
            key = "gui.create_nuclearindustry.machine.status.working";
        }
        return Component.translatable(key);
    }

    private ChatFormatting statusColor() {
        if (Math.abs(getSpeed()) < minimumSpeed() || isOverStressed() || outputBlocked) {
            return ChatFormatting.RED;
        }
        return currentRecipe == null ? ChatFormatting.YELLOW : ChatFormatting.GREEN;
    }

    private float progressFraction() {
        if (currentRecipe == null) {
            return 0;
        }
        return Math.min(1, progress / Math.max(1, currentRecipe.value().getProcessingDuration()));
    }

    private static void addStack(List<Component> tooltip, String key, ItemStack stack) {
        if (!stack.isEmpty()) {
            tooltip.add(Component.literal("     ").append(Component.translatable(key)).append(": " + stack.getCount() + "x ")
                    .append(stack.getHoverName()).withStyle(ChatFormatting.GRAY));
        }
    }

    private static void addTank(List<Component> tooltip, String key, FluidTank tank) {
        if (!tank.isEmpty()) {
            tooltip.add(Component.literal("     ").append(Component.translatable(key)).append(": ")
                    .append(tank.getFluid().getHoverName())
                    .append(" " + tank.getFluidAmount() + "/" + tank.getCapacity() + " mB").withStyle(ChatFormatting.GRAY));
        }
    }

    private static String progressBar(int percent) {
        int filled = Math.max(0, Math.min(10, Math.round(percent / 10.0f)));
        return "[" + "|".repeat(filled) + ".".repeat(10 - filled) + "]";
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Items", items.serializeNBT(registries));
        tag.put("InputTank", inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("ProductTank", productTank.writeToNBT(registries, new CompoundTag()));
        tag.put("TailsTank", tailsTank.writeToNBT(registries, new CompoundTag()));
        tag.putFloat("Progress", progress);
        tag.putBoolean("OutputBlocked", outputBlocked);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("Items")) {
            items.deserializeNBT(registries, tag.getCompound("Items"));
        }
        inputTank.readFromNBT(registries, tag.getCompound("InputTank"));
        productTank.readFromNBT(registries, tag.getCompound("ProductTank"));
        tailsTank.readFromNBT(registries, tag.getCompound("TailsTank"));
        progress = tag.getFloat("Progress");
        outputBlocked = tag.getBoolean("OutputBlocked");
        recipeDirty = true;
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            items.setStackInSlot(slot, ItemStack.EMPTY);
        }
        inputTank.setFluid(FluidStack.EMPTY);
        productTank.setFluid(FluidStack.EMPTY);
        tailsTank.setFluid(FluidStack.EMPTY);
        progress = 0;
    }

    /** Everything stored, for dropping when the block is broken. */
    public List<ItemStack> storedItems() {
        java.util.ArrayList<ItemStack> stacks = new java.util.ArrayList<>();
        for (int slot = 0; slot < items.getSlots(); slot++) {
            if (!items.getStackInSlot(slot).isEmpty()) {
                stacks.add(items.getStackInSlot(slot).copy());
            }
        }
        return stacks;
    }

    // ------------------------------------------------------------------ test hooks

    public ItemStack getItem(int slot) {
        return items.getStackInSlot(slot);
    }

    public FluidStack getInputFluid() {
        return inputTank.getFluid();
    }

    public FluidStack getProductFluid() {
        return productTank.getFluid();
    }

    public FluidStack getTailsFluid() {
        return tailsTank.getFluid();
    }

    public boolean isOutputBlocked() {
        return outputBlocked;
    }

    /**
     * Whether the machine looks busy from what is synced to clients: turning fast enough, not
     * overstressed, an input to work on and room for the output. For its running sound.
     */
    public boolean looksBusy() {
        return Math.abs(getSpeed()) >= minimumSpeed() && !isOverStressed() && !outputBlocked
                && (!items.getStackInSlot(INPUT_SLOT).isEmpty() || !inputTank.isEmpty());
    }

    // ------------------------------------------------------------------ side views

    /** A window onto part of the inventory that only allows insertion or extraction. */
    private record SlotRangeHandler(ItemStackHandler items, int first, int count, boolean insert, boolean extract)
            implements IItemHandler {
        @Override
        public int getSlots() {
            return count;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(first + slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return insert ? items.insertItem(first + slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return extract ? items.extractItem(first + slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(first + slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return insert && items.isItemValid(first + slot, stack);
        }
    }

    /** A tank that pipes may only fill, or only drain. */
    private record TankAccess(FluidTank tank, boolean fill, boolean drain) implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            return tank.getFluid();
        }

        @Override
        public int getTankCapacity(int index) {
            return tank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int index, FluidStack stack) {
            return fill && tank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return fill ? tank.fill(resource, action) : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return drain ? tank.drain(resource, action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return drain ? tank.drain(maxDrain, action) : FluidStack.EMPTY;
        }
    }
}
