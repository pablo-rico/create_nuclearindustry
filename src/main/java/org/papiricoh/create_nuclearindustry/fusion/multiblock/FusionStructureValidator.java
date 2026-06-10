package org.papiricoh.create_nuclearindustry.fusion.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionFluidPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Validates the fusion reactor multiblock: a hollow square ring of accelerator segments on a
 * single horizontal plane, with the controller (tokamak core) at the geometric centre and an
 * empty plasma chamber between the ring and the core.
 *
 * <p>Coplanar analogue of {@code ReactorStructureValidator}: a symmetric outward probe finds the
 * ring radius, then the perimeter and interior are validated and ports/magnets collected.
 */
public class FusionStructureValidator {
    public static final int MIN_SIZE = 5;
    public static final int MAX_SIZE = 11;

    public static FusionValidationResult validate(Level level, BlockPos controllerPos) {
        return validate(level, controllerPos, Optional.empty());
    }

    public static FusionValidationResult validate(Level level, BlockPos controllerPos, Optional<FusionStructure> previous) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (level == null) {
            errors.add("Level unavailable");
            return FusionValidationResult.invalid(errors, warnings);
        }
        if (!level.getBlockState(controllerPos).is(AllNuclearBlocks.FUSION_CONTROLLER.get())) {
            errors.add("Controller block missing");
            return FusionValidationResult.invalid(errors, warnings);
        }

        Optional<Integer> half = detectHalfSize(level, controllerPos, errors);
        if (half.isEmpty()) {
            return FusionValidationResult.invalid(errors, warnings);
        }

        int halfSize = half.get();
        int size = halfSize * 2 + 1;
        FusionStructure structure = new FusionStructure(controllerPos, size, controllerPos.getY());

        validateRing(level, structure, halfSize, errors);
        validateInterior(level, structure, halfSize, errors);

        if (structure.magnetInputCount < 1) {
            errors.add("Missing magnet input (kinetic drive)");
        }
        if (structure.coolantPortCount < 1) {
            errors.add("Missing coolant input port");
        }
        if (structure.plasmaPortCount < 1) {
            errors.add("Missing plasma steam output port");
        }
        if (structure.fuelInjectorCount < 1) {
            errors.add("Missing fuel injector port");
        }
        if (structure.electromagnetCount() < size) {
            warnings.add("Under-confined: add more electromagnets for a stable field");
        }

