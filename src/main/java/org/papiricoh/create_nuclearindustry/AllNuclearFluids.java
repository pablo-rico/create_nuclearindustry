package org.papiricoh.create_nuclearindustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.SoundActions;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class AllNuclearFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Create_NuclearIndustry.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, Create_NuclearIndustry.MODID);
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, Create_NuclearIndustry.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, Create_NuclearIndustry.MODID);

    private static final List<NuclearFluid> ALL = new ArrayList<>();

    /** Which animated texture pair a fluid is drawn with; both are tinted at runtime. */
    private enum Look {
        GAS(tex("block/steam_still"), tex("block/steam_flow")),
        LIQUID(ResourceLocation.withDefaultNamespace("block/water_still"), ResourceLocation.withDefaultNamespace("block/water_flow"));

        private final ResourceLocation still;
        private final ResourceLocation flowing;

        Look(ResourceLocation still, ResourceLocation flowing) {
            this.still = still;
            this.flowing = flowing;
        }
    }

    // ---------------------------------------------------------------- coolant loop
    public static final NuclearFluid STEAM_FLUID = gas("steam", 0xA0FFFFFF, -100, 100, 373, 0);
    public static final NuclearFluid HEAVY_WATER_FLUID = register("heavy_water", Look.GAS, 0xFF3F67C9, FluidType.Properties.create()
            .density(1100).viscosity(1500).temperature(300).canDrown(true).canSwim(true).supportsBoating(false), 4);
    public static final NuclearFluid HEAVY_STEAM_FLUID = gas("heavy_steam", 0xB07C9CFF, -80, 130, 393, 0);
    public static final NuclearFluid PLASMA_STEAM_FLUID = gas("plasma_steam", 0xC0FFD27C, -70, 120, 500, 6);
    // Spent low-pressure steam a turbine's exhaust port sends to a cooling tower. No turbine takes it.
    public static final NuclearFluid EXHAUST_STEAM_FLUID = gas("exhaust_steam", 0x90C8CCCC, -60, 120, 330, 0);
    public static final NuclearFluid HEAVY_EXHAUST_STEAM_FLUID = gas("heavy_exhaust_steam", 0x90A8B4D8, -60, 120, 330, 0);

    // ---------------------------------------------------------------- fusion fuel
    public static final NuclearFluid DEUTERIUM_FLUID = gas("deuterium", 0xB05CD6FF, -90, 80, 300, 0);
    public static final NuclearFluid TRITIUM_FLUID = gas("tritium", 0xB0FF66AA, -85, 80, 300, 2);

    // ---------------------------------------------------------------- process chemicals
    public static final NuclearFluid SULFURIC_ACID_FLUID = liquid("sulfuric_acid", 0xF0E3D46A, 1830, 2400, 300);
    public static final NuclearFluid HYDROFLUORIC_ACID_FLUID = liquid("hydrofluoric_acid", 0xD0BDE6D3, 1150, 1100, 290);
    public static final NuclearFluid NITRIC_ACID_FLUID = liquid("nitric_acid", 0xE0F2E6A0, 1510, 900, 300);

    // ---------------------------------------------------------------- spent fuel reprocessing (PUREX)
    public static final NuclearFluid SPENT_FUEL_SOLUTION_FLUID = liquid("spent_fuel_solution", 0xE0556B2F, 1400, 1300, 320);
    public static final NuclearFluid PLUTONIUM_NITRATE_FLUID = liquid("plutonium_nitrate", 0xE0A85A3C, 1600, 1300, 320);

    // ---------------------------------------------------------------- uranium hexafluoride, one fluid per enrichment tier
    public static final NuclearFluid DEPLETED_URANIUM_HEXAFLUORIDE_FLUID = gas("depleted_uranium_hexafluoride", 0xC8A3A796, 5000, 300, 340, 0);
    public static final NuclearFluid URANIUM_HEXAFLUORIDE_FLUID = gas("uranium_hexafluoride", 0xC8D6E39A, 5000, 300, 340, 0);
    public static final NuclearFluid LOW_ENRICHED_URANIUM_HEXAFLUORIDE_FLUID = gas("low_enriched_uranium_hexafluoride", 0xC8B5E06E, 5000, 300, 340, 1);
    public static final NuclearFluid HIGH_ENRICHED_URANIUM_HEXAFLUORIDE_FLUID = gas("high_enriched_uranium_hexafluoride", 0xC88ED35A, 5000, 300, 340, 2);
    public static final NuclearFluid WEAPONS_GRADE_URANIUM_HEXAFLUORIDE_FLUID = gas("weapons_grade_uranium_hexafluoride", 0xC869C64C, 5000, 300, 340, 4);

    // ---------------------------------------------------------------- the world: water that fallout reached
    public static final NuclearFluid CONTAMINATED_WATER_FLUID = register("contaminated_water", Look.LIQUID, 0xE0738C3A,
            FluidType.Properties.create().density(1000).viscosity(1000).temperature(300)
                    .canDrown(true).canSwim(true).supportsBoating(true).canExtinguish(true).canHydrate(false)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY), 4);

    // ---------------------------------------------------------------- pressurized water loop
    // Borated water under pressure, activated by the core: a leak leaves a radioactive puddle.
    public static final NuclearFluid PRIMARY_COOLANT_FLUID = register("primary_coolant", Look.LIQUID, 0xE05AA9D6,
            FluidType.Properties.create().density(1000).viscosity(1000).temperature(560)
                    .canDrown(true).canSwim(true).supportsBoating(true).canExtinguish(true).canHydrate(false)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY), 4);
    // The same water leaving the core: it only exists inside the loop, a leak flashes it away.
    public static final NuclearFluid HOT_PRIMARY_COOLANT_FLUID = liquid("hot_primary_coolant", 0xE0E08A5A, 700, 800, 600);

    // ---------------------------------------------------------------- short accessors used across the mod
    public static final Supplier<FluidType> STEAM_TYPE = STEAM_FLUID.type();
    public static final Supplier<FluidType> HEAVY_WATER_TYPE = HEAVY_WATER_FLUID.type();
    public static final Supplier<FluidType> HEAVY_STEAM_TYPE = HEAVY_STEAM_FLUID.type();
    public static final Supplier<FluidType> DEUTERIUM_TYPE = DEUTERIUM_FLUID.type();
    public static final Supplier<FluidType> TRITIUM_TYPE = TRITIUM_FLUID.type();
    public static final Supplier<FluidType> PLASMA_STEAM_TYPE = PLASMA_STEAM_FLUID.type();

    public static final Supplier<FlowingFluid> STEAM = STEAM_FLUID.source();
    public static final Supplier<FlowingFluid> FLOWING_STEAM = STEAM_FLUID.flowing();
    public static final Supplier<FlowingFluid> HEAVY_WATER = HEAVY_WATER_FLUID.source();
    public static final Supplier<FlowingFluid> NITRIC_ACID = NITRIC_ACID_FLUID.source();
    public static final Supplier<FlowingFluid> EXHAUST_STEAM = EXHAUST_STEAM_FLUID.source();
    public static final Supplier<FlowingFluid> HEAVY_EXHAUST_STEAM = HEAVY_EXHAUST_STEAM_FLUID.source();
    public static final Supplier<FlowingFluid> SPENT_FUEL_SOLUTION = SPENT_FUEL_SOLUTION_FLUID.source();
    public static final Supplier<FlowingFluid> PLUTONIUM_NITRATE = PLUTONIUM_NITRATE_FLUID.source();
    public static final Supplier<FlowingFluid> FLOWING_HEAVY_WATER = HEAVY_WATER_FLUID.flowing();
    public static final Supplier<FlowingFluid> HEAVY_STEAM = HEAVY_STEAM_FLUID.source();
    public static final Supplier<FlowingFluid> FLOWING_HEAVY_STEAM = HEAVY_STEAM_FLUID.flowing();
    public static final Supplier<FlowingFluid> DEUTERIUM = DEUTERIUM_FLUID.source();
    public static final Supplier<FlowingFluid> FLOWING_DEUTERIUM = DEUTERIUM_FLUID.flowing();
    public static final Supplier<FlowingFluid> TRITIUM = TRITIUM_FLUID.source();
    public static final Supplier<FlowingFluid> FLOWING_TRITIUM = TRITIUM_FLUID.flowing();
    public static final Supplier<FlowingFluid> PLASMA_STEAM = PLASMA_STEAM_FLUID.source();
    public static final Supplier<FlowingFluid> FLOWING_PLASMA_STEAM = PLASMA_STEAM_FLUID.flowing();
    public static final Supplier<FlowingFluid> SULFURIC_ACID = SULFURIC_ACID_FLUID.source();
    public static final Supplier<FlowingFluid> HYDROFLUORIC_ACID = HYDROFLUORIC_ACID_FLUID.source();
    public static final Supplier<FlowingFluid> DEPLETED_URANIUM_HEXAFLUORIDE = DEPLETED_URANIUM_HEXAFLUORIDE_FLUID.source();
    public static final Supplier<FlowingFluid> URANIUM_HEXAFLUORIDE = URANIUM_HEXAFLUORIDE_FLUID.source();
    public static final Supplier<FlowingFluid> LOW_ENRICHED_URANIUM_HEXAFLUORIDE = LOW_ENRICHED_URANIUM_HEXAFLUORIDE_FLUID.source();
    public static final Supplier<FlowingFluid> HIGH_ENRICHED_URANIUM_HEXAFLUORIDE = HIGH_ENRICHED_URANIUM_HEXAFLUORIDE_FLUID.source();
    public static final Supplier<FlowingFluid> WEAPONS_GRADE_URANIUM_HEXAFLUORIDE = WEAPONS_GRADE_URANIUM_HEXAFLUORIDE_FLUID.source();
    public static final Supplier<FlowingFluid> CONTAMINATED_WATER = CONTAMINATED_WATER_FLUID.source();
    public static final Supplier<FlowingFluid> PRIMARY_COOLANT = PRIMARY_COOLANT_FLUID.source();
    public static final Supplier<FlowingFluid> HOT_PRIMARY_COOLANT = HOT_PRIMARY_COOLANT_FLUID.source();

    // ---------------------------------------------------------------- world block and bucket (steam only)
    public static final Supplier<LiquidBlock> STEAM_BLOCK = BLOCKS.register("steam", () -> new LiquidBlock(
            STEAM.get(),
            BlockBehaviour.Properties.of()
                    .noCollission()
                    .noLootTable()
                    .replaceable()
                    .strength(100f)
                    .pushReaction(PushReaction.DESTROY)));

    public static final Supplier<Item> STEAM_BUCKET = ITEMS.register("steam_bucket",
            () -> new BucketItem(STEAM.get(), new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));

    // ---------------------------------------------------------------- contaminated water: lakes, rivers and buckets
    public static final DeferredHolder<Block, LiquidBlock> CONTAMINATED_WATER_BLOCK = BLOCKS.register("contaminated_water",
            () -> new LiquidBlock(CONTAMINATED_WATER.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).mapColor(MapColor.COLOR_GREEN)));

    public static final DeferredHolder<Item, BucketItem> CONTAMINATED_WATER_BUCKET = ITEMS.register("contaminated_water_bucket",
            () -> new BucketItem(CONTAMINATED_WATER.get(), new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));

    // ---------------------------------------------------------------- primary coolant: spills and buckets
    public static final DeferredHolder<Block, LiquidBlock> PRIMARY_COOLANT_BLOCK = BLOCKS.register("primary_coolant",
            () -> new LiquidBlock(PRIMARY_COOLANT.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).mapColor(MapColor.COLOR_LIGHT_BLUE)));

    public static final DeferredHolder<Item, BucketItem> PRIMARY_COOLANT_BUCKET = ITEMS.register("primary_coolant_bucket",
            () -> new BucketItem(PRIMARY_COOLANT.get(), new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));

    static {
        CONTAMINATED_WATER_FLUID.placeable(CONTAMINATED_WATER_BLOCK, CONTAMINATED_WATER_BUCKET);
        PRIMARY_COOLANT_FLUID.placeable(PRIMARY_COOLANT_BLOCK, PRIMARY_COOLANT_BUCKET);
    }

    public static List<NuclearFluid> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        for (NuclearFluid fluid : ALL) {
            Look look = fluid.look;
            int tint = fluid.tint;
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override public ResourceLocation getStillTexture() { return look.still; }
                @Override public ResourceLocation getFlowingTexture() { return look.flowing; }
                @Override public int getTintColor() { return tint; }
            }, fluid.type().get());
        }
    }

    private static NuclearFluid gas(String name, int tint, int density, int viscosity, int temperature, int light) {
        return register(name, Look.GAS, tint, FluidType.Properties.create()
                .density(density).viscosity(viscosity).temperature(temperature).lightLevel(light)
                .canDrown(false).canSwim(false).supportsBoating(false), 2);
    }

    private static NuclearFluid liquid(String name, int tint, int density, int viscosity, int temperature) {
        return register(name, Look.LIQUID, tint, FluidType.Properties.create()
                .density(density).viscosity(viscosity).temperature(temperature)
                .canDrown(true).canSwim(true).supportsBoating(false), 4);
    }

    private static NuclearFluid register(String name, Look look, int tint, FluidType.Properties properties, int slopeFindDistance) {
        properties.descriptionId("fluid." + Create_NuclearIndustry.MODID + "." + name);
        NuclearFluid fluid = new NuclearFluid(name, look, tint, slopeFindDistance);
        fluid.type = FLUID_TYPES.register(name, () -> new FluidType(properties));
        fluid.source = FLUIDS.register(name, () -> new BaseFlowingFluid.Source(fluid.properties()));
        fluid.flowing = FLUIDS.register("flowing_" + name, () -> new BaseFlowingFluid.Flowing(fluid.properties()));
        ALL.add(fluid);
        return fluid;
    }

    private static ResourceLocation tex(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }

    /** One registered fluid: its type plus source and flowing variants. */
    public static final class NuclearFluid {
        private final String name;
        private final Look look;
        private final int tint;
        private final int slopeFindDistance;
        private DeferredHolder<FluidType, FluidType> type;
        private DeferredHolder<Fluid, FlowingFluid> source;
        private DeferredHolder<Fluid, FlowingFluid> flowing;
        private BaseFlowingFluid.Properties properties;
        private Supplier<? extends LiquidBlock> block;
        private Supplier<? extends Item> bucket;

        private NuclearFluid(String name, Look look, int tint, int slopeFindDistance) {
            this.name = name;
            this.look = look;
            this.tint = tint;
            this.slopeFindDistance = slopeFindDistance;
        }

        public String name() {
            return name;
        }

        public DeferredHolder<FluidType, FluidType> type() {
            return type;
        }

        public DeferredHolder<Fluid, FlowingFluid> source() {
            return source;
        }

        public DeferredHolder<Fluid, FlowingFluid> flowing() {
            return flowing;
        }

        /** Gives the fluid a world block and a bucket, so it can be placed and picked up. */
        private void placeable(Supplier<? extends LiquidBlock> block, Supplier<? extends Item> bucket) {
            this.block = block;
            this.bucket = bucket;
        }

        private BaseFlowingFluid.Properties properties() {
            if (properties == null) {
                properties = new BaseFlowingFluid.Properties(type, source, flowing)
                        .slopeFindDistance(slopeFindDistance)
                        .levelDecreasePerBlock(1)
                        .tickRate(5)
                        .explosionResistance(100f);
                if (block != null) {
                    properties.block(block).bucket(bucket);
                }
            }
            return properties;
        }
    }
}
