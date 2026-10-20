package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;
import org.papiricoh.create_nuclearindustry.civildefense.blastdoor.BlastDoorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.civildefense.blastdoor.BlastDoorLayout;
import org.papiricoh.create_nuclearindustry.civildefense.shelter.ShelterFiltrationUnitBlockEntity;
import org.papiricoh.create_nuclearindustry.fuelcycle.FuelCycleInfo;
import org.papiricoh.create_nuclearindustry.fuelcycle.FuelCycleRegistry;
import org.papiricoh.create_nuclearindustry.fuelcycle.fan.NuclearFanTypes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.GuidePonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText;
import org.papiricoh.create_nuclearindustry.integration.jei.GuideJeiInfo;
import org.papiricoh.create_nuclearindustry.integration.jei.PlantJeiInfo;
import org.papiricoh.create_nuclearindustry.missile.MissileRegistry;
import org.papiricoh.create_nuclearindustry.missile.defense.DefenseRegistry;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.InterceptorBatteryBlock;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.InterceptorBatteryEntity;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.InterceptorBatteryPartBlock;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloPart;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloRegistry;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloTableBlock;
import org.papiricoh.create_nuclearindustry.missile.silo.RocketComposition;
import org.papiricoh.create_nuclearindustry.missile.silo.RocketSectionBlock;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry;
import org.papiricoh.create_nuclearindustry.reactor.visual.ReactorVisualsRegistry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The guide's Ponder schematics (tools/structgen/ponder_{civil_defense,weapons,fan_treatments}.py)
 * are GameTest templates too, so what the scenes show really works: the blast door forms, the
 * shelter is certified, and each fan treatment's catalyst makes that treatment. Templates are
 * placed one block above the test origin, so every position is the schematic's plus one in y. Also
 * the guide's JEI pages: each has its text and covers the parts it was written for.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class GuidePonderSchematicTests {
    /** Their own batch, so the templates never sit next to another area's machines. */
    private static final String BATCH = "guide_ponder";

    @GameTest(template = "gametest/civil_defense_blast_door_ponder", batch = BATCH, timeoutTicks = 100)
    public static void blastDoorSchematicForms(GameTestHelper helper) {
        helper.succeedWhen(() -> {
            BlastDoorControllerBlockEntity controller = at(helper, new BlockPos(2, 3, 2), BlastDoorControllerBlockEntity.class);
            helper.assertTrue(controller != null, "the controller is there");
            BlastDoorLayout layout = controller == null ? null : controller.getLayout();
            helper.assertTrue(layout != null, "the scene's framed door forms");
            helper.assertTrue(layout != null && layout.width() == 3 && layout.height() == 3, "as a 3x3 door: " + layout);
        });
    }

    @GameTest(template = "gametest/civil_defense_shelter_ponder", batch = BATCH, timeoutTicks = 200)
    public static void shelterSchematicIsCertified(GameTestHelper helper) {
        helper.succeedWhen(() -> {
            ShelterFiltrationUnitBlockEntity unit = at(helper, new BlockPos(4, 3, 4), ShelterFiltrationUnitBlockEntity.class);
            helper.assertTrue(unit != null, "the filtration unit is there");
            helper.assertTrue(unit != null && unit.getStatus().certified(),
                    "the scene's room is a certified shelter, got " + (unit == null ? "nothing" : unit.getStatus()));
            helper.assertBlockPresent(CivilDefenseRegistry.SEISMIC_DETECTOR.get(), new BlockPos(2, 3, 4));
        });
    }

    @GameTest(template = "gametest/civil_defense_sirens_ponder", batch = BATCH)
    public static void sirensSchematicLoads(GameTestHelper helper) {
        helper.assertBlockPresent(CivilDefenseRegistry.AIR_RAID_SIREN.get(), new BlockPos(1, 3, 2));
        helper.assertBlockPresent(CivilDefenseRegistry.HAND_CRANK_SIREN.get(), new BlockPos(4, 3, 2));
        helper.succeed();
    }

    @GameTest(template = "gametest/weapons_bombs_ponder", batch = BATCH)
    public static void bombsSchematicLoads(GameTestHelper helper) {
        helper.assertBlockPresent(AllNuclearBlocks.NUCLEAR_BOMB.get(), new BlockPos(2, 2, 2));
        helper.assertBlockPresent(AllNuclearBlocks.DIRTY_BOMB.get(), new BlockPos(4, 2, 2));
        helper.succeed();
    }

    @GameTest(template = "gametest/weapons_thermonuclear_ponder", batch = BATCH)
    public static void thermonuclearSchematicLoads(GameTestHelper helper) {
        helper.assertBlockPresent(ExplosiveRegistry.THERMONUCLEAR_BOMB.get(), new BlockPos(2, 2, 2));
        helper.succeed();
    }

    @GameTest(template = "gametest/weapons_missile_ponder", batch = BATCH)
    public static void missileSchematicLoads(GameTestHelper helper) {
        helper.assertBlockPresent(org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.ENGINE.get(), new BlockPos(2, 2, 2));
        helper.assertBlockPresent(org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.WARHEAD.get(), new BlockPos(2, 5, 2));
        helper.succeed();
    }

    /** The open-pad scene is a valid 3x3 base with its controller deliberately on the east side. */
    @GameTest(template = "gametest/weapons_launch_base_ponder", batch = BATCH)
    public static void launchBaseSchematicLoads(GameTestHelper helper) {
        var launchBase = org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.LAUNCH_BASE.get();
        for (int x = 2; x <= 4; x++) for (int z = 2; z <= 4; z++)
            helper.assertBlockPresent(launchBase, new BlockPos(x, 1, z));
        helper.assertBlockPresent(org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.CONTROLLER.get(), new BlockPos(5, 1, 3));
        helper.assertBlockPresent(org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.FUEL_PORT.get(), new BlockPos(3, 1, 1));
        helper.assertBlockPresent(org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.ENGINE.get(), new BlockPos(3, 2, 3));
        helper.assertBlockPresent(org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.ENGINE.get(), new BlockPos(3, 4, 3));
        helper.assertBlockPresent(org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.WARHEAD.get(), new BlockPos(3, 7, 3));
        // The scene builds a rocket the controller would take: two stages, each an engine with its tank, then guidance and warhead.
        java.util.List<org.papiricoh.create_nuclearindustry.missile.silo.RocketComposition.Section> column = new java.util.ArrayList<>();
        for (int y = 2; y <= 7; y++)
            if (helper.getBlockState(new BlockPos(3, y, 3)).getBlock() instanceof org.papiricoh.create_nuclearindustry.missile.silo.RocketSectionBlock section)
                column.add(section.section);
        boolean valid;
        try { valid = org.papiricoh.create_nuclearindustry.missile.silo.RocketComposition.parse(column).stageStarts().size() == 2; }
        catch (org.papiricoh.create_nuclearindustry.missile.silo.RocketComposition.Invalid e) { valid = false; }
        helper.assertTrue(valid, "the scene's rocket is a valid two-stage rocket: " + column);
        helper.succeed();
    }

    @GameTest(template = "gametest/defense_interceptor_battery_ponder", batch = BATCH)
    public static void interceptorBatterySchematicLoads(GameTestHelper helper) {
        BlockPos master = new BlockPos(2, 3, 2);
        helper.assertBlockPresent(DefenseRegistry.BATTERY.get(), master);
        helper.assertTrue(helper.getBlockState(master).getValue(InterceptorBatteryBlock.FACING) == net.minecraft.core.Direction.NORTH,
                "the battery faces the camera");
        helper.assertTrue(at(helper, master, InterceptorBatteryEntity.class) != null,
                "the schematic retains the master BE that renders the turret");
        for (int offset = 1; offset < 8; offset++) {
            BlockPos pos = master.offset(offset & 1, (offset >> 1) & 1, (offset >> 2) & 1);
            helper.assertBlockPresent(DefenseRegistry.BATTERY_PART.get(), pos);
            var state = helper.getBlockState(pos);
            helper.assertTrue(state.getValue(InterceptorBatteryPartBlock.OFFSET) == offset
                            && InterceptorBatteryPartBlock.master(pos, state).equals(master),
                    "part " + offset + " points back to the master");
        }
        helper.assertBlockPresent(createBlock("shaft"), new BlockPos(2, 2, 2));
        helper.assertBlockPresent(createBlock("creative_motor"), new BlockPos(2, 1, 2));
        helper.assertBlockPresent(createBlock("andesite_funnel"), new BlockPos(1, 3, 2));
        helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.LEVER, new BlockPos(1, 3, 3));
        helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.COMPARATOR, new BlockPos(3, 3, 1));
        helper.assertBlockPresent(org.papiricoh.create_nuclearindustry.missile.radar.RadarRegistry.RADAR.get(), new BlockPos(5, 2, 4));
        helper.succeed();
    }

    @GameTest(template = "gametest/defense_interceptor_silo_ponder", batch = BATCH)
    public static void interceptorSiloSchematicLoads(GameTestHelper helper) {
        for (int x = 2; x <= 4; x++) for (int z = 2; z <= 4; z++)
            helper.assertBlockPresent(SiloRegistry.LAUNCH_BASE.get(), new BlockPos(x, 1, z));
        helper.assertBlockPresent(SiloRegistry.CONTROLLER.get(), new BlockPos(5, 1, 3));
        helper.assertBlockPresent(SiloRegistry.FUEL_PORT.get(), new BlockPos(3, 1, 1));
        helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.LEVER, new BlockPos(5, 2, 3));
        helper.assertBlockPresent(SiloRegistry.ENGINE.get(), new BlockPos(3, 2, 3));
        helper.assertBlockPresent(SiloRegistry.TANK.get(), new BlockPos(3, 3, 3));
        helper.assertBlockPresent(SiloRegistry.ENGINE.get(), new BlockPos(3, 4, 3));
        helper.assertBlockPresent(SiloRegistry.TANK.get(), new BlockPos(3, 5, 3));
        helper.assertBlockPresent(SiloRegistry.GUIDANCE.get(), new BlockPos(3, 6, 3));
        helper.assertBlockPresent(SiloRegistry.KILL_VEHICLE.get(), new BlockPos(3, 7, 3));
        java.util.List<RocketComposition.Section> column = new java.util.ArrayList<>();
        for (int y = 2; y <= 7; y++)
            if (helper.getBlockState(new BlockPos(3, y, 3)).getBlock() instanceof RocketSectionBlock section)
                column.add(section.section);
        boolean valid;
        try {
            RocketComposition composition = RocketComposition.parse(column);
            valid = composition.interceptor() && composition.stageStarts().size() == 2;
        } catch (RocketComposition.Invalid e) { valid = false; }
        helper.assertTrue(valid, "the scene's rocket is a valid two-stage interceptor: " + column);
        for (int i = 0; i < 9; i++) {
            BlockPos pos = new BlockPos(1 + i % 3, 2, 6 + i / 3);
            helper.assertBlockPresent(HoloRegistry.TABLE.get(), pos);
            helper.assertTrue(helper.getBlockState(pos).getValue(HoloTableBlock.PART) == HoloPart.of(i),
                    "command table part " + i + " is formed");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/orbit_ground_station_ponder", batch = BATCH)
    public static void groundStationSchematicLoads(GameTestHelper helper) {
        helper.assertBlockPresent(org.papiricoh.create_nuclearindustry.missile.orbit.OrbitRegistry.GROUND_STATION.get(), new BlockPos(2, 2, 2));
        helper.assertBlockPresent(createBlock("creative_motor"), new BlockPos(2, 1, 2));
        for (int i = 0; i < 9; i++) {
            BlockPos pos = new BlockPos(3 + i % 3, 2, 3 + i / 3);
            helper.assertBlockPresent(HoloRegistry.TABLE.get(), pos);
            helper.assertTrue(helper.getBlockState(pos).getValue(HoloTableBlock.PART) == HoloPart.of(i), "command table part " + i + " is formed");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/orbit_satellite_launch_ponder", batch = BATCH)
    public static void satelliteLaunchSchematicLoads(GameTestHelper helper) {
        for (int x = 2; x <= 4; x++) for (int z = 2; z <= 4; z++)
            helper.assertBlockPresent(SiloRegistry.LAUNCH_BASE.get(), new BlockPos(x, 1, z));
        helper.assertBlockPresent(SiloRegistry.CONTROLLER.get(), new BlockPos(5, 1, 3));
        helper.assertBlockPresent(SiloRegistry.SATELLITE_FAIRING.get(), new BlockPos(3, 9, 3));
        helper.assertBlockPresent(org.papiricoh.create_nuclearindustry.missile.orbit.OrbitRegistry.GROUND_STATION.get(), new BlockPos(5, 2, 7));
        java.util.List<RocketComposition.Section> column = new java.util.ArrayList<>();
        for (int y = 2; y <= 9; y++)
            if (helper.getBlockState(new BlockPos(3, y, 3)).getBlock() instanceof RocketSectionBlock section)
                column.add(section.section);
        boolean valid;
        try {
            RocketComposition composition = RocketComposition.parse(column);
            valid = composition.satellite() && composition.stageStarts().size() == 2;
        } catch (RocketComposition.Invalid e) { valid = false; }
        helper.assertTrue(valid, "the scene's rocket is a valid two-stage launch vehicle: " + column);
        for (int i = 0; i < 9; i++)
            helper.assertBlockPresent(HoloRegistry.TABLE.get(), new BlockPos(1 + i % 3, 2, 6 + i / 3));
        helper.succeed();
    }

    @GameTest(template = "gametest/weapons_holo_table_ponder", batch = BATCH)
    public static void holoTableSchematicLoads(GameTestHelper helper) {
        var centre = helper.getBlockState(new BlockPos(2, 2, 2));
        helper.assertTrue(centre.is(org.papiricoh.create_nuclearindustry.missile.holo.HoloRegistry.TABLE.get())
                && centre.getValue(org.papiricoh.create_nuclearindustry.missile.holo.HoloTableBlock.PART) == org.papiricoh.create_nuclearindustry.missile.holo.HoloPart.CENTER,
                "the table's centre piece is in the middle, formed");
        helper.assertTrue(org.papiricoh.create_nuclearindustry.missile.holo.HoloTable.intact(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2))),
                "and the nine pieces make a whole table");
        helper.assertBlockPresent(org.papiricoh.create_nuclearindustry.missile.radar.RadarRegistry.RADAR.get(), new BlockPos(4, 2, 0));
        helper.succeed();
    }

    /** The acid in the scene is a leaching catalyst, stays put, and is no irradiation catalyst. */
    @GameTest(template = "gametest/fan_acid_leaching_ponder", batch = BATCH)
    public static void acidLeachingSchematicLeaches(GameTestHelper helper) {
        BlockPos acid = new BlockPos(2, 2, 2);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(NuclearFanTypes.ACID_LEACHING.get().isValidAt(helper.getLevel(), helper.absolutePos(acid)),
                    "the fan's air current passes through sulfuric acid");
            helper.assertFalse(NuclearFanTypes.IRRADIATION.get().isValidAt(helper.getLevel(), helper.absolutePos(acid)),
                    "acid does not irradiate");
            helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(new BlockPos(4, 2, 2))).isEmpty(),
                    "the walled-in acid does not run off past the depot");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/fan_irradiation_ponder", batch = BATCH)
    public static void irradiationSchematicIrradiates(GameTestHelper helper) {
        // An Irradiation Source: air passes its cage, where a full radioactive block would stop the current.
        BlockPos source = new BlockPos(2, 2, 2);
        helper.assertBlockPresent(FuelCycleRegistry.IRRADIATION_SOURCE.get(), source);
        helper.assertTrue(NuclearFanTypes.IRRADIATION.get().isValidAt(helper.getLevel(), helper.absolutePos(source)),
                "the irradiation source irradiates");
        helper.assertFalse(NuclearFanTypes.ACID_LEACHING.get().isValidAt(helper.getLevel(), helper.absolutePos(source)),
                "and does not leach");
        helper.succeed();
    }

    /** Every item a guide scene is shown on exists, Create's encased fan included. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void guidePonderItemsExist(GameTestHelper helper) {
        for (PlantPonderText.Scene scene : GuidePonderText.SCENES) {
            for (String component : scene.components()) {
                String id = component.contains(":") ? component : Create_NuclearIndustry.MODID + ":" + component;
                helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id)),
                        "no item " + id + " for the " + scene.id() + " scene");
            }
        }
        helper.succeed();
    }

    /** The guide's pages have text, only known ids, and cover weapons, reactor parts and the tokamak; the treatments have theirs. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void guideJeiPagesCoverTheirParts(GameTestHelper helper) {
        Language language = Language.getInstance();
        for (String id : GuideJeiInfo.IDS) {
            helper.assertTrue(language.has(PlantJeiInfo.PREFIX + id), "no text for the guide page " + id);
        }
        Set<Item> covered = new HashSet<>();
        Set<String> ids = new HashSet<>();
        for (PlantJeiInfo.Page page : GuideJeiInfo.pages()) {
            helper.assertTrue(GuideJeiInfo.IDS.contains(page.id()), "page " + page.id() + " is not among the guide's ids");
            ids.add(page.id());
            page.items().forEach(stack -> covered.add(stack.getItem()));
        }
        boolean shell = BuiltInRegistries.ITEM.containsKey(GuideJeiInfo.NUCLEAR_CANNON_SHELL);
        helper.assertTrue(ids.contains("nuclear_cannon_shell") == shell, "the cannon shell page shows exactly when the shell exists");
        helper.assertTrue(ids.contains("thermonuclear_cannon_shell") == BuiltInRegistries.ITEM.containsKey(GuideJeiInfo.THERMONUCLEAR_CANNON_SHELL),
                "the thermonuclear shell page shows exactly when the shell exists");
        List<Supplier<? extends Item>> parts = List.of(AllNuclearItems.NUCLEAR_BOMB,
                () -> org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.CONTROLLER.get().asItem(),
                () -> org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.ENGINE.get().asItem(),
                MissileRegistry.TARGET_DESIGNATOR, AllNuclearItems.REACTOR_FUEL_PORT, AllNuclearItems.REACTOR_FLUID_PORT,
                AllNuclearItems.CONTROL_ROD, AllNuclearItems.REACTOR_TEMPERATURE_SENSOR, AllNuclearItems.FUSION_ACCELERATOR_SEGMENT,
                AllNuclearItems.FUSION_ELECTROMAGNET, AllNuclearItems.FUSION_CENTRAL_SOLENOID, AllNuclearItems.FUSION_CRYOSTAT_CASING,
                AllNuclearItems.FUSION_BREEDING_BLANKET, AllNuclearItems.FUSION_MAGNET_INPUT, AllNuclearItems.FUSION_FLUID_PORT,
                AllNuclearItems.FUSION_FUEL_INJECTOR, AllNuclearItems.FUSION_TURBINE_CASING, AllNuclearItems.FUSION_TURBINE_ROTOR,
                AllNuclearItems.FUSION_TURBINE_FLUID_PORT, ReactorVisualsRegistry.REACTOR_VIEWPORT_ITEM,
                ExplosiveRegistry.THERMONUCLEAR_BOMB_ITEM, ExplosiveRegistry.LITHIUM_DEUTERIDE, ExplosiveRegistry.EXPLOSIVE_LENS,
                ExplosiveRegistry.IMPLOSION_ASSEMBLY, ExplosiveRegistry.FIRING_SET, ExplosiveRegistry.NEUTRON_INITIATOR,
                ExplosiveRegistry.BOMB_CASING, ExplosiveRegistry.THERMONUCLEAR_CASING);
        for (Supplier<? extends Item> part : parts) {
            helper.assertTrue(covered.contains(part.get()), BuiltInRegistries.ITEM.getKey(part.get()) + " has no guide page");
        }
        // The fan treatments' pages are the fuel cycle's, shown on Create's Encased Fan too.
        Set<String> treatments = new HashSet<>();
        Item fan = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "encased_fan"));
        for (PlantJeiInfo.Page page : FuelCycleInfo.pages()) {
            if (page.items().stream().anyMatch(stack -> stack.is(fan))) {
                treatments.add(page.id());
            }
        }
        helper.assertTrue(treatments.contains("acid_leaching") && treatments.contains("irradiation"),
                "both fan treatments have a page on the Encased Fan, " + treatments);
        helper.succeed();
    }

    private static <T> T at(GameTestHelper helper, BlockPos pos, Class<T> type) {
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        return type.isInstance(blockEntity) ? type.cast(blockEntity) : null;
    }

    /** A Create block by id: its registry entries are not on the compile classpath. */
    private static net.minecraft.world.level.block.Block createBlock(String id) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", id));
    }
}
