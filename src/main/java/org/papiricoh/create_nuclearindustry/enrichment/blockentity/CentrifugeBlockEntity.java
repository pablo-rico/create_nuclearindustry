package org.papiricoh.create_nuclearindustry.enrichment.blockentity;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Clearable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.AllNuclearRecipes;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.enrichment.recipe.CentrifugingRecipe;

import java.util.List;
import java.util.Optional;

public class CentrifugeBlockEntity extends KineticBlockEntity implements Clearable {
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final float MIN_SPEED = 16.0f;
    private static final float NATURAL_OUTPUT = 0.7f;
    private static final float ENRICHMENT_STEP = 0.2f;
    private static final float MAX_ENRICHMENT = 98.0f;
    private static final int RAW_PROCESSING_TIME = 120;
    private static final int ENRICHING_PROCESSING_TIME = 3000;

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case INPUT_SLOT -> stack.is(AllNuclearItems.URANIUM.get()) || stack.is(AllNuclearItems.RAW_URANIUM.get());
                case OUTPUT_SLOT -> stack.is(AllNuclearItems.URANIUM.get());
                default -> false;
            };
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final IItemHandler inputHandler = new SlotView(INPUT_SLOT, true, false);
    private final IItemHandler outputHandler = new SlotView(OUTPUT_SLOT, false, true);

    private int progress;
    private int processingTime;
    private int syncCooldown;

    public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
        this(AllNuclearEntities.CENTRIFUGE.get(), pos, state);
    }

    public CentrifugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }

        Optional<ProcessingPlan> plan = getCurrentPlan();
        if (Math.abs(getSpeed()) < MIN_SPEED || plan.isEmpty() || !canOutput(plan.get())) {
            if (progress != 0) {
                progress = 0;
                notifyUpdate();
            }
            return;
        }

        processingTime = Math.max(1, plan.get().processingTime());
        progress += getProcessingSpeed();
        if (progress >= processingTime) {
            process(plan.get());
            progress = 0;
        }
        syncCooldown++;
        if (syncCooldown >= 10) {
            syncCooldown = 0;
            notifyUpdate();
        } else {
            setChanged();
        }
    }

    public ItemInteractionResult useItem(Player player, InteractionHand hand, ItemStack heldStack) {
        if (level == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!heldStack.isEmpty() && (heldStack.is(AllNuclearItems.URANIUM.get()) || heldStack.is(AllNuclearItems.RAW_URANIUM.get()))) {
            ItemStack single = heldStack.copyWithCount(1);
            ItemStack remainder = inventory.insertItem(INPUT_SLOT, single, level.isClientSide);
            if (remainder.isEmpty()) {
                if (!level.isClientSide && !player.getAbilities().instabuild) {
                    heldStack.shrink(1);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        if (heldStack.isEmpty()) {
            ItemStack extracted = inventory.extractItem(OUTPUT_SLOT, 64, level.isClientSide);
            if (extracted.isEmpty() && player.isShiftKeyDown()) {
                extracted = inventory.extractItem(INPUT_SLOT, 64, level.isClientSide);
            }
            if (!extracted.isEmpty()) {
                if (!level.isClientSide) {
                    player.setItemInHand(hand, extracted);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public IItemHandler getItemHandler(Direction direction) {
        return direction == Direction.DOWN ? outputHandler : inputHandler;
    }

    @Override
    public float calculateStressApplied() {
        return 40.0f;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        addCentrifugeTooltip(tooltip);
        return true;
    }

    private void addCentrifugeTooltip(List<Component> tooltip) {
        tooltip.add(Component.literal("Centrifuge").withStyle(ChatFormatting.GOLD));
        if (Math.abs(getSpeed()) < MIN_SPEED) {
            tooltip.add(Component.literal("  Status: Waiting for speed").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("  Required: " + (int) MIN_SPEED + " RPM").withStyle(ChatFormatting.DARK_GRAY));
        } else if (getCurrentPlan().isEmpty()) {
            tooltip.add(Component.literal("  Status: Missing valid uranium").withStyle(ChatFormatting.YELLOW));
        } else if (!canOutput(getCurrentPlan().get())) {
            tooltip.add(Component.literal("  Status: Output blocked").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.literal("  Status: Enriching").withStyle(ChatFormatting.GREEN));
        }
        tooltip.add(Component.literal(String.format("  Speed: %.1f RPM", Math.abs(getSpeed()))).withStyle(ChatFormatting.GRAY));

        int percent = Math.round(getProgressPercent() * 100.0f);
        tooltip.add(Component.literal("  Progress: " + makeProgressBar(percent) + " " + percent + "%").withStyle(ChatFormatting.GRAY));

        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        if (!input.isEmpty()) {
            tooltip.add(Component.literal("  Input: ").withStyle(ChatFormatting.GRAY)
                    .append(input.getHoverName().copy().withStyle(ChatFormatting.WHITE)));
            if (input.is(AllNuclearItems.URANIUM.get())) {
                tooltip.add(Component.literal(String.format("  Input enrichment: %.2f%%", UraniumItem.getEnrichment(input))).withStyle(ChatFormatting.GRAY));
            }
        }

        ItemStack output = inventory.getStackInSlot(OUTPUT_SLOT);
        if (!output.isEmpty()) {
            tooltip.add(Component.literal("  Output: ").withStyle(ChatFormatting.GRAY)
                    .append(output.getHoverName().copy().withStyle(ChatFormatting.WHITE)));
            tooltip.add(Component.literal(String.format("  Output enrichment: %.2f%%", UraniumItem.getEnrichment(output))).withStyle(ChatFormatting.GRAY));
        }
    }

    private Optional<ProcessingPlan> getCurrentPlan() {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            return Optional.empty();
        }

        Optional<CentrifugingRecipe> dataRecipe = getCurrentRecipe().map(RecipeHolder::value);
        if (dataRecipe.isPresent()) {
            CentrifugingRecipe recipe = dataRecipe.get();
            return Optional.of(new ProcessingPlan(recipe.assemble(new SingleRecipeInput(input), level.registryAccess()), recipe.processingTime()));
        }

        if (input.is(AllNuclearItems.RAW_URANIUM.get())) {
            ItemStack output = new ItemStack(AllNuclearItems.URANIUM.get());
            UraniumItem.setEnrichment(output, NATURAL_OUTPUT);
            return Optional.of(new ProcessingPlan(output, RAW_PROCESSING_TIME));
        }

        if (input.is(AllNuclearItems.URANIUM.get())) {
            float current = UraniumItem.getEnrichment(input);
            if (current >= MAX_ENRICHMENT) {
                return Optional.empty();
            }
            ItemStack output = new ItemStack(AllNuclearItems.URANIUM.get());
            UraniumItem.setEnrichment(output, Math.min(MAX_ENRICHMENT, current + ENRICHMENT_STEP));
            return Optional.of(new ProcessingPlan(output, ENRICHING_PROCESSING_TIME));
        }

        return Optional.empty();
    }

    private Optional<RecipeHolder<CentrifugingRecipe>> getCurrentRecipe() {
        if (level == null) {
            return Optional.empty();
        }
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(
                AllNuclearRecipes.CENTRIFUGING_TYPE.get(),
                new SingleRecipeInput(input),
                level
        );
    }

    private boolean canOutput(ProcessingPlan plan) {
        ItemStack output = plan.output();
        ItemStack currentOutput = inventory.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return false;
        }
        if (currentOutput.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(currentOutput, output)
                && currentOutput.getCount() < currentOutput.getMaxStackSize();
    }

    private void process(ProcessingPlan plan) {
        ItemStack input = inventory.extractItem(INPUT_SLOT, 1, false);
        if (input.isEmpty()) {
            return;
        }

        ItemStack output = plan.output().copy();
        output.setCount(1);
        ItemStack remainder = inventory.insertItem(OUTPUT_SLOT, output, false);
        if (!remainder.isEmpty()) {
            inventory.insertItem(INPUT_SLOT, input, false);
        } else {
            notifyUpdate();
        }
    }

    private int getProcessingSpeed() {
        return Math.max(1, (int) (Math.abs(getSpeed()) / MIN_SPEED));
    }

    @Override
    public void clearContent() {
        inventory.setStackInSlot(INPUT_SLOT, ItemStack.EMPTY);
        inventory.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putInt("progress", progress);
        tag.putInt("processing_time", processingTime);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        progress = tag.getInt("progress");
        processingTime = tag.getInt("processing_time");
    }

    public float getProgressPercent() {
        return processingTime <= 0 ? 0.0f : Math.min(1.0f, (float) progress / processingTime);
    }

    private String makeProgressBar(int percent) {
        int bars = 10;
        int filled = Math.max(0, Math.min(bars, Math.round(percent / 10.0f)));
        return "[" + "|".repeat(filled) + ".".repeat(bars - filled) + "]";
    }

    private record ProcessingPlan(ItemStack output, int processingTime) {}

    private class SlotView implements IItemHandlerModifiable {
        private final int slot;
        private final boolean allowInsert;
        private final boolean allowExtract;

        private SlotView(int slot, boolean allowInsert, boolean allowExtract) {
            this.slot = slot;
            this.allowInsert = allowInsert;
            this.allowExtract = allowExtract;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (slot == 0) {
                inventory.setStackInSlot(this.slot, stack);
            }
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? inventory.getStackInSlot(this.slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || !allowInsert) {
                return stack;
            }
            return inventory.insertItem(this.slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 0 || !allowExtract) {
                return ItemStack.EMPTY;
            }
            return inventory.extractItem(this.slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? inventory.getSlotLimit(this.slot) : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && allowInsert && inventory.isItemValid(this.slot, stack);
        }
    }
}
