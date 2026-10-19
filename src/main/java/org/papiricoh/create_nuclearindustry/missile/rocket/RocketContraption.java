package org.papiricoh.create_nuclearindustry.missile.rocket;

import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.IntPredicate;

/**
 * A silo rocket as Create moves it: the column of sections standing on the silo floor and whatever is glued or attached to it,
 * captured the way a piston captures what it pushes. The silo floor is local block 0, so the tail's bottom centre is local
 * {@code (0.5, 1, 0.5)}, the flight's own point ({@link RocketPose}). The silo itself never moves with it: walls, ports and the
 * controller are unmovable to it. Each block remembers the stage it falls with ({@link #stageOf}; the payload is the stage after
 * the last). Never placed back in the world: it is only shown, by a {@link RocketContraptionEntity} or a falling
 * {@link RocketDebrisEntity}.
 */
public class RocketContraption extends Contraption {
    /** Sections in the column, bottom to top; set before {@link #assemble}. */
    private int height;
    /** Each captured block's stage; the payload (guidance, warhead and what rides on them) is {@code stages}. */
    private final Map<BlockPos, Integer> stageOf = new HashMap<>();

    public RocketContraption() {
    }

    public RocketContraption(int height) {
        this.height = height;
    }

    @Override
    public ContraptionType getType() {
        return RocketRegistry.ROCKET_CONTRAPTION.get();
    }

    /** Captures the column standing on {@code floor} (its local block 0) and what is stuck to it. */
    @Override
    public boolean assemble(Level level, BlockPos floor) throws AssemblyException {
        return searchMovedStructure(level, floor, null);
    }

    @Override
    protected boolean addToInitialFrontier(Level level, BlockPos pos, Direction forcedDirection, Queue<BlockPos> frontier) {
        for (int i = 1; i <= height; i++) frontier.add(pos.above(i));
        return true;
    }

    /** The silo is no part of its rocket. */
    @Override
    protected boolean movementAllowed(BlockState state, Level level, BlockPos pos) {
        if (isSilo(state)) return false;
        return super.movementAllowed(state, level, pos);
    }

    /** A block of the silo itself: wall, floor, fuel port or controller. */
    public static boolean isSilo(BlockState state) {
        return state.is(SiloRegistry.SILO_WALLS) || state.is(SiloRegistry.CONTROLLER.get());
    }

    @Override
    public boolean canBeStabilized(Direction facing, BlockPos localPos) {
        return false;
    }

    public int height() {
        return height;
    }

    /** Captured blocks by their local position. */
    public Map<BlockPos, StructureBlockInfo> blocks() {
        return getBlocks();
    }

    public Map<BlockPos, Integer> stageOf() {
        return stageOf;
    }

    /** The stage block {@code local} falls with, or {@code fallback} for one not mapped. */
    public int stageOf(BlockPos local, int fallback) {
        return stageOf.getOrDefault(local, fallback);
    }

    /**
     * Gives every block its stage from where it sits: a block at column index {@code y - 1} belongs to the last stage starting at
     * or below it, or to the payload from {@code payloadBottom} up.
     */
    public void assignStages(List<Integer> stageStarts, int payloadBottom) {
        stageOf.clear();
        for (BlockPos local : getBlocks().keySet()) {
            int index = local.getY() - 1;
            int stage = stageStarts.size();
            if (index < payloadBottom) {
                stage = 0;
                for (int s = 0; s < stageStarts.size(); s++) if (stageStarts.get(s) <= index) stage = s;
            }
            stageOf.put(local.immutable(), stage);
        }
    }

    /**
     * What each section of the rocket would spill if it were ever broken: emptied, so only the flight carries the warhead and
     * the propellant. Actors and interactions are dropped too: a rocket shown flying never drills, harvests or opens anything.
     */
    public void sanitize() {
        for (Map.Entry<BlockPos, StructureBlockInfo> entry : getBlocks().entrySet()) {
            StructureBlockInfo info = entry.getValue();
            if (info.nbt() != null) {
                CompoundTag nbt = info.nbt().copy();
                stripContents(nbt);
                entry.setValue(new StructureBlockInfo(info.pos(), info.state(), nbt));
            }
            CompoundTag update = updateTags.get(entry.getKey());
            if (update != null) stripContents(update);
        }
        getActors().clear();
        getInteractors().clear();
        getSeats().clear();
    }

    private static void stripContents(CompoundTag nbt) {
        nbt.remove("Contents");
        nbt.remove("Fuel");
        nbt.remove("Oxidizer");
    }

    /** A copy holding only the blocks whose stage passes {@code keep}, for showing part of the rocket. */
    public RocketContraption filtered(IntPredicate keep, HolderLookup.Provider registries, Level level) {
        RocketContraption copy = (RocketContraption) Contraption.fromNBT(level, writeNBT(registries, false), false);
        copy.getBlocks().keySet().removeIf(pos -> !keep.test(stageOf(pos, Integer.MAX_VALUE)));
        copy.updateTags.keySet().removeIf(pos -> !copy.getBlocks().containsKey(pos));
        copy.stageOf.keySet().removeIf(pos -> !copy.getBlocks().containsKey(pos));
        copy.getActors().clear();
        copy.getInteractors().clear();
        return copy;
    }

    @Override
    public CompoundTag writeNBT(HolderLookup.Provider registries, boolean spawnPacket) {
        CompoundTag tag = super.writeNBT(registries, spawnPacket);
        tag.putInt("Height", height);
        ListTag stages = new ListTag();
        stageOf.forEach((pos, stage) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", pos.asLong());
            entry.putInt("Stage", stage);
            stages.add(entry);
        });
        tag.put("StageOf", stages);
        return tag;
    }

    @Override
    public void readNBT(Level level, CompoundTag tag, boolean spawnData) {
        super.readNBT(level, tag, spawnData);
        height = Math.max(0, Math.min(32, tag.getInt("Height")));
        stageOf.clear();
        for (Tag t : tag.getList("StageOf", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            BlockPos pos = BlockPos.of(entry.getLong("Pos"));
            if (getBlocks().containsKey(pos)) stageOf.put(pos, entry.getInt("Stage"));
        }
    }
}
