package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.flight.Flight;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightTracker;
import org.papiricoh.create_nuclearindustry.missile.flight.RocketShape;
import org.papiricoh.create_nuclearindustry.missile.physics.RocketAttitude;
import org.papiricoh.create_nuclearindustry.missile.rocket.RocketContraption;
import org.papiricoh.create_nuclearindustry.missile.rocket.RocketContraptionEntity;
import org.papiricoh.create_nuclearindustry.missile.rocket.RocketDebrisEntity;
import org.papiricoh.create_nuclearindustry.missile.rocket.RocketRegistry;
import org.papiricoh.create_nuclearindustry.missile.rocket.RocketScan;
import org.papiricoh.create_nuclearindustry.missile.silo.RocketValidator;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloControllerEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * The rocket as a Create contraption: what it captures and refuses, the cargo a flight keeps, the parts shown as stages fall away,
 * spent stages that never touch the world, and puppets that are never saved and turn the way Create expects.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class RocketContraptionTests {
    private static final String BATCH = "rocket_contraption";

    private static SiloControllerEntity silo(GameTestHelper helper, SiloTestBuilder builder) {
        return SiloTests.aimed(helper, builder, helper.makeMockPlayer(GameType.SURVIVAL));
    }

    /** Glues the blocks from {@code from} to {@code to} (relative, inclusive) together. */
    private static void glue(GameTestHelper helper, BlockPos from, BlockPos to) {
        BlockPos a = helper.absolutePos(from), b = helper.absolutePos(to);
        AABB box = new AABB(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1);
        helper.getLevel().addFreshEntity(new SuperGlueEntity(helper.getLevel(), box));
    }

    /** A flight carrying the rocket of a freshly built silo as its cargo, launched from its floor. */
    private static Flight cargoFlight(GameTestHelper helper, SiloTestBuilder builder) {
        ServerLevel level = helper.getLevel();
        RocketValidator.Result result = RocketValidator.validate(level, helper.absolutePos(SiloTestBuilder.FLOOR).above());
        helper.assertTrue(result.valid(), "the rocket is valid, got " + result.error());
        RocketScan scan = RocketScan.scan(level, helper.absolutePos(SiloTestBuilder.FLOOR), result.rocket());
        helper.assertTrue(scan.valid() && scan.contraption() != null, "it assembles, got " + scan.error());
        CompoundTag cargo = scan.contraption().writeNBT(level.registryAccess(), false);
        Vec3 start = Vec3.atBottomCenterOf(helper.absolutePos(SiloTestBuilder.FLOOR).above());
        Vec3 aim = start.add(300, 0, 0);
        return FlightTracker.get(level).launchModular(level, java.util.UUID.randomUUID(), result.rocket().design(), start, aim, BlockPos.containing(aim),
                result.rocket().warhead(), null, scan.shape(result.rocket()), cargo, (float) RocketAttitude.heading(1, 0));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aFinGluedToTheRocketIsCapturedAndWeighs(GameTestHelper helper) {
        SiloControllerEntity silo = silo(helper, new SiloTestBuilder().tanks(2));
        BlockPos fin = SiloTestBuilder.FLOOR.offset(1, 2, 0);
        helper.setBlock(fin, Blocks.IRON_BLOCK);
        glue(helper, SiloTestBuilder.FLOOR.above(2), fin);
        helper.succeedWhen(() -> {
            helper.assertTrue(silo.state() == SiloControllerEntity.State.READY, "a fin glued to a tank flies with it, got " + silo.state() + " " + silo.error());
            RocketScan scan = silo.lastScan();
            helper.assertTrue(scan != null && scan.captured().contains(helper.absolutePos(fin)), "the fin is captured");
            helper.assertTrue(Math.abs(scan.extraMass()[0] - MissileConfig.extraBlockMass) < 1e-9, "and weighs on the first stage, got " + scan.extraMass()[0]);
            helper.assertTrue(scan.radius() >= 1.5f, "it widens the rocket, got " + scan.radius());
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aRocketGluedToItsSiloIsRefused(GameTestHelper helper) {
        SiloControllerEntity silo = silo(helper, new SiloTestBuilder().tanks(2));
        BlockPos fin = SiloTestBuilder.FLOOR.offset(1, 2, 0);
        helper.setBlock(fin, Blocks.IRON_BLOCK);
        // Tank, fin and the east wall all glued in one span.
        glue(helper, SiloTestBuilder.FLOOR.above(2), SiloTestBuilder.FLOOR.offset(2, 2, 0));
        helper.succeedWhen(() -> {
            helper.assertTrue("glued_to_silo".equals(silo.error()), "glue to the wall keeps it on the ground, got " + silo.error());
            helper.assertTrue(silo.errorPos().equals(helper.absolutePos(SiloTestBuilder.FLOOR.offset(2, 2, 0))), "pointing at the wall, got " + silo.errorPos());
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aCarriedChestMustBeEmpty(GameTestHelper helper) {
        SiloControllerEntity silo = silo(helper, new SiloTestBuilder().tanks(2));
        BlockPos chest = SiloTestBuilder.FLOOR.offset(-1, 3, 0);
        helper.setBlock(chest, Blocks.BARREL);
        glue(helper, chest, SiloTestBuilder.FLOOR.above(3));
        if (helper.getLevel().getBlockEntity(helper.absolutePos(chest)) instanceof net.minecraft.world.Container container)
            container.setItem(0, new net.minecraft.world.item.ItemStack(Blocks.DIRT));
        helper.succeedWhen(() -> helper.assertTrue("cargo_not_empty".equals(silo.error()), "a loaded barrel cannot fly, got " + silo.error()));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void theCargoSurvivesASaveAndItsStagesFilter(GameTestHelper helper) {
        SiloTestBuilder builder = new SiloTestBuilder().tanks(1).stages(2);
        silo(helper, builder);
        BlockPos fin = SiloTestBuilder.FLOOR.offset(0, 1, 1);
        helper.setBlock(fin, Blocks.IRON_BLOCK);
        glue(helper, SiloTestBuilder.FLOOR.above(1), fin);
        helper.runAfterDelay(5, () -> {
            ServerLevel level = helper.getLevel();
            Flight flight = cargoFlight(helper, builder);
            helper.assertTrue(flight != null && flight.cargo() != null, "the flight carries the contraption");
            if (flight == null) return;
            FlightTracker tracker = FlightTracker.get(level);
            CompoundTag saved = tracker.save(new CompoundTag(), level.registryAccess());
            Flight reloaded = FlightTracker.load(saved, level.registryAccess()).get(flight.id());
            tracker.remove(level, flight);
            helper.assertTrue(reloaded != null && reloaded.cargo() != null, "the cargo is saved with the flight");
            if (reloaded == null || reloaded.cargo() == null) return;
            Contraption read = Contraption.fromNBT(level, reloaded.cargo(), false);
            helper.assertTrue(read instanceof RocketContraption, "and reads back as a rocket, got " + read);
            if (!(read instanceof RocketContraption rocket)) return;
            // Engine, tank | engine, tank | guidance, warhead, plus the fin on the first engine.
            helper.assertTrue(rocket.blocks().size() == 7, "seven blocks, got " + rocket.blocks().size());
            helper.assertTrue(rocket.stageOf(net.minecraft.core.BlockPos.ZERO.offset(0, 1, 1), -1) == 0, "the fin falls with the first stage");
            RocketContraption upper = rocket.filtered(stage -> stage >= 1, level.registryAccess(), level);
            RocketContraption first = rocket.filtered(stage -> stage == 0, level.registryAccess(), level);
            RocketContraption payload = rocket.filtered(stage -> stage >= 2, level.registryAccess(), level);
            helper.assertTrue(upper.blocks().size() == 4, "without the first stage four blocks are left, got " + upper.blocks().size());
            helper.assertTrue(first.blocks().size() == 3, "the first stage is engine, tank and fin, got " + first.blocks().size());
            helper.assertTrue(payload.blocks().size() == 2, "the payload is guidance and warhead, got " + payload.blocks().size());
            RocketShape shape = reloaded.shape();
            helper.assertTrue(shape.stages().size() == 2 && shape.height() == 6, "its shape has both stages, got " + shape);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 300)
    public static void aFallingStageTouchesNothing(GameTestHelper helper) {
        SiloTestBuilder builder = new SiloTestBuilder().tanks(1).stages(2);
        silo(helper, builder);
        Map<BlockPos, BlockState> before = new HashMap<>();
        RocketDebrisEntity[] debris = {null};
        Flight[] flight = {null};
        helper.runAfterDelay(5, () -> {
            flight[0] = cargoFlight(helper, builder);
            if (flight[0] == null) return;
            // The column comes away from the silo: the arena below must stay as it is.
            for (int i = 1; i <= builder.height(); i++) {
                if (helper.getLevel().getBlockEntity(helper.absolutePos(SiloTestBuilder.FLOOR.above(i))) instanceof org.papiricoh.create_nuclearindustry.missile.silo.RocketSectionEntity section)
                    section.consume();
                helper.setBlock(SiloTestBuilder.FLOOR.above(i), Blocks.AIR);
            }
            BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 0, 0)), helper.absolutePos(new BlockPos(12, 12, 12)))
                    .forEach(p -> before.put(p.immutable(), helper.getLevel().getBlockState(p)));
            Vec3 drop = Vec3.atBottomCenterOf(helper.absolutePos(SiloTestBuilder.FLOOR.above(4)));
            debris[0] = RocketDebrisEntity.create(helper.getLevel(), flight[0], 0, drop, new Vec3(0, -0.2, 0));
            helper.assertTrue(debris[0] != null, "the first stage can fall away");
            if (debris[0] != null) helper.getLevel().addFreshEntity(debris[0]);
            helper.assertTrue(debris[0] != null && !debris[0].shouldBeSaved(), "a falling stage is never saved");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(debris[0] != null && debris[0].isRemoved(), "the stage is still falling");
            for (Map.Entry<BlockPos, BlockState> entry : before.entrySet()) {
                helper.assertTrue(helper.getLevel().getBlockState(entry.getKey()) == entry.getValue(), "nothing changed at " + entry.getKey());
            }
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(SiloTestBuilder.FLOOR)).inflate(10)).isEmpty(),
                    "and nothing dropped");
            if (flight[0] != null) FlightTracker.get(helper.getLevel()).remove(helper.getLevel(), flight[0]);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void thePuppetShedsItsStagesAndIsNeverSaved(GameTestHelper helper) {
        SiloTestBuilder builder = new SiloTestBuilder().tanks(1).stages(2);
        silo(helper, builder);
        helper.runAfterDelay(5, () -> {
            ServerLevel level = helper.getLevel();
            Flight flight = cargoFlight(helper, builder);
            if (flight == null) return;
            RocketContraptionEntity puppet = RocketContraptionEntity.create(level, flight);
            helper.assertTrue(puppet != null, "a puppet is made from the cargo");
            if (puppet == null) return;
            helper.assertTrue(!puppet.shouldBeSaved() && !RocketRegistry.ROCKET.get().canSerialize(), "a puppet is never saved");
            helper.assertTrue(puppet.isReadyForRender(), "it is ready to draw from its first packet");
            Vec3 pivot = puppet.toGlobalVector(new Vec3(0.5, 1, 0.5), 1);
            helper.assertTrue(pivot.distanceTo(flight.position()) < 1e-3, "its tail sits on the flight, off by " + pivot.distanceTo(flight.position()));
            // Fly until the first stage drops.
            for (int i = 0; i < 2000 && flight.flight().stage() == 0 && !flight.flight().burnedOut(); i++) flight.flight().step(MissileConfig.flightParams());
            puppet.follow(flight);
            long solid = puppet.getContraption().getBlocks().entrySet().stream().filter(e -> !e.getValue().state().isAir()).count();
            helper.assertTrue(solid == 4, "once the first stage drops four blocks are shown, got " + solid);
            Vec3 world = flight.position().add(3, 2, -1);
            Vec3 local = puppet.toLocalVector(world, 1);
            Vec3 viaCreate = ContraptionCollider.worldToLocalPos(world, puppet);
            helper.assertTrue(local.distanceTo(viaCreate) < 1e-3, "Create's rotation state turns the same way, off by " + local.distanceTo(viaCreate));
            FlightTracker.get(level).remove(level, flight);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 400)
    public static void aSiloLaunchCarriesItsContraption(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        SiloTestBuilder builder = new SiloTestBuilder().tanks(2);
        SiloControllerEntity silo = SiloTests.aimed(helper, builder, player);
        BlockPos fin = SiloTestBuilder.FLOOR.offset(-1, 1, 0);
        helper.setBlock(fin, Blocks.IRON_BLOCK);
        glue(helper, fin, SiloTestBuilder.FLOOR.above(1));
        BlockPos target = SiloTests.target(helper);
        helper.runAfterDelay(10, () -> {
            silo.arm(player);
            helper.setBlock(SiloTestBuilder.CONTROLLER.below(), Blocks.REDSTONE_BLOCK);
        });
        helper.succeedWhen(() -> {
            Flight launched = FlightTracker.get(level).flights().stream().filter(f -> f.target().equals(target)).findFirst().orElse(null);
            helper.assertTrue(launched != null, "the silo launches, state " + silo.state() + " " + silo.error());
            FlightTracker.get(level).remove(level, launched);
            helper.assertTrue(launched.cargo() != null, "carrying the contraption it was assembled into");
            helper.assertTrue(helper.getBlockState(fin).isAir(), "the glued fin went with it");
            helper.assertTrue(launched.shape().radius() >= 1.5f, "and widens its outline");
            helper.assertTrue(level.getEntitiesOfClass(SuperGlueEntity.class, new AABB(helper.absolutePos(SiloTestBuilder.FLOOR)).inflate(4)).isEmpty(),
                    "its glue went too");
            helper.setBlock(SiloTestBuilder.CONTROLLER.below(), Blocks.AIR);
        });
    }
}
