package org.papiricoh.create_nuclearindustry.coolingtower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.coolingtower.block.CoolingTowerFluidPortBlock;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockValidationResult;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import java.util.Optional;

/**
 * Validates a hyperboloid cooling tower from its controller, which sits in the bottom wall
 * layer facing out. {@link #locate} measures the tower cheaply along a few rays; {@link #check}
 * then walks every floor, wall and interior cell.
 */
public final class CoolingTowerStructureValidator {
    public static final int FAN_LAYERS = 3;
    public static final int PORT_LAYERS = 2;
    private static final int MAX_ERRORS = 4;

    private CoolingTowerStructureValidator() {}

    public static MultiblockValidationResult<CoolingTowerStructure> validate(Level level, BlockPos controller) {
        List<String> errors = new ArrayList<>();
        CoolingTowerStructure structure = locate(level, controller, errors);
        if (structure == null) {
            return MultiblockValidationResult.invalid(errors, List.of());
        }
        check(level, structure, errors);
        if (errors.isEmpty() && (structure.inputPorts == 0 || structure.outputPorts == 0)) {
            errors.add("Needs a Fluid Port set to INPUT for steam and one set to OUTPUT for water");
        }
        return errors.isEmpty()
                ? MultiblockValidationResult.valid(structure, List.of())
                : MultiblockValidationResult.invalid(errors, List.of());
    }

    /** The floor the locator found behind the controller: enough to know where the tower can be. */
    private record Base(Direction out, BlockPos center, int firstRadius, int baseRadius) {}

    /**
     * Where the blocks of the tower a controller is looking for can be, once its floor is laid:
     * the floor's footprint with room for the walls above it. Empty until the interior and the floor
     * behind the controller are there, so an unformed controller listens to the whole space where a
     * tower could stand and narrows down as it is built.
     */
    public static Optional<BoundingBox> locateFootprint(Level level, BlockPos controller) {
        Base base = locateBase(level, controller, new ArrayList<>());
        if (base == null) {
            return Optional.empty();
        }
        int reach = base.baseRadius() + 2;
        BlockPos center = base.center();
        return Optional.of(new BoundingBox(center.getX() - reach, center.getY() - 1, center.getZ() - reach,
                center.getX() + reach, center.getY() + CoolingTowerGeometry.maxHeight(base.baseRadius()) + 2, center.getZ() + reach));
    }

    private static CoolingTowerStructure locate(Level level, BlockPos controller, List<String> errors) {
        Base base = locateBase(level, controller, errors);
        if (base == null) {
            return null;
        }
        Direction out = base.out();
        BlockPos center = base.center();
        int firstRadius = base.firstRadius();
        int baseRadius = base.baseRadius();
        int height = CoolingTowerGeometry.ABSOLUTE_MAX_HEIGHT + 1;
        for (int y = 1; y <= CoolingTowerGeometry.ABSOLUTE_MAX_HEIGHT; y++) {
            boolean any = false;
            for (int r = 1; r <= baseRadius + 1 && !any; r++) {
                any = !level.getBlockState(center.above(y).relative(out, r)).isAir();
            }
            if (!any) {
                height = y;
                break;
            }
        }
        if (!CoolingTowerGeometry.isValid(baseRadius, height)) {
            errors.add("A tower of base radius " + baseRadius + " must be " + CoolingTowerGeometry.minHeight(baseRadius)
                    + " to " + CoolingTowerGeometry.maxHeight(baseRadius) + " high, found " + height);
            return null;
        }
        CoolingTowerGeometry geometry = CoolingTowerGeometry.of(baseRadius, height);
        if (geometry.radius(1) != firstRadius) {
            errors.add("The wall above the floor should be radius " + geometry.radius(1) + " for a tower of base radius "
                    + baseRadius + " and height " + height + ", found " + firstRadius);
            return null;
        }
        return new CoolingTowerStructure(center, controller, out, geometry);
    }

