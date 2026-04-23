package org.papiricoh.create_nuclearindustry.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Validates and detects nuclear reactor multiblock structures.
 *
 * Structure Format:
 * - REACTOR_CASING forms a hollow cube (cauldron shape)
 * - REACTOR_CONTROLLER at the top center (roof)
 * - HEAT_EXCHANGER in the center column from bottom to top (minus the controller)
 * - CONTROL_ROD and URANIUM_ROD protrude from the top (inside the cube)
 * - All fuel rods extend down and touch the HEAT_EXCHANGER column
 */
public class ReactorStructureValidator {

    private static final int MIN_WIDTH = 5;      // Minimum width for structure
    private static final int MIN_HEIGHT = 4;     // Minimum height for structure
    private static final int MAX_WIDTH = 11;
    private static final int MAX_HEIGHT = 15;

    /**
     * Attempts to detect and validate a reactor structure with controller at the given position.
     * @param level The world level
     * @param controllerPos The position of the reactor controller block
     * @return An optional containing the reactor structure if valid, empty otherwise
     */
    public static Optional<ReactorStructure> findReactorStructure(Level level, BlockPos controllerPos, ErrorHandler errorHandler) {
        if (level == null || level.getBlockState(controllerPos).getBlock() != AllNuclearBlocks.REACTOR_CONTROLLER.get()) {
            System.out.println("[Reactor Validator] No controller block at " + controllerPos);
            errorHandler.setError("No controller block");
            return Optional.empty();
        }

        System.out.println("[Reactor Validator] Checking structure starting at controller: " + controllerPos);

        ReactorStructure structure = new ReactorStructure(controllerPos);

        // Detect structure dimensions by scanning downward and outward
        if (!scanStructureDimensions(level, structure, errorHandler)) {
            System.out.println("[Reactor Validator] Failed to determine structure dimensions");
            return Optional.empty();
        }

        System.out.println("[Reactor Validator] Structure dimensions detected: " + structure.width + "×" + structure.height);

        if (!validateStructure(level, structure, errorHandler)) {
            System.out.println("[Reactor Validator] ❌ Structure validation FAILED");
            return Optional.empty();
        }

        System.out.println("[Reactor Validator] ✓ Structure validation SUCCESS - " + structure);
        return Optional.of(structure);
    }

    // Overload for backward compatibility
    public static Optional<ReactorStructure> findReactorStructure(Level level, BlockPos controllerPos) {
        return findReactorStructure(level, controllerPos, new ErrorHandler());
    }

    /**
     * Detects the dimensions of the reactor by scanning downward for the casing floor
     * and outward for the casing walls.
     */
    private static boolean scanStructureDimensions(Level level, ReactorStructure structure, ErrorHandler errorHandler) {
        int controllerY = structure.controllerPos.getY();
        int controllerX = structure.controllerPos.getX();
        int controllerZ = structure.controllerPos.getZ();

        // Scan downward to find the bottom REACTOR_CASING
        int height = 0;
        System.out.println("[Reactor Validator] Scanning downward from controller at Y=" + controllerY);

        for (int y = -1; y >= -MAX_HEIGHT; y--) {
            BlockPos checkPos = structure.controllerPos.above(y);
            Block block = level.getBlockState(checkPos).getBlock();

            // Log first 10 blocks scanned
            if (y >= -5) {
                System.out.println("[Reactor Validator]   Y=" + checkPos.getY() + " -> " + block.getName().getString());
            }

            if (block == AllNuclearBlocks.REACTOR_CASING.get()) {
                height = Math.abs(y) - 1;  // Height between controller and floor
                System.out.println("[Reactor Validator] ✓ Found casing floor at Y=" + checkPos.getY() + ", height=" + height);
                break;
            }
        }

        if (height < MIN_HEIGHT) {
            System.out.println("[Reactor Validator] ❌ Height " + height + " is less than minimum " + MIN_HEIGHT);
            String error = "Reactor too short (need " + MIN_HEIGHT + " high, got " + height + ")";
            errorHandler.setError(error);
            System.out.println("[Reactor Validator] Make sure REACTOR_CASING is directly below the controller!");
            return false;
        }

        structure.height = height;

        // Scan outward from center to find the width
        int width = 1;
        for (int offset = 1; offset <= MAX_WIDTH / 2; offset++) {
            BlockPos[] checkPositions = {
                    structure.controllerPos.offset(offset, 0, 0),
                    structure.controllerPos.offset(-offset, 0, 0),
                    structure.controllerPos.offset(0, 0, offset),
                    structure.controllerPos.offset(0, 0, -offset)
            };

            boolean allCasing = true;
            for (BlockPos pos : checkPositions) {
                Block block = level.getBlockState(pos).getBlock();
                if (block != AllNuclearBlocks.REACTOR_CASING.get()) {
                    allCasing = false;
                    break;
                }
            }

            if (allCasing) {
                width = 1 + (offset * 2);
            } else {
                break;
            }
        }

        if (width < MIN_WIDTH) {
            System.out.println("[Reactor Validator] Width " + width + " is less than minimum " + MIN_WIDTH);
            String error = "Reactor too small (need " + MIN_WIDTH + "×" + MIN_WIDTH + ", got " + width + "×" + width + ")";
            errorHandler.setError(error);
            return false;
        }

        structure.width = width;
        return true;
    }

