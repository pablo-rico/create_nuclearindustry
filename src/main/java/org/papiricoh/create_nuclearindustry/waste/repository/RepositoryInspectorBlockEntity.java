package org.papiricoh.create_nuclearindustry.waste.repository;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.papiricoh.create_nuclearindustry.radiation.contamination.GeologicalStorage;
import org.papiricoh.create_nuclearindustry.waste.WasteConfig;
import org.papiricoh.create_nuclearindustry.waste.WastePackage;

import java.util.List;
import java.util.Locale;

/**
 * Surveys the waste packages within {@link #RANGE} blocks every ten seconds (and once as soon as
 * it is placed or loaded): how many
 * there are, how many count as stored and the score breakdown of the weakest one, so a player
 * digging a repository sees which barrier is missing. Read with goggles, a Display Link or a
 * comparator (stored share as 0-15). The 17x17x17 box is read a chunk section at a time, and
 * sections whose palette holds no waste package are skipped without reading a block.
 */
public class RepositoryInspectorBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final int RANGE = 8;
    /** Ticks between surveys: repositories change slowly, and each survey assesses every package. */
    public static final int SCAN_INTERVAL = 200;
    /** Chunk sections the box's 2 * RANGE + 1 blocks span along each axis at most (two for RANGE 8). */
    private static final int SECTIONS_PER_AXIS = (2 * RANGE + 1 + 14) / 16 + 1;
    private static final byte UNKNOWN = 0;
    private static final byte MAY_HOLD = 1;
    private static final byte EMPTY = 2;

    private int packages;
    private int stored;
    private BlockPos weakestPos = BlockPos.ZERO;
    private Block weakestBlock = Blocks.AIR;
    private RepositoryQuality.Breakdown weakest = RepositoryQuality.Breakdown.NONE;
    private boolean scanned;
    private int scans;
    private int blocksRead;

    public RepositoryInspectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(SCAN_INTERVAL);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    /**
     * Surveys every {@link #SCAN_INTERVAL} ticks. {@code SmartBlockEntity.initialize} already calls
     * this on the first tick, so a placed or loaded inspector surveys at once without a second call.
     */
    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level != null && !level.isClientSide) {
            scan();
        }
    }

    /**
     * Whether the chunk section holding the block may hold a waste package. Sections whose palette
     * has none, and blocks outside the build height, are skipped: the block-by-block read found
     * nothing there either. Unloaded chunks are read as before. The verdict is worked out once per
     * section and survey and kept in {@code verdicts} ({@link #UNKNOWN}, {@link #MAY_HOLD} or
     * {@link #EMPTY}, one per section of the box, x then y then z), so a section full of drums does
     * not look its chunk and palette up again for every block.
     */
    private boolean mayHoldPackages(int x, int y, int z, byte[] verdicts) {
        int sx = SectionPos.blockToSectionCoord(x) - SectionPos.blockToSectionCoord(worldPosition.getX() - RANGE);
        int sy = SectionPos.blockToSectionCoord(y) - SectionPos.blockToSectionCoord(worldPosition.getY() - RANGE);
        int sz = SectionPos.blockToSectionCoord(z) - SectionPos.blockToSectionCoord(worldPosition.getZ() - RANGE);
        int index = (sx * SECTIONS_PER_AXIS + sy) * SECTIONS_PER_AXIS + sz;
        if (verdicts[index] == UNKNOWN) {
            verdicts[index] = sectionMayHoldPackages(x, y, z) ? MAY_HOLD : EMPTY;
        }
        return verdicts[index] == MAY_HOLD;
    }

    private boolean sectionMayHoldPackages(int x, int y, int z) {
        if (level.isOutsideBuildHeight(y)) {
            return false;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        if (chunk == null) {
            return true;
        }
        LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
        return !section.hasOnlyAir() && section.maybeHas(state -> WastePackage.of(state) != null);
    }

    /** Surveys run since the inspector was loaded; for tests. */
    public int getScans() {
        return scans;
    }

    /** Blocks the last survey read one by one (sections without packages are skipped); for tests. */
    public int getBlocksRead() {
        return blocksRead;
    }

    /** Surveys the packages in range now. Public so tests can drive it. */
    public void scan() {
        if (level == null) {
            return;
        }
        int count = 0;
        int safe = 0;
        double worst = Double.MAX_VALUE;
        BlockPos worstPos = BlockPos.ZERO;
        Block worstBlock = Blocks.AIR;
        RepositoryQuality.Breakdown worstBreakdown = RepositoryQuality.Breakdown.NONE;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int read = 0;
        byte[] verdicts = new byte[SECTIONS_PER_AXIS * SECTIONS_PER_AXIS * SECTIONS_PER_AXIS];
        int minZ = worldPosition.getZ() - RANGE;
        int maxZ = worldPosition.getZ() + RANGE;
        // Same x, then y, then z order as before, so ties still pick the same weakest package.
        for (int dx = -RANGE; dx <= RANGE; dx++) {
            for (int dy = -RANGE; dy <= RANGE; dy++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int x = worldPosition.getX() + dx;
                    int y = worldPosition.getY() + dy;
                    if (!mayHoldPackages(x, y, z, verdicts)) {
                        // None of this section can hold a package: jump to its last block along z.
                        z = Math.min(maxZ, SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(z)) + 15);
                        continue;
                    }
                    at.set(x, y, z);
                    read++;
                    BlockState state = level.getBlockState(at);
                    WastePackage pack = WastePackage.of(state);
                    if (pack == null) {
                        continue;
                    }
                    BlockPos pos = at.immutable();
                    count++;
                    if (GeologicalStorage.isStored(level, state, pos)) {
                        safe++;
                    }
                    RepositoryQuality.Breakdown breakdown = RepositoryProbe.assess(level, pos, pack);
                    if (breakdown.score() < worst) {
                        worst = breakdown.score();
                        worstPos = pos;
                        worstBlock = state.getBlock();
                        worstBreakdown = breakdown;
                    }
                }
            }
        }
        scans++;
        blocksRead = read;
        int signalBefore = signal();
        boolean changed = !scanned || count != packages || safe != stored || !worstBreakdown.equals(weakest) || !worstPos.equals(weakestPos);
        packages = count;
        stored = safe;
        weakestPos = worstPos;
        weakestBlock = worstBlock;
        weakest = worstBreakdown;
        scanned = true;
        if (changed && count >= 8 && safe == count) org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements.awardNearby(level, worldPosition, org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements.WITNESS_RADIUS, org.papiricoh.create_nuclearindustry.advancement.NuclearEvent.REPOSITORY_SEALED);
        if (changed) {
            notifyUpdate();
            if (signal() != signalBefore) {
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
        }
    }

    public int packages() {
        return packages;
    }

    public int stored() {
        return stored;
    }

    public RepositoryQuality.Breakdown weakest() {
        return weakest;
    }

    public BlockPos weakestPos() {
        return weakestPos;
    }

    /** Comparator output: the stored share of the packages in range, 0-15. */
    public int signal() {
        return packages == 0 ? 0 : Math.round(15f * stored / packages);
    }

    /** One line for displays: stored count and the weakest score. */
    public MutableComponent summary() {
        if (packages == 0) {
            return Component.translatable("create_nuclearindustry.repository_inspector.summary_none");
        }
        return Component.translatable("create_nuclearindustry.repository_inspector.summary", stored, packages, percent(weakest.score()));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        String key = "create_nuclearindustry.repository_inspector.";
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()).withStyle(ChatFormatting.GOLD));
        if (packages == 0) {
            tooltip.add(line(Component.translatable(key + "none", RANGE)).withStyle(ChatFormatting.GRAY));
            return true;
        }
        tooltip.add(line(Component.translatable(key + "stored", stored, packages))
                .withStyle(stored == packages ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        if (WasteConfig.legacyDepthRule) {
            tooltip.add(line(Component.translatable(key + "legacy")).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(line(Component.translatable(key + "weakest", weakestBlock.getName(),
                weakestPos.getX(), weakestPos.getY(), weakestPos.getZ())).withStyle(ChatFormatting.GRAY));
        boolean safe = RepositoryQuality.isStored(weakest.score(), WasteConfig.minScore);
        tooltip.add(line(Component.translatable(key + "score", percent(weakest.score()), percent(WasteConfig.minScore)))
                .withStyle(safe ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.add(term(key + "depth", weakest.depth(), WasteConfig.weights.depth()));
        tooltip.add(term(key + "host_rock", weakest.hostRock(), WasteConfig.weights.hostRock()));
        tooltip.add(term(key + "backfill", weakest.backfill(), WasteConfig.weights.backfill()));
        tooltip.add(term(key + "form", weakest.form(), WasteConfig.weights.form()));
        if (weakest.fluidPenalty() > 0) {
            tooltip.add(line(Component.translatable(key + "fluid", percent(weakest.fluidPenalty()))).withStyle(ChatFormatting.RED));
        }
        return true;
    }

    /** One barrier: what it adds out of the most it could. */
    private static Component term(String key, double value, double max) {
        ChatFormatting colour = value >= max - 1e-6 ? ChatFormatting.GREEN : value > 0 ? ChatFormatting.YELLOW : ChatFormatting.RED;
        return line(Component.translatable(key, percent(value), percent(max))).withStyle(colour);
    }

    private static MutableComponent line(Component text) {
        return Component.literal("     ").append(text);
    }

    private static String percent(double fraction) {
        return String.format(Locale.ROOT, "%d", Math.round(fraction * 100));
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Packages", packages);
        tag.putInt("Stored", stored);
        tag.putLong("WeakestPos", weakestPos.asLong());
        tag.putString("WeakestBlock", BuiltInRegistries.BLOCK.getKey(weakestBlock).toString());
        tag.putDouble("Depth", weakest.depth());
        tag.putDouble("HostRock", weakest.hostRock());
        tag.putDouble("Backfill", weakest.backfill());
        tag.putDouble("Form", weakest.form());
        tag.putDouble("Fluid", weakest.fluidPenalty());
        tag.putDouble("Score", weakest.score());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        packages = tag.getInt("Packages");
        stored = tag.getInt("Stored");
        weakestPos = BlockPos.of(tag.getLong("WeakestPos"));
        ResourceLocation block = ResourceLocation.tryParse(tag.getString("WeakestBlock"));
        weakestBlock = block == null ? Blocks.AIR : BuiltInRegistries.BLOCK.get(block);
        weakest = new RepositoryQuality.Breakdown(tag.getDouble("Depth"), tag.getDouble("HostRock"), tag.getDouble("Backfill"),
                tag.getDouble("Form"), tag.getDouble("Fluid"), tag.getDouble("Score"));
    }
}