    private static Base locateBase(Level level, BlockPos controller, List<String> errors) {
        BlockState state = level.getBlockState(controller);
        if (!state.is(AllNuclearBlocks.COOLING_TOWER_CONTROLLER.get())) {
            errors.add("Not a cooling tower controller");
            return null;
        }
        Direction out = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Direction in = out.getOpposite();
        int reach = 2 * CoolingTowerGeometry.MAX_BASE_RADIUS + 2;

        int start = -1;
        int end = -1;
        for (int k = 1; k <= reach; k++) {
            boolean air = level.getBlockState(controller.relative(in, k)).isAir();
            if (start < 0) {
                if (air) {
                    start = k;
                    end = k;
                }
            } else if (air) {
                end = k;
            } else {
                break;
            }
        }
        if (start < 0) {
            errors.add("No tower interior behind the controller: it must face out of the tower");
            return null;
        }
        if ((start + end) % 2 != 0) {
            errors.add("The controller must be centred on a side of the tower");
            return null;
        }
        int distance = (start + end) / 2;
        int firstRadius = (end - start) / 2 + 1;
        BlockPos center = controller.relative(in, distance).below();
        if (!isFloorBlock(level.getBlockState(center))) {
            errors.add("Missing casing in the floor under the tower's centre");
            return null;
        }

        int baseRadius = 0;
        while (baseRadius < CoolingTowerGeometry.MAX_BASE_RADIUS + 1 && isFloorBlock(level.getBlockState(center.relative(out, baseRadius + 1)))) {
            baseRadius++;
        }
        if (baseRadius < CoolingTowerGeometry.MIN_BASE_RADIUS || baseRadius > CoolingTowerGeometry.MAX_BASE_RADIUS) {
            errors.add("The floor's radius must be " + CoolingTowerGeometry.MIN_BASE_RADIUS + " to "
                    + CoolingTowerGeometry.MAX_BASE_RADIUS + ", found " + baseRadius);
            return null;
        }
        return new Base(out, center, firstRadius, baseRadius);
    }

    private static void check(Level level, CoolingTowerStructure structure, List<String> errors) {
        CoolingTowerGeometry geometry = structure.geometry;
        int r = geometry.baseRadius();
        for (int y = 0; y < geometry.height(); y++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    CoolingTowerGeometry.Cell cell = geometry.classify(dx, y, dz);
                    if (cell == CoolingTowerGeometry.Cell.OUTSIDE) {
                        continue;
                    }
                    BlockPos pos = structure.at(dx, y, dz);
                    String error = checkCell(level, structure, cell, pos, dx, y, dz);
                    if (error != null) {
                        errors.add(error + " at " + pos.toShortString());
                        if (errors.size() >= MAX_ERRORS) {
                            return;
                        }
                    }
                }
            }
        }
    }

    private static String checkCell(Level level, CoolingTowerStructure structure, CoolingTowerGeometry.Cell cell,
                                    BlockPos pos, int dx, int y, int dz) {
        BlockState state = level.getBlockState(pos);
        if (cell == CoolingTowerGeometry.Cell.INTERIOR) {
            return state.isAir() ? null : "The tower's interior must be empty";
        }
        if (state.is(AllNuclearBlocks.COOLING_TOWER_CASING.get())) {
            return null;
        }
        if (state.is(AllNuclearBlocks.COOLING_TOWER_FLUID_PORT.get())) {
            if (cell == CoolingTowerGeometry.Cell.WALL && y > PORT_LAYERS) {
                return "Fluid ports go in the floor or the two wall layers above it";
            }
            structure.ports.add(pos.asLong());
            if (state.getValue(CoolingTowerFluidPortBlock.MODE) == ReactorFluidPortMode.INPUT) {
                structure.inputPorts++;
            } else {
                structure.outputPorts++;
            }
            return null;
        }
        if (cell == CoolingTowerGeometry.Cell.FLOOR) {
            return "Missing cooling tower casing in the floor";
        }
        if (state.is(AllNuclearBlocks.COOLING_TOWER_CONTROLLER.get())) {
            return pos.equals(structure.controller) ? null : "A tower has a single controller";
        }
        if (state.is(AllNuclearBlocks.COOLING_TOWER_FAN.get())) {
            if (y > FAN_LAYERS) {
                return "Fans go in the three wall layers above the floor";
            }
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            int rho = TokamakGeometry.rho(dx + facing.getStepX(), dz + facing.getStepZ());
            if (rho <= structure.geometry.outerRadius(y)) {
                return "Fans must face out of the tower";
            }
            structure.fans.add(pos);
            return null;
        }
        return "Missing cooling tower casing";
    }

    private static boolean isFloorBlock(BlockState state) {
        return state.is(AllNuclearBlocks.COOLING_TOWER_CASING.get()) || state.is(AllNuclearBlocks.COOLING_TOWER_FLUID_PORT.get());
    }
}
