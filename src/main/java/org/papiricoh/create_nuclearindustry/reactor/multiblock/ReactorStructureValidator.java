package org.papiricoh.create_nuclearindustry.reactor.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockText;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockValidationResult;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFuelPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;
import org.papiricoh.create_nuclearindustry.reactor.control.ControlRodGeometry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PressurizerStack;
import org.papiricoh.create_nuclearindustry.reactor.visual.ReactorVisualsRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Checks a fission reactor's structure. Errors and warnings are {@link MultiblockText} messages (a
 * lang key under {@link #KEY} and its arguments), so each player reads them in their own language.
 */
public class ReactorStructureValidator {
    /** Lang key prefix of every validation message. */
    public static final String KEY = "create_nuclearindustry.reactor.validation.";
    /** A control rod channel holds something other than rods, pistons or air; moving rods count as this too. */
    public static final String CHANNEL_BLOCKED = KEY + "channel_blocked";
    public static final String CASING_MISSING = KEY + "casing_missing";
    public static final int MIN_WIDTH = 3;
    public static final int MAX_WIDTH = 9;
    public static final int MIN_HEIGHT = 4;
    public static final int MAX_HEIGHT = 12;

    public static MultiblockValidationResult<ReactorStructure> validate(Level level, BlockPos controllerPos) {
        return validate(level, controllerPos, false);
    }

    /**
     * The control channels are detected afresh every time (see {@link #detectControlChannels}), never
     * carried over from the last structure, so a channel turned into a reflector or moderator column
     * stops counting as one.
     *
     * @param primaryCoolant the reactor holds pressurized loop coolant, so a missing pressurizer is worth a warning
     */
    public static MultiblockValidationResult<ReactorStructure> validate(Level level, BlockPos controllerPos, boolean primaryCoolant) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (level == null) {
            errors.add(message("no_level"));
            return MultiblockValidationResult.invalid(errors, warnings);
        }
        if (!level.getBlockState(controllerPos).is(AllNuclearBlocks.REACTOR_CONTROLLER.get())) {
            errors.add(message("no_controller"));
            return MultiblockValidationResult.invalid(errors, warnings);
        }

        Optional<DetectedBounds> detected = detectBounds(level, controllerPos, errors);
        if (detected.isEmpty()) {
            return MultiblockValidationResult.invalid(errors, warnings);
        }

        DetectedBounds bounds = detected.get();
        ReactorStructure structure = new ReactorStructure(controllerPos, bounds.width(), bounds.height(), bounds.bottomY());
        structure.controlChannels.addAll(detectControlChannels(level, structure));
        validateShell(level, structure, errors);
        validateInterior(level, structure, errors, warnings);
        detectPressurizer(level, structure, warnings);
        if (primaryCoolant && structure.pressurizer == null) {
            warnings.add(message("needs_pressurizer"));
        }

        if (!errors.isEmpty()) {
            return MultiblockValidationResult.invalid(errors, warnings);
        }
        return MultiblockValidationResult.valid(structure, warnings);
    }

    private static Optional<DetectedBounds> detectBounds(Level level, BlockPos controllerPos, List<String> errors) {
        int bottomY = Integer.MIN_VALUE;
        for (int y = controllerPos.getY() - 1; y >= controllerPos.getY() - MAX_HEIGHT; y--) {
            if (level.getBlockState(new BlockPos(controllerPos.getX(), y, controllerPos.getZ())).is(AllNuclearBlocks.REACTOR_CASING.get())) {
                bottomY = y;
                break;
            }
        }

        if (bottomY == Integer.MIN_VALUE) {
            errors.add(message("no_floor"));
            return Optional.empty();
        }

        int height = controllerPos.getY() - bottomY + 1;
        if (height < MIN_HEIGHT || height > MAX_HEIGHT) {
            errors.add(message("height", MIN_HEIGHT, MAX_HEIGHT, height));
            return Optional.empty();
        }

        int halfWidth = 0;
        for (int offset = 1; offset <= MAX_WIDTH / 2; offset++) {
            boolean ringContinues =
                    level.getBlockState(new BlockPos(controllerPos.getX() + offset, bottomY, controllerPos.getZ())).is(AllNuclearBlocks.REACTOR_CASING.get()) &&
                    level.getBlockState(new BlockPos(controllerPos.getX() - offset, bottomY, controllerPos.getZ())).is(AllNuclearBlocks.REACTOR_CASING.get()) &&
                    level.getBlockState(new BlockPos(controllerPos.getX(), bottomY, controllerPos.getZ() + offset)).is(AllNuclearBlocks.REACTOR_CASING.get()) &&
                    level.getBlockState(new BlockPos(controllerPos.getX(), bottomY, controllerPos.getZ() - offset)).is(AllNuclearBlocks.REACTOR_CASING.get());
            if (!ringContinues) {
                break;
            }
            halfWidth = offset;
        }

        int width = halfWidth * 2 + 1;
        if (width < MIN_WIDTH || width > MAX_WIDTH) {
            errors.add(message("width", MIN_WIDTH, MAX_WIDTH, width));
            return Optional.empty();
        }

        return Optional.of(new DetectedBounds(width, height, bottomY));
    }

    private static void validateShell(Level level, ReactorStructure structure, List<String> errors) {
        for (BlockPos pos : structure.allBlocks) {
            boolean centerTop = pos.equals(structure.controllerPos);
            boolean shell = structure.isShell(pos);
            Block block = level.getBlockState(pos).getBlock();

            if (centerTop) {
                if (block != AllNuclearBlocks.REACTOR_CONTROLLER.get()) {
                    errors.add(message("controller_centre"));
                }
                continue;
            }

            if (shell && structure.isControlChannelTop(pos) && isAllowedControlChannelTop(block)) {
                continue;
            }

            if (shell && block == AllNuclearBlocks.REACTOR_FLUID_PORT.get()) {
                structure.fluidPorts.add(pos);
                if (level.getBlockState(pos).getValue(ReactorFluidPortBlock.MODE) == ReactorFluidPortMode.INPUT) {
                    structure.coolantInputPortCount++;
                } else {
                    structure.steamOutputPortCount++;
                }
                continue;
            }

            if (shell && block == AllNuclearBlocks.REACTOR_FUEL_PORT.get()) {
                structure.fuelPorts.add(pos);
                if (level.getBlockState(pos).getValue(ReactorFuelPortBlock.MODE) == ReactorFluidPortMode.INPUT) {
                    structure.fuelInputPortCount++;
                } else {
                    structure.fuelOutputPortCount++;
                }
                structure.fuelPortCount++;
                continue;
            }

            if (shell && block == AllNuclearBlocks.REACTOR_TEMPERATURE_SENSOR.get()) {
                structure.temperatureSensors.add(pos);
                continue;
            }

            if (shell && block == AllNuclearBlocks.REACTOR_REDSTONE_PORT.get()) {
                structure.redstonePorts.add(pos);
                continue;
            }

            if (shell && !isCasing(block)) {
                errors.add(MultiblockText.message(CASING_MISSING, shortPos(pos)));
                if (errors.size() >= 6) {
                    errors.add(message("more_casing"));
                    return;
                }
            }
        }
    }

    /**
     * The interior columns that are control rod channels, by their lowest interior block. Only the
     * bounds of {@code structure} are read, so the reactor can call this between validations to
     * notice a channel appearing or going away. A column is a channel when any of:
     * <ul>
     *     <li>its lid block is open: air, or a piston part reaching in to drive the rods;</li>
     *     <li>a control rod block sits anywhere in it or in its travel space above the lid;</li>
     *     <li>a contraption carries a control rod through it (rods on a moving piston).</li>
     * </ul>
     * Air under a closed lid is an empty column: in play the rods leave upwards, so a channel in use
     * always shows rods, a piston pole or an open lid.
     */
    public static Set<BlockPos> detectControlChannels(Level level, ReactorStructure structure) {
        Set<BlockPos> channels = new HashSet<>();
        List<AABB> movingRods = ControlRodGeometry.movingControlRods(level, structure);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int halfWidth = structure.width / 2;
        for (int x = structure.controllerPos.getX() - halfWidth + 1; x <= structure.controllerPos.getX() + halfWidth - 1; x++) {
            for (int z = structure.controllerPos.getZ() - halfWidth + 1; z <= structure.controllerPos.getZ() + halfWidth - 1; z++) {
                if (isControlChannelColumn(level, structure, x, z, movingRods, cursor)) {
                    channels.add(new BlockPos(x, structure.bottomY + 1, z));
                }
            }
        }
        return channels;
    }

    private static boolean isControlChannelColumn(Level level, ReactorStructure structure, int x, int z, List<AABB> movingRods,
                                                  BlockPos.MutableBlockPos cursor) {
        BlockState lid = level.getBlockState(cursor.set(x, structure.topY, z));
        if (lid.isAir() || isAllowedControlChannelMechanism(lid.getBlock())) {
            return true;
        }
        int top = structure.topY + structure.innerHeight();
        for (int y = structure.bottomY + 1; y <= top; y++) {
            if (level.getBlockState(cursor.set(x, y, z)).is(AllNuclearBlocks.CONTROL_ROD.get())) {
                return true;
            }
        }
        for (AABB rod : movingRods) {
            for (int y = structure.bottomY + 1; y <= top; y++) {
                if (ControlRodGeometry.overlapsControlCell(rod, x, y, z)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void validateInterior(Level level, ReactorStructure structure, List<String> errors, List<String> warnings) {
        int halfWidth = structure.width / 2;
        for (int x = structure.controllerPos.getX() - halfWidth + 1; x <= structure.controllerPos.getX() + halfWidth - 1; x++) {
            for (int z = structure.controllerPos.getZ() - halfWidth + 1; z <= structure.controllerPos.getZ() + halfWidth - 1; z++) {
                validateInteriorColumn(level, structure, x, z, errors);
            }
        }

        if (structure.uraniumColumnCount < 1) {
            errors.add(message("no_fuel"));
        }
        if (structure.heatExchangerColumnCount < 1) {
            errors.add(message("no_exchanger"));
        }
        if (structure.coolantInputPortCount < 1) {
            errors.add(message("no_coolant_port"));
        }
        if (structure.steamOutputPortCount < 1) {
            errors.add(message("no_steam_port"));
        }
        if (structure.controlRodColumnCount < 1) {
            warnings.add(message("no_control_rods"));
        }
        structure.controlRodCount = structure.controlChannels.size() * structure.innerHeight();
        structure.moderation = moderation(structure);
        structure.reflection = reflection(structure);
    }

    /**
     * How well graphite surrounds the fuel: each fuel column counts fully with two Graphite
     * Moderator columns beside it (sharing a face), half with one. Averaged over the fuel columns.
     */
    static double moderation(ReactorStructure structure) {
        if (structure.fuelColumns.isEmpty() || structure.moderatorColumns.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (BlockPos fuel : structure.fuelColumns) {
            int beside = 0;
            for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                if (structure.moderatorColumns.contains(fuel.relative(direction))) {
                    beside++;
                }
            }
            total += Math.min(2, beside) / 2.0;
        }
        return total / structure.fuelColumns.size();
    }

    /** Fraction of the core's inner ring (the columns touching the shell) taken by Beryllium Reflector. */
    static double reflection(ReactorStructure structure) {
        int ring = 0;
        int reflectors = 0;
        int inner = structure.width / 2 - 1;
        for (int dx = -inner; dx <= inner; dx++) {
            for (int dz = -inner; dz <= inner; dz++) {
                if (Math.abs(dx) != inner && Math.abs(dz) != inner) {
                    continue;
                }
                ring++;
                BlockPos column = new BlockPos(structure.controllerPos.getX() + dx, structure.bottomY + 1, structure.controllerPos.getZ() + dz);
                if (structure.reflectorColumns.contains(column)) {
                    reflectors++;
                }
            }
        }
        return ring == 0 ? 0.0 : (double) reflectors / ring;
    }

    private static void validateInteriorColumn(Level level, ReactorStructure structure, int x, int z, List<String> errors) {
        BlockPos samplePos = new BlockPos(x, structure.bottomY + 1, z);
        Block sample = level.getBlockState(samplePos).getBlock();

        if (structure.isControlChannel(x, z)) {
            validateControlChannel(level, structure, x, z, errors);
            return;
        }

        if (sample == Blocks.AIR) {
            for (int y = structure.bottomY + 1; y < structure.topY; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (!level.getBlockState(pos).isAir()) {
                    errors.add(message("mixed_column", shortPos(pos)));
                    return;
                }
            }
            return;
        }

        if (!isAllowedInteriorColumnBlock(sample)) {
            errors.add(message("invalid_interior", shortPos(samplePos)));
            return;
        }

        int blockCount = 0;
        for (int y = structure.bottomY + 1; y < structure.topY; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            Block block = level.getBlockState(pos).getBlock();
            if (block != sample) {
                errors.add(message("not_homogeneous", shortPos(pos)));
                return;
            }
            structure.functionalBlocks.add(pos);
            blockCount++;
        }

        if (sample == AllNuclearBlocks.URANIUM_ROD.get()) {
            structure.uraniumColumnCount++;
            structure.uraniumRodCount += blockCount;
            structure.fuelColumns.add(samplePos);
        } else if (sample == AllNuclearBlocks.HEAT_EXCHANGER.get()) {
            structure.heatExchangerColumnCount++;
            structure.heatExchangerCount += blockCount;
        } else if (sample == AllNuclearBlocks.GRAPHITE_MODERATOR.get()) {
            structure.moderatorColumns.add(samplePos);
        } else if (sample == AllNuclearBlocks.BERYLLIUM_REFLECTOR.get()) {
            structure.reflectorColumns.add(samplePos);
        }
    }

    private static void validateControlChannel(Level level, ReactorStructure structure, int x, int z, List<String> errors) {
        int inserted = 0;
        for (int y = structure.bottomY + 1; y < structure.topY; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            Block block = level.getBlockState(pos).getBlock();
            if (block == AllNuclearBlocks.CONTROL_ROD.get()) {
                structure.functionalBlocks.add(pos);
                inserted++;
            } else if (block == Blocks.AIR || isAllowedControlChannelMechanism(block)) {
                continue;
            } else {
                errors.add(MultiblockText.message(CHANNEL_BLOCKED, shortPos(pos)));
                return;
            }
        }
        structure.controlRodColumnCount++;
        structure.controlRodCount += inserted;
    }

    /**
     * Finds the pressurizer standing on the lid, away from the controller and the control rod channels.
     * A reactor works without one (it boils its water directly); a broken stack only earns a warning.
     */
    private static void detectPressurizer(Level level, ReactorStructure structure, List<String> warnings) {
        int halfWidth = structure.width / 2;
        for (int x = structure.controllerPos.getX() - halfWidth; x <= structure.controllerPos.getX() + halfWidth; x++) {
            for (int z = structure.controllerPos.getZ() - halfWidth; z <= structure.controllerPos.getZ() + halfWidth; z++) {
                if (!structure.isPressurizerColumn(x, z)) {
                    continue;
                }
                BlockPos base = new BlockPos(x, structure.topY + 1, z);
                if (!PressurizerStack.isSegment(level.getBlockState(base))) {
                    continue;
                }
                PressurizerStack stack = PressurizerStack.read(level, base, warnings);
                if (stack == null) {
                    continue;
                }
                if (structure.pressurizer == null) {
                    structure.pressurizer = stack;
                } else {
                    warnings.add(message("extra_pressurizer", shortPos(base)));
                }
            }
        }
    }

    private static boolean isAllowedInteriorColumnBlock(Block block) {
        return block == AllNuclearBlocks.URANIUM_ROD.get()
                || block == AllNuclearBlocks.HEAT_EXCHANGER.get()
                || block == AllNuclearBlocks.GRAPHITE_MODERATOR.get()
                || block == AllNuclearBlocks.BERYLLIUM_REFLECTOR.get();
    }

    private static boolean isAllowedControlChannelTop(Block block) {
        return block == Blocks.AIR
                || block == AllNuclearBlocks.REACTOR_CASING.get()
                || block == AllNuclearBlocks.CONTROL_ROD.get()
                || isAllowedControlChannelMechanism(block);
    }

    private static boolean isAllowedControlChannelMechanism(Block block) {
        String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
        return block == Blocks.PISTON_HEAD
                || block == Blocks.MOVING_PISTON
                || "create:mechanical_piston_head".equals(blockId)
                || "create:piston_extension_pole".equals(blockId);
    }

    /**
     * Casing, or a viewport in its place: lead glass walls the core in just as well. The floor
     * cross under the controller, which {@link #detectBounds} reads to find the reactor, must be
     * plain casing.
     */
    private static boolean isCasing(Block block) {
        return block == AllNuclearBlocks.REACTOR_CASING.get() || block == ReactorVisualsRegistry.REACTOR_VIEWPORT.get();
    }

    /** A validation message: a lang key under {@link #KEY} and its arguments. */
    private static String message(String id, Object... args) {
        return MultiblockText.message(KEY + id, args);
    }

    private static String shortPos(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private record DetectedBounds(int width, int height, int bottomY) {}

    public static class ReactorStructure {
        public final BlockPos controllerPos;
        public final int width;
        public final int height;
        public final int bottomY;
        public final int topY;
        public final Set<BlockPos> allBlocks = new HashSet<>();
        public final Set<BlockPos> functionalBlocks = new HashSet<>();
        public final Set<BlockPos> controlChannels = new HashSet<>();
        public final Set<BlockPos> fluidPorts = new HashSet<>();
        public final Set<BlockPos> fuelPorts = new HashSet<>();
        public final Set<BlockPos> temperatureSensors = new HashSet<>();
        public final Set<BlockPos> redstonePorts = new HashSet<>();
        /** Bottom block of each interior column, by what fills it. */
        public final Set<BlockPos> fuelColumns = new HashSet<>();
        public final Set<BlockPos> moderatorColumns = new HashSet<>();
        public final Set<BlockPos> reflectorColumns = new HashSet<>();
        /** See {@link ReactorStructureValidator#moderation} and {@link ReactorStructureValidator#reflection}. */
        public double moderation;
        public double reflection;
        /** The pressurizer on the lid, or null for a reactor that boils its water directly. */
        public PressurizerStack pressurizer;
        public int uraniumRodCount;
        public int controlRodCount;
        public int heatExchangerCount;
        public int uraniumColumnCount;
        public int controlRodColumnCount;
        public int heatExchangerColumnCount;
        public int coolantInputPortCount;
        public int steamOutputPortCount;
        public int fuelPortCount;
        public int fuelInputPortCount;
        public int fuelOutputPortCount;

        public ReactorStructure(BlockPos controllerPos, int width, int height, int bottomY) {
            this.controllerPos = controllerPos;
            this.width = width;
            this.height = height;
            this.bottomY = bottomY;
            this.topY = controllerPos.getY();
            int halfWidth = width / 2;
            for (int y = bottomY; y <= topY; y++) {
                for (int x = controllerPos.getX() - halfWidth; x <= controllerPos.getX() + halfWidth; x++) {
                    for (int z = controllerPos.getZ() - halfWidth; z <= controllerPos.getZ() + halfWidth; z++) {
                        allBlocks.add(new BlockPos(x, y, z));
                    }
                }
            }
        }

        public boolean contains(BlockPos pos) {
            return allBlocks.contains(pos);
        }

        public boolean containsFluidPort(BlockPos pos) {
            return fluidPorts.contains(pos);
        }

        public boolean containsFuelPort(BlockPos pos) {
            return fuelPorts.contains(pos);
        }

        public boolean containsTemperatureSensor(BlockPos pos) {
            return temperatureSensors.contains(pos);
        }

        public boolean containsRedstonePort(BlockPos pos) {
            return redstonePorts.contains(pos);
        }

        public boolean isControlArea(BlockPos pos) {
            if (!isControlChannel(pos.getX(), pos.getZ())) {
                return false;
            }
            return pos.getY() >= bottomY + 1 && pos.getY() <= topY + innerHeight();
        }

        /**
         * In an interior column, from the floor up to the top of the rods' travel space above the lid:
         * where a block change can make a column a control channel or stop it being one.
         */
        public boolean isChannelReach(BlockPos pos) {
            int halfWidth = width / 2;
            return Math.abs(pos.getX() - controllerPos.getX()) < halfWidth
                    && Math.abs(pos.getZ() - controllerPos.getZ()) < halfWidth
                    && pos.getY() >= bottomY + 1 && pos.getY() <= topY + innerHeight();
        }

        public boolean isControlChannel(int x, int z) {
            return controlChannels.contains(new BlockPos(x, bottomY + 1, z));
        }

        public boolean isControlChannelTop(BlockPos pos) {
            return pos.getY() == topY && isControlChannel(pos.getX(), pos.getZ()) && !isPerimeter(pos);
        }

        public boolean isShell(BlockPos pos) {
            return pos.getY() == bottomY
                    || pos.getY() == topY
                    || isPerimeter(pos);
        }

        private boolean isPerimeter(BlockPos pos) {
            int halfWidth = width / 2;
            return pos.getX() == controllerPos.getX() - halfWidth
                    || pos.getX() == controllerPos.getX() + halfWidth
                    || pos.getZ() == controllerPos.getZ() - halfWidth
                    || pos.getZ() == controllerPos.getZ() + halfWidth;
        }

        /** Every block of the structure plus the control rod travel space above it. */
        public net.minecraft.world.level.levelgen.structure.BoundingBox bounds() {
            int halfWidth = width / 2;
            return new net.minecraft.world.level.levelgen.structure.BoundingBox(
                    controllerPos.getX() - halfWidth, bottomY, controllerPos.getZ() - halfWidth,
                    controllerPos.getX() + halfWidth, topY + innerHeight(), controllerPos.getZ() + halfWidth);
        }

        public int innerHeight() {
            return height - 2;
        }

        public boolean hasPressurizer() {
            return pressurizer != null;
        }

        /** Lid columns a pressurizer may stand on: all but the controller's and the control rod channels'. */
        public boolean isPressurizerColumn(int x, int z) {
            int halfWidth = width / 2;
            if (Math.abs(x - controllerPos.getX()) > halfWidth || Math.abs(z - controllerPos.getZ()) > halfWidth) {
                return false;
            }
            return !(x == controllerPos.getX() && z == controllerPos.getZ()) && !isControlChannel(x, z);
        }

        /** Space above the lid where a pressurizer can stand; placing or breaking anything here can change it. */
        public List<BlockPos> pressurizerZone() {
            List<BlockPos> zone = new ArrayList<>();
            int halfWidth = width / 2;
            for (int x = controllerPos.getX() - halfWidth; x <= controllerPos.getX() + halfWidth; x++) {
                for (int z = controllerPos.getZ() - halfWidth; z <= controllerPos.getZ() + halfWidth; z++) {
                    if (!isPressurizerColumn(x, z)) {
                        continue;
                    }
                    for (int dy = 1; dy <= PressurizerStack.ZONE_HEIGHT; dy++) {
                        zone.add(new BlockPos(x, topY + dy, z));
                    }
                }
            }
            return zone;
        }

        /** {@link #bounds()} plus the space a pressurizer may take above the lid. */
        public net.minecraft.world.level.levelgen.structure.BoundingBox interestBounds() {
            net.minecraft.world.level.levelgen.structure.BoundingBox box = bounds();
            return new net.minecraft.world.level.levelgen.structure.BoundingBox(box.minX(), box.minY(), box.minZ(),
                    box.maxX(), Math.max(box.maxY(), topY + PressurizerStack.ZONE_HEIGHT), box.maxZ());
        }

        public int getUraniumRodCount() {
            return uraniumRodCount;
        }

        public int getControlRodCount() {
            return controlRodCount;
        }

        public String dimensionsText() {
            return width + "x" + width + "x" + height;
        }
    }
}
