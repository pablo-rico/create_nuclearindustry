package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoomDevice;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ConfigureModulePayload;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlock;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModule;
import org.papiricoh.create_nuclearindustry.controlroom.desk.DeskShape;
import org.papiricoh.create_nuclearindustry.controlroom.desk.GaugeReading;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleFunction;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleType;
import org.papiricoh.create_nuclearindustry.controlroom.desk.OutputMode;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;

/** Control desks: how they join, what their modules do, and where their redstone goes. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ControlDeskTests {

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void desksInARowJoinIntoOneConsole(GameTestHelper helper) {
        BlockPos[] row = {new BlockPos(4, 1, 8), new BlockPos(5, 1, 8), new BlockPos(6, 1, 8)};
        placeDesks(helper, Direction.NORTH, row);
        BlockState middle = helper.getBlockState(row[1]);
        helper.assertTrue(middle.getValue(ControlDeskBlock.SHAPE) == DeskShape.STRAIGHT, "a row stays straight");
        helper.assertTrue(middle.getValue(ControlDeskBlock.LEFT) && middle.getValue(ControlDeskBlock.RIGHT),
                "the middle desk is joined on both ends");
        BlockState end = helper.getBlockState(row[0]);
        helper.assertTrue(end.getValue(ControlDeskBlock.LEFT) != end.getValue(ControlDeskBlock.RIGHT), "an end desk keeps one cheek");
        helper.succeedWhen(() -> {
            BlockPos origin = helper.<ControlDeskBlockEntity>getBlockEntity(row[0]).origin().orElseThrow().getBlockPos();
            for (BlockPos pos : row) {
                ControlDeskBlockEntity desk = helper.getBlockEntity(pos);
                helper.assertTrue(desk.consoleSize() == 3, "three desks make one console");
                helper.assertTrue(desk.origin().map(o -> o.getBlockPos().equals(origin)).orElse(false), "with one origin");
            }
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void desksAtRightAnglesTurnACorner(GameTestHelper helper) {
        // The second desk sits behind the first and faces across it: the first becomes an outer corner.
        BlockPos first = new BlockPos(5, 1, 5);
        BlockPos behind = new BlockPos(5, 1, 6);
        helper.setBlock(behind, desk(Direction.EAST));
        helper.setBlock(first, desk(Direction.NORTH));
        refresh(helper, first, behind);
        DeskShape shape = helper.getBlockState(first).getValue(ControlDeskBlock.SHAPE);
        helper.assertTrue(shape == DeskShape.OUTER_LEFT || shape == DeskShape.OUTER_RIGHT, "an outer corner, got " + shape);
        helper.succeedWhen(() -> {
            ControlDeskBlockEntity corner = helper.getBlockEntity(first);
            helper.assertFalse(corner.hasSlots(), "corners hold no modules");
            helper.assertFalse(corner.insert(0, ModuleType.LAMP), "so nothing fits");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aDeskInFrontAcrossMakesAnInnerCorner(GameTestHelper helper) {
        BlockPos first = new BlockPos(5, 1, 5);
        BlockPos front = new BlockPos(5, 1, 4);
        helper.setBlock(front, desk(Direction.EAST));
        helper.setBlock(first, desk(Direction.NORTH));
        refresh(helper, first, front);
        DeskShape shape = helper.getBlockState(first).getValue(ControlDeskBlock.SHAPE);
        helper.assertTrue(shape == DeskShape.INNER_LEFT || shape == DeskShape.INNER_RIGHT, "an inner corner, got " + shape);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aConsoleSharesItsLinkAndKeepsItWhenSplit(GameTestHelper helper) {
        BlockPos[] row = {new BlockPos(4, 1, 8), new BlockPos(5, 1, 8), new BlockPos(6, 1, 8)};
        placeDesks(helper, Direction.NORTH, row);
        GlobalPos target = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(1, 1, 1)));
        helper.runAfterDelay(3, () -> {
            helper.<ControlDeskBlockEntity>getBlockEntity(row[2]).setTarget(target);
            for (BlockPos pos : row) {
                helper.assertTrue(target.equals(helper.<ControlDeskBlockEntity>getBlockEntity(pos).getTarget()),
                        "linking one desk links the console");
            }
            helper.destroyBlock(row[1]);
        });
        helper.runAfterDelay(50, () -> {
            for (BlockPos pos : new BlockPos[]{row[0], row[2]}) {
                ControlDeskBlockEntity desk = helper.getBlockEntity(pos);
                helper.assertTrue(desk.consoleSize() == 1, "split into single desks");
                helper.assertTrue(target.equals(desk.getTarget()), "each part keeps the link");
            }
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void modulesFitInEmptySlotsAndComeOut(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 1, 5);
        placeDesks(helper, Direction.NORTH, pos);
        ControlDeskBlockEntity desk = helper.getBlockEntity(pos);
        helper.assertTrue(desk.insert(0, ModuleType.LAMP), "a lamp fits an empty slot");
        helper.assertFalse(desk.insert(0, ModuleType.DIAL), "but not on top of another module");
        helper.assertFalse(desk.insert(4, ModuleType.DIAL), "and there are only four slots");
        helper.assertTrue(desk.remove(0) == ModuleType.LAMP, "it comes back out as a lamp");
        helper.assertTrue(desk.module(0) == null, "leaving the slot empty");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void slotsFollowWhereTheTopIsClicked(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 1, 5);
        placeDesks(helper, Direction.NORTH, pos);
        BlockState state = helper.getBlockState(pos);
        BlockPos abs = helper.absolutePos(pos);
        // Operator to the north: their left is east (+x), the near row is at low z.
        helper.assertTrue(ControlDeskBlock.slotAt(state, abs, abs.getCenter().add(0.25, 0.2, -0.25)) == 0, "near left");
        helper.assertTrue(ControlDeskBlock.slotAt(state, abs, abs.getCenter().add(-0.25, 0.2, -0.25)) == 1, "near right");
        helper.assertTrue(ControlDeskBlock.slotAt(state, abs, abs.getCenter().add(0.25, 0.3, 0.25)) == 2, "far left");
        helper.assertTrue(ControlDeskBlock.slotAt(state, abs, abs.getCenter().add(-0.25, 0.3, 0.25)) == 3, "far right");
        helper.assertTrue(ControlDeskBlock.slotAt(state, abs, abs.getCenter().add(0, -0.3, -0.5)) == -1, "the cabinet is no slot");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void leversAndDialsDriveTheReactor(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        BlockPos pos = new BlockPos(8, 1, 8);
        placeDesks(helper, Direction.NORTH, pos);
        helper.runAfterDelay(5, () -> {
            ControlDeskBlockEntity desk = helper.getBlockEntity(pos);
            desk.insert(0, ModuleType.LEVER);
            desk.insert(1, ModuleType.DIAL);
            desk.configure(1, desk.module(1).withFunction(ModuleFunction.SCRAM_THRESHOLD).writeConfig(helper.getLevel().registryAccess()));
            desk.setTarget(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(FissionTestBuilder.CONTROLLER)));
            boolean auto = reactor.getAutoScram().isEnabled();
            desk.press(0, false);
            helper.assertTrue(reactor.getAutoScram().isEnabled() != auto, "the lever switches the automatic SCRAM");
            int threshold = reactor.getAutoScram().getThreshold();
            desk.press(1, false);
            helper.assertTrue(reactor.getAutoScram().getThreshold() == threshold + ControlModule.THRESHOLD_STEP,
                    "the dial raises the threshold by a step");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void lampsAndGaugesShowTheReactor(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        BlockPos pos = new BlockPos(8, 1, 8);
        placeDesks(helper, Direction.NORTH, pos);
        helper.runAfterDelay(5, () -> {
            ControlDeskBlockEntity desk = helper.getBlockEntity(pos);
            desk.insert(0, ModuleType.LAMP);
            desk.insert(1, ModuleType.GAUGE);
            desk.configure(1, desk.module(1).withReading(GaugeReading.REACTOR_RODS).writeConfig(helper.getLevel().registryAccess()));
            desk.setTarget(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(FissionTestBuilder.CONTROLLER)));
            reactor.setManualScram(true);
        });
        helper.succeedWhen(() -> {
            ControlDeskBlockEntity desk = helper.getBlockEntity(pos);
            helper.assertTrue(desk.module(0) != null && desk.module(1) != null, "the modules should be fitted");
            helper.assertTrue(desk.module(0).shown() == 1 && desk.module(0).signal() == 15, "the SCRAM lamp lights");
            helper.assertTrue(desk.module(1).shown() == 1, "the rod gauge reads fully in, got " + desk.module(1).shown());
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void belowOutputPowersTheBlockUnderneath(GameTestHelper helper) {
        BlockPos lamp = new BlockPos(5, 1, 5);
        BlockPos pos = new BlockPos(5, 2, 5);
        helper.setBlock(lamp, Blocks.REDSTONE_LAMP);
        placeDesks(helper, Direction.NORTH, pos);
        helper.runAfterDelay(2, () -> {
            ControlDeskBlockEntity desk = helper.getBlockEntity(pos);
            desk.insert(0, ModuleType.KEY_SWITCH);
            desk.configure(0, desk.module(0).withOutput(OutputMode.BELOW).writeConfig(helper.getLevel().registryAccess()));
            helper.assertTrue(helper.getLevel().getBestNeighborSignal(helper.absolutePos(lamp)) == 0, "off at first");
            desk.press(0, false);
        });
        helper.succeedWhen(() -> helper.assertTrue(helper.getLevel().getBestNeighborSignal(helper.absolutePos(lamp)) == 15,
                "the switch powers the block below the desk"));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void linkOutputReachesTheRedstoneLinkNetwork(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 1, 5);
        placeDesks(helper, Direction.NORTH, pos);
        Receiver receiver = new Receiver(helper.absolutePos(new BlockPos(9, 1, 9)), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.GOLD_INGOT));
        Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(helper.getLevel(), receiver);
        helper.runAfterDelay(2, () -> {
            ControlDeskBlockEntity desk = helper.getBlockEntity(pos);
            desk.insert(0, ModuleType.KEY_SWITCH);
            desk.configure(0, desk.module(0).withOutput(OutputMode.LINK)
                    .withFrequencies(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.GOLD_INGOT))
                    .writeConfig(helper.getLevel().registryAccess()));
            helper.assertTrue(desk.link(0) != null, "the module joined the network");
            desk.press(0, false);
            helper.assertTrue(receiver.received == 15, "a receiver on the same frequency gets the signal, got " + receiver.received);
            desk.press(0, false);
            helper.assertTrue(receiver.received == 0, "and loses it when switched off");
            receiver.alive = false;
            Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(helper.getLevel(), receiver);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void oldPanelsBecomeDesksWithTheirControls(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 1, 5);
        placeDesks(helper, Direction.NORTH, pos);
        ControlDeskBlockEntity desk = helper.getBlockEntity(pos);
        GlobalPos target = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(1, 1, 1)));
        CompoundTag old = new CompoundTag();
        ControlRoomDevice.writeTarget(old, target);
        old.put("Shown", new CompoundTag());
        desk.loadWithComponents(old, helper.getLevel().registryAccess());
        helper.assertTrue(target.equals(desk.getTarget()), "the panel's link is kept");
        helper.assertTrue(desk.module(0).function() == ModuleFunction.SCRAM && desk.module(1).function() == ModuleFunction.AUTO_SCRAM
                        && desk.module(2).function() == ModuleFunction.ROD_TARGET && desk.module(3).function() == ModuleFunction.SCRAM_THRESHOLD,
                "its buttons become SCRAM, auto SCRAM, rod target and threshold modules");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void setupFromTheScreenIsChecked(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 1, 5);
        placeDesks(helper, Direction.NORTH, pos);
        ControlDeskBlockEntity desk = helper.getBlockEntity(pos);
        desk.insert(0, ModuleType.PUSH_BUTTON);
        CompoundTag config = new ControlModule(ModuleType.PUSH_BUTTON).writeConfig(helper.getLevel().registryAccess());
        config.putString("Function", ModuleFunction.ROD_TARGET.name());
        config.putString("Label", "§cA label far too long to fit");
        Player far = helper.makeMockPlayer(GameType.SURVIVAL);
        far.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(5, 1, 5)).add(40, 0, 0));
        helper.assertFalse(ConfigureModulePayload.apply(far, new ConfigureModulePayload(helper.absolutePos(pos), 0, config)),
                "a player out of reach cannot change it");
        Player near = helper.makeMockPlayer(GameType.SURVIVAL);
        near.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(5, 1, 3)));
        helper.assertTrue(ConfigureModulePayload.apply(near, new ConfigureModulePayload(helper.absolutePos(pos), 0, config)),
                "a player in reach can");
        ControlModule module = desk.module(0);
        helper.assertTrue(module.function() == ModuleFunction.SCRAM, "a button cannot take a dial's function");
        helper.assertTrue(module.label().length() <= ControlModule.LABEL_MAX && !module.label().contains("§"),
                "the label is trimmed and stripped of formatting, got " + module.label());
        helper.assertFalse(ConfigureModulePayload.apply(near, new ConfigureModulePayload(helper.absolutePos(pos), 1, config)),
                "an empty slot cannot be set up");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void aLeverSwitchesTokamakFuel(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, true, 0);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
        BlockPos pos = new BlockPos(1, 1, 1);
        placeDesks(helper, Direction.NORTH, pos);
        helper.succeedWhen(() -> {
            helper.assertTrue(tokamak.isFormed(), "the tokamak should form");
            ControlDeskBlockEntity desk = helper.getBlockEntity(pos);
            if (desk.module(0) == null) {
                desk.insert(0, ModuleType.LEVER);
                desk.configure(0, desk.module(0).withFunction(ModuleFunction.TOKAMAK_FUEL).writeConfig(helper.getLevel().registryAccess()));
                desk.setTarget(GlobalPos.of(helper.getLevel().dimension(), tokamak.getBlockPos()));
            }
            boolean fuel = tokamak.isFuelEnabled();
            helper.assertTrue(desk.press(0, false), "the lever acts on the tokamak");
            helper.assertTrue(tokamak.isFuelEnabled() != fuel, "and switches its fuel");
        });
    }

    // ------------------------------------------------------------------ helpers

    private static BlockState desk(Direction facing) {
        return AllNuclearBlocks.REACTOR_CONTROL_PANEL.get().defaultBlockState().setValue(ControlDeskBlock.FACING, facing);
    }

    /** Places desks then works out their shapes and ends, as placing them by hand would. */
    private static void placeDesks(GameTestHelper helper, Direction facing, BlockPos... positions) {
        for (BlockPos pos : positions) {
            helper.setBlock(pos, desk(facing));
        }
        refresh(helper, positions);
    }

    private static void refresh(GameTestHelper helper, BlockPos... positions) {
        for (int pass = 0; pass < 2; pass++) {
            for (BlockPos pos : positions) {
                BlockState state = helper.getBlockState(pos);
                helper.getLevel().setBlock(helper.absolutePos(pos),
                        ControlDeskBlock.withConnections(state, helper.getLevel(), helper.absolutePos(pos)), 3);
            }
        }
    }

    /** A bare listener on a Redstone Link frequency. */
    private static final class Receiver implements IRedstoneLinkable {
        private final BlockPos pos;
        private final Couple<Frequency> key;
        private int received;
        private boolean alive = true;

        private Receiver(BlockPos pos, ItemStack first, ItemStack last) {
            this.pos = pos;
            this.key = Couple.create(Frequency.of(first), Frequency.of(last));
        }

        @Override
        public int getTransmittedStrength() {
            return 0;
        }

        @Override
        public void setReceivedStrength(int power) {
            received = power;
        }

        @Override
        public boolean isListening() {
            return true;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public Couple<Frequency> getNetworkKey() {
            return key;
        }

        @Override
        public BlockPos getLocation() {
            return pos;
        }
    }
}
