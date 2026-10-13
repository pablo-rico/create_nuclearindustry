package org.papiricoh.create_nuclearindustry.fusion.cryo.plant;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoRegistry;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockValidationResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates a cryogenic plant from its controller, which sits in the middle of a side wall facing
 * out: behind it runs the plant's centre column, 3 to 5 blocks of plant parts high. Every block of
 * the 3x3 footprint is casing, a port, a compressor (shaft facing out) or the controller; the
 * centre of the inner layers is the cold box and must be plain casing. Errors are translation
 * keys with their arguments after {@code |}, so goggles and chat show them translated.
 */
public final class CryoplantStructureValidator {
    public static final String KEY = "create_nuclearindustry.cryoplant.error.";
    private static final int MAX_ERRORS = 4;

    private CryoplantStructureValidator() {}

    public static MultiblockValidationResult<CryoplantStructure> validate(Level level, BlockPos controller) {
        List<String> errors = new ArrayList<>();
        CryoplantStructure structure = locate(level, controller, errors);
        if (structure != null) {
            check(level, structure, errors);
            if (errors.isEmpty()) {
                requireParts(structure, errors);
            }
        }
        return errors.isEmpty() && structure != null
                ? MultiblockValidationResult.valid(structure, List.of())
                : MultiblockValidationResult.invalid(errors, List.of());
    }

    private static CryoplantStructure locate(Level level, BlockPos controller, List<String> errors) {
        BlockState state = level.getBlockState(controller);
        if (!state.is(CryoRegistry.CRYOPLANT_CONTROLLER.get())) {
            errors.add(KEY + "no_controller");
            return null;
        }
        Direction in = state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        BlockPos column = controller.relative(in);
        if (!isPart(level, column)) {
            errors.add(KEY + "no_casing");
            return null;
        }
        int below = 0;
        while (below <= CryoplantStructure.MAX_HEIGHT && isPart(level, column.below(below + 1))) {
            below++;
        }
        int above = 0;
        while (above <= CryoplantStructure.MAX_HEIGHT && isPart(level, column.above(above + 1))) {
            above++;
        }
        int height = below + above + 1;
        if (height < CryoplantStructure.MIN_HEIGHT || height > CryoplantStructure.MAX_HEIGHT) {
            errors.add(KEY + "height|" + CryoplantStructure.MIN_HEIGHT + "|" + CryoplantStructure.MAX_HEIGHT + "|" + height);
            return null;
        }
        return new CryoplantStructure(column.below(below), controller, height);
    }

    private static void check(Level level, CryoplantStructure structure, List<String> errors) {
        for (int y = 0; y < structure.height; y++) {
            boolean cap = y == 0 || y == structure.height - 1;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = structure.bottomCenter.offset(dx, y, dz);
                    String error = checkCell(level, structure, pos, !cap && dx == 0 && dz == 0);
                    if (error != null) {
                        errors.add(error + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ());
                        if (errors.size() >= MAX_ERRORS) {
                            return;
                        }
                    }
                }
            }
        }
    }

    private static String checkCell(Level level, CryoplantStructure structure, BlockPos pos, boolean coldBox) {
        BlockState state = level.getBlockState(pos);
        if (state.is(CryoRegistry.CRYOPLANT_CASING.get())) {
            return null;
        }
        if (coldBox) {
            return KEY + "cold_box";
        }
        if (state.is(CryoRegistry.CRYOPLANT_PORT.get())) {
            structure.addPort(pos, state.getValue(CryoplantPortBlock.MODE));
            return null;
        }
        if (state.is(CryoRegistry.CRYOPLANT_COMPRESSOR.get())) {
            if (structure.contains(pos.relative(state.getValue(CryoplantCompressorBlock.FACING)))) {
                return KEY + "compressor_inward";
            }
            structure.addCompressor(pos);
            return null;
        }
        if (state.is(CryoRegistry.CRYOPLANT_CONTROLLER.get())) {
            return pos.equals(structure.controller) ? null : KEY + "two_controllers";
        }
        return KEY + "casing";
    }

    private static void requireParts(CryoplantStructure structure, List<String> errors) {
        if (structure.compressorPositions().isEmpty()) {
            errors.add(KEY + "no_compressor");
            return;
        }
        if (structure.ports(CryoplantPortMode.LIQUID_AIR_OUT) > 0) {
            return;
        }
        for (CryoplantPortMode mode : new CryoplantPortMode[]{CryoplantPortMode.HELIUM_IN, CryoplantPortMode.LIQUID_HELIUM_OUT}) {
            if (structure.ports(mode) == 0) {
                errors.add(KEY + "missing_port|" + mode.translationKey());
                return;
            }
        }
    }

    private static boolean isPart(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(CryoRegistry.CRYOPLANT_CASING.get()) || state.is(CryoRegistry.CRYOPLANT_PORT.get())
                || state.is(CryoRegistry.CRYOPLANT_COMPRESSOR.get()) || state.is(CryoRegistry.CRYOPLANT_CONTROLLER.get());
    }

    /** A validator error as text: its key translated with the arguments that follow it. */
    public static Component describe(String error) {
        String[] parts = error.split("\\|");
        if (!parts[0].startsWith(KEY)) {
            return Component.literal(error);
        }
        Object[] args = new Object[parts.length - 1];
        for (int i = 1; i < parts.length; i++) {
            args[i - 1] = parts[i].startsWith("create_nuclearindustry.") ? Component.translatable(parts[i]) : parts[i];
        }
        return Component.translatable(parts[0], args);
    }
}
