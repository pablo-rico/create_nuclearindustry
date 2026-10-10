package org.papiricoh.create_nuclearindustry.civildefense.shelter;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseConfig;
import org.papiricoh.create_nuclearindustry.civildefense.shelter.ShelterCertification.Status;
import org.papiricoh.create_nuclearindustry.multiblock.SealedVolume;
import org.papiricoh.create_nuclearindustry.radiation.RadiationTransmission;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Surveys the room around the unit with a {@link SealedVolume} flood fill, a budgeted number of
 * cells per tick, and again every {@code civilDefense.shelter.rescanTicks} so new holes and walls
 * are noticed. Air is anything without a full collision box; every wall block the air touches is
 * rated by its explosion resistance and radiation transmission. A certified room goes into the
 * {@link ShelterRegistry}, filtering while the unit turns fast enough and is not overstressed.
 * Clients get the result for goggles.
 */
public class ShelterFiltrationUnitBlockEntity extends KineticBlockEntity {
    /** Where the survey starts: the air above the unit first, so a floor unit surveys its room. */
    private static final Direction[] START_ORDER = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST,
            Direction.WEST, Direction.DOWN};

    @Nullable
    private Survey survey;
    private int surveyTimer;
    /** The room of the last certification, kept to switch its filter without a new survey. */
    @Nullable
    private SealedVolume certifiedRoom;
    private boolean registeredFiltering;
    private Status status = Status.SCANNING;
    private int volume;
    /** The wall block that failed the certification, and its value and the value it needed. */
    @Nullable
    private BlockPos weakWall;
    private double weakValue;
    private double needed;

    public ShelterFiltrationUnitBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (survey == null && --surveyTimer <= 0) {
            startSurvey();
        }
        if (survey != null && survey.room.step(survey, CivilDefenseConfig.shelterCellsPerTick) != SealedVolume.State.FILLING) {
            finishSurvey(survey);
        }
        // Speed changes switch the filter at once; the room itself waits for the next survey.
        boolean filtering = certifiedRoom != null && isRunning();
        if (certifiedRoom != null && filtering != registeredFiltering) {
            registeredFiltering = filtering;
            ShelterRegistry.certify(level, worldPosition, certifiedRoom, filtering);
        }
    }

    /** Turning at {@code shelter.minSpeed} RPM or more, either way, with enough stress capacity. */
    public boolean isRunning() {
        return !isOverStressed() && Math.abs(getSpeed()) >= CivilDefenseConfig.shelterMinSpeed;
    }

    private void startSurvey() {
        surveyTimer = CivilDefenseConfig.shelterRescanTicks;
        for (Direction direction : START_ORDER) {
            BlockPos start = worldPosition.relative(direction);
            if (level.isLoaded(start) && Survey.isAir(level, start)) {
                survey = new Survey(level, start, CivilDefenseConfig.shelterMaxVolume);
                return;
            }
        }
        conclude(Status.NO_ROOM, null, 0, null, 0, 0);
    }

    private void finishSurvey(Survey done) {
        survey = null;
        SealedVolume room = done.room;
        Status result = ShelterCertification.evaluate(room.state(), room.volume(), CivilDefenseConfig.shelterMaxVolume,
                done.minResistance, done.maxTransmission,
                CivilDefenseConfig.shelterMinWallResistance, CivilDefenseConfig.shelterMaxWallTransmission);
        switch (result) {
            case WEAK_WALL -> conclude(result, room, room.volume(), done.weakest, done.minResistance,
                    CivilDefenseConfig.shelterMinWallResistance);
            case LEAKY_WALL -> conclude(result, room, room.volume(), done.leakiest, done.maxTransmission,
                    CivilDefenseConfig.shelterMaxWallTransmission);
            case TOO_BIG -> conclude(result, room, room.volume(), null, 0, CivilDefenseConfig.shelterMaxVolume);
            default -> conclude(result, room, room.volume(), null, 0, 0);
        }
    }

    private void conclude(Status result, @Nullable SealedVolume room, int cells, @Nullable BlockPos wall, double value, double need) {
        boolean changed = result != status || cells != volume || !Objects.equals(wall, weakWall)
                || value != weakValue || need != needed;
        status = result;
        volume = cells;
        weakWall = wall;
        weakValue = value;
        needed = need;
        if (result.certified() && room != null) {
            certifiedRoom = room;
            registeredFiltering = isRunning();
            ShelterRegistry.certify(level, worldPosition, room, registeredFiltering);
        } else {
            certifiedRoom = null;
            registeredFiltering = false;
            ShelterRegistry.revoke(level, worldPosition);
        }
        if (changed) {
            sendData();
        }
    }

    public Status getStatus() {
        return status;
    }

    /** Blocks of air in the room of the last survey. */
    public int getVolume() {
        return volume;
    }

    @Override
    public void invalidate() {
        if (level != null) {
            ShelterRegistry.revoke(level, worldPosition);
        }
        super.invalidate();
    }

    @Override
    public float calculateStressApplied() {
        float impact = (float) CivilDefenseConfig.shelterStress;
        lastStressApplied = impact;
        return impact;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("    ").append(Component.translatable("block.create_nuclearindustry.shelter_filtration_unit"))
                .withStyle(ChatFormatting.GOLD));
        String key = "create_nuclearindustry.shelter." + status.name().toLowerCase(Locale.ROOT);
        Component line = switch (status) {
            case CERTIFIED -> Component.translatable(key, volume).withStyle(ChatFormatting.GREEN);
            case TOO_BIG -> Component.translatable(key, (int) needed).withStyle(ChatFormatting.RED);
            case WEAK_WALL -> Component.translatable(key, coordinates(), format(weakValue), format(needed))
                    .withStyle(ChatFormatting.RED);
            case LEAKY_WALL -> Component.translatable(key, coordinates(), percent(weakValue), percent(needed))
                    .withStyle(ChatFormatting.RED);
            case SCANNING -> Component.translatable(key).withStyle(ChatFormatting.GRAY);
            default -> Component.translatable(key).withStyle(ChatFormatting.RED);
        };
        tooltip.add(indent(line));
        if (status.certified()) {
            tooltip.add(indent(isRunning()
                    ? Component.translatable("create_nuclearindustry.shelter.filtering", percent(CivilDefenseConfig.shelterFilterFactor))
                            .withStyle(ChatFormatting.GREEN)
                    : Component.translatable("create_nuclearindustry.shelter.not_filtering", CivilDefenseConfig.shelterMinSpeed)
                            .withStyle(ChatFormatting.YELLOW)));
            tooltip.add(indent(Component.translatable("create_nuclearindustry.shelter.blast",
                    percent(CivilDefenseConfig.shelterBlastFactor)).withStyle(ChatFormatting.GRAY)));
        }
        return true;
    }

    private String coordinates() {
        return weakWall == null ? "?" : weakWall.getX() + " " + weakWall.getY() + " " + weakWall.getZ();
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String percent(double fraction) {
        return String.format(Locale.ROOT, "%.0f", fraction * 100);
    }

    private static Component indent(Component line) {
        return Component.literal("     ").append(line);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (!clientPacket) {
            return;
        }
        tag.putString("Status", status.name());
        tag.putInt("Volume", volume);
        if (weakWall != null) {
            tag.putLong("WeakWall", weakWall.asLong());
        }
        tag.putDouble("WeakValue", weakValue);
        tag.putDouble("Needed", needed);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (!clientPacket) {
            return;
        }
        try {
            status = Status.valueOf(tag.getString("Status"));
        } catch (IllegalArgumentException e) {
            status = Status.SCANNING;
        }
        volume = tag.getInt("Volume");
        weakWall = tag.contains("WeakWall") ? BlockPos.of(tag.getLong("WeakWall")) : null;
        weakValue = tag.getDouble("WeakValue");
        needed = tag.getDouble("Needed");
    }

    /** One survey: the flood fill, and the weakest walls it has met so far. */
    private static final class Survey implements SealedVolume.World {
        private final Level level;
        private final SealedVolume room;
        private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        private double minResistance = Double.POSITIVE_INFINITY;
        private double maxTransmission;
        @Nullable
        private BlockPos weakest;
        @Nullable
        private BlockPos leakiest;

        private Survey(Level level, BlockPos start, int maxVolume) {
            this.level = level;
            this.room = new SealedVolume(start.getX(), start.getY(), start.getZ(), maxVolume);
        }

        /** Air for a shelter: anything a body or a draught gets through, i.e. without a full collision box. */
        static boolean isAir(Level level, BlockPos pos) {
            return !level.getBlockState(pos).isCollisionShapeFullBlock(level, pos);
        }

        @Override
        public boolean known(int x, int y, int z) {
            cursor.set(x, y, z);
            return !level.isOutsideBuildHeight(y) && level.isLoaded(cursor);
        }

        @Override
        public boolean passable(int x, int y, int z) {
            return isAir(level, cursor.set(x, y, z));
        }

        @Override
        public double wallRating(int x, int y, int z) {
            BlockState state = level.getBlockState(cursor.set(x, y, z));
            double resistance = state.getBlock().getExplosionResistance();
            double transmission = RadiationTransmission.of(state);
            if (resistance < minResistance) {
                minResistance = resistance;
                weakest = cursor.immutable();
            }
            if (transmission > maxTransmission) {
                maxTransmission = transmission;
                leakiest = cursor.immutable();
            }
            return resistance;
        }
    }
}
