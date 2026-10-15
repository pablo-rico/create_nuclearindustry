package org.papiricoh.create_nuclearindustry.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorConfig;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamPhase;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamTripCause;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.interlock.BeamInterlockBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.interlock.PersonnelInterlock;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.AttachmentRole;

import java.util.List;
import java.util.UUID;

/**
 * The personnel interlock: a Beam Interlock panel beside the beamline must be secured before the
 * beam may start, opening it trips a running beam, the doors beside it stay shut while the beam is
 * on, and with {@code accelerator.interlock.requirePlayersOutside} nobody may be inside the machine.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class BeamInterlockTests {
    private static final int CAVITIES = 8;
    private static final BlockPos PANEL = AcceleratorTestBuilder.cell(5).east();
    private static final BlockPos GATE = PANEL.east();

    /** The linac with a panel beside it, formed, supplied and pumped down, not started. */
    private static AcceleratorControllerBlockEntity ready(GameTestHelper helper) {
        AcceleratorTestBuilder.linac(helper, CAVITIES, true);
        helper.setBlock(PANEL, AcceleratorRegistry.BEAM_INTERLOCK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        AcceleratorTestBuilder.fillGas(helper, 4000);
        AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.controller(helper);
        if (controller != null) {
            controller.attemptFormation(null);
            controller.setSupplyOverride(AcceleratorTestBuilder.PLENTY);
            controller.primeVacuum();
        }
        return controller;
    }

    private static BeamInterlockBlockEntity panel(GameTestHelper helper) {
        return AcceleratorTestBuilder.entity(helper, PANEL, BeamInterlockBlockEntity.class);
    }

    @GameTest(template = NuclearGameTests.LONG_ARENA)
    public static void anOpenPanelVetoesTheStartAndASecuredOneAllowsIt(GameTestHelper helper) {
        AcceleratorControllerBlockEntity controller = ready(helper);
        helper.assertTrue(controller != null && controller.isFormed(), "the linac should form");
        helper.assertTrue(controller.getStructure().map(s -> s.attachments(AttachmentRole.INTERLOCK).size()).orElse(0) == 1,
                "the panel is one of its machines");
        BeamInterlockBlockEntity panel = panel(helper);
        helper.assertTrue(panel != null && !panel.secured(), "a new panel is open");
        helper.assertTrue(PersonnelInterlock.count(controller.attachments()) == 1, "the controller counts it");
        BeamTripCause refused = controller.requestStart();
        helper.assertTrue(refused == BeamTripCause.INTERLOCK, "an open panel refuses the start: " + refused);
        helper.assertTrue(controller.phase() == BeamPhase.OFFLINE, "and the linac stays offline");
        helper.assertFalse(controller.readings().interlocksClear(), "the readings say so");
        panel.secure(null);
        helper.assertTrue(panel.secured(), "searched and secured");
        BeamTripCause accepted = controller.requestStart();
        helper.assertTrue(accepted == BeamTripCause.NONE, "now it may start: " + accepted);
        helper.assertTrue(controller.phase() == BeamPhase.PUMP_DOWN, "and does");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.LONG_ARENA)
    public static void aPanelWithAnOpenDoorBesideItCannotBeSecured(GameTestHelper helper) {
        AcceleratorControllerBlockEntity controller = ready(helper);
        helper.assertTrue(controller != null && controller.isFormed(), "the linac should form");
        helper.setBlock(GATE, Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(FenceGateBlock.OPEN, true));
        BeamInterlockBlockEntity panel = panel(helper);
        helper.assertTrue(panel != null, "the panel should be there");
        panel.secure(null);
        helper.assertFalse(panel.secured(), "the vault is not closed, so the search does not hold");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.LONG_ARENA, timeoutTicks = 400)
    public static void openingThePanelTripsTheBeamAndDoorsStayShutMeanwhile(GameTestHelper helper) {
        AcceleratorControllerBlockEntity controller = ready(helper);
        helper.assertTrue(controller != null && controller.isFormed(), "the linac should form");
        helper.setBlock(GATE, Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        BeamInterlockBlockEntity panel = panel(helper);
        helper.assertTrue(panel != null, "the panel should be there");
        panel.secure(null);
        helper.assertTrue(controller.requestStart() == BeamTripCause.NONE, "a secured linac starts");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos gate = helper.absolutePos(GATE);
        boolean[] opened = {false};
        boolean[] lockedWhileOn = {false};
        helper.onEachTick(() -> {
            if (!opened[0] && controller.phase() == BeamPhase.BEAM_ON && panel.beamOn()) {
                PlayerInteractEvent.RightClickBlock event = NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player,
                        InteractionHand.MAIN_HAND, gate, new BlockHitResult(Vec3.atCenterOf(gate), Direction.WEST, gate, false)));
                lockedWhileOn[0] = event.isCanceled();
                opened[0] = true;
                panel.open(null);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(opened[0], "waiting for beam, at " + controller.phase() + " (" + controller.tripCause() + ")");
            helper.assertTrue(lockedWhileOn[0], "the gate beside the panel stayed shut while the beam was on");
            helper.assertTrue(controller.tripCause() == BeamTripCause.INTERLOCK, "opening the panel trips the beam: " + controller.tripCause());
            helper.assertTrue(controller.phase() == BeamPhase.OFFLINE, "which is dumped and ramps down: " + controller.phase());
            helper.assertFalse(panel.beamOn(), "the panel hears the beam is gone");
            PlayerInteractEvent.RightClickBlock after = NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player,
                    InteractionHand.MAIN_HAND, gate, new BlockHitResult(Vec3.atCenterOf(gate), Direction.WEST, gate, false)));
            helper.assertFalse(after.isCanceled(), "and the gate opens again");
        });
    }

    @GameTest(template = NuclearGameTests.LONG_ARENA, timeoutTicks = 100)
    public static void nobodyMayBeInsideWhenTheConfigAsks(GameTestHelper helper) {
        AcceleratorControllerBlockEntity controller = ready(helper);
        helper.assertTrue(controller != null && controller.isFormed(), "the linac should form");
        BeamInterlockBlockEntity panel = panel(helper);
        helper.assertTrue(panel != null, "the panel should be there");
        panel.secure(null);
        // A server player standing on the beamline: mock players are not in the level's player list.
        FakePlayer inside = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "interlock_test"));
        Vec3 onTheLine = Vec3.atBottomCenterOf(helper.absolutePos(AcceleratorTestBuilder.cell(7).above()));
        inside.setPos(onTheLine.x, onTheLine.y, onTheLine.z);
        List<ServerPlayer> players = helper.getLevel().players();
        boolean saved = AcceleratorConfig.interlockRequiresPlayersOutside;
        try {
            AcceleratorConfig.interlockRequiresPlayersOutside = false;
            players.add(inside);
            helper.assertTrue(controller.requestStart() == BeamTripCause.NONE, "by default a player inside does not stop it");
            controller.requestStop();
        } finally {
            players.remove(inside);
            AcceleratorConfig.interlockRequiresPlayersOutside = saved;
        }
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(controller.phase() == BeamPhase.OFFLINE, "stopped again first: " + controller.phase());
            boolean before = AcceleratorConfig.interlockRequiresPlayersOutside;
            try {
                AcceleratorConfig.interlockRequiresPlayersOutside = true;
                players.add(inside);
                BeamTripCause vetoed = controller.requestStart();
                helper.assertTrue(vetoed == BeamTripCause.INTERLOCK, "with the config on, a player inside vetoes the start: " + vetoed);
                helper.assertTrue(controller.phase() == BeamPhase.OFFLINE, "it stays offline");
                inside.setPos(onTheLine.x + 40, onTheLine.y, onTheLine.z);
                helper.assertFalse(controller.getStructure().map(s -> PersonnelInterlock.occupied(helper.getLevel(), s.bounds())).orElse(true),
                        "once outside, the machine is clear");
                BeamTripCause accepted = controller.requestStart();
                helper.assertTrue(accepted == BeamTripCause.NONE, "and the beam may start at once: " + accepted);
            } finally {
                players.remove(inside);
                AcceleratorConfig.interlockRequiresPlayersOutside = before;
            }
            helper.succeed();
        });
    }
}
