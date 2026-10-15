package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorConfig;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.TargetStationBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.physics.AnnihilationModel;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterRegistry;
import org.papiricoh.create_nuclearindustry.antimatter.generator.AnnihilationGeneratorBlockEntity;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.isotope.AgedPlutoniumIngredient;
import org.papiricoh.create_nuclearindustry.isotope.IsotopeConfig;
import org.papiricoh.create_nuclearindustry.isotope.IsotopeData;
import org.papiricoh.create_nuclearindustry.isotope.IsotopeRegistry;
import org.papiricoh.create_nuclearindustry.isotope.Isotopes;
import org.papiricoh.create_nuclearindustry.isotope.MedicalIsotopeKitItem;
import org.papiricoh.create_nuclearindustry.isotope.RadiographyCameraItem;
import org.papiricoh.create_nuclearindustry.isotope.SterileFood;
import org.papiricoh.create_nuclearindustry.isotope.TechnetiumGeneratorItem;
import org.papiricoh.create_nuclearindustry.isotope.channel.ChannelStatus;
import org.papiricoh.create_nuclearindustry.isotope.channel.IrradiationChannelBlock;
import org.papiricoh.create_nuclearindustry.isotope.channel.IrradiationChannelBlockEntity;
import org.papiricoh.create_nuclearindustry.isotope.irradiator.GammaIrradiatorBlockEntity;
import org.papiricoh.create_nuclearindustry.isotope.physics.Isotope;
import org.papiricoh.create_nuclearindustry.isotope.physics.IsotopeDecay;
import org.papiricoh.create_nuclearindustry.isotope.physics.NeutronActivation;
import org.papiricoh.create_nuclearindustry.isotope.smoke.SmokeDetectorBlock;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.rtg.RtgFuel;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Isotopes and nuclear medicine: decay clocks read lazily from the item's date, the dose following
 * the activity left, the reactor's irradiation channel turning cobalt into cobalt-60 only with
 * neutrons, the Target Station making technetium generators below the antiproton threshold and
 * antiprotons above it, milking the generator, americium from aged plutonium only, the Gamma
 * Irradiator sterilising food on a depot (and letting it pass with a spent source), the smoke
 * detector, the strontium pellet, the medical kit and the radiography camera.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class IsotopeTests {
    /**
     * Their own batch: the lit campfires, depots and fire here must never sit beside another area's
     * fans, whose air currents they would turn into smoking.
     */
    private static final String BATCH = "isotopes";
    private static final TagKey<Block> NON_MOVABLE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("create", "non_movable"));

    /** A stack of {@code item} that was fresh {@code daysAgo} scaled days before {@code now}. */
    private static ItemStack aged(Item item, long now, double daysAgo) {
        ItemStack stack = new ItemStack(item);
        long ticks = (long) (daysAgo * IsotopeDecay.TICKS_PER_DAY / IsotopeConfig.timeScale);
        stack.set(IsotopeRegistry.ISOTOPE.get(), new IsotopeData(now - ticks, 1.0f, 100));
        return stack;
    }

    // ------------------------------------------------------------------ decay

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void anIsotopeHalvesEachHalfLifeAndCatchesUpWhenRefreshed(GameTestHelper helper) {
        long now = helper.getLevel().getGameTime();
        double halfLife = IsotopeConfig.halfLifeDays(Isotope.COBALT_60);
        ItemStack fresh = Isotopes.fresh(IsotopeRegistry.COBALT_60.get(), 1, now, 1.0);
        ItemStack old = aged(IsotopeRegistry.COBALT_60.get(), now, halfLife);
        helper.assertTrue(Math.abs(Isotopes.activity(fresh, now) - 1.0) < 0.03, "a fresh source is at full activity: " + Isotopes.activity(fresh, now));
        helper.assertTrue(Math.abs(Isotopes.activity(old, now) - 0.5) < 0.01, "one half-life later it is at half: " + Isotopes.activity(old, now));
        helper.assertTrue(Isotopes.storedActivity(old) == 1.0, "until it is looked at, the stored percent is stale");
        helper.assertTrue(Isotopes.refresh(old, now) && Isotopes.data(old).percent() == 50, "a refresh catches it up: " + Isotopes.data(old).percent());
        ItemStack undated = new ItemStack(IsotopeRegistry.COBALT_60.get());
        helper.assertTrue(Isotopes.activity(undated, now) == 1.0, "an undated source counts as fresh");
        helper.assertTrue(Isotopes.refresh(undated, now) && Isotopes.data(undated).dated(), "and is dated on its first refresh");
        ItemStack gone = aged(IsotopeRegistry.TECHNETIUM_99M.get(), now, IsotopeConfig.halfLifeDays(Isotope.TECHNETIUM_99M) * 5);
        helper.assertFalse(Isotopes.usable(gone, now), "technetium five half-lives old is too weak to use");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aCarriedSourceDosesByItsActivityLeft(GameTestHelper helper) {
        long now = helper.getLevel().getGameTime();
        Player fresh = helper.makeMockPlayer(GameType.SURVIVAL);
        Player decayed = helper.makeMockPlayer(GameType.SURVIVAL);
        fresh.getInventory().add(Isotopes.fresh(IsotopeRegistry.COBALT_60.get(), 1, now, 1.0));
        decayed.getInventory().add(aged(IsotopeRegistry.COBALT_60.get(), now, IsotopeConfig.halfLifeDays(Isotope.COBALT_60) * 2));
        Isotopes.refreshInventory(fresh.getInventory(), now);
        Isotopes.refreshInventory(decayed.getInventory(), now);
        double full = RadiationHandler.inventoryRate(fresh);
        double quarter = RadiationHandler.inventoryRate(decayed);
        helper.assertTrue(full > 0, "a fresh cobalt-60 source doses whoever carries it");
        helper.assertTrue(Math.abs(quarter / full - 0.25) < 0.02, "two half-lives later a quarter as much: " + quarter + " vs " + full);
        helper.succeed();
    }

    // ------------------------------------------------------------------ the reactor's irradiation channel

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 2400)
    public static void theChannelActivatesCobaltOnlyWhileTheReactorGivesNeutrons(GameTestHelper helper) {
        FissionTestBuilder.build(helper, false);
        BlockPos channelPos = FissionTestBuilder.WALL_CASING;
        helper.setBlock(channelPos, IsotopeRegistry.IRRADIATION_CHANNEL.get().defaultBlockState()
                .setValue(IrradiationChannelBlock.FACING, Direction.NORTH));
        IrradiationChannelBlockEntity channel = helper.getBlockEntity(channelPos);
        if (channel != null) {
            channel.automation().insertItem(0, new ItemStack(IsotopeRegistry.COBALT_INGOT.get(), 4), false);
        }
        helper.runAfterDelay(30, () -> {
            ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
            IrradiationChannelBlockEntity idle = helper.getBlockEntity(channelPos);
            helper.assertTrue(reactor != null && reactor.getStructure().isPresent(), "the channel takes a casing block's place in the wall");
            helper.assertTrue(idle != null && idle.reactor() == reactor, "and joins the reactor");
            helper.assertTrue(idle.fluence() == 0 && idle.automation().getStackInSlot(1).isEmpty(),
                    "without fuel there are no neutrons and the batch waits: " + idle.fluence());
            helper.assertTrue(helper.getBlockState(channelPos).getValue(IrradiationChannelBlock.STATUS) == ChannelStatus.LOADED,
                    "the tube shows a capsule waiting in the core");
            for (int i = 0; i < 6; i++) {
                reactor.getPhysicsSimulator().loadFuelAssembly(9f);
            }
        });
        // Fed with water and drained of steam, the fuelled core comes up; once it gives neutrons the
        // batch is set just short of done, so the test does not wait the five minutes.
        boolean[] primed = new boolean[1];
        helper.onEachTick(() -> {
            ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
            IrradiationChannelBlockEntity live = AcceleratorTestBuilder.entity(helper, channelPos, IrradiationChannelBlockEntity.class);
            if (reactor == null || live == null || helper.getTick() < 31) {
                return;
            }
            reactor.fillCoolant(new FluidStack(Fluids.WATER, 400), IFluidHandler.FluidAction.EXECUTE);
            reactor.drainSteam(2000, IFluidHandler.FluidAction.EXECUTE);
            if (!primed[0] && reactor.getNeutronLevel() > 1 && live.fluence() > 0) {
                primed[0] = true;
                live.setFluence(NeutronActivation.requiredFluence(IsotopeConfig.channel()) - 1);
            }
        });
        helper.succeedWhen(() -> {
            IrradiationChannelBlockEntity done = helper.getBlockEntity(channelPos);
            helper.assertTrue(done != null && primed[0], "the core should come up");
            ItemStack sources = done.automation().getStackInSlot(1);
            helper.assertTrue(sources.is(IsotopeRegistry.COBALT_60.get()) && sources.getCount() == 4,
                    "with neutrons the whole batch comes out as cobalt-60: " + sources);
            helper.assertTrue(Isotopes.data(sources) != null && Isotopes.data(sources).dated(), "dated as it leaves the core");
            helper.assertTrue(done.comparatorSignal() == 15, "comparators read a ready batch");
            helper.assertTrue(done.automation().insertItem(1, new ItemStack(IsotopeRegistry.COBALT_INGOT.get()), true).getCount() == 1,
                    "pipes cannot push anything into the output");
            helper.assertTrue(done.automation().extractItem(1, 4, false).getCount() == 4, "but take the sources out");
            FissionTestBuilder.controller(helper).getPhysicsSimulator().setControlRodInsertion(1.0f);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void cobaltAddedLaterSharesTheBatchsFluence(GameTestHelper helper) {
        helper.setBlock(new BlockPos(6, 2, 6), IsotopeRegistry.IRRADIATION_CHANNEL.get());
        IrradiationChannelBlockEntity channel = helper.getBlockEntity(new BlockPos(6, 2, 6));
        helper.assertTrue(channel != null, "the channel should be there");
        channel.automation().insertItem(0, new ItemStack(IsotopeRegistry.COBALT_INGOT.get(), 2), false);
        channel.setFluence(1000);
        channel.automation().insertItem(0, new ItemStack(IsotopeRegistry.COBALT_INGOT.get(), 2), false);
        helper.assertTrue(Math.abs(channel.fluence() - 500) < 1.0e-6, "two more ingots halve the batch's fluence: " + channel.fluence());
        helper.assertTrue(channel.automation().insertItem(0, new ItemStack(Items.IRON_INGOT), true).getCount() == 1, "only cobalt goes in");
        helper.succeed();
    }

    // ------------------------------------------------------------------ the target station

    private static final BlockPos STATION = new BlockPos(6, 2, 6);
    /** A beam of 60 MW: one technetium generator a second. */
    private static final double POWER_KW = 60_000;

    private static TargetStationBlockEntity station(GameTestHelper helper, double energyMev) {
        helper.setBlock(STATION, AcceleratorRegistry.TARGET_STATION.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
        TargetStationBlockEntity station = helper.getBlockEntity(STATION);
        if (station != null) {
            station.targetSlot().insertItem(0, UraniumItem.withEnrichment(new ItemStack(AllNuclearItems.URANIUM.get(), 4), 0.2f), false);
        }
        helper.onEachTick(() -> {
            TargetStationBlockEntity target = AcceleratorTestBuilder.entity(helper, STATION, TargetStationBlockEntity.class);
            if (target != null) {
                target.receiveBeam(energyMev, POWER_KW * 1000 / energyMev, POWER_KW);
            }
        });
        return station;
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 200)
    public static void belowTheThresholdTheStationMakesTechnetiumGenerators(GameTestHelper helper) {
        station(helper, AcceleratorConfig.antimatter().thresholdGev() * 0.4 * 1000);
        helper.succeedWhen(() -> {
            TargetStationBlockEntity station = AcceleratorTestBuilder.entity(helper, STATION, TargetStationBlockEntity.class);
            helper.assertTrue(station != null, "the station should be there");
            IItemHandler slots = station.automation();
            int made = 0;
            for (int slot = 1; slot < slots.getSlots(); slot++) {
                ItemStack product = slots.getStackInSlot(slot);
                if (product.is(IsotopeRegistry.TECHNETIUM_GENERATOR.get())) {
                    made += product.getCount();
                    helper.assertTrue(TechnetiumGeneratorItem.data(product).dated(), "each generator starts its clock as it is made");
                }
            }
            helper.assertTrue(made >= 2, "the beam turns depleted uranium into molybdenum-99 columns: " + made);
            helper.assertTrue(station.productionPerMinute() == 0, "and makes no antiprotons below the threshold");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 100)
    public static void aboveTheThresholdTheTargetOnlyWaits(GameTestHelper helper) {
        station(helper, AcceleratorConfig.antimatter().thresholdGev() * 2 * 1000);
        helper.runAfterDelay(60, () -> {
            TargetStationBlockEntity station = AcceleratorTestBuilder.entity(helper, STATION, TargetStationBlockEntity.class);
            helper.assertTrue(station != null, "the station should be there");
            helper.assertTrue(station.targetSlot().getStackInSlot(0).getCount() == 4, "above the threshold the target is left alone");
            helper.assertTrue(station.products().getStackInSlot(0).isEmpty(), "and nothing is made from it");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ technetium, americium

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aTechnetiumGeneratorIsMilkedBestADayAfterTheLastTime(GameTestHelper helper) {
        long start = 1_000_000L;
        ItemStack cow = new ItemStack(IsotopeRegistry.TECHNETIUM_GENERATOR.get());
        TechnetiumGeneratorItem.refresh(cow, start);
        helper.assertTrue(TechnetiumGeneratorItem.elute(cow, start).isEmpty(), "a fresh column has no technetium yet");
        long day = (long) (IsotopeDecay.TICKS_PER_DAY / IsotopeConfig.timeScale);
        List<ItemStack> doses = TechnetiumGeneratorItem.elute(cow, start + day);
        helper.assertTrue(doses.size() == 1 && doses.get(0).is(IsotopeRegistry.TECHNETIUM_99M.get()) && doses.get(0).getCount() >= 3,
                "a day later it gives several doses: " + doses);
        helper.assertTrue(TechnetiumGeneratorItem.elute(cow, start + day + 20).isEmpty(), "milked again at once it is empty");
        helper.assertTrue(TechnetiumGeneratorItem.state(cow, start + day * 20).parent() < 0.01,
                "three weeks on its molybdenum is gone");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void onlyAgedPlutoniumGivesUpAmericium(GameTestHelper helper) {
        long now = helper.getLevel().getServer().overworld().getGameTime();
        AgedPlutoniumIngredient aged = new AgedPlutoniumIngredient(Ingredient.of(AllNuclearItems.PLUTONIUM.get()));
        ItemStack old = aged(AllNuclearItems.PLUTONIUM.get(), now, 30);
        ItemStack young = aged(AllNuclearItems.PLUTONIUM.get(), now, 1);
        helper.assertTrue(aged.test(old), "a month-old plutonium has grown enough americium: " + Isotopes.americiumShare(old, now));
        helper.assertFalse(aged.test(young), "a day-old one has not: " + Isotopes.americiumShare(young, now));
        helper.assertFalse(aged.test(new ItemStack(AllNuclearItems.PLUTONIUM.get())), "undated plutonium has not started ageing");
        helper.assertFalse(aged.test(new ItemStack(IsotopeRegistry.COBALT_60.get())), "and nothing but plutonium matches");
        helper.succeed();
    }

    // ------------------------------------------------------------------ gamma irradiator

    private static final BlockPos DEPOT = new BlockPos(6, 1, 6);
    private static final BlockPos IRRADIATOR = DEPOT.above(2);

    private static void irradiator(GameTestHelper helper, double activity, ItemStack food) {
        helper.setBlock(DEPOT, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "depot")));
        helper.setBlock(IRRADIATOR, IsotopeRegistry.GAMMA_IRRADIATOR.get());
        GammaIrradiatorBlockEntity head = helper.getBlockEntity(IRRADIATOR);
        if (head != null) {
            head.sources().insertItem(0, Isotopes.fresh(IsotopeRegistry.COBALT_60.get(), 1, helper.getLevel().getGameTime(), activity), false);
        }
        IItemHandler depot = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(DEPOT), null);
        if (depot != null) {
            depot.insertItem(0, food, false);
        }
    }

    private static ItemStack onDepot(GameTestHelper helper) {
        IItemHandler depot = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(DEPOT), null);
        return depot == null ? ItemStack.EMPTY : depot.getStackInSlot(0);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 300)
    public static void theGammaIrradiatorSterilisesFoodOnADepot(GameTestHelper helper) {
        irradiator(helper, 1.0, new ItemStack(Items.ROTTEN_FLESH, 3));
        AtomicBoolean opened = new AtomicBoolean();
        helper.onEachTick(() -> {
            GammaIrradiatorBlockEntity head = AcceleratorTestBuilder.entity(helper, IRRADIATOR, GammaIrradiatorBlockEntity.class);
            if (head != null && head.isOpen()) {
                opened.set(true);
            }
        });
        helper.succeedWhen(() -> {
            ItemStack food = onDepot(helper);
            helper.assertTrue(food.is(Items.ROTTEN_FLESH) && food.getCount() == 3 && SterileFood.sterile(food),
                    "the whole stack comes out sterile: " + food);
            var properties = food.get(DataComponents.FOOD);
            helper.assertTrue(properties != null && properties.effects().isEmpty(), "rotten flesh no longer gives hunger");
            helper.assertTrue(properties.nutrition() == Items.ROTTEN_FLESH.components().get(DataComponents.FOOD).nutrition(),
                    "and still feeds as much");
            helper.assertTrue(opened.get(), "the shutter opened over the food");
            GammaIrradiatorBlockEntity head = helper.getBlockEntity(IRRADIATOR);
            helper.assertTrue(head != null && !head.isOpen(), "and closes once it is done");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 200)
    public static void aSpentSourceLetsFoodPass(GameTestHelper helper) {
        irradiator(helper, 0.05, new ItemStack(Items.SPIDER_EYE));
        helper.runAfterDelay(150, () -> {
            ItemStack food = onDepot(helper);
            helper.assertTrue(food.is(Items.SPIDER_EYE) && !SterileFood.sterile(food), "a source at 5% cannot sterilise: " + food);
            GammaIrradiatorBlockEntity head = helper.getBlockEntity(IRRADIATOR);
            helper.assertTrue(head != null && !head.isOpen(), "and keeps its shutter shut");
            helper.assertFalse(SterileFood.sterilisable(new ItemStack(Items.BREAD)), "harmless food is never held");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ smoke detector

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theSmokeDetectorSignalsAFireNearby(GameTestHelper helper) {
        BlockPos alarmed = new BlockPos(1, 2, 6);
        BlockPos quiet = new BlockPos(11, 2, 6);
        helper.setBlock(alarmed.below(), Blocks.STONE);
        helper.setBlock(quiet.below(), Blocks.STONE);
        var floor = IsotopeRegistry.SMOKE_DETECTOR.get().defaultBlockState().setValue(SmokeDetectorBlock.FACE, AttachFace.FLOOR);
        helper.setBlock(alarmed, floor);
        helper.setBlock(quiet, floor);
        helper.setBlock(alarmed.east(2), Blocks.CAMPFIRE);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(helper.getBlockState(alarmed).getValue(SmokeDetectorBlock.POWER) > 0, "a lit campfire two blocks off trips it");
            helper.assertTrue(helper.getLevel().getDirectSignalTo(helper.absolutePos(alarmed.below())) > 0,
                    "the block it is fixed to is powered, as by a button");
            helper.assertTrue(helper.getBlockState(quiet).getValue(SmokeDetectorBlock.POWER) == 0, "one with no fire in range stays quiet");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ strontium, medicine, radiography

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void theStrontiumPelletIsWeakerButLastsLonger(GameTestHelper helper) {
        helper.assertTrue(RtgFuel.STRONTIUM.capacityPerRpm() < RtgFuel.PLUTONIUM.capacityPerRpm(), "strontium gives less power");
        helper.assertTrue(RtgFuel.STRONTIUM.halfLifeTicks() > RtgFuel.PLUTONIUM.halfLifeTicks(), "but decays slower");
        helper.assertTrue(IsotopeRegistry.STRONTIUM_RTG_PELLET.get().fuel() == RtgFuel.STRONTIUM, "and the pellet is made of it");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void theMedicalKitScansWithADoseAndBoostsTheMedicines(GameTestHelper helper) {
        long now = helper.getLevel().getGameTime();
        Player doctor = helper.makeMockPlayer(GameType.SURVIVAL);
        Player plain = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertFalse(MedicalIsotopeKitItem.scan(helper.getLevel(), doctor, doctor), "without a dose there is no scan");
        doctor.getInventory().add(Isotopes.fresh(IsotopeRegistry.TECHNETIUM_99M.get(), 2, now, 1.0));
        helper.assertTrue(MedicalIsotopeKitItem.scan(helper.getLevel(), doctor, doctor), "with a dose it scans");
        helper.assertTrue(doctor.getInventory().countItem(IsotopeRegistry.TECHNETIUM_99M.get()) == 1, "and uses one dose");
        helper.assertTrue(doctor.hasEffect(IsotopeRegistry.TRACER), "the tracer stays in the body");
        helper.assertTrue(MedicalIsotopeKitItem.diagnosis(doctor).size() >= 6, "the diagnosis covers dose, sources and medicines");
        doctor.addEffect(new MobEffectInstance(AllNuclearRadiation.RADIOPROTECTION, 200, 0));
        plain.addEffect(new MobEffectInstance(AllNuclearRadiation.RADIOPROTECTION, 200, 0));
        helper.assertTrue(RadiationHandler.absorption(doctor) < RadiationHandler.absorption(plain),
                "iodide works better under the tracer: " + RadiationHandler.absorption(doctor) + " vs " + RadiationHandler.absorption(plain));
        doctor.addEffect(new MobEffectInstance(AllNuclearRadiation.RADIATION_PURGE, 200, 0));
        plain.addEffect(new MobEffectInstance(AllNuclearRadiation.RADIATION_PURGE, 200, 0));
        helper.assertTrue(RadiationHandler.purge(doctor) > RadiationHandler.purge(plain), "and so does Prussian blue");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 100)
    public static void aRadiographShowsAReactorsInsides(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        helper.runAfterDelay(30, () -> {
            var lines = RadiographyCameraItem.radiograph(helper.getLevel(), helper.absolutePos(FissionTestBuilder.WALL_CASING));
            helper.assertTrue(lines != null && lines.size() >= 5, "a reactor wall shows the core behind it: " + lines);
            helper.assertTrue(RadiographyCameraItem.radiograph(helper.getLevel(), helper.absolutePos(new BlockPos(10, 2, 10))) == null,
                    "open ground shows nothing");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ the generator's grace

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = PenningTrapTests.BLASTS, timeoutTicks = 200)
    public static void aLitGeneratorRidesOutAShortGapInItsSupply(GameTestHelper helper) {
        BlockPos gen = AntimatterTestBuilder.MIDDLE;
        AntimatterTestBuilder.listen();
        helper.setBlock(gen, AntimatterRegistry.ANNIHILATION_GENERATOR.get().defaultBlockState()
                .setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.EAST));
        helper.setBlock(gen.above(), Blocks.REDSTONE_BLOCK);
        AnnihilationGeneratorBlockEntity generator = helper.getBlockEntity(gen);
        if (generator != null) {
            generator.setMode(AnnihilationModel.Mode.ROTATION);
            generator.primeField();
            generator.setStored(0.05);
        }
        // Lit; then the supply stops, and no cable will ever refill its field buffer.
        helper.runAfterDelay(5, () -> {
            AnnihilationGeneratorBlockEntity lit = AcceleratorTestBuilder.entity(helper, gen, AnnihilationGeneratorBlockEntity.class);
            helper.assertTrue(lit != null && lit.lit(), "the generator lights on its first burn");
            lit.fieldBuffer().set(0);
            lit.setStored(0);
        });
        helper.runAfterDelay(5 + AnnihilationModel.LIT_GRACE_TICKS / 2, () -> {
            AnnihilationGeneratorBlockEntity waiting = AcceleratorTestBuilder.entity(helper, gen, AnnihilationGeneratorBlockEntity.class);
            helper.assertTrue(waiting != null && waiting.lit(), "half the grace later it still holds its field");
            helper.assertTrue(waiting.acceptsAntimatter(), "so a conduit may deliver again");
            helper.assertTrue(waiting.insert(0.05, false) > 0, "and the delivery is taken");
        });
        helper.runAfterDelay(5 + AnnihilationModel.LIT_GRACE_TICKS / 2 + 5, () -> {
            AnnihilationGeneratorBlockEntity burning = AcceleratorTestBuilder.entity(helper, gen, AnnihilationGeneratorBlockEntity.class);
            helper.assertTrue(burning != null && burning.lit() && burning.burning(), "it burns on without a cable");
            helper.assertTrue(AntimatterTestBuilder.blastsNear(helper, gen, 3) == 0, "nothing annihilated");
            burning.setStored(0);
        });
        helper.runAfterDelay(5 + AnnihilationModel.LIT_GRACE_TICKS * 3, () -> {
            AnnihilationGeneratorBlockEntity cold = AcceleratorTestBuilder.entity(helper, gen, AnnihilationGeneratorBlockEntity.class);
            helper.assertTrue(cold != null && !cold.lit(), "a longer gap lets it go cold");
            helper.assertFalse(cold.acceptsAntimatter(), "and a cold chamber without energy takes nothing in");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ the Ponder scene's layout

    @GameTest(template = "gametest/isotopes_ponder", batch = BATCH, timeoutTicks = 300)
    public static void thePonderLayoutWorks(GameTestHelper helper) {
        // The template sits one block above the test origin: every position is the schematic's plus one in y.
        helper.assertBlockPresent(IsotopeRegistry.IRRADIATION_CHANNEL.get(), new BlockPos(1, 3, 2));
        helper.assertBlockPresent(IsotopeRegistry.GAMMA_IRRADIATOR.get(), new BlockPos(4, 4, 4));
        helper.assertBlockPresent(IsotopeRegistry.SMOKE_DETECTOR.get(), new BlockPos(7, 2, 2));
        GammaIrradiatorBlockEntity head = helper.getBlockEntity(new BlockPos(4, 4, 4));
        if (head != null) {
            head.sources().insertItem(0, new ItemStack(IsotopeRegistry.COBALT_60.get()), false);
        }
        IItemHandler depot = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(new BlockPos(4, 2, 4)), null);
        if (depot != null) {
            depot.insertItem(0, new ItemStack(Items.CHICKEN), false);
        }
        helper.setBlock(new BlockPos(7, 2, 4), Blocks.CAMPFIRE);
        helper.succeedWhen(() -> {
            IItemHandler held = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(new BlockPos(4, 2, 4)), null);
            helper.assertTrue(held != null && SterileFood.sterile(held.getStackInSlot(0)), "the irradiator over the depot sterilises raw chicken");
            helper.assertTrue(helper.getBlockState(new BlockPos(7, 2, 2)).getValue(SmokeDetectorBlock.POWER) > 0,
                    "the detector sees the campfire");
            helper.assertBlockProperty(new BlockPos(6, 2, 2), RedstoneLampBlock.LIT, true);
        });
    }

    // ------------------------------------------------------------------ antimatter holders stay put

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void contraptionsCannotCarryAntimatterHolders(GameTestHelper helper) {
        for (Block block : List.of(AntimatterRegistry.SMALL_PENNING_TRAP.get(), AntimatterRegistry.PENNING_TRAP.get(),
                AntimatterRegistry.LARGE_PENNING_TRAP.get(), AntimatterRegistry.ANNIHILATION_GENERATOR.get(),
                AntimatterRegistry.ANTIPROTON_INJECTOR.get())) {
            helper.assertTrue(block.defaultBlockState().is(NON_MOVABLE), BuiltInRegistries.BLOCK.getKey(block) + " should be non_movable");
        }
        helper.succeed();
    }
}
