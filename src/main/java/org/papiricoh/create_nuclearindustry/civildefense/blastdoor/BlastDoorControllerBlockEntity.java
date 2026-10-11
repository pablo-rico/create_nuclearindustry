package org.papiricoh.create_nuclearindustry.civildefense.blastdoor;

import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseConfig;

import java.util.List;
import java.util.Objects;

/**
 * Drives a blast door. Rotation wins while there is any (positive speed opens, negative shuts, at a
 * rate set by the speed); otherwise the last redstone edge sets where the door goes. The panel's
 * offset is saved; the leaves' block states follow it a column at a time, so collision, explosion
 * resistance and shielding are always those of what is really in the doorway. Clients get the rate
 * only when it changes and slide the panel themselves.
 */
public class BlastDoorControllerBlockEntity extends KineticBlockEntity {
    private static final int VALIDATE_INTERVAL = 10;
    /** The panel is drawn a little thinner than a block so it hides inside the frame when retracted. */
    public static final float PANEL_THICKNESS = 14 / 16f;

    @Nullable
    private BlastDoorLayout layout;
    /** Blocks the panel has slid, 0 (shut) to the door's width (open). */
    private double offset;
    /** Signed blocks per tick the panel is moving at; what clients extrapolate from. */
    private double motion;
    private double previousOffset;
    private boolean powered;
    /** Where the last redstone edge sent the door, until it gets there: true open, false shut. */
    @Nullable
    private Boolean redstoneTarget;
    private boolean validationRequested = true;
    private int validateTimer;
    private int lastSignal;

