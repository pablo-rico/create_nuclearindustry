package org.papiricoh.create_nuclearindustry.spentfuel.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import org.papiricoh.create_nuclearindustry.multiblock.SealedVolume;
import org.papiricoh.create_nuclearindustry.spentfuel.CoolingGrade;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuel;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelConfig;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelPool;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * A pool rack. Water straight above it is the shield: every block of it cuts the radiation that
 * gets out, so a rack under two or more blocks of water is safe to swim over and an uncovered one
 * is deadly. The pool around it (found with a flood fill through water source blocks) soaks up
 * its decay heat; when the heat outruns the pool's cooling (its volume plus any Pool Coolers, shared
 * between the racks in it) the surplus builds up and boils the surface off, a layer at a time.
 * Open water bigger than the scan limit never boils.
 */
public class SpentFuelRackBlockEntity extends SpentFuelStorageBlockEntity {
    /** Updates between pool scans. */
    private static final int SCAN_EVERY = 5;
    private static final int MAX_DEPTH = 16;
    /** Longest gap, in seconds, the heat balance catches up on after a pause. */
    private static final double MAX_CATCH_UP = 5.0;

    private int waterDepth;
    private double decayHeat;
    private double cooling;
    private boolean boiling;
    private double boilStored;
    private long lastHeatTime = -1;
    private int scanCountdown;
    private Pool pool = Pool.NONE;

    /**
     * The water around a rack. {@code sealed} is false for water bigger than the scan limit (or
     * reaching unloaded chunks); {@code surface} is the top layer that boils off first.
     */
    public record Pool(int volume, boolean sealed, int racks, double coolerCapacity, int topY, List<BlockPos> surface) {
        static final Pool NONE = new Pool(0, false, 1, 0.0, Integer.MIN_VALUE, List.of());
    }

    public SpentFuelRackBlockEntity(BlockPos pos, BlockState state) {
        super(SpentFuelRegistry.SPENT_FUEL_RACK_ENTITY.get(), pos, state);
    }

    @Override
    protected void onUpdate(long gameTime) {
        if (level == null) {
            return;
        }
        double oldHeat = decayHeat;
        double oldCooling = cooling;
        int oldDepth = waterDepth;
        boolean wasBoiling = boiling;

        waterDepth = countWaterAbove();
        double heat = 0;
        for (int slot = 0; slot < SLOTS; slot++) {
            heat += SpentFuel.decayHeat(inventory.getStackInSlot(slot), gameTime);
        }
        decayHeat = heat;
        if (--scanCountdown <= 0) {
            scanPool();
        }
        double seconds = lastHeatTime < 0 ? 1.0 : Mth.clamp((gameTime - lastHeatTime) / 20.0, 0.0, MAX_CATCH_UP);
        lastHeatTime = gameTime;
        applyHeat(seconds);
        updateBlockState();

        if (waterDepth != oldDepth || boiling != wasBoiling || Math.abs(decayHeat - oldHeat) > 0.05
                || Math.abs(cooling - oldCooling) > 0.05) {
            setChanged();
            sendData();
        }
    }

    /**
     * Lets {@code seconds} of decay heat into the pool: the surplus over the cooling builds up, the
     * spare capacity drains it, and enough of it boils off the whole surface layer. Public for tests.
     */
    public void applyHeat(double seconds) {
        cooling = poolCooling();
        if (!pool.sealed()) {
            boilStored = 0;
            boiling = false;
            return;
        }
        boilStored = SpentFuelPool.accumulateBoil(boilStored, decayHeat, cooling, seconds);
        boiling = decayHeat > cooling && pool.topY() >= worldPosition.getY();
        if (boiling) {
            steam(2, 0.02);
        }
        if (SpentFuelPool.boilsLayer(boilStored, pool.surface().size(), SpentFuelConfig.boilEnergyPerBlock)
                && pool.topY() >= worldPosition.getY()) {
            boilStored -= pool.surface().size() * SpentFuelConfig.boilEnergyPerBlock;
            boilOffSurface();
            scanPool();
            waterDepth = countWaterAbove();
        }
    }

    /** This rack's share of what its pool can carry away, in kW. */
    private double poolCooling() {
        if (pool.volume() == 0) {
            return 0.0;
        }
        int water = Math.min(pool.volume(), SpentFuelConfig.poolScanLimit);
        return (SpentFuelPool.passiveCooling(water, SpentFuelConfig.poolCoolingPerBlock) + pool.coolerCapacity())
                / Math.max(1, pool.racks());
    }

