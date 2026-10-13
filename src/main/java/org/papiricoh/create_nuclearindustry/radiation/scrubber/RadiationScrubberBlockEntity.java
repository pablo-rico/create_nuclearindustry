package org.papiricoh.create_nuclearindustry.radiation.scrubber;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitter;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitters;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Once a second, removes contamination from its chunk and the chunks around it, most
 * contaminated first, at a rate that grows with speed up to 256 RPM. What it absorbs comes out
 * as low-level waste in its one output slot; full, it stops. The waste inside irradiates.
 * Scrubbers saved with the old (intermediate-level) waste inside stay full until it is taken out.
 */
public class RadiationScrubberBlockEntity extends KineticBlockEntity implements RadiationEmitter {
    public static final float RATED_SPEED = 256.0f;
    private static final int INTERVAL = 20;
    /** Dose per second from each waste item waiting inside. */
    private static final double EMISSION_PER_WASTE = 0.1;

    private final ItemStackHandler output = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(WasteRegistry.LOW_LEVEL_WASTE.get()) || stack.is(AllNuclearItems.RADIOACTIVE_WASTE.get());
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** Funnels and hoppers can only take the waste out. */
    private final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return output.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return output.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return output.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };

    private float absorbed;
    private float lastRemoved;
    private int timer;

    public RadiationScrubberBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.RADIATION_SCRUBBER.get(), pos, state);
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
        if (level == null || level.isClientSide || ++timer < INTERVAL) {
            return;
        }
        timer = 0;
        float before = lastRemoved;
        scrub();
        if (before != lastRemoved) {
            sendData();
        }
    }

    /** One second of scrubbing; returns the contamination removed. Public so tests can drive it. */
    public float scrub() {
        lastRemoved = 0;
        if (!(level instanceof ServerLevel serverLevel) || isFull()) {
            return 0;
        }
        float budget = (float) (Config.scrubberRate * speedFactor());
        if (budget <= 0) {
            return 0;
        }
        for (LevelChunk chunk : chunksByContamination(serverLevel)) {
            float value = Contamination.read(chunk);
            if (value <= 0 || budget <= 0) {
                break;
            }
            float taken = Math.min(value, budget);
            Contamination.set(chunk, value - taken);
            budget -= taken;
            lastRemoved += taken;
        }
        absorbed += lastRemoved;
        while (absorbed >= Config.scrubberWastePer && !isFull()) {
            absorbed -= (float) Config.scrubberWastePer;
            output.insertItem(0, new ItemStack(WasteRegistry.LOW_LEVEL_WASTE.get()), false);
        }
        return lastRemoved;
    }

    /** 0 to 1 with speed; nothing when overstressed. */
    public double speedFactor() {
        if (isOverStressed()) {
            return 0;
        }
        return Math.min(1.0, Math.abs(getSpeed()) / RATED_SPEED);
    }

    /** No room for one more low-level waste: the slot is full, or still holds another kind of waste. */
    public boolean isFull() {
        return !output.insertItem(0, new ItemStack(WasteRegistry.LOW_LEVEL_WASTE.get()), true).isEmpty();
    }

    private List<LevelChunk> chunksByContamination(ServerLevel level) {
        ChunkPos center = new ChunkPos(worldPosition);
        int radius = Config.scrubberRadius;
        List<LevelChunk> chunks = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                LevelChunk chunk = Contamination.loaded(level, center.x + dx, center.z + dz);
                if (chunk != null && Contamination.read(chunk) > 0) {
                    chunks.add(chunk);
                }
            }
        }
        chunks.sort(Comparator.comparingDouble(chunk -> -Contamination.read(chunk)));
        return chunks;
    }

    public ItemStackHandler getOutput() {
        return output;
    }

    public IItemHandler getAutomationHandler() {
        return automation;
    }

    public float getLastRemoved() {
        return lastRemoved;
    }

    @Override
    public float calculateStressApplied() {
        float impact = (float) Config.scrubberStress;
        lastStressApplied = impact;
        return impact;
    }

    @Override
    public Vec3 radiationOrigin() {
        return Vec3.atCenterOf(worldPosition);
    }

    @Override
    public double radiationStrength() {
        return output.getStackInSlot(0).getCount() * EMISSION_PER_WASTE;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("Radiation Scrubber").withStyle(ChatFormatting.GREEN));
        int waste = output.getStackInSlot(0).getCount();
        if (isFull()) {
            tooltip.add(Component.literal("  Full: take the waste out").withStyle(ChatFormatting.RED));
        } else if (speedFactor() <= 0) {
            tooltip.add(Component.literal("  Needs rotation").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.literal(String.format("  Absorbing %.2f rad/s each second", lastRemoved))
                    .withStyle(lastRemoved > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        }
        tooltip.add(Component.literal("  Waste inside: " + waste).withStyle(waste > 0 ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
        return true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Output", output.serializeNBT(registries));
        tag.putFloat("Absorbed", absorbed);
        tag.putFloat("Removed", lastRemoved);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        output.deserializeNBT(registries, tag.getCompound("Output"));
        absorbed = tag.getFloat("Absorbed");
        lastRemoved = tag.getFloat("Removed");
    }
}
