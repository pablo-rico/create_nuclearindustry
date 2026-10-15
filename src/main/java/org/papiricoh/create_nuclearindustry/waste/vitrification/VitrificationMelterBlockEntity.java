package org.papiricoh.create_nuclearindustry.waste.vitrification;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitter;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitters;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Melts high-level waste with glass formers into a steel canister of borosilicate glass. It has
 * no heater of its own: a Blaze Burner below has to be at least lit (heated), and a superheated
 * one melts twice as fast. Each kind of ingredient gets its own input slot, so a hopper full of
 * waste cannot crowd out the sand. The waste waiting inside irradiates through the melter's walls.
 *
 * <p>The recipe the melter can run is looked up again only when its contents or its heat change, not
 * every tick. Clients get the melter's state when a melt starts or stops and when its contents
 * change (at most every {@link #SYNC_INTERVAL} ticks), and advance the progress of a melt themselves.
 */
public class VitrificationMelterBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, RadiationEmitter {
    public static final int INPUT_SLOTS = 3;
    public static final int OUTPUT_SLOT = INPUT_SLOTS;
    /** Share of the radiation of the waste inside that gets through the melter's shielding. */
    private static final double SHIELDING_TRANSMISSION = 0.25;
    /** Shortest gap between two updates to clients for contents that changed. */
    private static final int SYNC_INTERVAL = 10;

    private final ItemStackHandler inventory = new ItemStackHandler(INPUT_SLOTS + 1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == OUTPUT_SLOT || acceptsInput(slot, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            recipeStale = true;
            contentsUnsent = true;
        }
    };
    /** Funnels, hoppers and chutes fill the inputs and take the canisters out. */
    private final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == OUTPUT_SLOT ? stack : inventory.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == OUTPUT_SLOT ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot != OUTPUT_SLOT && inventory.isItemValid(slot, stack);
        }
    };

    @Nullable
    private ResourceLocation recipeId;
    private int progress;
    private int duration;
    /** The contents changed since {@link #cachedRecipe} was looked up. */
    private boolean recipeStale = true;
    @Nullable
    private HeatLevel cachedHeat;
    /** What {@link #currentRecipe} found last, null for nothing; valid while neither contents nor heat change. */
    @Nullable
    private RecipeHolder<VitrificationRecipe> cachedRecipe;
    /** The contents changed since the last update to clients. */
    private boolean contentsUnsent;
    private int recipeLookups;
    private int syncsSent;

    public VitrificationMelterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(SYNC_INTERVAL);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

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
    public void destroy() {
        if (org.papiricoh.create_nuclearindustry.integration.sable.ShipAssembly.isMoving()) {
            // Carried to or from a ship: the copy at the new place keeps everything.
            super.destroy();
            return;
        }
        super.destroy();
        if (level != null) {
            ItemHelper.dropContents(level, worldPosition, inventory);
        }
    }

    /** Only ingredients of some vitrification recipe, and each kind of item in one slot only. */
    private boolean acceptsInput(int slot, ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        for (int other = 0; other < INPUT_SLOTS; other++) {
            if (other != slot && ItemStack.isSameItemSameComponents(inventory.getStackInSlot(other), stack)) {
                return false;
            }
        }
        for (RecipeHolder<VitrificationRecipe> holder : WasteRecipeTypes.vitrification(level)) {
            if (holder.value().uses(stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            org.papiricoh.create_nuclearindustry.reactor.visual.ReactorVisualHooks.machineClientTick.accept(this);
            // The server only says when a melt starts and stops: the progress in between runs here.
            if (recipeId != null && progress < duration) {
                progress = Math.min(duration, progress + (heatLevel() == HeatLevel.SEETHING ? 2 : 1));
            }
            return;
        }
        HeatLevel heat = heatLevel();
        RecipeHolder<VitrificationRecipe> recipe = currentRecipe(heat);
        if (recipe == null) {
            if (recipeId != null || progress != 0) {
                recipeId = null;
                progress = 0;
                duration = 0;
                sendData();
            }
            return;
        }
        if (!recipe.id().equals(recipeId)) {
            recipeId = recipe.id();
            progress = 0;
            duration = Math.max(1, recipe.value().getProcessingDuration());
            sendData();
        }
        progress += heat == HeatLevel.SEETHING ? 2 : 1;
        if (progress >= duration) {
            finish(recipe.value());
            recipeId = null;
            progress = 0;
            duration = 0;
            sendData();
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level != null && !level.isClientSide && contentsUnsent) {
            sendData();
        }
    }

    @Override
    public void sendData() {
        if (level != null && !level.isClientSide) {
            contentsUnsent = false;
            syncsSent++;
        }
        super.sendData();
    }

    /** The heat of the block below, read like a basin reads its burner. */
    public HeatLevel heatLevel() {
        return level == null ? HeatLevel.NONE : BasinBlockEntity.getHeatLevelOf(level.getBlockState(worldPosition.below()));
    }

    /**
     * The recipe the melter can run right now: ingredients present, hot enough, room for the result.
     * Every recipe is tried again only when the contents or the heat changed since the last look.
     */
    @Nullable
    private RecipeHolder<VitrificationRecipe> currentRecipe(HeatLevel heat) {
        if (!recipeStale && heat == cachedHeat) {
            return cachedRecipe;
        }
        recipeStale = false;
        cachedHeat = heat;
        cachedRecipe = findRecipe(heat);
        recipeLookups++;
        return cachedRecipe;
    }

    @Nullable
    private RecipeHolder<VitrificationRecipe> findRecipe(HeatLevel heat) {
        List<ItemStack> inputs = inputs();
        if (inputs.stream().allMatch(ItemStack::isEmpty)) {
            return null;
        }
        for (RecipeHolder<VitrificationRecipe> holder : WasteRecipeTypes.vitrification(level)) {
            VitrificationRecipe recipe = holder.value();
            if (recipe.getRequiredHeat().testBlazeBurner(heat) && recipe.consumption(inputs) != null
                    && inventory.insertItem(OUTPUT_SLOT, recipe.getResultItem(level.registryAccess()).copy(), true).isEmpty()) {
                return holder;
            }
        }
        return null;
    }

    private void finish(VitrificationRecipe recipe) {
        int[] taken = recipe.consumption(inputs());
        if (taken == null) {
            return;
        }
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            if (taken[slot] > 0) {
                inventory.extractItem(slot, taken[slot], false);
            }
        }
        for (ItemStack result : recipe.rollResults(level.random)) {
            inventory.insertItem(OUTPUT_SLOT, result, false);
        }
    }

    private List<ItemStack> inputs() {
        List<ItemStack> inputs = new ArrayList<>(INPUT_SLOTS);
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            inputs.add(inventory.getStackInSlot(slot));
        }
        return inputs;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public IItemHandler getAutomationHandler() {
        return automation;
    }

    public boolean isMelting() {
        return recipeId != null;
    }

    /** Updates sent to clients since the melter was loaded; for tests. */
    public int getSyncsSent() {
        return syncsSent;
    }

    /** Times every recipe was tried since the melter was loaded; for tests. */
    public int getRecipeLookups() {
        return recipeLookups;
    }

    /** 0 to 1 through the current melt. */
    public float progress() {
        return duration <= 0 ? 0 : Math.min(1f, progress / (float) duration);
    }

    @Override
    public Vec3 radiationOrigin() {
        return Vec3.atCenterOf(worldPosition);
    }

    @Override
    public double radiationStrength() {
        double rate = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            rate += RadiationHandler.stackRate(inventory.getStackInSlot(slot));
        }
        return rate * SHIELDING_TRANSMISSION;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        String key = "create_nuclearindustry.goggles.vitrification_melter.";
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()).withStyle(ChatFormatting.GOLD));
        HeatLevel heat = heatLevel();
        if (!heat.isAtLeast(HeatLevel.FADING)) {
            tooltip.add(Component.literal("     ").append(Component.translatable(key + "no_heat")).withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.literal("     ").append(Component.translatable(key + (heat == HeatLevel.SEETHING ? "superheated" : "heated")))
                    .withStyle(ChatFormatting.GOLD));
        }
        if (isMelting()) {
            tooltip.add(Component.literal("     ").append(Component.translatable(key + "melting", Math.round(progress() * 100)))
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.literal("     ").append(Component.translatable(key + "idle")).withStyle(ChatFormatting.GRAY));
        }
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                tooltip.add(Component.literal("      " + stack.getCount() + "x ").append(stack.getHoverName())
                        .withStyle(slot == OUTPUT_SLOT ? ChatFormatting.AQUA : ChatFormatting.GRAY));
            }
        }
        return true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Inventory", inventory.serializeNBT(registries));
        if (recipeId != null) {
            tag.putString("Recipe", recipeId.toString());
        }
        tag.putInt("Progress", progress);
        tag.putInt("Duration", duration);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        recipeStale = true;
        recipeId = tag.contains("Recipe") ? ResourceLocation.tryParse(tag.getString("Recipe")) : null;
        progress = tag.getInt("Progress");
        duration = tag.getInt("Duration");
    }
}
