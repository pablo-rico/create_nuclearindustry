package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen.SteamGeneratorPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen.SteamGeneratorPortRole;

/**
 * Builds a 3x3 steam generator around {@code bottomCenter}: casing, a column of tube bundles, the
 * controller in the north wall of the first tube layer and one port of each role: primary in on the
 * west wall, primary out on the south wall, feedwater on the east wall and steam out on top.
 */
public final class SteamGeneratorTestBuilder {

    private SteamGeneratorTestBuilder() {}

    public static void build(GameTestHelper helper, BlockPos bottomCenter, int height) {
        build(BlockPlacer.of(helper), bottomCenter, height);
    }

    /** The generator standing on {@code bottomCenter} of {@code placer}. */
    public static void build(BlockPlacer helper, BlockPos bottomCenter, int height) {
        for (int y = 0; y < height; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    boolean tube = dx == 0 && dz == 0 && y > 0 && y < height - 1;
                    helper.set(bottomCenter.offset(dx, y, dz), tube
                            ? PwrRegistry.STEAM_GENERATOR_TUBE_BUNDLE.get() : PwrRegistry.STEAM_GENERATOR_CASING.get());
                }
            }
        }
        helper.set(controller(bottomCenter), PwrRegistry.STEAM_GENERATOR_CONTROLLER.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        port(helper, primaryIn(bottomCenter), SteamGeneratorPortRole.PRIMARY_IN);
        port(helper, primaryOut(bottomCenter), SteamGeneratorPortRole.PRIMARY_OUT);
        port(helper, feedIn(bottomCenter), SteamGeneratorPortRole.FEED_IN);
        port(helper, steamOut(bottomCenter, height), SteamGeneratorPortRole.STEAM_OUT);
    }

    public static void port(GameTestHelper helper, BlockPos pos, SteamGeneratorPortRole role) {
        port(BlockPlacer.of(helper), pos, role);
    }

    public static void port(BlockPlacer helper, BlockPos pos, SteamGeneratorPortRole role) {
        helper.set(pos, PwrRegistry.STEAM_GENERATOR_PORT.get().defaultBlockState().setValue(SteamGeneratorPortBlock.ROLE, role));
    }

    public static BlockPos controller(BlockPos bottomCenter) {
        return bottomCenter.offset(0, 1, -1);
    }

    /** West wall, in the top tube layer of a 4-high generator. */
    public static BlockPos primaryIn(BlockPos bottomCenter) {
        return bottomCenter.offset(-1, 2, 0);
    }

    /** South wall, in the first tube layer. */
    public static BlockPos primaryOut(BlockPos bottomCenter) {
        return bottomCenter.offset(0, 1, 1);
    }

    /** East wall, in the first tube layer. */
    public static BlockPos feedIn(BlockPos bottomCenter) {
        return bottomCenter.offset(1, 1, 0);
    }

    /** Centre of the top layer. */
    public static BlockPos steamOut(BlockPos bottomCenter, int height) {
        return bottomCenter.offset(0, height - 1, 0);
    }
}
