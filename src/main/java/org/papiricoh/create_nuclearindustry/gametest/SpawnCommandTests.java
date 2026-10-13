package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.commands.StructurePreset;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;

/**
 * {@code /nuclear spawn} places every preset formed: each one is built in an arena (over terrain,
 * which it clears) and its controller reports it formed within a few ticks; the command itself
 * places a fuelled reactor.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class SpawnCommandTests {
    private static final BlockPos CORNER = new BlockPos(1, 1, 1);

    private static void spawns(GameTestHelper helper, StructurePreset preset, int settleTicks) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(CORNER);
        // Something in the way: the preset clears its space before building.
        helper.setBlock(CORNER.offset(1, 1, 1), Blocks.STONE);
        StructurePreset.Spawned[] spawned = new StructurePreset.Spawned[1];
        helper.runAfterDelay(1, () -> spawned[0] = preset.spawn(level, origin, false));
        helper.runAfterDelay(1 + settleTicks, () -> {
            helper.assertTrue(spawned[0] != null, "spawned");
            helper.assertTrue(StructurePreset.formed(level, spawned[0].controller()),
                    preset.id() + " forms: " + StructurePreset.problem(level, spawned[0].controller()).getString());
            helper.assertTrue(spawned[0].bounds().isInside(spawned[0].controller()), "the controller lies inside the reported bounds");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void reactorSpawnsFormed(GameTestHelper helper) {
        spawns(helper, StructurePreset.REACTOR, 5);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void turbineSpawnsFormed(GameTestHelper helper) {
        spawns(helper, StructurePreset.TURBINE, 5);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void fusionTurbineSpawnsFormed(GameTestHelper helper) {
        spawns(helper, StructurePreset.FUSION_TURBINE, 5);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void coolingTowerSpawnsFormed(GameTestHelper helper) {
        spawns(helper, StructurePreset.COOLING_TOWER, 5);
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void tokamakSpawnsFormed(GameTestHelper helper) {
        spawns(helper, StructurePreset.TOKAMAK, 5);
    }

    @GameTest(template = NuclearGameTests.LONG_ARENA)
    public static void acceleratorSpawnsFormed(GameTestHelper helper) {
        spawns(helper, StructurePreset.ACCELERATOR, 5);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void steamGeneratorSpawnsFormed(GameTestHelper helper) {
        spawns(helper, StructurePreset.STEAM_GENERATOR, 5);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void cryoplantSpawnsFormed(GameTestHelper helper) {
        spawns(helper, StructurePreset.CRYOPLANT, 5);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void containmentSpawnsSealed(GameTestHelper helper) {
        spawns(helper, StructurePreset.CONTAINMENT, 60);
    }

    /** The command itself, with a position and {@code fuelled}: a formed reactor with fuel and water. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void theCommandPlacesAFuelledReactor(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(CORNER);
        helper.runAfterDelay(1, () -> {
            CommandSourceStack source = level.getServer().createCommandSourceStack().withLevel(level)
                    .withPosition(Vec3.atCenterOf(origin)).withSuppressedOutput();
            level.getServer().getCommands().performPrefixedCommand(source,
                    "nuclear spawn reactor " + origin.getX() + " " + origin.getY() + " " + origin.getZ() + " fuelled");
        });
        helper.runAfterDelay(5, () -> {
            ReactorBlockEntity reactor = helper.getBlockEntity(FissionTestBuilder.controllerAt(CORNER));
            helper.assertTrue(reactor != null && reactor.isFormed(), "the reactor stands formed");
            helper.assertTrue(reactor.getFuelRemaining() > 0, "with fuel: " + reactor.getFuelRemaining());
            helper.assertTrue(reactor.getCoolantAmount() == reactor.getTankCapacity(), "and a full tank: " + reactor.getCoolantAmount());
            helper.succeed();
        });
    }
}