    public BlastDoorControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            clientTick();
            return;
        }
        if (validationRequested || ++validateTimer >= VALIDATE_INTERVAL) {
            validationRequested = false;
            validateTimer = 0;
            revalidate();
        }
        boolean nowPowered = level.hasNeighborSignal(worldPosition);
        if (nowPowered != powered) {
            powered = nowPowered;
            redstoneTarget = nowPowered;
            setChanged();
        }
        move();
        int signal = comparatorSignal();
        if (signal != lastSignal) {
            lastSignal = signal;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    private void move() {
        double nextMotion = 0;
        if (layout != null) {
            float speed = getSpeed();
            if (speed != 0 && !isOverStressed()) {
                double rate = BlastDoorTravel.rotationRate(speed, CivilDefenseConfig.blastDoorBlocksPerRevolution);
                nextMotion = speed > 0 ? rate : -rate;
                redstoneTarget = null;
            } else if (redstoneTarget != null) {
                double rate = BlastDoorTravel.redstoneRate(CivilDefenseConfig.blastDoorRedstoneTicksPerBlock);
                nextMotion = redstoneTarget ? rate : -rate;
            }
            double next = BlastDoorTravel.step(offset, layout.width(), Math.abs(nextMotion), nextMotion > 0);
            if (next == offset) {
                if (nextMotion != 0 && redstoneTarget != null) {
                    redstoneTarget = null;
                }
                nextMotion = 0;
            } else {
                boolean starting = motion == 0;
                offset = next;
                applyLeaves();
                setChanged();
                if (starting) {
                    playSound(true);
                } else if (offset == 0 || offset == layout.width()) {
                    playSound(false);
                    if (offset == 0) org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements.awardNearby(level, worldPosition, org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements.WITNESS_RADIUS, org.papiricoh.create_nuclearindustry.advancement.NuclearEvent.BLAST_DOOR_SHUT);
                }
            }
        }
        if (nextMotion != motion) {
            motion = nextMotion;
            sendData();
        }
    }

    private void playSound(boolean starting) {
        if (level != null) {
            level.playSound(null, worldPosition, starting ? SoundEvents.PISTON_EXTEND : SoundEvents.IRON_DOOR_CLOSE,
                    SoundSource.BLOCKS, 1.0f, starting ? 0.5f : 0.6f);
        }
    }

    /** Looks for the door again on the next tick, after a block next to the controller changed. */
    public void requestValidation() {
        validationRequested = true;
    }

    private void revalidate() {
        if (level == null) {
            return;
        }
        BlastDoorLayout found = BlastDoorLayout.find(level, worldPosition, getBlockState().getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS));
        if (Objects.equals(found, layout)) {
            return;
        }
        if (layout != null) {
            // Something in the door or its frame changed: the door slams shut.
            shut(layout);
            offset = 0;
        }
        layout = found;
        if (layout != null) {
            offset = Mth.clamp(offset, 0, layout.width());
            applyLeaves();
        } else {
            offset = 0;
        }
        setChanged();
        sendData();
    }

    /** Sets every leaf to what the panel's offset leaves it: shut, under the moving panel, or clear. */
    private void applyLeaves() {
        if (level == null || layout == null) {
            return;
        }
        for (int column = 0; column < layout.width(); column++) {
            LeafState wanted = LeafState.of(BlastDoorTravel.cell(layout.fromPocket(column), layout.width(), offset));
            for (int row = 0; row < layout.height(); row++) {
                setLeaf(layout.cell(column, row), wanted);
            }
        }
    }

    private void shut(BlastDoorLayout door) {
        for (int column = 0; column < door.width(); column++) {
            for (int row = 0; row < door.height(); row++) {
                setLeaf(door.cell(column, row), LeafState.CLOSED);
            }
        }
    }

    private void setLeaf(BlockPos pos, LeafState wanted) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BlastDoorBlock && state.getValue(BlastDoorBlock.LEAF) != wanted) {
            level.setBlock(pos, state.setValue(BlastDoorBlock.LEAF, wanted), Block.UPDATE_ALL);
        }
    }

    /** Broken: whatever was open shuts, so no invisible leaves are left behind. */
    @Override
    public void destroy() {
        if (level != null && !level.isClientSide && layout != null) {
            shut(layout);
        }
        super.destroy();
    }

    private void clientTick() {
        if (validationRequested || ++validateTimer >= VALIDATE_INTERVAL) {
            validationRequested = false;
            validateTimer = 0;
            layout = BlastDoorLayout.find(level, worldPosition, getBlockState().getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS));
        }
        previousOffset = offset;
        if (layout != null && motion != 0) {
            offset = BlastDoorTravel.step(offset, layout.width(), Math.abs(motion), motion > 0);
        }
    }

    public boolean covers(BlockPos pos) {
        return layout != null && layout.contains(pos);
    }

    @Nullable
    public BlastDoorLayout getLayout() {
        return layout;
    }

    public double getOffset() {
        return offset;
    }

    /** The panel's offset between the last two client ticks. */
    public double getRenderedOffset(float partialTicks) {
        return Mth.lerp(partialTicks, previousOffset, offset);
    }

    public boolean isFullyOpen() {
        return layout != null && offset >= layout.width();
    }

    public boolean isShut() {
        return offset <= 0;
    }

    public int comparatorSignal() {
        return layout == null ? 0 : BlastDoorTravel.comparatorSignal(offset, layout.width());
    }

    public Direction.Axis shaftAxis() {
        return getBlockState().getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS);
    }

    @Override
    public float calculateStressApplied() {
        float impact = (float) CivilDefenseConfig.blastDoorStress;
        lastStressApplied = impact;
        return impact;
    }

    @Override
    protected AABB createRenderBoundingBox() {
        // The panel can be drawn up to a door's width beyond the frame, on either side.
        return new AABB(worldPosition).inflate(BlastDoorLayout.MAX_SIZE * 2 + 2);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("    ").append(Component.translatable("block.create_nuclearindustry.blast_door_controller"))
                .withStyle(ChatFormatting.GOLD));
        if (layout == null) {
            tooltip.add(indent(Component.translatable("create_nuclearindustry.blast_door.no_door").withStyle(ChatFormatting.RED)));
            tooltip.add(indent(Component.translatable("create_nuclearindustry.blast_door.no_door_hint").withStyle(ChatFormatting.DARK_GRAY)));
            return true;
        }
        tooltip.add(indent(Component.translatable("create_nuclearindustry.blast_door.size", layout.width(), layout.height())
                .withStyle(ChatFormatting.GRAY)));
        String status = motion > 0 ? "opening" : motion < 0 ? "closing" : isShut() ? "shut" : isFullyOpen() ? "open" : "stopped";
        int percent = (int) Math.round(100.0 * offset / layout.width());
        tooltip.add(indent(Component.translatable("create_nuclearindustry.blast_door." + status, percent)
                .withStyle(isShut() ? ChatFormatting.GREEN : ChatFormatting.YELLOW)));
        return true;
    }

    private static Component indent(Component line) {
        return Component.literal("     ").append(line);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("Offset", offset);
        tag.putDouble("Motion", motion);
        tag.putBoolean("Powered", powered);
        if (redstoneTarget != null) {
            tag.putBoolean("Target", redstoneTarget);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        offset = Math.max(0, tag.getDouble("Offset"));
        motion = tag.getDouble("Motion");
        powered = tag.getBoolean("Powered");
        redstoneTarget = tag.contains("Target") ? tag.getBoolean("Target") : null;
        if (clientPacket) {
            validationRequested = true;
        } else {
            previousOffset = offset;
        }
    }
}
