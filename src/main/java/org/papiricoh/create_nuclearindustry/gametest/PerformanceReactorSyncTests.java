package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.multiblock.ReactorStructureValidator;
import org.papiricoh.create_nuclearindustry.reactor.physics.FuelKind;
import org.papiricoh.create_nuclearindustry.reactor.physics.ReactorPhysicsSimulator;
import org.papiricoh.create_nuclearindustry.reactor.visual.RodVisualSync;

import java.util.ArrayList;
import java.util.List;

/**
 * What a fission reactor sends its clients: an update packet the same size however much fuel the
 * core holds (a summary of the fuel, not one entry per assembly), no more than one update every
 * {@link ReactorBlockEntity#SYNC_INTERVAL} ticks while it fills, nothing while nothing visible
 * changes until the forced update, and fuel channels that are only rewritten when their glow step
 * changes.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PerformanceReactorSyncTests {
    /** The biggest reactor: 9 wide, 12 tall, its lowest corner here. */
    private static final BlockPos BIG_ORIGIN = new BlockPos(1, 1, 1);
    private static final int BIG_WIDTH = ReactorStructureValidator.MAX_WIDTH;
    private static final int BIG_HEIGHT = ReactorStructureValidator.MAX_HEIGHT;
    private static final BlockPos BIG_CONTROLLER = BIG_ORIGIN.offset(BIG_WIDTH / 2, BIG_HEIGHT - 1, BIG_WIDTH / 2);
    /**
     * Ceiling of a steady update packet, as {@link CompoundTag#sizeInBytes()} counts it. A 5x5x5 core
     * with two assemblies used to send 14.7 kB without its structure (5.8 kB of it the named display
     * snapshot, 3.2 kB the physics); the packet of a full big core grew by some 380 B per assembly,
     * past 90 kB.
     */
    private static final int STEADY_PACKET_LIMIT = 12_288;

    /**
     * A 9x12x9 shell around 7x7 columns: a heat exchanger in the middle, fuel and control rods in a
     * checkerboard around it (control rods fully in, so the core stays shut down), a coolant input on
     * the west wall and a steam output on the east wall.
     */
    private static void buildBig(GameTestHelper helper) {
        BlockState casing = AllNuclearBlocks.REACTOR_CASING.get().defaultBlockState();
        for (int x = 0; x < BIG_WIDTH; x++) {
            for (int z = 0; z < BIG_WIDTH; z++) {
                boolean wallColumn = x == 0 || z == 0 || x == BIG_WIDTH - 1 || z == BIG_WIDTH - 1;
                Block column = wallColumn ? null
                        : x == BIG_WIDTH / 2 && z == BIG_WIDTH / 2 ? AllNuclearBlocks.HEAT_EXCHANGER.get()
                        : (x + z) % 2 == 0 ? AllNuclearBlocks.URANIUM_ROD.get()
                        : AllNuclearBlocks.CONTROL_ROD.get();
                for (int y = 0; y < BIG_HEIGHT; y++) {
                    BlockPos pos = BIG_ORIGIN.offset(x, y, z);
                    if (wallColumn || y == 0 || y == BIG_HEIGHT - 1) {
                        helper.setBlock(pos, casing);
                    } else {
                        helper.setBlock(pos, column);
                    }
                }
            }
        }
        helper.setBlock(BIG_ORIGIN.offset(0, 2, BIG_WIDTH / 2), port(Direction.WEST, ReactorFluidPortMode.INPUT));
        helper.setBlock(BIG_ORIGIN.offset(BIG_WIDTH - 1, 2, BIG_WIDTH / 2), port(Direction.EAST, ReactorFluidPortMode.OUTPUT));
        helper.setBlock(BIG_CONTROLLER, AllNuclearBlocks.REACTOR_CONTROLLER.get());
    }

    private static BlockState port(Direction facing, ReactorFluidPortMode mode) {
        return AllNuclearBlocks.REACTOR_FLUID_PORT.get().defaultBlockState()
                .setValue(ReactorFluidPortBlock.FACING, facing)
                .setValue(ReactorFluidPortBlock.MODE, mode);
    }

    private static int packetBytes(ReactorBlockEntity reactor) {
        var packet = reactor.getUpdatePacket();
        return packet == null ? -1 : packet.getTag().sizeInBytes();
    }

    /**
     * The packet of a big core with four assemblies and with every channel full weighs the same; the
     * save still holds every assembly, and a client rebuilt from the packet reads the same fuel.
     * Measured and emptied within one tick, so the loaded core never runs.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 100)
    public static void aBigCoresUpdatePacketDoesNotGrowWithItsFuel(GameTestHelper helper) {
        buildBig(helper);
        helper.succeedWhen(() -> {
            ReactorBlockEntity reactor = helper.getBlockEntity(BIG_CONTROLLER);
            helper.assertTrue(reactor != null, "the controller has its block entity");
            helper.assertTrue(reactor.isFormed(), "the big reactor forms: " + reactor.getDisplaySnapshot().validationMessage());
            ReactorPhysicsSimulator physics = reactor.getPhysicsSimulator();
            HolderLookup.Provider registries = helper.getLevel().registryAccess();
            int small;
            int big;
            int loaded;
            CompoundTag saved;
            CompoundTag chunk;
            ReactorBlockEntity client;
            ReactorBlockEntity.ReactorDisplaySnapshot shown;
            try {
                physics.clearFuel();
                // The first packet carries the structure; the steady ones after it do not.
                reactor.getUpdatePacket();
                for (int i = 0; i < 4; i++) {
                    physics.loadFuelAssembly(20f);
                }
                small = packetBytes(reactor);
                while (physics.canLoadFuelAssembly()) {
                    physics.loadFuelAssembly(physics.getLoadedAssemblyCount() % 3 == 0 ? FuelKind.MOX : FuelKind.URANIUM,
                            physics.getLoadedAssemblyCount() % 2 == 0 ? 20f : 5f);
                }
                loaded = physics.getLoadedAssemblyCount();
                big = packetBytes(reactor);
                saved = reactor.saveWithoutMetadata(registries);
                client = new ReactorBlockEntity(reactor.getBlockPos(), reactor.getBlockState());
                chunk = reactor.getUpdateTag(registries);
                client.handleUpdateTag(chunk, registries);
                shown = reactor.getDisplaySnapshot();
            } finally {
                physics.clearFuel();
            }
            helper.assertTrue(loaded >= 100, "the big core holds hundreds of assemblies, " + loaded);
            helper.assertTrue(small > 0 && big > 0, "update packets are made");
            helper.assertTrue(big <= small + 64, "the packet does not grow with the fuel: " + small + " B with 4 assemblies, "
                    + big + " B with " + loaded);
            helper.assertTrue(big < STEADY_PACKET_LIMIT, "and stays under " + STEADY_PACKET_LIMIT + " B, " + big + " B");
            int savedAssemblies = saved.getCompound("physics").getList("assemblies", Tag.TAG_COMPOUND).size();
            helper.assertTrue(savedAssemblies == loaded, "the save keeps every assembly, " + savedAssemblies + " of " + loaded);

            ReactorPhysicsSimulator seen = client.getPhysicsSimulator();
            helper.assertTrue(seen.getLoadedAssemblyCount() == loaded, "the client counts the same assemblies, "
                    + seen.getLoadedAssemblyCount() + " of " + loaded);
            helper.assertTrue(client.isFormed(), "and sees the reactor formed");
            ReactorBlockEntity.ReactorDisplaySnapshot got = client.receivedDisplaySnapshot();
            helper.assertTrue(got != null && got.formed() && got.dimensions().equals(shown.dimensions())
                            && got.statusDisplayName().equals(shown.statusDisplayName())
                            && got.uraniumRodCount() == shown.uraniumRodCount() && got.coolantAmount() == shown.coolantAmount()
                            && got.fuelRemaining() == shown.fuelRemaining() && got.fuelEnrichment() == shown.fuelEnrichment()
                            && got.coreTemperature() == shown.coreTemperature() && got.manualScram() == shown.manualScram(),
                    "the packed display snapshot reads back what the server shows: " + got + " against " + shown);
            helper.assertTrue(!chunk.getCompound("physics").contains("assemblies"),
                    "the chunk data carries no list of assemblies either");
            helper.succeed();
        });
    }

    /** The fuel summary a client gets reads back the same totals as the server's assemblies. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void theFuelSummaryReadsBackTheSameTotals(GameTestHelper helper) {
        helper.runAfterDelay(1, () -> {
            ReactorPhysicsSimulator server = new ReactorPhysicsSimulator(40, 4);
            for (int i = 0; i < 30; i++) {
                server.loadFuelAssembly(i % 4 == 0 ? FuelKind.MOX : FuelKind.URANIUM, i % 2 == 0 ? 20f : 5f);
            }
            server.burnFuel(server.getFuelRemaining() * 0.3);
            CompoundTag packet = new CompoundTag();
            server.serializeForClient(packet);
            ReactorPhysicsSimulator client = new ReactorPhysicsSimulator(0);
            client.deserializeNBT(packet);

            helper.assertTrue(!packet.contains("assemblies"), "no list of assemblies is sent");
            helper.assertTrue(client.getLoadedAssemblyCount() == server.getLoadedAssemblyCount(),
                    "same assemblies, " + client.getLoadedAssemblyCount() + " against " + server.getLoadedAssemblyCount());
            helper.assertTrue(Math.abs(client.getFuelRemaining() - server.getFuelRemaining()) < 1e-6 * Math.max(1, server.getFuelRemaining()),
                    "same fuel left, " + client.getFuelRemaining() + " against " + server.getFuelRemaining());
            helper.assertTrue(Math.abs(client.getFuelCapacity() - server.getFuelCapacity()) < 1e-6 * Math.max(1, server.getFuelCapacity()),
                    "same capacity, " + client.getFuelCapacity() + " against " + server.getFuelCapacity());
            helper.assertTrue(Math.abs(client.getAverageFuelEnrichment() - server.getAverageFuelEnrichment()) < 1e-3,
                    "same average enrichment, " + client.getAverageFuelEnrichment() + " against " + server.getAverageFuelEnrichment());
            helper.assertTrue(Math.abs(client.getCoreTemperature() - server.getCoreTemperature()) < 1e-9, "same core temperature");
            helper.succeed();
        });
    }

    /**
     * Coolant pumped in every tick for five seconds: updates keep going out while the level climbs,
     * never two within {@link ReactorBlockEntity#SYNC_INTERVAL} ticks.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void aFillingReactorUpdatesAtMostOncePerSyncInterval(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        int[] pumped = {0};
        int[] lastCount = {-1};
        List<Long> sendTicks = new ArrayList<>();
        helper.onEachTick(() -> {
            ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
            if (reactor == null || !reactor.isFormed() || pumped[0] >= 100) {
                return;
            }
            int count = reactor.clientSyncCount();
            if (lastCount[0] >= 0) {
                for (int i = lastCount[0]; i < count; i++) {
                    sendTicks.add(helper.getLevel().getGameTime());
                }
            }
            lastCount[0] = count;
            reactor.fillCoolant(new FluidStack(Fluids.WATER, 50), IFluidHandler.FluidAction.EXECUTE);
            pumped[0]++;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(pumped[0] >= 100, "coolant is pumped in for 100 ticks");
            helper.assertTrue(sendTicks.size() >= 3, "the clients see the level climb: " + sendTicks.size() + " updates");
            helper.assertTrue(sendTicks.size() <= 100 / ReactorBlockEntity.SYNC_INTERVAL + 1,
                    "a few updates, not one every few ticks: " + sendTicks);
            for (int i = 1; i < sendTicks.size(); i++) {
                long gap = sendTicks.get(i) - sendTicks.get(i - 1);
                helper.assertTrue(gap >= ReactorBlockEntity.SYNC_INTERVAL,
                        "never two updates within " + ReactorBlockEntity.SYNC_INTERVAL + " ticks: " + sendTicks);
            }
        });
    }

    /**
     * A formed reactor with nothing changing (cold, empty) sends nothing after an update until the
     * forced one, {@link ReactorBlockEntity#forcedSyncInterval()} ticks later. It used to send one a second.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void anIdleReactorStaysQuietUntilTheForcedUpdate(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        int[] start = {-1};
        long[] startTick = {0};
        int[] quiet = {-1};
        helper.onEachTick(() -> {
            ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
            if (reactor == null || !reactor.isFormed()) {
                return;
            }
            long now = helper.getLevel().getGameTime();
            if (start[0] < 0 && helper.getTick() > 10) {
                // A SCRAM goes out at once: the quiet stretch is counted from it.
                reactor.setManualScram(true);
                start[0] = reactor.clientSyncCount();
                startTick[0] = now;
            } else if (start[0] >= 0 && quiet[0] < 0 && now - startTick[0] == ReactorBlockEntity.forcedSyncInterval() - 5) {
                quiet[0] = reactor.clientSyncCount() - start[0];
            }
        });
        helper.succeedWhen(() -> {
            ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
            helper.assertTrue(reactor != null && start[0] >= 0 && quiet[0] >= 0, "the quiet stretch is over");
            helper.assertTrue(quiet[0] == 0, "nothing changed, nothing is sent: " + quiet[0] + " updates in "
                    + (ReactorBlockEntity.forcedSyncInterval() - 5) + " ticks");
            helper.assertTrue(reactor.clientSyncCount() - start[0] >= 1, "until the forced update");
        });
    }

    /**
     * A tank half full, then topped up by 8 mB a tick for 100 ticks (half a percent of the tank per
     * second, like a slow drain the other way): the clients see it move, but only once it has moved
     * by two percent since the last update, not at every percent it crosses (which sent five).
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 400)
    public static void aSlowlyChangingTankSendsOnlyPastItsHysteresis(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        long[] formedAt = {-1};
        int[] start = {-1};
        int[] pumped = {0};
        int[] sent = {-1};
        helper.onEachTick(() -> {
            ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
            if (reactor == null || !reactor.isFormed() || sent[0] >= 0) {
                return;
            }
            long now = helper.getLevel().getGameTime();
            if (formedAt[0] < 0) {
                formedAt[0] = now;
                reactor.fillCoolant(new FluidStack(Fluids.WATER, 8_000), IFluidHandler.FluidAction.EXECUTE);
            } else if (now - formedAt[0] >= 40) {
                if (start[0] < 0) {
                    start[0] = reactor.clientSyncCount();
                }
                if (pumped[0] < 100) {
                    reactor.fillCoolant(new FluidStack(Fluids.WATER, 8), IFluidHandler.FluidAction.EXECUTE);
                    pumped[0]++;
                } else {
                    sent[0] = reactor.clientSyncCount() - start[0];
                }
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(sent[0] >= 0, "the tank is topped up slowly for 100 ticks");
            helper.assertTrue(sent[0] >= 1, "the clients see the level move");
            helper.assertTrue(sent[0] <= 3, "but not at every percent: " + sent[0] + " updates for 5 % in 100 ticks");
        });
    }

    /**
     * Tanks changing every tick (as a running reactor's do) have the visible key compared at most
     * once every {@link ReactorBlockEntity#SYNC_INTERVAL} ticks, five times in 100 ticks; it used to
     * be compared every tick once an interval had passed since the last update.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void theVisibleKeyIsComparedAtMostOncePerSyncInterval(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        int[] start = {-1};
        int[] ticks = {0};
        int[] checks = {-1};
        helper.onEachTick(() -> {
            ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
            if (reactor == null || !reactor.isFormed() || checks[0] >= 0) {
                return;
            }
            if (start[0] < 0) {
                start[0] = reactor.visibleKeyCheckCount();
            }
            // a millibucket a tick: the contents change every tick, the visible level hardly
            reactor.fillCoolant(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE);
            if (++ticks[0] == 100) {
                // read at the same point of the tick as the start: exactly 100 reactor ticks apart
                checks[0] = reactor.visibleKeyCheckCount() - start[0];
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(checks[0] >= 0, "the contents change every tick for 100 ticks");
            helper.assertTrue(checks[0] >= 3, "the key is still compared: " + checks[0] + " times");
            helper.assertTrue(checks[0] <= 100 / ReactorBlockEntity.SYNC_INTERVAL,
                    "at most once every " + ReactorBlockEntity.SYNC_INTERVAL + " ticks: " + checks[0] + " times in 100 ticks");
        });
    }

    /**
     * The fuel channels are only rewritten when what they show changes: a power change within a glow
     * step, or a fall that stays above the step's lower edge, rewrites none of them (and so causes no
     * light update); crossing a step rewrites each channel once.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void fuelChannelsAreOnlyRewrittenWhenTheGlowStepChanges(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        helper.runAfterDelay(5, () -> {
            ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
            helper.assertTrue(reactor != null && reactor.isFormed(), "the reactor forms");
            ReactorStructureValidator.ReactorStructure structure = reactor.getStructure().orElse(null);
            helper.assertTrue(structure != null, "with a structure");
            RodVisualSync visuals = reactor.getRodVisuals();
            List<ReactorPhysicsSimulator.AssemblyView> fuel = List.of(
                    new ReactorPhysicsSimulator.AssemblyView(FuelKind.URANIUM, 0.0),
                    new ReactorPhysicsSimulator.AssemblyView(FuelKind.URANIUM, 0.0));
            int channels = RodVisualSync.rodsOf(structure).size();

            visuals.applyNow(helper.getLevel(), structure, fuel, 0.5, true);
            int lit = visuals.writes();
            int glow = visuals.glow();
            visuals.applyNow(helper.getLevel(), structure, fuel, 0.6, true);
            visuals.applyNow(helper.getLevel(), structure, fuel, 0.4, true);
            visuals.applyNow(helper.getLevel(), structure, fuel, 0.32, true);
            helper.assertTrue(visuals.glow() == glow, "power within the step keeps it, " + glow + " -> " + visuals.glow());
            helper.assertTrue(visuals.writes() == lit, "and rewrites no channel: " + (visuals.writes() - lit) + " writes");

            visuals.applyNow(helper.getLevel(), structure, fuel, 0.9, true);
            helper.assertTrue(visuals.glow() == glow + 1, "full power is the next step, " + visuals.glow());
            helper.assertTrue(visuals.writes() == lit + channels, "each of the " + channels + " channels is rewritten once: "
                    + (visuals.writes() - lit) + " writes");
            visuals.applyNow(helper.getLevel(), structure, List.of(), 0.0, false);
            helper.succeed();
        });
    }
}
