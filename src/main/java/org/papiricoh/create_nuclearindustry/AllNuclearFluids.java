package org.papiricoh.create_nuclearindustry;

import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Consumer;

public class AllNuclearFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, Create_NuclearIndustry.MODID);
    public static final DeferredRegister<net.minecraft.world.level.material.Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, Create_NuclearIndustry.MODID);
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Create_NuclearIndustry.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Create_NuclearIndustry.MODID);

    // Helper 1.20.1-friendly (evita APIs nuevas de 1.20.6)
    private static ResourceLocation tex(String path) {
        // En 1.20.1 este ctor NO está deprecado.
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }

    // --------------------------------------------------------------------------------------------
    // FluidType: Steam (gas)
    // --------------------------------------------------------------------------------------------
    public static final RegistryObject<FluidType> STEAM_TYPE = FLUID_TYPES.register("steam", () ->
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
    public static final RegistryObject<FlowingFluid> STEAM = FLUIDS.register(
            "steam",
            () -> new ForgeFlowingFluid.Source(steamProps())  // <- lazy
    );

    public static final RegistryObject<FlowingFluid> FLOWING_STEAM = FLUIDS.register(
            "flowing_steam",
            () -> new ForgeFlowingFluid.Flowing(steamProps()) // <- lazy
    );

    // -------------------- Bloque y cubo (opcionales) --------------------
    public static final RegistryObject<LiquidBlock> STEAM_BLOCK = BLOCKS.register(
            "steam",
            () -> new LiquidBlock(
                    STEAM, // Supplier<? extends FlowingFluid>
                    BlockBehaviour.Properties.of()
                            .noCollission()
                            .noLootTable()
                            .replaceable()
                            .strength(100f)
                            .pushReaction(PushReaction.DESTROY)
            )
    );

    public static final RegistryObject<Item> STEAM_BUCKET = ITEMS.register(
            "steam_bucket",
            () -> new BucketItem(STEAM, new Item.Properties().stacksTo(1)
                    .craftRemainder(net.minecraft.world.item.Items.BUCKET))
    );

    // -------------------- Lazy properties --------------------
    private static ForgeFlowingFluid.Properties steamProps; // no final a propósito

    private static ForgeFlowingFluid.Properties steamProps() {
        if (steamProps == null) {
            steamProps = new ForgeFlowingFluid.Properties(
                    STEAM_TYPE,     // RegistryObject<FluidType> es Supplier
                    STEAM,          // RegistryObject<FlowingFluid> es Supplier
                    FLOWING_STEAM   // RegistryObject<FlowingFluid> es Supplier
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
