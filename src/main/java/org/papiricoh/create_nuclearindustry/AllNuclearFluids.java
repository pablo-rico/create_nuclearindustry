package org.papiricoh.create_nuclearindustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class AllNuclearFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Create_NuclearIndustry.MODID);
    public static final DeferredRegister<net.minecraft.world.level.material.Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, Create_NuclearIndustry.MODID);
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, Create_NuclearIndustry.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, Create_NuclearIndustry.MODID);

    // Helper 1.20.1-friendly (evita APIs nuevas de 1.20.6)
    private static ResourceLocation tex(String path) {
        // En 1.20.1 este ctor NO está deprecado.
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }

    // --------------------------------------------------------------------------------------------
    // FluidType: Steam (gas)
    // --------------------------------------------------------------------------------------------
    public static final Supplier<FluidType> STEAM_TYPE = FLUID_TYPES.register("steam", () ->
            new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid." + Create_NuclearIndustry.MODID + ".steam")
                    .density(-100)          // negativo → “gas”
                    .viscosity(100)         // poco viscoso
                    .temperature(373)       // ~100 ºC
                    .lightLevel(0)
                    .canDrown(false)
                    .canSwim(false)
                    .supportsBoating(false)

            ) {

                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        @Override public ResourceLocation getStillTexture()   { return tex("block/steam_still"); }
                        @Override public ResourceLocation getFlowingTexture() { return tex("block/steam_flow"); }
                        @Override public int getTintColor() { return 0xA0FFFFFF; } // blanco translúcido
                    });
                }
            }
    );

    // -------------------- Fluids --------------------
    public static final Supplier<FlowingFluid> STEAM = FLUIDS.register(
            "steam",
            () -> new BaseFlowingFluid.Source(steamProps())  // <- lazy
    );

    public static final Supplier<FlowingFluid> FLOWING_STEAM = FLUIDS.register(
            "flowing_steam",
            () -> new BaseFlowingFluid.Flowing(steamProps()) // <- lazy
    );

    // -------------------- Bloque y cubo (opcionales) --------------------
    public static final Supplier<LiquidBlock> STEAM_BLOCK = BLOCKS.register(
            "steam",
            () -> new LiquidBlock(
                    STEAM.get(),
                    BlockBehaviour.Properties.of()
                            .noCollission()
                            .noLootTable()
                            .replaceable()
                            .strength(100f)
                            .pushReaction(PushReaction.DESTROY)
            )
    );

    public static final Supplier<Item> STEAM_BUCKET = ITEMS.register(
            "steam_bucket",
            () -> new BucketItem(STEAM.get(), new Item.Properties().stacksTo(1)
                    .craftRemainder(net.minecraft.world.item.Items.BUCKET))
    );

    // -------------------- Lazy properties --------------------
    private static BaseFlowingFluid.Properties steamProps; // no final a propósito

    private static BaseFlowingFluid.Properties steamProps() {
        if (steamProps == null) {
            steamProps = new BaseFlowingFluid.Properties(
                    STEAM_TYPE,
                    STEAM,
                    FLOWING_STEAM
            )
                    .slopeFindDistance(2)
                    .levelDecreasePerBlock(1)
                    .tickRate(5)
                    .explosionResistance(100f);
                    //.block(STEAM_BLOCK);     // comenta si no quieres bloque en mundo
                    //.bucket(STEAM_BUCKET);  // comenta si no quieres cubo
        }
        return steamProps;
    }
}
