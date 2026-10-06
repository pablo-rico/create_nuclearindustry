package org.papiricoh.create_nuclearindustry.rtg;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitter;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitters;

import java.util.List;

/**
 * Turns the decay heat of one loaded pellet into a steady 16 RPM. Capacity follows the pellet's
 * remaining charge, which halves every half-life (also while the chunk is unloaded); below a
 * quarter the pellet comes out spent and the next one goes in. Unshielded, it irradiates.
 */
public class RtgBlockEntity extends GeneratingKineticBlockEntity implements RadiationEmitter {
    public static final float RPM = 16.0f;
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    private static final int UPDATE_INTERVAL = 20;

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == INPUT_SLOT ? stack.getItem() instanceof RtgPelletItem : stack.is(AllNuclearItems.DEPLETED_RTG_PELLET.get());
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** Hoppers and pipes: pellets in, spent pellets out. */
    private final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == INPUT_SLOT ? inventory.insertItem(slot, stack, simulate) : stack;
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
            return slot == INPUT_SLOT && inventory.isItemValid(slot, stack);
        }
    };

    @Nullable
    private RtgFuel fuel;
    private double charge;
    private long lastDecayTime = -1;
    private int timer;
    private float generatedSpeed;
    private float generatedCapacity;

    public RtgBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.RTG.get(), pos, state);
    }

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
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (++timer >= UPDATE_INTERVAL) {
            timer = 0;
            long now = level.getGameTime();
            if (fuel != null && lastDecayTime >= 0) {
                advanceDecay(now - lastDecayTime);
            }
            lastDecayTime = now;
            refuel();
            refreshGeneration();
        }
    }

    /** Lets {@code ticks} of decay pass at once. Public so tests can age a pellet. */
    public void advanceDecay(long ticks) {
        if (fuel != null && ticks > 0) {
            charge = fuel.decay(charge, ticks);
        }
    }

    /** Ejects a spent pellet and loads the next one. */
    public void refuel() {
        if (fuel != null && charge < RtgFuel.SPENT_BELOW) {
            ItemStack spent = new ItemStack(AllNuclearItems.DEPLETED_RTG_PELLET.get());
            if (!inventory.insertItem(OUTPUT_SLOT, spent, true).isEmpty()) {
                return;
            }
            inventory.insertItem(OUTPUT_SLOT, spent, false);
            fuel = null;
            charge = 0;
        }
        if (fuel == null) {
            ItemStack next = inventory.extractItem(INPUT_SLOT, 1, false);
            if (next.getItem() instanceof RtgPelletItem pellet) {
                fuel = pellet.fuel();
                charge = RtgPelletItem.charge(next);
            }
        }
    }

    public void refreshGeneration() {
        float speed = fuel != null ? signedRpm() : 0;
        float capacity = fuel != null ? (float) (fuel.capacityPerRpm() * charge) : 0;
        if (speed != generatedSpeed) {
            generatedSpeed = speed;
            generatedCapacity = capacity;
            updateGeneratedRotation();
        } else if (Math.abs(capacity - generatedCapacity) > 0.02f * Math.max(1, generatedCapacity)) {
            generatedCapacity = capacity;
            if (hasNetwork()) {
                notifyStressCapacityChange(generatedCapacity);
            }
        }
        setChanged();
    }

    private float signedRpm() {
        Direction facing = getBlockState().getValue(BlockStateProperties.FACING);
        return facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? -RPM : RPM;
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

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public IItemHandler getAutomationHandler() {
        return automation;
    }

    @Nullable
    public RtgFuel getFuel() {
        return fuel;
    }

    public double getCharge() {
        return charge;
    }

    // ------------------------------------------------------------------ radiation

    @Override
    public Vec3 radiationOrigin() {
        return Vec3.atCenterOf(getBlockPos());
    }

    @Override
    public double radiationStrength() {
        return fuel == null ? 0.0 : fuel.emission() * charge;
    }

    // ------------------------------------------------------------------ goggles and saving

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable("block.create_nuclearindustry.rtg")).withStyle(ChatFormatting.GOLD));
        if (fuel == null) {
            tooltip.add(Component.literal("     No pellet loaded").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.literal("     ").append(Component.translatable("item.create_nuclearindustry." + fuel.id() + "_rtg_pellet"))
                    .append(String.format(": %.0f%%", charge * 100)).withStyle(ChatFormatting.GREEN));
        }
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        return true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Inventory", inventory.serializeNBT(registries));
        if (fuel != null) {
            tag.putString("Fuel", fuel.id());
            tag.putDouble("Charge", charge);
        }
        tag.putLong("LastDecay", lastDecayTime);
        tag.putFloat("Speed", generatedSpeed);
        tag.putFloat("Capacity", generatedCapacity);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        fuel = tag.contains("Fuel") ? RtgFuel.fromId(tag.getString("Fuel")) : null;
        charge = fuel == null ? 0 : tag.getDouble("Charge");
        lastDecayTime = tag.contains("LastDecay") ? tag.getLong("LastDecay") : -1;
        generatedSpeed = tag.getFloat("Speed");
        generatedCapacity = tag.getFloat("Capacity");
    }
}
