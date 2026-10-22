package org.papiricoh.create_nuclearindustry.radiation.decontamination;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;

import java.util.List;
import java.util.Locale;

/**
 * Once a second, while it turns and has water, sprays it over the ground of its chunk and the
 * chunks around it, washing up to {@code decontamination.sprinklerRate} rad/s off each (more with
 * speed up to 256 RPM). What it washes off is collected as contaminated water in its second tank,
 * for pipes to take to a centrifuge; with that tank full, or the water gone, it stops. Whoever
 * stands in the spray gets their dose rinsed off, as under a decontamination shower.
 * <p>
 * Pipes fill the water tank from any side but the shaft's and drain the contaminated one.
 */
public class DecontaminationSprinklerBlockEntity extends KineticBlockEntity {
    public static final float RATED_SPEED = 256.0f;
    public static final int CAPACITY = 2000;
    private static final int INTERVAL = 20;
    private static final String KEY = "create_nuclearindustry.decontamination_sprinkler.";

    /** Why it is or is not spraying, shown on the goggles. */
    public enum Status { SPRAYING, NO_ROTATION, NO_WATER, FULL }

    private final FluidTank water = new FluidTank(CAPACITY, stack -> stack.getFluid().is(FluidTags.WATER)
            && !stack.is(AllNuclearFluids.CONTAMINATED_WATER.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank contaminated = new FluidTank(CAPACITY, stack -> stack.is(AllNuclearFluids.CONTAMINATED_WATER.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IFluidHandler pipes = new Pipes();

    private Status status = Status.NO_ROTATION;
    private float lastRemoved;
    /** Contaminated water owed to the tank, mB, below one. */
    private double owed;
    private int timer;
    /** Both tank levels as last sent to clients. */
    private int syncedTanks = -1;

    public DecontaminationSprinklerBlockEntity(BlockPos pos, BlockState state) {
        super(DecontaminationRegistry.SPRINKLER_ENTITY.get(), pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            if (status == Status.SPRAYING) {
                spawnSpray();
            }
            return;
        }
        if (++timer < INTERVAL) {
            return;
        }
        timer = 0;
        Status before = status;
        float removed = lastRemoved;
        spray();
        // Pipes fill and drain the tanks in between: the goggles see their levels once a second.
        int tanks = water.getFluidAmount() * (CAPACITY + 1) + contaminated.getFluidAmount();
        if (before != status || removed != lastRemoved || tanks != syncedTanks) {
            syncedTanks = tanks;
            sendData();
        }
    }

    /** One second of spraying; returns the contamination washed off. Public so tests can drive it. */
    public float spray() {
        lastRemoved = 0;
        if (!(level instanceof ServerLevel serverLevel)) {
            return 0;
        }
        double speed = speedFactor();
        int waterNeeded = Math.max(1, (int) Math.ceil(DecontaminationConfig.sprinklerWaterPerSecond * speed));
        float rate = (float) (DecontaminationConfig.sprinklerRate * speed);
        List<LevelChunk> chunks = chunksInReach(serverLevel);
        if (speed <= 0) {
            status = Status.NO_ROTATION;
            return 0;
        }
        if (water.getFluidAmount() < waterNeeded) {
            status = Status.NO_WATER;
            return 0;
        }
        float[] takes = new float[chunks.size()];
        float planned = 0;
        for (int i = 0; i < takes.length; i++) {
            takes[i] = Math.min(Contamination.read(chunks.get(i)), rate);
            planned += takes[i];
        }
        // Room for all it is about to wash off, or it sprays nothing.
        if (contaminated.getSpace() < Math.ceil(planned * DecontaminationConfig.sprinklerContaminatedPerRad + owed)) {
            status = Status.FULL;
            return 0;
        }
        status = Status.SPRAYING;
        water.drain(waterNeeded, IFluidHandler.FluidAction.EXECUTE);
        for (int i = 0; i < takes.length; i++) {
            if (takes[i] > 0) {
                Contamination.set(chunks.get(i), Contamination.read(chunks.get(i)) - takes[i]);
                lastRemoved += takes[i];
            }
        }
        owed += lastRemoved * DecontaminationConfig.sprinklerContaminatedPerRad;
        int collected = (int) Math.floor(owed);
        if (collected > 0) {
            owed -= collected;
            contaminated.fill(new FluidStack(AllNuclearFluids.CONTAMINATED_WATER.get(), collected), IFluidHandler.FluidAction.EXECUTE);
        }
        float rinse = (float) (DecontaminationConfig.sprinklerShowerPerSecond * speed);
        for (Player player : level.getEntitiesOfClass(Player.class, sprayArea())) {
            RadiationHandler.rinse(player, rinse);
        }
        return lastRemoved;
    }

    /** Where the spray falls: the block, the ring of blocks two out around it, and a little above. */
    public AABB sprayArea() {
        return new AABB(worldPosition).inflate(2, 0, 2).expandTowards(0, 2, 0).expandTowards(0, -1, 0);
    }

    /** 0 to 1 with speed; nothing when overstressed. */
    public double speedFactor() {
        if (isOverStressed()) {
            return 0;
        }
        return Math.min(1.0, Math.abs(getSpeed()) / RATED_SPEED);
    }

    private List<LevelChunk> chunksInReach(ServerLevel level) {
        ChunkPos center = new ChunkPos(worldPosition);
        int radius = DecontaminationConfig.sprinklerRadius;
        List<LevelChunk> chunks = new java.util.ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                LevelChunk chunk = Contamination.loaded(level, center.x + dx, center.z + dz);
                if (chunk != null) {
                    chunks.add(chunk);
                }
            }
        }
        return chunks;
    }

    /** Droplets thrown from the four nozzles as the head turns, falling around the sprinkler. */
    private void spawnSpray() {
        float angle = (float) (level.getGameTime() * Math.abs(getSpeed()) * 0.003);
        for (int nozzle = 0; nozzle < 4; nozzle++) {
            float a = angle + nozzle * Mth.HALF_PI;
            double reach = 0.5 + level.random.nextDouble() * 2.0;
            double x = worldPosition.getX() + 0.5 + Mth.cos(a) * reach;
            double z = worldPosition.getZ() + 0.5 + Mth.sin(a) * reach;
            level.addParticle(ParticleTypes.FALLING_WATER, x, worldPosition.getY() + 1.1 + level.random.nextDouble() * 0.4, z, 0, 0, 0);
            if (level.random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.SPLASH, worldPosition.getX() + 0.5 + Mth.cos(a) * 0.45, worldPosition.getY() + 0.9,
                        worldPosition.getZ() + 0.5 + Mth.sin(a) * 0.45, Mth.cos(a) * 0.2, 0.15, Mth.sin(a) * 0.2);
            }
        }
    }

    @Nullable
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        return side == Direction.DOWN ? null : pipes;
    }

