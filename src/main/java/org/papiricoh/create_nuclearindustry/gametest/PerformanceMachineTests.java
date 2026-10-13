package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.fluids.block.DualFluidPipeBlock;
import org.papiricoh.create_nuclearindustry.fluids.blockentity.DualFluidPipeBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantPortBlockEntity;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen.SteamGeneratorControllerBlockEntity;

import java.util.function.Consumer;

/**
 * What machine controllers, ports and pipes cost the server: a controller at rest updates its clients
 * only every {@code performance.forcedSyncTicks} instead of every ten ticks, parts are looked up once a
 * second instead of every tick, ports keep their neighbours' handlers and their controller, a pipe
 * changing colour does not wake its neighbours, and an unformed tokamak or accelerator listens to a
 * smaller area, while a broken tokamak still re-forms by itself when it is repaired.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PerformanceMachineTests {
    private static final BlockPos CENTER = new BlockPos(6, 1, 6);

    // ------------------------------------------------------------------ controllers at rest

    /** Used to send every 10 ticks: twenty updates in 200 ticks. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void aSteamGeneratorAtRestOnlySendsTheForcedUpdates(GameTestHelper helper) {
        SteamGeneratorTestBuilder.build(helper, CENTER, 4);
        BlockPos pos = SteamGeneratorTestBuilder.controller(CENTER);
        int[] before = new int[1];
        helper.runAfterDelay(40, () -> {
            SteamGeneratorControllerBlockEntity controller = helper.getBlockEntity(pos);
            helper.assertTrue(controller.getStructure().isPresent(), "the steam generator forms");
            before[0] = controller.clientSyncCount();
        });
        helper.runAfterDelay(240, () -> {
            SteamGeneratorControllerBlockEntity controller = helper.getBlockEntity(pos);
            int sent = controller.clientSyncCount() - before[0];
            helper.assertTrue(sent <= 3, "200 ticks at rest send the forced updates only, not twenty: " + sent);
            helper.succeed();
        });
    }

    /** A visible change (1% of a tank) goes out with the next check, not the next forced update. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void aSteamGeneratorSendsAVisibleChangeAtOnce(GameTestHelper helper) {
        SteamGeneratorTestBuilder.build(helper, CENTER, 4);
        BlockPos pos = SteamGeneratorTestBuilder.controller(CENTER);
        int[] before = new int[1];
        helper.runAfterDelay(40, () -> {
            SteamGeneratorControllerBlockEntity controller = helper.getBlockEntity(pos);
            before[0] = controller.clientSyncCount();
            controller.getFeedTank().fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE);
        });
        helper.runAfterDelay(52, () -> {
            SteamGeneratorControllerBlockEntity controller = helper.getBlockEntity(pos);
            helper.assertTrue(controller.clientSyncCount() > before[0], "1000 mB of feedwater is sent within ten ticks");
            helper.succeed();
        });
    }

    /**
     * A cryogenic plant at rest sends the forced updates only and looks its compressors up once a
     * second, not every tick; its ports keep their controller and their neighbours.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void aCryoplantAtRestLooksItsPartsUpOnceASecond(GameTestHelper helper) {
        CryoplantTestBuilder.build(helper, CENTER, 3, 2, 0);
        BlockPos pos = CryoplantTestBuilder.controller(CENTER);
        BlockPos portPos = CryoplantTestBuilder.heliumIn(CENTER);
        int[] before = new int[4];
        helper.runAfterDelay(40, () -> {
            CryoplantControllerBlockEntity plant = helper.getBlockEntity(pos);
            CryoplantPortBlockEntity port = helper.getBlockEntity(portPos);
            helper.assertTrue(plant.getStructure().isPresent(), "the plant forms: " + plant.getValidationMessage());
            before[0] = plant.clientSyncCount();
            before[1] = plant.compressorLookups();
            before[2] = port.neighbourLookups();
            before[3] = port.controllerLookups();
        });
        helper.runAfterDelay(240, () -> {
            CryoplantControllerBlockEntity plant = helper.getBlockEntity(pos);
            CryoplantPortBlockEntity port = helper.getBlockEntity(portPos);
            int sent = plant.clientSyncCount() - before[0];
            int compressors = plant.compressorLookups() - before[1];
            helper.assertTrue(sent <= 3, "200 ticks at rest send the forced updates only, not twenty: " + sent);
            helper.assertTrue(compressors >= 5 && compressors <= 11, "compressors looked up once a second, not 200 times: " + compressors);
            helper.assertTrue(port.isLinked(), "the port is linked");
            helper.assertTrue(port.neighbourLookups() == before[2], "nothing changed around the port, so no side is looked up again: "
                    + (port.neighbourLookups() - before[2]));
            int controllerLookups = port.controllerLookups();
            IFluidHandler handler = port.getFluidHandler(null);
            for (int i = 0; i < 100; i++) {
                helper.assertTrue(handler.getTankCapacity(0) == CryoplantControllerBlockEntity.TANK_CAPACITY, "the port reaches the plant's tank");
            }
            helper.assertTrue(port.controllerLookups() == controllerLookups, "a hundred calls through the port reuse the controller found");
            helper.assertTrue(port.controllerLookups() - before[3] <= 11, "which is looked up again once a second, "
                    + (port.controllerLookups() - before[3]));
            helper.succeed();
        });
    }

    /** A tokamak at rest: forced updates only, and its shell's parts looked up once a second. */
    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, timeoutTicks = 300)
    public static void aTokamakAtRestLooksItsPartsUpOnceASecond(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, true, 0);
        int[] before = new int[2];
        helper.runAfterDelay(20, () -> {
            FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
            helper.assertTrue(tokamak.getStructure().isPresent(), "the tokamak forms");
            before[0] = tokamak.clientSyncCount();
            before[1] = tokamak.partLookups();
        });
        helper.runAfterDelay(220, () -> {
            FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
            int sent = tokamak.clientSyncCount() - before[0];
            int lookups = tokamak.partLookups() - before[1];
            helper.assertTrue(sent <= 3, "200 ticks at rest send the forced updates only, not twenty: " + sent);
            helper.assertTrue(lookups >= 5 && lookups <= 11, "parts looked up once a second, not every tick: " + lookups);
            helper.succeed();
        });
    }

    /** A linac at rest: forced updates only, and the path goes out with the first packet only. */
    @GameTest(template = NuclearGameTests.LONG_ARENA, timeoutTicks = 300)
    public static void anAcceleratorSendsItsPathOnlyWhenItChanges(GameTestHelper helper) {
        AcceleratorTestBuilder.linac(helper, 8, true);
        int[] before = new int[2];
        helper.runAfterDelay(5, () -> {
            AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.controller(helper);
            helper.assertTrue(controller != null, "the controller is there");
            controller.attemptFormation(null);
            helper.assertTrue(controller.isFormed(), "the linac forms: " + controller.validationText().getString());
            ClientboundBlockEntityDataPacket first = controller.getUpdatePacket();
            ClientboundBlockEntityDataPacket stray = controller.getUpdatePacket();
            helper.assertTrue(first != null && stray != null, "update packets are made");
            CompoundTag full = controller.getUpdateTag(helper.getLevel().registryAccess());
            helper.assertTrue(full.contains("injector") && first.getTag().contains("injector"),
                    "the chunk and the packet of the broadcast that formed it carry the path");
            // A packet NeoForge makes for one player is no broadcast: it must not keep the path from the others.
            helper.assertTrue(stray.getTag().contains("injector"), "a second packet before the next broadcast still carries it");
        });
        helper.runAfterDelay(6, () -> {
            AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.controller(helper);
            helper.assertTrue(controller != null, "the controller is there");
            // The next broadcast: the operator touches the energy setpoint.
            controller.setEnergySetpoint(1.0);
            ClientboundBlockEntityDataPacket later = controller.getUpdatePacket();
            CompoundTag full = controller.getUpdateTag(helper.getLevel().registryAccess());
            helper.assertTrue(later != null && !later.getTag().contains("injector") && later.getTag().getBoolean("pathUnchanged"),
                    "a later broadcast leaves it out");
            helper.assertTrue(later.getTag().sizeInBytes() < full.sizeInBytes(), "and is smaller");
        });
        helper.runAfterDelay(20, () -> {
            AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.controller(helper);
            helper.assertTrue(controller != null, "the controller is there");
            before[0] = controller.clientSyncCount();
            before[1] = controller.partLookups();
        });
        helper.runAfterDelay(220, () -> {
            AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.controller(helper);
            helper.assertTrue(controller != null, "the controller is there");
            int sent = controller.clientSyncCount() - before[0];
            int lookups = controller.partLookups() - before[1];
            helper.assertTrue(sent <= 3, "200 ticks at rest send the forced updates only, not twenty: " + sent);
            helper.assertTrue(lookups <= 11, "the source, terminal and attachments looked up once a second at most: " + lookups);
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ unformed interest

    /** A lone accelerator controller listened to ±160 blocks (some 441 chunks); now ±32. */
    @GameTest(template = NuclearGameTests.LONG_ARENA)
    public static void anUnformedAcceleratorListensNearby(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 2, 5);
        helper.setBlock(pos, AcceleratorRegistry.CONTROLLER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        helper.runAfterDelay(3, () -> {
            AcceleratorControllerBlockEntity controller = helper.getBlockEntity(pos);
            BoundingBox bounds = MultiblockRegistry.indexedBounds(controller);
            helper.assertTrue(bounds != null, "the controller is indexed");
            helper.assertTrue(bounds.getXSpan() <= 2 * AcceleratorControllerBlockEntity.UNFORMED_REACH + 1
                            && bounds.getZSpan() <= 2 * AcceleratorControllerBlockEntity.UNFORMED_REACH + 1,
                    "it listens within " + AcceleratorControllerBlockEntity.UNFORMED_REACH + " blocks, got " + bounds);
            helper.succeed();
        });
    }

    /** A lone tokamak controller listened to ±36 blocks; now as far as it looks for its solenoid. */
    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void anUnformedTokamakListensWhereItsTorusCanBe(GameTestHelper helper) {
        BlockPos pos = new BlockPos(20, 4, 20);
        helper.setBlock(pos, AllNuclearBlocks.FUSION_CONTROLLER.get());
        helper.runAfterDelay(3, () -> {
            FusionReactorBlockEntity tokamak = helper.getBlockEntity(pos);
            BoundingBox bounds = MultiblockRegistry.indexedBounds(tokamak);
            helper.assertTrue(bounds != null, "the controller is indexed");
            int reach = org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry.controllerToAxis(
                    Math.max(org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry.MIN_RADIUS,
                            org.papiricoh.create_nuclearindustry.fusion.FusionConfig.maxMajorRadius));
            helper.assertTrue(bounds.getXSpan() <= 2 * reach + 1 && bounds.getZSpan() <= 2 * reach + 1,
                    "it listens within " + reach + " blocks, got " + bounds);
            helper.succeed();
        });
    }

    /** A broken tokamak still listens to its whole torus, so mending the hole re-forms it without a click. */
    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, timeoutTicks = 100)
    public static void aBrokenTokamakReformsWhenRepaired(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, true, 0);
        BlockPos innerWall = TokamakTestBuilder.CENTER.offset(-2, 0, 0);
        BlockState[] wall = new BlockState[1];
        BoundingBox[] torus = new BoundingBox[1];
        helper.runAfterDelay(5, () -> {
            FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
            helper.assertTrue(tokamak.getStructure().isPresent(), "the tokamak forms first");
            torus[0] = tokamak.getStructure().get().bounds();
            wall[0] = helper.getBlockState(innerWall);
            helper.destroyBlock(innerWall);
        });
        helper.runAfterDelay(10, () -> {
            FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
            helper.assertTrue(tokamak.getStructure().isEmpty(), "a hole unforms it");
            BoundingBox bounds = MultiblockRegistry.indexedBounds(tokamak);
            helper.assertTrue(bounds != null && bounds.isInside(torus[0].getCenter())
                            && bounds.isInside(new net.minecraft.core.Vec3i(torus[0].minX(), torus[0].minY(), torus[0].minZ()))
                            && bounds.isInside(new net.minecraft.core.Vec3i(torus[0].maxX(), torus[0].maxY(), torus[0].maxZ())),
                    "unformed, it still listens to the whole torus " + torus[0] + ", got " + bounds);
            helper.setBlock(innerWall, wall[0]);
        });
        helper.runAfterDelay(20, () -> {
            FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
            helper.assertTrue(tokamak.getStructure().isPresent(), "mending the hole re-forms it by itself");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ pipes

    /** The pipe's colour used to change with block updates all round, waking radiation caches and multiblocks. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aPipeChangingColourDoesNotWakeItsNeighbours(GameTestHelper helper) {
        BlockPos pos = new BlockPos(6, 2, 6);
        helper.setBlock(pos, AllNuclearBlocks.DUAL_FLUID_PIPE.get());
        BlockPos absolute = helper.absolutePos(pos);
        int[] notified = new int[1];
        Consumer<BlockEvent.NeighborNotifyEvent> listener = event -> {
            if (event.getPos().equals(absolute)) {
                notified[0]++;
            }
        };
        helper.runAfterDelay(2, () -> {
            DualFluidPipeBlockEntity pipe = helper.getBlockEntity(pos);
            NeoForge.EVENT_BUS.addListener(BlockEvent.NeighborNotifyEvent.class, listener);
            pipe.getFluidHandler(null).fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE);
        });
        helper.runAfterDelay(5, () -> {
            NeoForge.EVENT_BUS.unregister(listener);
            helper.assertTrue(helper.getBlockState(pos).getValue(DualFluidPipeBlock.VISUAL_FLUID) == DualFluidPipeBlock.VisualFluid.WATER,
                    "the pipe shows its water");
            helper.assertTrue(notified[0] == 0, "and no neighbour was told, notified " + notified[0] + " times");
            helper.succeed();
        });
    }

    /**
     * A pipe used to look up all six sides every tick; now once, and again only where something changed.
     *
     * <p>Two dual pipes push into each other every tick (as they always did): the first pushes 320 mB
     * into the new one, which ticks after it and pushes them straight back, so at the end of a tick
     * (when the test looks) the new pipe is empty. What shows they are connected is that the first
     * pipe's handler across that side is the new pipe's, and that no water was lost or made.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void aPipeKeepsItsNeighboursUntilOneChanges(GameTestHelper helper) {
        BlockPos first = new BlockPos(6, 2, 6);
        BlockPos second = first.east();
        helper.setBlock(first, AllNuclearBlocks.DUAL_FLUID_PIPE.get());
        int[] before = new int[1];
        helper.runAfterDelay(2, () -> {
            DualFluidPipeBlockEntity pipe = helper.getBlockEntity(first);
            pipe.getFluidHandler(null).fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE);
            before[0] = pipe.neighbourLookups();
        });
        helper.runAfterDelay(42, () -> {
            DualFluidPipeBlockEntity pipe = helper.getBlockEntity(first);
            helper.assertTrue(pipe.neighbourLookups() == before[0], "40 ticks with nothing around need no lookup, made "
                    + (pipe.neighbourLookups() - before[0]));
            helper.setBlock(second, AllNuclearBlocks.DUAL_FLUID_PIPE.get());
        });
        helper.runAfterDelay(46, () -> {
            DualFluidPipeBlockEntity pipe = helper.getBlockEntity(first);
            DualFluidPipeBlockEntity next = helper.getBlockEntity(second);
            helper.assertTrue(next != null, "the second pipe is there");
            helper.assertTrue(pipe.neighbourLookups() > before[0], "a pipe placed alongside is looked up");
            boolean connected = false;
            for (Direction side : Direction.values()) {
                connected |= pipe.neighbourHandler(side) == next.getFluidHandler(side.getOpposite());
            }
            helper.assertTrue(connected, "the first pipe pushes into the one placed alongside");
            int total = pipe.getFluidHandler(null).getFluidInTank(1).getAmount() + next.getFluidHandler(null).getFluidInTank(1).getAmount();
            helper.assertTrue(total == 1_000, "and the water goes between them, none lost or made: " + total + " mB");
            helper.succeed();
        });
    }
}