    /** Flood-fills the water this rack sits in (or under), counting racks and coolers in it. Public for tests. */
    public void scanPool() {
        scanCountdown = SCAN_EVERY;
        if (level == null) {
            return;
        }
        BlockPos start = isWaterSource(worldPosition) ? worldPosition
                : isWaterSource(worldPosition.above()) ? worldPosition.above() : null;
        if (start == null) {
            pool = Pool.NONE;
            return;
        }
        int[] racks = {start.equals(worldPosition) ? 0 : 1};
        double[] coolers = {0.0};
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        SealedVolume.World water = new SealedVolume.World() {
            @Override
            public boolean known(int x, int y, int z) {
                return !level.isOutsideBuildHeight(y) && level.isLoaded(cursor.set(x, y, z));
            }

            @Override
            public boolean passable(int x, int y, int z) {
                cursor.set(x, y, z);
                if (!isWaterSource(cursor)) {
                    return false;
                }
                if (level.getBlockState(cursor).hasBlockEntity()) {
                    BlockEntity entity = level.getBlockEntity(cursor);
                    if (entity instanceof SpentFuelRackBlockEntity) {
                        racks[0]++;
                    } else if (entity instanceof PoolCoolerBlockEntity cooler) {
                        coolers[0] += cooler.coolingCapacity();
                    }
                }
                return true;
            }

            @Override
            public double wallRating(int x, int y, int z) {
                return 0.0;
            }
        };
        SealedVolume volume = SealedVolume.fill(water, start.getX(), start.getY(), start.getZ(), SpentFuelConfig.poolScanLimit);
        boolean sealed = volume.state() == SealedVolume.State.SEALED;
        List<BlockPos> surface = new ArrayList<>();
        if (sealed) {
            for (int x = volume.minX(); x <= volume.maxX(); x++) {
                for (int z = volume.minZ(); z <= volume.maxZ(); z++) {
                    if (volume.contains(x, volume.maxY(), z)) {
                        surface.add(new BlockPos(x, volume.maxY(), z));
                    }
                }
            }
        }
        pool = new Pool(volume.volume(), sealed, Math.max(1, racks[0]), coolers[0], volume.maxY(), List.copyOf(surface));
    }

    private boolean isWaterSource(BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        return fluid.is(FluidTags.WATER) && fluid.isSource();
    }

    private int countWaterAbove() {
        BlockPos.MutableBlockPos cursor = worldPosition.mutable();
        int depth = 0;
        while (depth < MAX_DEPTH && isWaterSource(cursor.move(0, 1, 0))) {
            depth++;
        }
        return depth;
    }

    /** The whole top layer goes at once, so the water around cannot flow back in as new sources. */
    private void boilOffSurface() {
        for (BlockPos pos : pool.surface()) {
            BlockState state = level.getBlockState(pos);
            if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) {
                level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, false), 3);
            } else if (state.getBlock() instanceof LiquidBlock || state.canBeReplaced()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        level.playSound(null, worldPosition.above(Math.max(1, pool.topY() - worldPosition.getY())), SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS, 1.0f, 0.6f);
        steam(12, 0.08);
    }

    private void steam(int puffs, double speed) {
        if (!(level instanceof ServerLevel server) || pool.surface().isEmpty()) {
            return;
        }
        for (int i = 0; i < puffs; i++) {
            BlockPos at = pool.surface().get(server.random.nextInt(pool.surface().size()));
            server.sendParticles(ParticleTypes.CLOUD, at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, 1, 0.3, 0.1, 0.3, speed);
        }
    }

    private void updateBlockState() {
        BlockState state = getBlockState();
        if (!state.hasProperty(SpentFuelRackBlock.LOAD)) {
            return;
        }
        CoolingGrade hottest = getHottest();
        RackLoad load = getLoaded() == 0 ? RackLoad.EMPTY
                : waterDepth > 0 && hottest != null && hottest.isHotterThan(CoolingGrade.COOLED) ? RackLoad.GLOWING
                : RackLoad.LOADED;
        if (state.getValue(SpentFuelRackBlock.LOAD) != load) {
            level.setBlock(worldPosition, state.setValue(SpentFuelRackBlock.LOAD, load), 3);
        }
    }

    @Override
    protected double containment() {
        return SpentFuelPool.waterShielding(waterDepth, SpentFuelConfig.waterTransmission);
    }

    public int getWaterDepth() {
        return waterDepth;
    }

    public double getDecayHeat() {
        return decayHeat;
    }

    public double getCooling() {
        return cooling;
    }

    public boolean isBoiling() {
        return boiling;
    }

    public Pool getPool() {
        return pool;
    }

    // ------------------------------------------------------------------ goggles and saving

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        addContentsTooltip(tooltip);
        String key = "create_nuclearindustry.spent_fuel.goggles.";
        if (waterDepth > 0) {
            tooltip.add(Component.literal("     ").append(Component.translatable(key + "water_above", waterDepth))
                    .withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.literal("     ").append(Component.translatable(key + "uncovered")).withStyle(ChatFormatting.RED));
        }
        tooltip.add(Component.literal("     ").append(Component.translatable(key + "radiation_out",
                String.format("%.1f", containment() * 100))).withStyle(ChatFormatting.GRAY));
        if (getLoaded() > 0) {
            tooltip.add(Component.literal("     ").append(Component.translatable(key + "decay_heat",
                    String.format("%.1f", decayHeat))).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("     ").append(Component.translatable(key + "pool_cooling",
                    String.format("%.1f", cooling))).withStyle(cooling >= decayHeat ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        if (boiling) {
            tooltip.add(Component.literal("     ").append(Component.translatable(key + "boiling")).withStyle(ChatFormatting.RED));
        }
        return true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (clientPacket) {
            tag.putInt("WaterDepth", waterDepth);
            tag.putDouble("DecayHeat", decayHeat);
            tag.putDouble("Cooling", cooling);
            tag.putBoolean("Boiling", boiling);
        } else {
            tag.putDouble("BoilStored", boilStored);
            tag.putLong("LastHeat", lastHeatTime);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (clientPacket) {
            waterDepth = tag.getInt("WaterDepth");
            decayHeat = tag.getDouble("DecayHeat");
            cooling = tag.getDouble("Cooling");
            boiling = tag.getBoolean("Boiling");
        } else {
            boilStored = tag.getDouble("BoilStored");
            lastHeatTime = tag.contains("LastHeat") ? tag.getLong("LastHeat") : -1;
        }
    }
}
