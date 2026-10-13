package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitter;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitters;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.radiation.RadiationRay;
import org.papiricoh.create_nuclearindustry.radiation.RadiationTransmission;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;

import java.util.ArrayList;
import java.util.List;

/** Blocks between a source and a player cut radiation; running reactors become sources. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class RadiationShieldingTests {

    private static Vec3 centre(GameTestHelper helper, int x, int y, int z) {
        return Vec3.atCenterOf(helper.absolutePos(new BlockPos(x, y, z)));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void rayThroughTwoCasings(GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 2, 6), AllNuclearBlocks.REACTOR_CASING.get());
        helper.setBlock(new BlockPos(7, 2, 6), AllNuclearBlocks.REACTOR_CASING.get());
        double t = RadiationRay.transmission(helper.getLevel(), centre(helper, 1, 2, 6), centre(helper, 10, 2, 6));
        helper.assertTrue(Math.abs(t - 0.35 * 0.35) < 1e-3, "two casings should pass 0.35^2, got " + t);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void rayIgnoresSourceAndTargetBlocks(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 6), AllNuclearBlocks.REACTOR_CASING.get());
        helper.setBlock(new BlockPos(10, 2, 6), AllNuclearBlocks.REACTOR_CASING.get());
        double t = RadiationRay.transmission(helper.getLevel(), centre(helper, 1, 2, 6), centre(helper, 10, 2, 6));
        helper.assertTrue(t == 1.0, "the source's and target's own blocks do not shield, got " + t);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void diagonalRayCountsEveryBlockCrossed(GameTestHelper helper) {
        helper.setBlock(new BlockPos(5, 5, 5), Blocks.IRON_BLOCK);
        double t = RadiationRay.transmission(helper.getLevel(), centre(helper, 3, 3, 3), centre(helper, 7, 7, 7));
        helper.assertTrue(Math.abs(t - 0.5) < 1e-6, "a diagonal ray through an iron block passes half, got " + t);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void transmissionComesFromTheDataMap(GameTestHelper helper) {
        helper.assertTrue(RadiationTransmission.of(Blocks.AIR.defaultBlockState()) == 1.0, "air passes everything");
        helper.assertTrue(Math.abs(RadiationTransmission.of(Blocks.WATER.defaultBlockState()) - 0.85) < 1e-6, "water passes 85%");
        helper.assertTrue(Math.abs(RadiationTransmission.of(Blocks.STONE.defaultBlockState()) - 0.9) < 1e-6,
                "unlisted solid blocks pass 90%");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void casingWallCutsAPointSource(GameTestHelper helper) {
        Level level = helper.getLevel();
        Vec3 source = centre(helper, 2, 3, 6);
        Vec3 body = centre(helper, 8, 3, 6);
        double open = RadiationHandler.pointExposure(level, source, 40, body, null);
        for (int y = 2; y <= 4; y++) {
            for (int z = 5; z <= 7; z++) {
                helper.setBlock(new BlockPos(5, y, z), AllNuclearBlocks.REACTOR_CASING.get());
            }
        }
        double walled = RadiationHandler.pointExposure(level, source, 40, body, null);
        helper.assertTrue(open > 0 && walled <= 0.36 * open, "a casing wall should cut it to 35%, " + open + " -> " + walled);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void emittersAreCappedToTheNearest(GameTestHelper helper) {
        Level level = helper.getLevel();
        Vec3 at = centre(helper, 6, 3, 6);
        List<RadiationEmitter> emitters = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            emitters.add(testEmitter(level, at.add(i * 0.5, 0, 0)));
        }
        try {
            emitters.forEach(RadiationEmitters::register);
            List<RadiationEmitter> near = RadiationEmitters.near(level, at, RadiationHandler.EMITTER_RANGE, RadiationHandler.EMITTER_LIMIT);
            helper.assertTrue(near.size() == RadiationHandler.EMITTER_LIMIT, "only the 16 nearest count, got " + near.size());
            helper.assertTrue(near.get(0) == emitters.get(0), "nearest first");
        } finally {
            emitters.forEach(RadiationEmitters::unregister);
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void idleReactorEmitsNothing(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            helper.assertTrue(reactor.radiationStrength() == 0.0, "a reactor without a chain reaction emits nothing");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void runningReactorIrradiatesThroughItsShell(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            CompoundTag physics = new CompoundTag();
            reactor.getPhysicsSimulator().serializeNBT(physics);
            physics.putDouble("neutronLevel", 1000);
            reactor.getPhysicsSimulator().deserializeNBT(physics);
            double strength = reactor.radiationStrength();
            Vec3 body = centre(helper, 10, 3, 3);
            double open = RadiationHandler.pointExposure(helper.getLevel(), reactor.radiationOrigin(), strength, body, null)
                    / RadiationRay.transmission(helper.getLevel(), reactor.radiationOrigin(), body, null);
            double shielded = RadiationHandler.pointExposure(helper.getLevel(), reactor.radiationOrigin(), strength, body,
                    reactor.radiationIgnoreBox());
            physics.putDouble("neutronLevel", 0);
            reactor.getPhysicsSimulator().deserializeNBT(physics);
            helper.assertTrue(Math.abs(strength - 2.0) < 1e-6, "1000 neutrons emit 2/s, got " + strength);
            helper.assertTrue(shielded > 0 && shielded < 0.4 * open, "the casing shell should cut it, " + open + " -> " + shielded);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void backgroundDoesNotAccumulate(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().add(UraniumItem.withEnrichment(new ItemStack(AllNuclearItems.URANIUM.get()), 0.7f));
        player.setData(AllNuclearRadiation.DOSE, 10.0f);
        RadiationHandler.update(player);
        float dose = player.getData(AllNuclearRadiation.DOSE);
        helper.assertTrue(dose < 10.0f, "a natural uranium ingot is background: the dose should decay, got " + dose);
        helper.succeed();
    }

    private static RadiationEmitter testEmitter(Level level, Vec3 origin) {
        return new RadiationEmitter() {
            @Override
            public Level getLevel() {
                return level;
            }

            @Override
            public boolean isRemoved() {
                return false;
            }

            @Override
            public Vec3 radiationOrigin() {
                return origin;
            }

            @Override
            public double radiationStrength() {
                return 1.0;
            }
        };
    }
}
