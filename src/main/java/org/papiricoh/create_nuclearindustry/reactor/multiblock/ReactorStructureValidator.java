package org.papiricoh.create_nuclearindustry.reactor.multiblock;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFuelPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;
import org.papiricoh.create_nuclearindustry.reactor.control.ControlRodGeometry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ReactorStructureValidator {
    public static final int MIN_WIDTH = 3;
    public static final int MAX_WIDTH = 9;
    public static final int MIN_HEIGHT = 4;
    public static final int MAX_HEIGHT = 12;

    public static ReactorValidationResult validate(Level level, BlockPos controllerPos) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (level == null) {
            errors.add("Level unavailable");
            return ReactorValidationResult.invalid(errors, warnings);
        }
        if (!level.getBlockState(controllerPos).is(AllNuclearBlocks.REACTOR_CONTROLLER.get())) {
            errors.add("Controller block missing");
            return ReactorValidationResult.invalid(errors, warnings);
        }

        Optional<DetectedBounds> detected = detectBounds(level, controllerPos, errors);
        if (detected.isEmpty()) {
            return ReactorValidationResult.invalid(errors, warnings);
        }

        DetectedBounds bounds = detected.get();
        ReactorStructure structure = new ReactorStructure(controllerPos, bounds.width(), bounds.height(), bounds.bottomY());
        detectControlChannels(level, structure);
        validateShell(level, structure, errors);
        validateInterior(level, structure, errors, warnings);

        if (!errors.isEmpty()) {
            return ReactorValidationResult.invalid(errors, warnings);
        }
        return ReactorValidationResult.valid(structure, warnings);
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
            errors.add("Missing casing floor below controller");
            return Optional.empty();
        }

        int height = controllerPos.getY() - bottomY + 1;
        if (height < MIN_HEIGHT || height > MAX_HEIGHT) {
            errors.add("Height must be " + MIN_HEIGHT + "-" + MAX_HEIGHT + " blocks, got " + height);
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
            errors.add("Width must be odd and between " + MIN_WIDTH + " and " + MAX_WIDTH + ", got " + width);
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
                    errors.add("Controller must be centered on the top face");
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
                continue;
            }

            if (shell && block != AllNuclearBlocks.REACTOR_CASING.get()) {
                errors.add("Missing casing at " + shortPos(pos));
                if (errors.size() >= 6) {
                    errors.add("More casing errors omitted");
                    return;
                }
            }
        }
    }

    private static void detectControlChannels(Level level, ReactorStructure structure) {
        int halfWidth = structure.width / 2;
        for (int x = structure.controllerPos.getX() - halfWidth + 1; x <= structure.controllerPos.getX() + halfWidth - 1; x++) {
            for (int z = structure.controllerPos.getZ() - halfWidth + 1; z <= structure.controllerPos.getZ() + halfWidth - 1; z++) {
                if (hasControlRodInColumnOrAbove(level, structure, x, z)) {
                    structure.controlChannels.add(new BlockPos(x, structure.bottomY + 1, z));
                }
            }
        }
    }

    private static boolean hasControlRodInColumnOrAbove(Level level, ReactorStructure structure, int x, int z) {
        if (isOpenControlChannelTop(level, structure, x, z)) {
            return true;
        }
        for (int y = structure.bottomY + 1; y <= structure.topY + structure.innerHeight(); y++) {
            if (level.getBlockState(new BlockPos(x, y, z)).is(AllNuclearBlocks.CONTROL_ROD.get())) {
                return true;
            }
        }
        return hasMovingControlRodInColumn(level, structure, x, z);
    }

    private static boolean isOpenControlChannelTop(Level level, ReactorStructure structure, int x, int z) {
        return level.getBlockState(new BlockPos(x, structure.topY, z)).isAir();
    }

    private static boolean hasMovingControlRodInColumn(Level level, ReactorStructure structure, int x, int z) {
        AABB searchBox = ControlRodGeometry.createControlRodSearchBox(structure);
        for (AbstractContraptionEntity entity : level.getEntitiesOfClass(AbstractContraptionEntity.class, searchBox)) {
            Contraption contraption = entity.getContraption();
            if (contraption == null) {
                continue;
            }
            for (Map.Entry<BlockPos, StructureTemplate.StructureBlockInfo> entry : contraption.getBlocks().entrySet()) {
                StructureTemplate.StructureBlockInfo info = entry.getValue();
                if (!info.state().is(AllNuclearBlocks.CONTROL_ROD.get())) {
                    continue;
                }
                AABB rodBounds = ControlRodGeometry.movingBlockBounds(entity, entry.getKey());
                for (int y = structure.bottomY + 1; y <= structure.topY + structure.innerHeight(); y++) {
                    if (ControlRodGeometry.overlapsControlCell(rodBounds, new BlockPos(x, y, z))) {
                        return true;
                    }
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
            errors.add("Missing uranium rod column");
        }
        if (structure.heatExchangerColumnCount < 1) {
            errors.add("Missing heat exchanger column");
        }
        if (structure.coolantInputPortCount < 1) {
            errors.add("Missing coolant input port");
        }
        if (structure.steamOutputPortCount < 1) {
            errors.add("Missing steam output port");
        }
        if (structure.controlRodColumnCount < 1) {
            warnings.add("No control rod columns; reactor will be difficult to control");
        }
        structure.controlRodCount = structure.controlChannels.size() * structure.innerHeight();
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
                    errors.add("Mixed interior column at " + shortPos(pos));
                    return;
                }
            }
            return;
        }

        if (!isAllowedInteriorColumnBlock(sample)) {
            errors.add("Invalid interior block at " + shortPos(samplePos));
            return;
        }

        int blockCount = 0;
        for (int y = structure.bottomY + 1; y < structure.topY; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            Block block = level.getBlockState(pos).getBlock();
            if (block != sample) {
                errors.add("Interior columns must be vertical and homogeneous at " + shortPos(pos));
                return;
            }
            structure.functionalBlocks.add(pos);
            blockCount++;
        }

        if (sample == AllNuclearBlocks.URANIUM_ROD.get()) {
            structure.uraniumColumnCount++;
            structure.uraniumRodCount += blockCount;
        } else if (sample == AllNuclearBlocks.HEAT_EXCHANGER.get()) {
            structure.heatExchangerColumnCount++;
            structure.heatExchangerCount += blockCount;
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
            } else if (block != Blocks.AIR) {
                errors.add("Control rod channel blocked at " + shortPos(pos));
                return;
            }
        }
        structure.controlRodColumnCount++;
        structure.controlRodCount += inserted;
    }

    private static boolean isAllowedInteriorColumnBlock(Block block) {
        return block == AllNuclearBlocks.URANIUM_ROD.get()
                || block == AllNuclearBlocks.HEAT_EXCHANGER.get();
    }

    private static boolean isAllowedControlChannelTop(Block block) {
        return block == Blocks.AIR
                || block == AllNuclearBlocks.REACTOR_CASING.get()
                || block == AllNuclearBlocks.CONTROL_ROD.get();
    }

    private static String shortPos(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private record DetectedBounds(int width, int height, int bottomY) {}

    public record ReactorValidationResult(boolean formed, Optional<ReactorStructure> structure, List<String> errors, List<String> warnings) {
        public static ReactorValidationResult valid(ReactorStructure structure, List<String> warnings) {
            return new ReactorValidationResult(true, Optional.of(structure), List.of(), List.copyOf(warnings));
        }

        public static ReactorValidationResult invalid(List<String> errors, List<String> warnings) {
            return new ReactorValidationResult(false, Optional.empty(), List.copyOf(errors), List.copyOf(warnings));
        }

        public String primaryMessage() {
            if (!errors.isEmpty()) {
                return errors.get(0);
            }
            if (!warnings.isEmpty()) {
                return warnings.get(0);
            }
            return formed ? "Structure formed" : "Structure incomplete";
        }
    }

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

        public boolean isControlArea(BlockPos pos) {
            if (!isControlChannel(pos.getX(), pos.getZ())) {
                return false;
            }
            return pos.getY() >= bottomY + 1 && pos.getY() <= topY + innerHeight();
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

        public int innerHeight() {
            return height - 2;
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
