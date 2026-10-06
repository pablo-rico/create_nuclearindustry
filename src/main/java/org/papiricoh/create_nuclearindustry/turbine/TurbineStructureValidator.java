package org.papiricoh.create_nuclearindustry.turbine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockValidationResult;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Finds and checks a box turbine around its controller.
 *
 * <p>Rules: the controller sits at the centre of one end face, facing out along the rotor
 * axis. Edges and corners are casing; faces may also hold glass and ports of the same tier;
 * the interior is the rotor column on the axis and air everywhere else.
 */
public final class TurbineStructureValidator {
    public static final int MIN_HALF = 1;
    public static final int MAX_HALF = 3;
    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 16;
    public static final String LEGACY_LINE_ERROR = "legacy_turbine_line";
    private static final int MAX_ERRORS = 4;

    private TurbineStructureValidator() {}

    public static MultiblockValidationResult<TurbineStructure> validate(Level level, BlockPos controllerPos, TurbineTier tier) {
        List<String> errors = new ArrayList<>();
        Optional<TurbineStructure> located = locate(level, controllerPos, tier, errors);
        if (located.isEmpty()) {
            return MultiblockValidationResult.invalid(errors, List.of());
        }
        TurbineStructure structure = located.get();

        for (int a = 0; a < structure.length && errors.size() < MAX_ERRORS; a++) {
            for (int i = -structure.hu; i <= structure.hu && errors.size() < MAX_ERRORS; i++) {
                for (int j = -structure.hv; j <= structure.hv && errors.size() < MAX_ERRORS; j++) {
                    check(level, structure, a, i, j, errors);
                }
            }
        }
        if (errors.isEmpty()) {
            if (structure.inputPorts < 1) {
                errors.add("Missing " + tier.fluidName.toLowerCase() + " input port");
            }
            if (structure.outputPorts < 1 && structure.exhaustPorts < 1) {
                errors.add("Missing condensate output port (or an exhaust port to a cooling tower)");
            }
        }
        return errors.isEmpty()
                ? MultiblockValidationResult.valid(structure, List.of())
                : MultiblockValidationResult.invalid(errors, List.of());
    }

    private static void check(Level level, TurbineStructure s, int a, int i, int j, List<String> errors) {
        BlockPos pos = s.at(a, i, j);
        BlockState state = level.getBlockState(pos);
        int boundaries = (a == 0 || a == s.length - 1 ? 1 : 0) + (Math.abs(i) == s.hu ? 1 : 0) + (Math.abs(j) == s.hv ? 1 : 0);

        if (boundaries == 0) {
            if (i == 0 && j == 0) {
                if (!isRotor(s.tier, state, s.axis)) {
                    errors.add("Rotor missing or misaligned at " + shortPos(pos));
                } else {
                    s.rotors.add(pos);
                }
            } else if (!state.isAir()) {
                errors.add("Turbine interior must be empty at " + shortPos(pos));
            }
            return;
        }

        if (boundaries >= 2) {
            if (s.tier.isCasing(state)) {
                return;
            }
            if (TurbineTier.isGlass(state)) {
                errors.add("Glass cannot sit on an edge or corner at " + shortPos(pos));
            } else if (s.tier.isPort(state) || TurbineTier.isExhaustPort(state)) {
                errors.add("Ports must be on a face, not an edge, at " + shortPos(pos));
            } else {
                errors.add("Invalid frame block at " + shortPos(pos));
            }
            return;
        }

        if (pos.equals(s.controllerPos)) {
            return;
        }
        if (s.tier.isCasing(state)) {
            return;
        }
        if (TurbineTier.isGlass(state)) {
            s.glassCount++;
            return;
        }
        if (s.tier.isPort(state)) {
            s.ports.add(pos.asLong());
            if (s.tier.portMode(state) == ReactorFluidPortMode.INPUT) {
                s.inputPorts++;
            } else {
                s.outputPorts++;
            }
            return;
        }
        if (TurbineTier.isExhaustPort(state)) {
            s.ports.add(pos.asLong());
            s.exhaustPorts++;
            return;
        }
        if (TurbineTier.isAnyController(state)) {
            errors.add("Second turbine controller at " + shortPos(pos));
        } else {
            errors.add("Invalid wall block at " + shortPos(pos));
        }
    }

