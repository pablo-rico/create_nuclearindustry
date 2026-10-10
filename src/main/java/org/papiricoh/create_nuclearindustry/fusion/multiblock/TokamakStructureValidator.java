package org.papiricoh.create_nuclearindustry.fusion.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.fusion.FusionConfig;
import org.papiricoh.create_nuclearindustry.fusion.physics.PlasmaModel;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockValidationResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Finds and checks a tokamak around its controller.
 *
 * <p>The controller sits in the shell on the outer equator. Looking towards the axis from
 * it, there are three air blocks (plasma chamber), one inner shell block, the open centre
 * and finally the central solenoid; the distance to the solenoid gives the major radius.
 *
 * <p>Errors and warnings are lang keys with their arguments, joined by {@code |}, so the server can
 * hand them to clients and goggles in the reader's language; {@link #text} turns one into a component.
 */
public final class TokamakStructureValidator {
    private static final String ERROR = "create_nuclearindustry.tokamak.error.";
    private static final String WARNING = "create_nuclearindustry.tokamak.warning.";
    public static final String LEGACY_RING_ERROR = ERROR + "legacy_ring";
    public static final String NOT_FORMED = ERROR + "not_formed";
    public static final String CHAMBER_BLOCKED = ERROR + "chamber_blocked";
    /** Auxiliary heating, MW per ceil(R/4), the warning assumes a player will fit: two neutral beam injectors. */
    public static final double REFERENCE_HEATING = 40.0;

    private TokamakStructureValidator() {}

    public static MultiblockValidationResult<TokamakStructure> validate(Level level, BlockPos controllerPos) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        Optional<TokamakStructure> located = locate(level, controllerPos);
        if (located.isEmpty()) {
            errors.add(LegacyFusionRingDetector.isLegacyRing(level, controllerPos)
                    ? LEGACY_RING_ERROR
                    : message(ERROR + "not_found"));
            return MultiblockValidationResult.invalid(errors, warnings);
        }
        TokamakStructure structure = located.get();
        TokamakGeometry geometry = structure.geometry;
        BlockPos center = structure.center;

        for (int dy = -TokamakGeometry.HALF_HEIGHT; dy <= TokamakGeometry.HALF_HEIGHT; dy++) {
            BlockPos pos = center.offset(0, dy, 0);
            if (!level.getBlockState(pos).is(AllNuclearBlocks.FUSION_CENTRAL_SOLENOID.get())) {
                errors.add(message(ERROR + "solenoid", shortPos(pos)));
                return MultiblockValidationResult.invalid(errors, warnings);
            }
        }

        for (int i = 0; i < geometry.plasmaCount(); i++) {
            BlockPos pos = center.offset(geometry.plasmaX(i), geometry.plasmaY(i), geometry.plasmaZ(i));
            if (!level.getBlockState(pos).isAir()) {
                errors.add(message(CHAMBER_BLOCKED, shortPos(pos)));
                return MultiblockValidationResult.invalid(errors, warnings);
            }
        }

        for (int i = 0; i < geometry.shellCount(); i++) {
            BlockPos pos = center.offset(geometry.shellX(i), geometry.shellY(i), geometry.shellZ(i));
            BlockState state = level.getBlockState(pos);
            if (!classifyShell(structure, pos, state, geometry.shellSector(i))) {
                errors.add(message(ERROR + "invalid_shell", shortPos(pos)));
                if (errors.size() >= 4) {
                    break;
                }
            }
        }
        if (!errors.isEmpty()) {
            return MultiblockValidationResult.invalid(errors, warnings);
        }

        if (structure.magnetInputs.isEmpty()) {
            errors.add(message(ERROR + "no_magnet_drive"));
        }
        if (structure.fuelInjectorCount < 1) {
            errors.add(message(ERROR + "no_fuel_injector"));
        }
        if (structure.coolantPortCount < 1) {
            errors.add(message(ERROR + "no_coolant_port"));
        }
        if (structure.steamPortCount < 1) {
            errors.add(message(ERROR + "no_steam_port"));
        }
        if (!errors.isEmpty()) {
            return MultiblockValidationResult.invalid(errors, warnings);
        }