        if (!errors.isEmpty()) {
            return FusionValidationResult.invalid(errors, warnings);
        }
        return FusionValidationResult.valid(structure, warnings);
    }

    private static Optional<Integer> detectHalfSize(Level level, BlockPos c, List<String> errors) {
        int y = c.getY();
        for (int offset = MIN_SIZE / 2; offset <= MAX_SIZE / 2; offset++) {
            boolean ring = isRingBlock(level, new BlockPos(c.getX() + offset, y, c.getZ()))
                    && isRingBlock(level, new BlockPos(c.getX() - offset, y, c.getZ()))
                    && isRingBlock(level, new BlockPos(c.getX(), y, c.getZ() + offset))
                    && isRingBlock(level, new BlockPos(c.getX(), y, c.getZ() - offset));
            if (ring) {
                return Optional.of(offset);
            }
        }
        errors.add("No accelerator ring found around controller (build a " + MIN_SIZE + "x" + MIN_SIZE
                + " to " + MAX_SIZE + "x" + MAX_SIZE + " square ring)");
        return Optional.empty();
    }

    private static void validateRing(Level level, FusionStructure structure, int halfSize, List<String> errors) {
        BlockPos c = structure.controllerPos;
        int y = structure.ringY;
        for (int dx = -halfSize; dx <= halfSize; dx++) {
            for (int dz = -halfSize; dz <= halfSize; dz++) {
                boolean perimeter = Math.abs(dx) == halfSize || Math.abs(dz) == halfSize;
                if (!perimeter) {
                    continue;
                }
                BlockPos pos = new BlockPos(c.getX() + dx, y, c.getZ() + dz);
                Block block = level.getBlockState(pos).getBlock();
                boolean corner = Math.abs(dx) == halfSize && Math.abs(dz) == halfSize;

                if (corner) {
                    if (block != AllNuclearBlocks.FUSION_ACCELERATOR_CORNER.get()) {
                        addError(errors, "Missing accelerator corner at " + shortPos(pos));
                    }
                    continue;
                }

                if (block == AllNuclearBlocks.FUSION_ACCELERATOR_SEGMENT.get()
                        || block == AllNuclearBlocks.FUSION_CRYOSTAT_CASING.get()) {
                    continue;
                }
                if (block == AllNuclearBlocks.FUSION_ELECTROMAGNET.get()) {
                    structure.electromagnets.add(pos);
                    continue;
                }
                if (block == AllNuclearBlocks.FUSION_MAGNET_INPUT.get()) {
                    structure.magnetInputs.add(pos);
                    structure.magnetInputCount++;
                    continue;
                }
                if (block == AllNuclearBlocks.FUSION_FLUID_PORT.get()) {
                    structure.fluidPorts.add(pos);
                    if (level.getBlockState(pos).getValue(FusionFluidPortBlock.MODE) == ReactorFluidPortMode.INPUT) {
                        structure.coolantPortCount++;
                    } else {
                        structure.plasmaPortCount++;
                    }
                    continue;
                }
                if (block == AllNuclearBlocks.FUSION_FUEL_INJECTOR.get()) {
                    structure.fuelInjectors.add(pos);
                    structure.fuelInjectorCount++;
                    continue;
                }
                addError(errors, "Missing accelerator segment at " + shortPos(pos));
            }
        }
    }

    private static void validateInterior(Level level, FusionStructure structure, int halfSize, List<String> errors) {
        BlockPos c = structure.controllerPos;
        int y = structure.ringY;
        for (int dx = -halfSize + 1; dx <= halfSize - 1; dx++) {
            for (int dz = -halfSize + 1; dz <= halfSize - 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue; // controller
                }
                BlockPos pos = new BlockPos(c.getX() + dx, y, c.getZ() + dz);
                if (!level.getBlockState(pos).isAir()) {
                    addError(errors, "Plasma chamber obstructed at " + shortPos(pos));
                }
            }
        }
    }

    private static boolean isRingBlock(Level level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        return block == AllNuclearBlocks.FUSION_ACCELERATOR_SEGMENT.get()
                || block == AllNuclearBlocks.FUSION_ACCELERATOR_CORNER.get()
                || block == AllNuclearBlocks.FUSION_ELECTROMAGNET.get()
                || block == AllNuclearBlocks.FUSION_CRYOSTAT_CASING.get()
                || block == AllNuclearBlocks.FUSION_MAGNET_INPUT.get()
                || block == AllNuclearBlocks.FUSION_FLUID_PORT.get()
                || block == AllNuclearBlocks.FUSION_FUEL_INJECTOR.get();
    }

    private static void addError(List<String> errors, String message) {
        if (errors.size() >= 6) {
            return;
        }
        errors.add(message);
    }

    private static String shortPos(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    public record FusionValidationResult(boolean formed, Optional<FusionStructure> structure, List<String> errors, List<String> warnings) {
        public static FusionValidationResult valid(FusionStructure structure, List<String> warnings) {
            return new FusionValidationResult(true, Optional.of(structure), List.of(), List.copyOf(warnings));
        }

        public static FusionValidationResult invalid(List<String> errors, List<String> warnings) {
            return new FusionValidationResult(false, Optional.empty(), List.copyOf(errors), List.copyOf(warnings));
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

    public static class FusionStructure {
        public final BlockPos controllerPos;
        public final int size;
        public final int ringY;
        public final Set<BlockPos> ringBlocks = new HashSet<>();
        public final Set<BlockPos> interiorCells = new HashSet<>();
        public final Set<BlockPos> electromagnets = new HashSet<>();
        public final Set<BlockPos> magnetInputs = new HashSet<>();
        public final Set<BlockPos> fluidPorts = new HashSet<>();
        public final Set<BlockPos> fuelInjectors = new HashSet<>();
        public int magnetInputCount;
        public int coolantPortCount;
        public int plasmaPortCount;
        public int fuelInjectorCount;

        public FusionStructure(BlockPos controllerPos, int size, int ringY) {
            this.controllerPos = controllerPos;
            this.size = size;
            this.ringY = ringY;
            int half = size / 2;
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    BlockPos pos = new BlockPos(controllerPos.getX() + dx, ringY, controllerPos.getZ() + dz);
                    boolean perimeter = Math.abs(dx) == half || Math.abs(dz) == half;
                    if (perimeter) {
                        ringBlocks.add(pos);
                    } else if (!(dx == 0 && dz == 0)) {
                        interiorCells.add(pos);
                    }
                }
            }
        }

        public int electromagnetCount() {
            return electromagnets.size() + magnetInputs.size();
        }

        public boolean contains(BlockPos pos) {
            return ringBlocks.contains(pos) || interiorCells.contains(pos) || pos.equals(controllerPos);
        }

        public boolean containsFluidPort(BlockPos pos) {
            return fluidPorts.contains(pos);
        }

        public boolean containsFuelInjector(BlockPos pos) {
            return fuelInjectors.contains(pos);
        }

        public String dimensionsText() {
            return size + "x" + size + " ring";
        }
    }
}