    public FluidTank waterTank() {
        return water;
    }

    public FluidTank contaminatedTank() {
        return contaminated;
    }

    public Status status() {
        return status;
    }

    public float lastRemoved() {
        return lastRemoved;
    }

    @Override
    public float calculateStressApplied() {
        float impact = (float) DecontaminationConfig.sprinklerStress;
        lastStressApplied = impact;
        return impact;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("    ").append(Component.translatable("block.create_nuclearindustry.decontamination_sprinkler"))
                .withStyle(ChatFormatting.AQUA));
        Component state = switch (status) {
            case SPRAYING -> Component.translatable(KEY + "spraying", String.format(Locale.ROOT, "%.2f", lastRemoved))
                    .withStyle(lastRemoved > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY);
            case NO_ROTATION -> Component.translatable(KEY + "no_rotation").withStyle(ChatFormatting.RED);
            case NO_WATER -> Component.translatable(KEY + "no_water").withStyle(ChatFormatting.RED);
            case FULL -> Component.translatable(KEY + "full").withStyle(ChatFormatting.RED);
        };
        tooltip.add(line(state));
        tooltip.add(line(Component.translatable(KEY + "water", water.getFluidAmount(), CAPACITY).withStyle(ChatFormatting.BLUE)));
        tooltip.add(line(Component.translatable(KEY + "contaminated", contaminated.getFluidAmount(), CAPACITY)
                .withStyle(contaminated.getFluidAmount() > 0 ? ChatFormatting.YELLOW : ChatFormatting.GRAY)));
        return true;
    }

    private static Component line(Component text) {
        return Component.literal("     ").append(text);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Water", water.writeToNBT(registries, new CompoundTag()));
        tag.put("Contaminated", contaminated.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Status", status.ordinal());
        tag.putFloat("Removed", lastRemoved);
        tag.putDouble("Owed", owed);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        water.readFromNBT(registries, tag.getCompound("Water"));
        contaminated.readFromNBT(registries, tag.getCompound("Contaminated"));
        Status[] statuses = Status.values();
        status = statuses[Mth.clamp(tag.getInt("Status"), 0, statuses.length - 1)];
        lastRemoved = tag.getFloat("Removed");
        double read = tag.getDouble("Owed");
        owed = Double.isFinite(read) ? Mth.clamp(read, 0, 1) : 0;
    }

    /** Tank 0 takes water in, tank 1 gives contaminated water out. */
    private final class Pipes implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? water.getFluid() : contaminated.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && water.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return water.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return contaminated.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return contaminated.drain(maxDrain, action);
        }
    }
}