    /**
     * Validates that the reactor structure meets all requirements.
     */
    private static boolean validateStructure(Level level, ReactorStructure structure, ErrorHandler errorHandler) {
        int centerX = structure.controllerPos.getX();
        int centerZ = structure.controllerPos.getZ();
        int topY = structure.controllerPos.getY();
        int bottomY = topY - structure.height;
        int halfWidth = structure.width / 2;

        int casingCount = 0;
        int heatExchangerCount = 0;
        int uraniumRodCount = 0;
        int controlRodCount = 0;

        // Validate structure layer by layer
        for (int y = bottomY; y <= topY; y++) {
            for (int x = centerX - halfWidth; x <= centerX + halfWidth; x++) {
                for (int z = centerZ - halfWidth; z <= centerZ + halfWidth; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = level.getBlockState(pos).getBlock();

                    // Check center column (should be HEAT_EXCHANGER or CONTROLLER)
                    if (x == centerX && z == centerZ) {
                        if (y == topY) {
                            // Top center must be CONTROLLER
                            if (block != AllNuclearBlocks.REACTOR_CONTROLLER.get()) {
                                System.out.println("[Reactor Validator] ❌ Top center is not REACTOR_CONTROLLER at " + pos);
                                return false;
                            }
                        } else {
                            // Rest of center column must be HEAT_EXCHANGER
                            if (block != AllNuclearBlocks.HEAT_EXCHANGER.get()) {
                                System.out.println("[Reactor Validator] ❌ Center column missing HEAT_EXCHANGER at " + pos);
                                return false;
                            }
                            heatExchangerCount++;
                        }
                    }
                    // Check perimeter (walls must be CASING)
                    else if (x == centerX - halfWidth || x == centerX + halfWidth ||
                             z == centerZ - halfWidth || z == centerZ + halfWidth) {
                        if (y == topY) {
                            // Top perimeter - can be CONTROL_ROD or URANIUM_ROD (protrusions)
                            if (block == AllNuclearBlocks.CONTROL_ROD.get()) {
                                controlRodCount++;
                            } else if (block == AllNuclearBlocks.URANIUM_ROD.get()) {
                                uraniumRodCount++;
                            } else if (block != AllNuclearBlocks.REACTOR_CASING.get()) {
                                System.out.println("[Reactor Validator] ⚠ Unexpected block at top perimeter: " + block + " at " + pos);
                            }
                        } else {
                            // Wall blocks at other heights must be CASING
                            if (block != AllNuclearBlocks.REACTOR_CASING.get()) {
                                System.out.println("[Reactor Validator] ❌ Wall missing REACTOR_CASING at " + pos + ", found: " + block);
                                return false;
                            }
                            casingCount++;
                        }
                    }
                    // Interior (non-center, non-perimeter)
                    else {
                        if (block == AllNuclearBlocks.URANIUM_ROD.get()) {
                            uraniumRodCount++;
                        } else if (block == AllNuclearBlocks.CONTROL_ROD.get()) {
                            controlRodCount++;
                        }
                        // Interior can be empty (air) or have fuel rods
                    }
                }
            }
        }

        // Validation checks
        System.out.println("[Reactor Validator] Structure composition:");
        System.out.println("  - REACTOR_CASING blocks: " + casingCount);
        System.out.println("  - HEAT_EXCHANGER blocks: " + heatExchangerCount);
        System.out.println("  - URANIUM_ROD blocks: " + uraniumRodCount);
        System.out.println("  - CONTROL_ROD blocks: " + controlRodCount);

        if (casingCount < MIN_WIDTH * 2 + structure.height * 2) {
            System.out.println("[Reactor Validator] ❌ Not enough casing blocks");
            errorHandler.setError("Missing REACTOR_CASING blocks");
            return false;
        }

        if (heatExchangerCount < structure.height - 1) {
            System.out.println("[Reactor Validator] ❌ Heat exchanger column is incomplete");
            errorHandler.setError("Heat exchanger column incomplete");
            return false;
        }

        if (uraniumRodCount < 1) {
            System.out.println("[Reactor Validator] ❌ No uranium rods found in reactor");
            errorHandler.setError("Missing uranium rods");
            return false;
        }

        if (controlRodCount < 1) {
            System.out.println("[Reactor Validator] ⚠ Warning: No control rods found (reactor may not be controllable)");
        }

        // Store counts
        structure.uraniumRodCount = uraniumRodCount;
        structure.controlRodCount = controlRodCount;

        return true;
    }

    /**
     * Data class representing a valid reactor structure.
     */
    public static class ReactorStructure {
        public final BlockPos controllerPos;
        public int width;
        public int height;
        public int uraniumRodCount;
        public int controlRodCount;
        public Set<BlockPos> blocks;

        public ReactorStructure(BlockPos controllerPos) {
            this.controllerPos = controllerPos;
            this.blocks = new HashSet<>();
            this.uraniumRodCount = 0;
            this.controlRodCount = 0;
        }

        public int getUraniumRodCount() {
            return uraniumRodCount;
        }

        public int getControlRodCount() {
            return controlRodCount;
        }

        @Override
        public String toString() {
            return String.format("ReactorStructure{%d×%d, uranium=%d, control=%d, pos=%s}",
                    width, height, uraniumRodCount, controlRodCount, controllerPos);
        }
    }

    /**
     * Helper class for passing error messages back to the caller.
     */
    public static class ErrorHandler {
        private String errorMessage = "";

        public void setError(String message) {
            this.errorMessage = message;
        }

        public String getError() {
            return errorMessage;
        }

        public boolean hasError() {
            return !errorMessage.isEmpty();
        }
    }
}
