package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.ControlMonitorBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorPanelBlock;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorPanelBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleSurfaceBlock;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.ControlRoomLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The control room Ponder schematic (tools/structgen/ponder_control_room.py) is also a GameTest
 * template, placed one block above the test origin. Ponder never runs the joins of the blocks it
 * shows, so the schematic must hold them already: every desk, table and wall panel as the game
 * would join it, and every monitor screen where the scenes draw it.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ControlRoomPonderSchematicTests {
    private static final int SIZE = 19;
    private static final int HEIGHT = 7;

    @GameTest(template = "gametest/control_room_ponder")
    public static void everyConsoleIsJoinedAndFilled(GameTestHelper helper) {
        List<String> wrong = new ArrayList<>();
        Set<BlockPos> fitted = new HashSet<>();
        ControlRoomLayout.fits().forEach(fit -> fitted.add(fit.pos()));
        int surfaces = 0;
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < SIZE; z++) {
                    BlockPos pos = new BlockPos(x, y + 1, z);
                    BlockState state = helper.getBlockState(pos);
                    if (!(state.getBlock() instanceof ModuleSurfaceBlock)) {
                        continue;
                    }
                    surfaces++;
                    if (!fitted.contains(new BlockPos(x, y, z))) {
                        wrong.add("(" + x + ", " + y + ", " + z + ") has no modules in the layout");
                    }
                    BlockState joined = ModuleSurfaceBlock.refresh(state, helper.getLevel(), helper.absolutePos(pos));
                    if (!joined.equals(state)) {
                        wrong.add("(" + x + ", " + y + ", " + z + ") " + state + " should be " + joined);
                    }
                }
            }
        }
        helper.assertTrue(surfaces == fitted.size(), "the layout fills " + fitted.size() + " blocks, the room has " + surfaces);
        helper.assertTrue(wrong.isEmpty(), wrong.size() + " blocks differ: " + String.join("; ", wrong));
        helper.succeed();
    }

    @GameTest(template = "gametest/control_room_ponder", timeoutTicks = 100)
    public static void theScreensAreWhereTheScenesDrawThem(GameTestHelper helper) {
        helper.succeedWhen(() -> {
            for (ControlRoomLayout.Screen screen : ControlRoomLayout.SCREENS) {
                BlockPos origin = screen.origin().above();
                ControlMonitorBlockEntity monitor = helper.getLevel().getBlockEntity(helper.absolutePos(origin))
                        instanceof ControlMonitorBlockEntity found ? found : null;
                helper.assertTrue(monitor != null, "a monitor at " + screen.origin());
                helper.assertTrue(monitor.isOrigin(), screen.origin() + " is the origin of its screen");
                helper.assertTrue(monitor.screenWidth() == screen.width() && monitor.screenHeight() == screen.height(),
                        screen.origin() + " heads a " + screen.width() + "x" + screen.height() + " screen, got "
                                + monitor.screenWidth() + "x" + monitor.screenHeight());
            }
        });
    }

    @GameTest(template = "gametest/control_room_ponder")
    public static void theScenesFitModulesOnlyWhereThereAreSlots(GameTestHelper helper) {
        for (ControlRoomLayout.Fit fit : ControlRoomLayout.fits()) {
            BlockPos pos = fit.pos().above();
            ControlDeskBlockEntity entity = helper.getLevel().getBlockEntity(helper.absolutePos(pos))
                    instanceof ControlDeskBlockEntity found ? found : null;
            helper.assertTrue(entity != null, "a module surface at " + fit.pos());
            for (int slot = 0; slot < fit.faces().length; slot++) {
                if (fit.faces()[slot] != null) {
                    helper.assertTrue(entity.insert(slot, fit.faces()[slot].type()),
                            fit.pos() + " has a free slot " + slot + " for " + fit.faces()[slot].type());
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/control_room_ponder", timeoutTicks = 100)
    public static void theAnnunciatorsHangAsTheBoardsTheScenesDraw(GameTestHelper helper) {
        List<String> wrong = new ArrayList<>();
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < SIZE; z++) {
                    BlockPos pos = new BlockPos(x, y + 1, z);
                    BlockState state = helper.getBlockState(pos);
                    if (!(state.getBlock() instanceof AnnunciatorPanelBlock)) {
                        continue;
                    }
                    BlockState joined = AnnunciatorPanelBlock.connect(state, helper.getLevel(), helper.absolutePos(pos));
                    if (!joined.equals(state) || !state.getValue(AnnunciatorPanelBlock.MOUNTED)
                            || !state.canSurvive(helper.getLevel(), helper.absolutePos(pos))) {
                        wrong.add("(" + x + ", " + y + ", " + z + ") " + state + " should be hung as " + joined);
                    }
                }
            }
        }
        helper.assertTrue(wrong.isEmpty(), wrong.size() + " annunciators differ: " + String.join("; ", wrong));
        helper.succeedWhen(() -> {
            board(helper, ControlRoomLayout.ANNUNCIATOR_BOARD, ControlRoomLayout.ANNUNCIATOR_BOARD_WIDTH);
            board(helper, ControlRoomLayout.TOKAMAK_BOARD, ControlRoomLayout.TOKAMAK_BOARD_WIDTH);
        });
    }

    private static void board(GameTestHelper helper, BlockPos origin, int width) {
        AnnunciatorPanelBlockEntity panel = helper.getLevel().getBlockEntity(helper.absolutePos(origin.above()))
                instanceof AnnunciatorPanelBlockEntity found ? found : null;
        helper.assertTrue(panel != null, "an annunciator at " + origin);
        helper.assertTrue(panel.isOrigin() && panel.boardWidth() == width && panel.boardHeight() == 1,
                origin + " heads a board " + width + " wide, got " + panel.boardWidth() + "x" + panel.boardHeight());
    }
}
