package org.papiricoh.create_nuclearindustry.enrichment.blockentity;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.createmod.catnip.math.VecHelper;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.enrichment.block.CentrifugeBlock;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;

import java.util.List;

public class CentrifugeBlockEntity extends KineticBlockEntity implements Clearable {
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final float MIN_SPEED = 16.0f;
    private static final int RAW_PROCESSING_TIME = 120;
    private static final int TICKS_PER_PERCENT = 150;
    private static final float ENRICHMENT_STEP = 1.0f;
    private static final int TARGET_UNIT = 5;
    private static final int DEFAULT_TARGET_UNITS = 4; // 20%

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

    public ScrollValueBehaviour targetEnrichment;

    private ItemStack processingStack = ItemStack.EMPTY;
    private int progress;
    private int syncCooldown;

    public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
        this(AllNuclearEntities.CENTRIFUGE.get(), pos, state);
    }

    public CentrifugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        targetEnrichment = new ScrollValueBehaviour(
                Component.translatable("create_nuclearindustry.centrifuge.target_enrichment"),
                this, new CentrifugeValueBox())
                .between(1, 95 / TARGET_UNIT)
                .withFormatter(units -> (units * TARGET_UNIT) + "%")
                .withCallback(units -> notifyUpdate());
        targetEnrichment.setValue(DEFAULT_TARGET_UNITS);
        behaviours.add(targetEnrichment);
    }

    public float getTargetEnrichment() {
        return targetEnrichment.getValue() * (float) TARGET_UNIT;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }

        boolean changed = false;

        if (processingStack.isEmpty()) {
            ItemStack extracted = inventory.extractItem(INPUT_SLOT, 1, false);
            if (!extracted.isEmpty()) {
                processingStack = extracted;
                progress = 0;
                changed = true;
            }
        }

        if (processingStack.isEmpty()) {
            if (progress != 0) {
                progress = 0;
                notifyUpdate();
            }
            return;
        }

        if (isProcessingDone()) {
            ItemStack remainder = inventory.insertItem(OUTPUT_SLOT, processingStack, false);
            if (remainder.getCount() != processingStack.getCount()) {
                changed = true;
            }
            processingStack = remainder;
            progress = 0;
        } else if (Math.abs(getSpeed()) >= MIN_SPEED) {
            progress += getProcessingSpeed();
            changed |= advanceProcessing();
            changed = true;
        }

        if (!changed) {
            return;
        }
        syncCooldown++;
        if (syncCooldown >= 10) {
            syncCooldown = 0;
            notifyUpdate();
        } else {
            setChanged();
        }
    }

    private boolean isProcessingDone() {
        return processingStack.is(AllNuclearItems.URANIUM.get())
                && UraniumItem.getEnrichment(processingStack) >= getTargetEnrichment();
    }

    private boolean advanceProcessing() {
        boolean changed = false;
        if (processingStack.is(AllNuclearItems.RAW_URANIUM.get())) {
            if (progress < RAW_PROCESSING_TIME) {
                return false;
            }
            progress -= RAW_PROCESSING_TIME;
            ItemStack refined = new ItemStack(AllNuclearItems.URANIUM.get());
            UraniumItem.setEnrichment(refined, UraniumItem.NATURAL_ENRICHMENT);
            processingStack = refined;
            changed = true;
        }

        if (!processingStack.is(AllNuclearItems.URANIUM.get())) {
            return changed;
        }

        float target = getTargetEnrichment();
        while (progress >= TICKS_PER_PERCENT && UraniumItem.getEnrichment(processingStack) < target) {
            progress -= TICKS_PER_PERCENT;
            float next = Math.min(target, UraniumItem.getEnrichment(processingStack) + ENRICHMENT_STEP);
            UraniumItem.setEnrichment(processingStack, next);
            changed = true;
        }
        return changed;
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
                if (extracted.isEmpty() && !processingStack.isEmpty()) {
                    extracted = level.isClientSide ? processingStack.copy() : processingStack;
                    if (!level.isClientSide) {
                        processingStack = ItemStack.EMPTY;
                        progress = 0;
                    }
                }
            }
            if (!extracted.isEmpty()) {
                if (!level.isClientSide) {
                    player.setItemInHand(hand, extracted);
                    notifyUpdate();
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
        boolean hasWork = !processingStack.isEmpty() || !inventory.getStackInSlot(INPUT_SLOT).isEmpty();
        if (Math.abs(getSpeed()) < MIN_SPEED) {
            tooltip.add(Component.literal("  Status: Waiting for speed").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("  Required: " + (int) MIN_SPEED + " RPM").withStyle(ChatFormatting.DARK_GRAY));
        } else if (!hasWork) {
            tooltip.add(Component.literal("  Status: Missing uranium").withStyle(ChatFormatting.YELLOW));
        } else if (isProcessingDone()) {
            tooltip.add(Component.literal("  Status: Output blocked").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.literal("  Status: Enriching").withStyle(ChatFormatting.GREEN));
        }
        tooltip.add(Component.literal(String.format("  Speed: %.1f RPM", Math.abs(getSpeed()))).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(String.format("  Target: %.0f%%", getTargetEnrichment())).withStyle(ChatFormatting.AQUA));

        if (!processingStack.isEmpty()) {
            tooltip.add(Component.literal("  Processing: ").withStyle(ChatFormatting.GRAY)
                    .append(processingStack.getHoverName().copy().withStyle(ChatFormatting.WHITE)));
            if (processingStack.is(AllNuclearItems.URANIUM.get())) {
                float current = UraniumItem.getEnrichment(processingStack);
                int percent = Math.round(Math.min(1.0f, current / getTargetEnrichment()) * 100.0f);
                tooltip.add(Component.literal(String.format("  Enrichment: %.2f%%", current)).withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal("  Progress: " + makeProgressBar(percent) + " " + percent + "%").withStyle(ChatFormatting.GRAY));
            }
        }

        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        if (!input.isEmpty()) {
            tooltip.add(Component.literal("  Queued: " + input.getCount() + "x ").withStyle(ChatFormatting.GRAY)
                    .append(input.getHoverName().copy().withStyle(ChatFormatting.WHITE)));
        }

        ItemStack output = inventory.getStackInSlot(OUTPUT_SLOT);
        if (!output.isEmpty()) {
            tooltip.add(Component.literal("  Output: " + output.getCount() + "x ").withStyle(ChatFormatting.GRAY)
                    .append(output.getHoverName().copy().withStyle(ChatFormatting.WHITE)));
            tooltip.add(Component.literal(String.format("  Output enrichment: %.2f%%", UraniumItem.getEnrichment(output))).withStyle(ChatFormatting.GRAY));
        }
    }

    private int getProcessingSpeed() {
        return Math.max(1, (int) (Math.abs(getSpeed()) / MIN_SPEED));
    }

    @Override
    public void clearContent() {
        inventory.setStackInSlot(INPUT_SLOT, ItemStack.EMPTY);
        inventory.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
        processingStack = ItemStack.EMPTY;
        progress = 0;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("inventory", inventory.serializeNBT(registries));
        if (!processingStack.isEmpty()) {
            tag.put("processing", processingStack.save(registries));
        }
        tag.putInt("progress", progress);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        processingStack = tag.contains("processing")
                ? ItemStack.parseOptional(registries, tag.getCompound("processing"))
                : ItemStack.EMPTY;
        progress = tag.getInt("progress");
    }

    private String makeProgressBar(int percent) {
        int bars = 10;
        int filled = Math.max(0, Math.min(bars, Math.round(percent / 10.0f)));
        return "[" + "|".repeat(filled) + ".".repeat(bars - filled) + "]";
    }

    private static class CentrifugeValueBox extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 15.5);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            Direction facing = state.getValue(CentrifugeBlock.FACING);
            return direction.getAxis() != facing.getAxis() || direction == facing;
        }
    }

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