    /** Measures the box from the controller without checking every block. */
    static Optional<TurbineStructure> locate(Level level, BlockPos controllerPos, TurbineTier tier, List<String> errors) {
        BlockState controllerState = level.getBlockState(controllerPos);
        if (!tier.isController(controllerState)) {
            errors.add("Controller block missing");
            return Optional.empty();
        }
        Direction outward = controllerState.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        Direction inward = outward.getOpposite();
        Direction.Axis axis = outward.getAxis();
        BlockPos first = controllerPos.relative(inward);

        if (!isRotor(tier, level.getBlockState(first), axis)) {
            if (isRotor(tier, level.getBlockState(controllerPos.relative(outward)), axis)) {
                errors.add("Controller faces into the turbine: wrench it so the shaft points out");
            } else {
                errors.add("No rotor behind the controller");
            }
            return Optional.empty();
        }

        Direction[] cross = TurbineStructure.crossSection(axis);
        int hu = halfExtent(level, tier, first, cross[0], errors);
        int hv = halfExtent(level, tier, first, cross[1], errors);
        if (hu < 0 || hv < 0) {
            // The old three-block line has a rotor behind the controller but no walls around it.
            if (LegacyTurbineLineDetector.isLegacyLine(level, controllerPos, tier)) {
                errors.clear();
                errors.add(LEGACY_LINE_ERROR);
            }
            return Optional.empty();
        }

        int rotors = 0;
        BlockPos cursor = first;
        while (isRotor(tier, level.getBlockState(cursor), axis)) {
            rotors++;
            if (rotors > MAX_LENGTH - 2) {
                errors.add("Turbine longer than " + MAX_LENGTH + " blocks");
                return Optional.empty();
            }
            cursor = cursor.relative(inward);
        }
        BlockState end = level.getBlockState(cursor);
        if (end.isAir() || tier.isRotor(end)) {
            errors.add("Rotor column broken at " + shortPos(cursor) + (end.isAir() ? "" : ": rotor turned the wrong way"));
            return Optional.empty();
        }
        return Optional.of(new TurbineStructure(tier, controllerPos, outward, hu, hv, rotors + 2));
    }

    /** Distance from the rotor to the wall along {@code dir}, which must match the opposite side. */
    private static int halfExtent(Level level, TurbineTier tier, BlockPos rotor, Direction dir, List<String> errors) {
        int positive = probe(level, tier, rotor, dir);
        int negative = probe(level, tier, rotor, dir.getOpposite());
        if (positive < 0 || negative < 0) {
            errors.add("Turbine walls must be " + (2 * MIN_HALF + 1) + " to " + (2 * MAX_HALF + 1) + " blocks wide, with an empty interior");
            return -1;
        }
        if (positive != negative) {
            errors.add("Controller must be centred on the end face");
            return -1;
        }
        return positive;
    }

    private static int probe(Level level, TurbineTier tier, BlockPos from, Direction dir) {
        for (int k = 1; k <= MAX_HALF; k++) {
            BlockState state = level.getBlockState(from.relative(dir, k));
            if (state.isAir()) {
                continue;
            }
            return isWall(tier, state) ? k : -1;
        }
        return -1;
    }

    private static boolean isWall(TurbineTier tier, BlockState state) {
        return tier.isCasing(state) || TurbineTier.isGlass(state) || tier.isPort(state) || TurbineTier.isExhaustPort(state);
    }

    private static boolean isRotor(TurbineTier tier, BlockState state, Direction.Axis axis) {
        return tier.isRotor(state) && state.getValue(RotatedPillarBlock.AXIS) == axis;
    }

    private static String shortPos(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
