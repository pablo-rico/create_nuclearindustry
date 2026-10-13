package org.papiricoh.create_nuclearindustry.accelerator.multiblock;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorConfig;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockText;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockValidationResult;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds the accelerator a controller heads. The ion source must sit right in front of the controller
 * (its shaft across the beam); from it the injector line runs on until a terminal. A line ending in
 * a beam dump or target station is a linac. Missing machines are warnings, not errors: the machine
 * forms, and the startup sequence says what it lacks.
 */
public final class AcceleratorValidator {
    public static final String PREFIX = "create_nuclearindustry.accelerator.";
    public static final String NOT_FORMED = PREFIX + "error.not_formed";

    private AcceleratorValidator() {}

    public static MultiblockValidationResult<AcceleratorStructure> validate(Level level, BlockPos controller) {
        BlockState controllerState = level.getBlockState(controller);
        if (!controllerState.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return invalid(NOT_FORMED);
        }
        Direction facing = controllerState.getValue(HorizontalDirectionalBlock.FACING);
        BlockPos source = controller.relative(facing);
        BlockState sourceState = level.getBlockState(source);
        if (!(sourceState.getBlock() instanceof BeamElementBlock element) || element.beamRole(sourceState) != BeamElementRole.ION_SOURCE) {
            return invalid(MultiblockText.message(PREFIX + "error.no_source"));
        }
        if (sourceState.hasProperty(BlockStateProperties.FACING)
                && sourceState.getValue(BlockStateProperties.FACING).getAxis() == facing.getAxis()) {
            return invalid(MultiblockText.message(PREFIX + "error.source_shaft"));
        }

        LongOpenHashSet visited = new LongOpenHashSet();
        visited.add(controller.asLong());
        visited.add(source.asLong());
        BeamlineWalker.Result injector = BeamlineWalker.walk(level, source.relative(facing), facing,
                AcceleratorConfig.maxLineLength, visited);
        if (!injector.ok()) {
            return invalid(injector.error());
        }
        BeamlinePath line = injector.path();
        Map<AttachmentRole, Set<BlockPos>> aroundSource = new EnumMap<>(AttachmentRole.class);
        BeamlineWalker.collectAround(level, source, facing, aroundSource);
        Map<AttachmentRole, List<BlockPos>> sourceMachines = new EnumMap<>(AttachmentRole.class);
        aroundSource.forEach((role, set) -> sourceMachines.put(role, List.copyOf(set)));

        RingSection ring = null;
        BeamlinePath extraction = null;
        List<Map<AttachmentRole, List<BlockPos>>> machineMaps = new ArrayList<>(List.of(line.attachments(), sourceMachines));
        if (line.terminalRole() == BeamElementRole.INJECTION) {
            RingValidator.Result found = RingValidator.find(level, line.terminal(), line.exit(), visited);
            if (!found.ok()) {
                return invalid(found.error());
            }
            ring = found.ring();
            extraction = found.extraction();
            machineMaps.add(ring.attachments());
            machineMaps.add(extraction.attachments());
            if (extraction.terminalRole() == BeamElementRole.INJECTION) {
                return invalid(MultiblockText.message(PREFIX + "error.extraction_into_ring"));
            }
        }

        AcceleratorStructure structure = new AcceleratorStructure(controller, facing, source, line, ring, extraction,
                AcceleratorStructure.merge(machineMaps));
        return MultiblockValidationResult.valid(structure, warnings(structure));
    }

    private static List<String> warnings(AcceleratorStructure s) {
        Set<String> warnings = new LinkedHashSet<>();
        int cavities = s.copperCavities() + s.superconductingCavities() + (s.ring == null ? 0 : s.ring.cavities());
        if (cavities == 0) {
            warnings.add(MultiblockText.message(PREFIX + "warning.no_cavities"));
        } else if (s.attachments(AttachmentRole.KLYSTRON).isEmpty()) {
            warnings.add(MultiblockText.message(PREFIX + "warning.no_klystrons"));
        }
        if (s.magnets() > 0 && s.attachments(AttachmentRole.MAGNET_POWER).isEmpty()) {
            warnings.add(MultiblockText.message(PREFIX + "warning.no_magnet_supplies"));
        }
        if (s.attachments(AttachmentRole.VACUUM_PUMP).isEmpty()) {
            warnings.add(MultiblockText.message(PREFIX + "warning.no_pumps"));
        }
        if (s.superconducting() > 0 && s.attachments(AttachmentRole.CRYO).isEmpty()) {
            warnings.add(MultiblockText.message(PREFIX + "warning.no_cryo"));
        }
        return new ArrayList<>(warnings);
    }

    private static MultiblockValidationResult<AcceleratorStructure> invalid(String error) {
        return MultiblockValidationResult.invalid(List.of(error), List.of());
    }
}
