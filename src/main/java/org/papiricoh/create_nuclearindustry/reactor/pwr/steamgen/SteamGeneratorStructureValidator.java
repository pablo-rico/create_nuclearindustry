package org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockValidationResult;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates a steam generator from its controller, which sits in a side wall facing out: behind it
 * stands the column of tube bundles, capped above and below by full layers of casing. Errors are
 * translation keys, with their arguments after {@code |}, so goggles and chat show them translated.
 */
public final class SteamGeneratorStructureValidator {
    private static final String KEY = "create_nuclearindustry.steam_generator.error.";
    private static final int MAX_ERRORS = 4;

    private SteamGeneratorStructureValidator() {}

    public static MultiblockValidationResult<SteamGeneratorStructure> validate(Level level, BlockPos controller) {
        List<String> errors = new ArrayList<>();
        SteamGeneratorStructure structure = locate(level, controller, errors);
        if (structure != null) {
            check(level, structure, errors);
            if (errors.isEmpty()) {
                for (SteamGeneratorPortRole role : SteamGeneratorPortRole.values()) {
                    if (structure.ports(role) == 0) {
                        errors.add(KEY + "missing_port|" + role.translationKey());
                        break;
                    }
                }
            }
        }
        return errors.isEmpty() && structure != null
                ? MultiblockValidationResult.valid(structure, List.of())
                : MultiblockValidationResult.invalid(errors, List.of());
    }

    private static SteamGeneratorStructure locate(Level level, BlockPos controller, List<String> errors) {
        BlockState state = level.getBlockState(controller);
        if (!state.is(PwrRegistry.STEAM_GENERATOR_CONTROLLER.get())) {
            errors.add(KEY + "no_controller");
            return null;
        }
        Direction in = state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        BlockPos column = controller.relative(in);
        if (!isTube(level, column)) {
            errors.add(KEY + "no_tubes");
            return null;
        }
        int below = 0;
        while (below <= SteamGeneratorStructure.MAX_HEIGHT && isTube(level, column.below(below + 1))) {
            below++;
        }
        int above = 0;
        while (above <= SteamGeneratorStructure.MAX_HEIGHT && isTube(level, column.above(above + 1))) {
            above++;
        }
        int height = below + above + 3;
        if (height < SteamGeneratorStructure.MIN_HEIGHT || height > SteamGeneratorStructure.MAX_HEIGHT) {
            errors.add(KEY + "height|" + SteamGeneratorStructure.MIN_HEIGHT + "|" + SteamGeneratorStructure.MAX_HEIGHT + "|" + height);
            return null;
        }
        return new SteamGeneratorStructure(column.below(below + 1), controller, height);
    }

    private static void check(Level level, SteamGeneratorStructure structure, List<String> errors) {
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

    private static String checkCell(Level level, SteamGeneratorStructure structure, BlockPos pos, boolean interior) {
        BlockState state = level.getBlockState(pos);
        if (interior) {
            if (!state.is(PwrRegistry.STEAM_GENERATOR_TUBE_BUNDLE.get())) {
                return KEY + "tube";
            }
            structure.tubes++;
            return null;
        }
        if (state.is(PwrRegistry.STEAM_GENERATOR_CASING.get())) {
            return null;
        }
        if (state.is(PwrRegistry.STEAM_GENERATOR_PORT.get())) {
            structure.addPort(pos, state.getValue(SteamGeneratorPortBlock.ROLE));
            return null;
        }
        if (state.is(PwrRegistry.STEAM_GENERATOR_CONTROLLER.get())) {
            return pos.equals(structure.controller) ? null : KEY + "two_controllers";
        }
        return KEY + "casing";
    }

    private static boolean isTube(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(PwrRegistry.STEAM_GENERATOR_TUBE_BUNDLE.get());
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
