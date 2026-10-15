package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.coolingtower.blockentity.CoolingTowerControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentMonitorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen.SteamGeneratorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelRegistry;
import org.papiricoh.create_nuclearindustry.turbine.TurbineStructureValidator;
import org.papiricoh.create_nuclearindustry.turbine.TurbineTier;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;

/**
 * The schematics of the "Nuclear power plant" Ponder series (tools/structgen/ponder_plant_*.py) are
 * also GameTest templates: what the scenes show must be a plant that really works in the game.
 * Templates are placed one block above the test origin, so every position is the schematic's plus
 * one in y.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PlantPonderSchematicTests {

    /** The overview's plant is real: reactor with pressurizer, steam generator, turbine, tower and a sealed containment. */
    @GameTest(template = "gametest/plant_overview_ponder", timeoutTicks = 300)
    public static void overviewSchematicIsAWorkingPlant(GameTestHelper helper) {
        helper.succeedWhen(() -> {
            ReactorBlockEntity reactor = at(helper, new BlockPos(5, 6, 8), ReactorBlockEntity.class);
            SteamGeneratorControllerBlockEntity generator = at(helper, new BlockPos(12, 3, 6), SteamGeneratorControllerBlockEntity.class);
            CoolingTowerControllerBlockEntity tower = at(helper, new BlockPos(20, 3, 12), CoolingTowerControllerBlockEntity.class);
            ContainmentMonitorBlockEntity monitor = at(helper, new BlockPos(8, 2, 12), ContainmentMonitorBlockEntity.class);
            var turbine = TurbineStructureValidator.validate(helper.getLevel(), helper.absolutePos(new BlockPos(22, 3, 7)), TurbineTier.STEAM);
            helper.assertTrue(reactor != null && reactor.isFormed(), "the overview's reactor forms");
            helper.assertTrue(reactor.hasPressurizer(), "with the pressurizer on its lid");
            helper.assertTrue(generator != null && generator.isFormed(), "its steam generator forms");
            helper.assertTrue(turbine.formed(), "its turbine forms: " + turbine.primaryMessage());
            helper.assertTrue(tower != null && tower.getStructure().isPresent(),
                    "its cooling tower forms: " + (tower == null ? "missing" : tower.getValidationMessage()));
            helper.assertTrue(monitor != null && monitor.sealed(), "and its containment is sealed, penetrations and all");
        });
    }

    @GameTest(template = "gametest/plant_heat_ponder", timeoutTicks = 200)
    public static void heatLoopSchematicForms(GameTestHelper helper) {
        helper.succeedWhen(() -> {
            ReactorBlockEntity reactor = at(helper, new BlockPos(3, 6, 7), ReactorBlockEntity.class);
            SteamGeneratorControllerBlockEntity generator = at(helper, new BlockPos(10, 3, 5), SteamGeneratorControllerBlockEntity.class);
            helper.assertTrue(reactor != null && reactor.isFormed(), "the reactor of the heat loop scene forms");
            helper.assertTrue(reactor.hasPressurizer(), "with the pressurizer on its lid");
            helper.assertTrue(generator != null && generator.isFormed(), "and so does its steam generator");
        });
    }

    @GameTest(template = "gametest/plant_generation_ponder", timeoutTicks = 200)
    public static void generationSchematicForms(GameTestHelper helper) {
        helper.succeedWhen(() -> {
            SteamGeneratorControllerBlockEntity generator = at(helper, new BlockPos(2, 3, 1), SteamGeneratorControllerBlockEntity.class);
            CoolingTowerControllerBlockEntity tower = at(helper, new BlockPos(10, 3, 6), CoolingTowerControllerBlockEntity.class);
            var turbine = TurbineStructureValidator.validate(helper.getLevel(), helper.absolutePos(new BlockPos(5, 3, 2)), TurbineTier.STEAM);
            helper.assertTrue(generator != null && generator.isFormed(), "the steam generator of the generation scene forms");
            helper.assertTrue(turbine.formed(), "the turbine forms: " + turbine.primaryMessage());
            helper.assertTrue(tower != null, "the cooling tower controller is there");
            helper.assertTrue(tower.getStructure().isPresent(), "and the cooling tower forms: " + tower.getValidationMessage());
        });
    }

    @GameTest(template = "gametest/plant_control_ponder", timeoutTicks = 300)
    public static void controlSchematicIsASealedContainment(GameTestHelper helper) {
        helper.succeedWhen(() -> {
            ReactorBlockEntity reactor = at(helper, new BlockPos(9, 7, 9), ReactorBlockEntity.class);
            ContainmentMonitorBlockEntity monitor = at(helper, new BlockPos(6, 2, 12), ContainmentMonitorBlockEntity.class);
            helper.assertTrue(reactor != null && reactor.isFormed(), "the reactor of the control scene forms");
            helper.assertTrue(monitor != null && monitor.sealed(), "its containment is sealed");
            helper.assertTrue(Math.abs(monitor.getShape().weakestRating() - 1.05) < 1e-6,
                    "and the window is its weakest wall, " + monitor.getShape().weakestRating());
        });
    }

    @GameTest(template = "gametest/plant_waste_ponder")
    public static void wasteSchematicKeepsTheRacksUnderWater(GameTestHelper helper) {
        BlockPos rack = new BlockPos(2, 6, 2);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(SpentFuelRegistry.SPENT_FUEL_RACK.get(), rack);
            helper.assertBlockProperty(rack, BlockStateProperties.WATERLOGGED, true);
            helper.assertBlockPresent(Blocks.WATER, rack.above());
            helper.assertBlockPresent(Blocks.WATER, rack.above(3));
            helper.assertBlockPresent(SpentFuelRegistry.DRY_CASK.get(), new BlockPos(8, 10, 1));
            helper.assertBlockPresent(WasteRegistry.VITRIFIED_WASTE_CANISTER.get(), new BlockPos(8, 2, 1));
            helper.assertBlockPresent(WasteRegistry.BENTONITE.get(), new BlockPos(9, 2, 1));
            helper.assertBlockPresent(WasteRegistry.REPOSITORY_INSPECTOR.get(), new BlockPos(7, 2, 1));
        });
    }

    /**
     * The shutdown scenes' reactor forms inside its ordinary building, with as many control rod blocks
     * as fuel rod blocks: "rods at least as many as the fuel" holds for the core the scenes show.
     */
    @GameTest(template = "gametest/plant_shutdown_ponder", timeoutTicks = 200)
    public static void shutdownSchematicFormsInsideItsBuilding(GameTestHelper helper) {
        helper.succeedWhen(() -> {
            ReactorBlockEntity reactor = at(helper, new BlockPos(6, 6, 7), ReactorBlockEntity.class);
            helper.assertTrue(reactor != null && reactor.isFormed(), "the shutdown scenes' reactor forms");
            helper.assertTrue(reactor.getFuelRodCount() == 12, "with twelve fuel rod blocks, " + reactor.getFuelRodCount());
            helper.assertTrue(reactor.getEffectiveRods() >= reactor.getFuelRodCount(),
                    "and every rod in is worth at least the fuel: " + reactor.getEffectiveRods());
            helper.assertBlockPresent(Blocks.STONE_BRICKS, new BlockPos(6, 9, 7));
            helper.assertBlockPresent(Blocks.STONE_BRICKS, new BlockPos(2, 4, 7));
        });
    }

    /** Every item the series is registered on, and every item under its ponder tag, exists. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void plantPonderItemsExist(GameTestHelper helper) {
        for (String id : PlantPonderText.tagComponents()) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, id)),
                    "no item " + id + " for the nuclear power plant ponder tag");
        }
        helper.succeed();
    }

    private static <T> T at(GameTestHelper helper, BlockPos pos, Class<T> type) {
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        return type.isInstance(blockEntity) ? type.cast(blockEntity) : null;
    }
}
