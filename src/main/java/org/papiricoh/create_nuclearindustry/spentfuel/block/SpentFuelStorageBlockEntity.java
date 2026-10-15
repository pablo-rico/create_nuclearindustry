package org.papiricoh.create_nuclearindustry.spentfuel.block;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitter;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitters;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.spentfuel.CoolingGrade;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuel;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelText;

import java.util.List;

/**
 * Nine cells holding one spent assembly each, open to hoppers and funnels. Once a second it brings
 * its assemblies' cooling grades up to date from the game time (so they catch up after the chunk
 * was unloaded) and irradiates its surroundings through whatever shields the store.
 */
public abstract class SpentFuelStorageBlockEntity extends SmartBlockEntity implements RadiationEmitter, IHaveGoggleInformation {
    public static final int SLOTS = 9;
    protected static final int UPDATE_INTERVAL = 20;

    protected final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return accepts(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            contentsChanged = true;
            setChanged();
        }
    };

    private boolean contentsChanged = true;
    private int timer;
    /** Dose per second of the contents with nothing around them. */
    private double bareEmission;
    private int loaded;
    @Nullable
    private CoolingGrade hottest;

    protected SpentFuelStorageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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
        if (level != null && !level.isClientSide) {
            ItemHelper.dropContents(level, worldPosition, inventory);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (++timer >= UPDATE_INTERVAL || contentsChanged) {
            timer = 0;
            update();
        }
    }

    /** One update: grades, emission and whatever the subclass simulates. Public so tests can drive it. */
    public void update() {
        if (level == null) {
            return;
        }
        refreshContents(level.getGameTime());
        onUpdate(level.getGameTime());
    }

    /** Brings every assembly's grade up to date and re-reads what the store holds. */
    protected void refreshContents(long gameTime) {
        boolean changed = contentsChanged;
        contentsChanged = false;
        double emission = 0;
        int count = 0;
        CoolingGrade hot = null;
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            changed |= SpentFuel.refresh(stack, gameTime);
            emission += RadiationHandler.stackRate(stack);
            count += stack.getCount();
            CoolingGrade grade = SpentFuel.gradeOf(stack);
            if (hot == null || grade.isHotterThan(hot)) {
                hot = grade;
            }
        }
        bareEmission = emission;
        if (changed || count != loaded || hot != hottest) {
            loaded = count;
            hottest = hot;
            setChanged();
            sendData();
        }
    }

    /** Called once a second after the contents are refreshed. */
    protected void onUpdate(long gameTime) {}

    /** Whether {@code stack} may go in; only spent assemblies by default. */
    public boolean accepts(ItemStack stack) {
        return SpentFuel.isSpentFuel(stack);
    }

    /** Fraction of the contents' radiation that gets out of the store itself. */
    protected abstract double containment();

    /** Puts as much of {@code stack} in as fits and returns the rest. */
    public ItemStack insert(ItemStack stack) {
        return ItemHandlerHelper.insertItem(inventory, stack, false);
    }

    /** Takes out the last assembly put in, or nothing. */
    public ItemStack extractLast() {
        for (int slot = SLOTS - 1; slot >= 0; slot--) {
            if (!inventory.getStackInSlot(slot).isEmpty()) {
                return inventory.extractItem(slot, 1, false);
            }
        }
        return ItemStack.EMPTY;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public int getLoaded() {
        return loaded;
    }

    @Nullable
    public CoolingGrade getHottest() {
        return hottest;
    }

    public double getBareEmission() {
        return bareEmission;
    }

    // ------------------------------------------------------------------ radiation

    @Override
    public Vec3 radiationOrigin() {
        return Vec3.atCenterOf(getBlockPos());
    }

    @Override
    public double radiationStrength() {
        return bareEmission * containment();
    }

    // ------------------------------------------------------------------ goggles and saving

    /** The block's name, the number of assemblies and the hottest grade. */
    protected void addContentsTooltip(List<Component> tooltip) {
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("     ").append(Component.translatable("create_nuclearindustry.spent_fuel.goggles.assemblies",
                loaded, SLOTS)).withStyle(ChatFormatting.GRAY));
        if (hottest != null) {
            tooltip.add(Component.literal("     ").append(Component.translatable("create_nuclearindustry.spent_fuel.goggles.hottest",
                    SpentFuelText.grade(hottest))).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Inventory", inventory.serializeNBT(registries));
        if (clientPacket) {
            tag.putInt("Loaded", loaded);
            if (hottest != null) {
                tag.putString("Hottest", hottest.id());
            }
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        if (clientPacket) {
            loaded = tag.getInt("Loaded");
            hottest = tag.contains("Hottest") ? CoolingGrade.fromId(tag.getString("Hottest")) : null;
        } else {
            contentsChanged = true;
        }
    }
}
