package org.papiricoh.create_nuclearindustry.fusion.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionFluidPortBlock;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionFuelInjectorBlock;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockValidationResult;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Finds and checks a tokamak around its controller.
 *
 * <p>The controller sits in the shell on the outer equator. Looking towards the axis from
 * it, there are three air blocks (plasma chamber), one inner shell block, the open centre
 * and finally the central solenoid; the distance to the solenoid gives the major radius.
 */
public final class TokamakStructureValidator {
    public static final String LEGACY_RING_ERROR = "legacy_ring";

    private TokamakStructureValidator() {}

    public static MultiblockValidationResult<TokamakStructure> validate(Level level, BlockPos controllerPos) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        Optional<TokamakStructure> located = locate(level, controllerPos);
        if (located.isEmpty()) {
            errors.add(LegacyFusionRingDetector.isLegacyRing(level, controllerPos)
                    ? LEGACY_RING_ERROR
                    : "No tokamak found: place the controller on the outer equator, facing a 3-block plasma chamber, "
                    + "with the central solenoid at the axis");
            return MultiblockValidationResult.invalid(errors, warnings);
        }
        TokamakStructure structure = located.get();
        TokamakGeometry geometry = structure.geometry;
        BlockPos center = structure.center;

        for (int dy = -TokamakGeometry.HALF_HEIGHT; dy <= TokamakGeometry.HALF_HEIGHT; dy++) {
            BlockPos pos = center.offset(0, dy, 0);
            if (!level.getBlockState(pos).is(AllNuclearBlocks.FUSION_CENTRAL_SOLENOID.get())) {
                errors.add("Central solenoid incomplete at " + shortPos(pos));
                return MultiblockValidationResult.invalid(errors, warnings);
            }
        }

        for (int i = 0; i < geometry.plasmaCount(); i++) {
            BlockPos pos = center.offset(geometry.plasmaX(i), geometry.plasmaY(i), geometry.plasmaZ(i));
            if (!level.getBlockState(pos).isAir()) {
                errors.add("Plasma chamber blocked at " + shortPos(pos));
                return MultiblockValidationResult.invalid(errors, warnings);
            }
        }

        for (int i = 0; i < geometry.shellCount(); i++) {
            BlockPos pos = center.offset(geometry.shellX(i), geometry.shellY(i), geometry.shellZ(i));
            BlockState state = level.getBlockState(pos);
            if (!classifyShell(structure, pos, state, geometry.shellSector(i))) {
                errors.add("Invalid shell block at " + shortPos(pos));
                if (errors.size() >= 4) {
                    break;
                }
            }
        }
        if (!errors.isEmpty()) {
            return MultiblockValidationResult.invalid(errors, warnings);
        }

        if (structure.magnetInputs.isEmpty()) {
            errors.add("Missing magnet drive");
        }
        if (structure.fuelInjectorCount < 1) {
            errors.add("Missing fuel injector set to INPUT");
        }
        if (structure.coolantPortCount < 1) {
            errors.add("Missing coolant input port");
        }
        if (structure.steamPortCount < 1) {
            errors.add("Missing plasma steam output port");
        }
        if (!errors.isEmpty()) {
            return MultiblockValidationResult.invalid(errors, warnings);
        }

        if (structure.coilCoverage(Config.fusionCoilsPerSector) < 0.65) {
            warnings.add("Too few electromagnets for stable confinement");
        }
        if (structure.blanketCount == 0) {
            warnings.add("No breeding blankets: tritium will not be replaced");
        }
        return MultiblockValidationResult.valid(structure, warnings);
    }

    /** Finds the axis and radius without checking every block. */
    public static Optional<TokamakStructure> locate(Level level, BlockPos controllerPos) {
        if (!level.getBlockState(controllerPos).is(AllNuclearBlocks.FUSION_CONTROLLER.get())) {
            return Optional.empty();
        }
        int maxRadius = Math.max(TokamakGeometry.MIN_RADIUS, Config.fusionMaxMajorRadius);
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

    private static boolean classifyShell(TokamakStructure structure, BlockPos pos, BlockState state, int sector) {
        Block block = state.getBlock();
        if (pos.equals(structure.controllerPos)) {
            return block == AllNuclearBlocks.FUSION_CONTROLLER.get();
        }
        if (block == AllNuclearBlocks.FUSION_ELECTROMAGNET.get()) {
            structure.coilCount++;
            structure.coilsPerSector[sector]++;
            return true;
        }
        if (block == AllNuclearBlocks.FUSION_BREEDING_BLANKET.get()) {
            structure.blanketCount++;
            return true;
        }
        if (block == AllNuclearBlocks.FUSION_MAGNET_INPUT.get()) {
            structure.magnetInputs.add(pos);
            return true;
        }
        if (block == AllNuclearBlocks.FUSION_FLUID_PORT.get()) {
            structure.fluidPorts.add(pos);
            if (state.getValue(FusionFluidPortBlock.MODE) == ReactorFluidPortMode.INPUT) {
                structure.coolantPortCount++;
            } else {
                structure.steamPortCount++;
            }
            return true;
        }
        if (block == AllNuclearBlocks.FUSION_FUEL_INJECTOR.get()) {
            structure.injectors.add(pos);
            if (state.getValue(FusionFuelInjectorBlock.MODE) == ReactorFluidPortMode.INPUT) {
                structure.fuelInjectorCount++;
            } else {
                structure.tritiumOutputCount++;
            }
            return true;
        }
        return isVesselBlock(block);
    }

    private static boolean isShellBlock(BlockState state) {
        Block block = state.getBlock();
        return isVesselBlock(block)
                || block == AllNuclearBlocks.FUSION_ELECTROMAGNET.get()
                || block == AllNuclearBlocks.FUSION_BREEDING_BLANKET.get()
                || block == AllNuclearBlocks.FUSION_MAGNET_INPUT.get()
                || block == AllNuclearBlocks.FUSION_FLUID_PORT.get()
                || block == AllNuclearBlocks.FUSION_FUEL_INJECTOR.get();
    }

    private static boolean isVesselBlock(Block block) {
        return block == AllNuclearBlocks.FUSION_ACCELERATOR_SEGMENT.get()
                || block == AllNuclearBlocks.FUSION_ACCELERATOR_CORNER.get()
                || block == AllNuclearBlocks.FUSION_CRYOSTAT_CASING.get();
    }

    private static String shortPos(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