        warnings.addAll(burnWarnings(structure.majorRadius(), structure.plasmaVolume(),
                structure.coilCoverage(FusionConfig.coilsPerSector), FusionConfig.plasmaParams(), FusionConfig.defaultDensity));
        if (structure.blanketCount == 0) {
            warnings.add(message(WARNING + "no_blankets"));
        }
        return MultiblockValidationResult.valid(structure, warnings);
    }

    /**
     * What stops this tokamak from ever burning, worked out with the plasma model itself (full field,
     * the default fuelling and {@link #REFERENCE_HEATING}), so no coil coverage forms quietly and then
     * never burns.
     */
    public static List<String> burnWarnings(int majorRadius, int volume, double coverage, PlasmaModel.Params params,
                                            double densityFraction) {
        int coverPercent = (int) Math.round(coverage * 100);
        double heating = REFERENCE_HEATING * Math.max(1, (int) Math.ceil(majorRadius / 4.0));
        return switch (PlasmaModel.outlook(params, volume, majorRadius, coverage, heating, densityFraction)) {
            case CAN_BURN -> List.of();
            case UNSTABLE -> List.of(message(WARNING + "unstable", coverPercent, (int) Math.round(PlasmaModel.STABLE_CONFINEMENT * 100)));
            case TOO_COLD -> List.of(message(WARNING + "no_burn", coverPercent, (int) Math.round(heating)));
        };
    }

    /** Finds the axis and radius without checking every block. */
    public static Optional<TokamakStructure> locate(Level level, BlockPos controllerPos) {
        if (!level.getBlockState(controllerPos).is(AllNuclearBlocks.FUSION_CONTROLLER.get())) {
            return Optional.empty();
        }
        int maxRadius = Math.max(TokamakGeometry.MIN_RADIUS, FusionConfig.maxMajorRadius);
        for (Direction inward : Direction.Plane.HORIZONTAL) {
            if (!level.getBlockState(controllerPos.relative(inward, 1)).isAir()
                    || !level.getBlockState(controllerPos.relative(inward, 2)).isAir()
                    || !level.getBlockState(controllerPos.relative(inward, 3)).isAir()
                    || !isShellBlock(level.getBlockState(controllerPos.relative(inward, 4)))) {
                continue;
            }
            for (int radius = TokamakGeometry.MIN_RADIUS; radius <= maxRadius; radius++) {
                BlockPos axis = controllerPos.relative(inward, TokamakGeometry.controllerToAxis(radius));
                if (level.getBlockState(axis).is(AllNuclearBlocks.FUSION_CENTRAL_SOLENOID.get())) {
                    return Optional.of(new TokamakStructure(controllerPos, axis, TokamakGeometry.of(radius)));
                }
            }
        }
        return Optional.empty();
    }

    /** A lang key and its arguments, as stored in a validation result. */
    public static String message(String key, Object... args) {
        StringBuilder out = new StringBuilder(key);
        for (Object arg : args) {
            out.append('|').append(arg);
        }
        return out.toString();
    }

    /** The lang key of a message from {@link #message}. */
    public static String key(String encoded) {
        int bar = encoded.indexOf('|');
        return bar < 0 ? encoded : encoded.substring(0, bar);
    }

    /** A message from {@link #message}, or plain text from elsewhere, as a component. */
    public static Component text(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return Component.empty();
        }
        if (!encoded.startsWith("create_nuclearindustry.")) {
            return Component.literal(encoded);
        }
        String[] parts = encoded.split("\\|");
        return Component.translatable(parts[0], (Object[]) Arrays.copyOfRange(parts, 1, parts.length));
    }

    private static boolean classifyShell(TokamakStructure structure, BlockPos pos, BlockState state, int sector) {
        if (pos.equals(structure.controllerPos)) {
            return state.is(AllNuclearBlocks.FUSION_CONTROLLER.get());
        }
        TokamakShellRole role = TokamakShellRoles.roleOf(state);
        if (role == null) {
            return false;
        }
        structure.addPart(role, pos);
        switch (role) {
            case COIL -> {
                structure.coilCount++;
                structure.coilsPerSector[sector]++;
            }
            case BLANKET -> structure.blanketCount++;
            case MAGNET_DRIVE -> structure.magnetInputs.add(pos);
            case COOLANT_PORT -> {
                structure.fluidPorts.add(pos);
                structure.coolantPortCount++;
            }
            case STEAM_PORT -> {
                structure.fluidPorts.add(pos);
                structure.steamPortCount++;
            }
            case FUEL_IN -> {
                structure.injectors.add(pos);
                structure.fuelInjectorCount++;
            }
            case FUEL_OUT -> {
                structure.injectors.add(pos);
                structure.tritiumOutputCount++;
            }
            default -> {
            }
        }
        return true;
    }

    private static boolean isShellBlock(BlockState state) {
        return TokamakShellRoles.roleOf(state) != null;
    }

    private static String shortPos(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
