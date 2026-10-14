package org.papiricoh.create_nuclearindustry.integration.cbc;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import rbasamoyai.createbigcannons.index.CBCMunitionPropertiesHandlers;
import rbasamoyai.createbigcannons.munitions.big_cannon.BigCannonProjectileBlockEntity;
import rbasamoyai.createbigcannons.munitions.big_cannon.BigCannonProjectileRenderer;
import rbasamoyai.createbigcannons.munitions.big_cannon.ProjectileBlockItem;
import rbasamoyai.createbigcannons.munitions.config.MunitionPropertiesHandler;

public final class CBCNuclearContent {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, Create_NuclearIndustry.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, Create_NuclearIndustry.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Create_NuclearIndustry.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Create_NuclearIndustry.MODID);
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Create_NuclearIndustry.MODID);

    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>,
            net.minecraft.world.item.crafting.RecipeSerializer<NuclearCannonShellRecipe>> NUCLEAR_CANNON_SHELL_RECIPE =
            RECIPE_SERIALIZERS.register("nuclear_cannon_shell",
                    () -> new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(NuclearCannonShellRecipe::new));

    public static final DeferredHolder<EntityType<?>, EntityType<NuclearCannonShellProjectile>> NUCLEAR_CANNON_SHELL_PROJECTILE =
            ENTITY_TYPES.register("nuclear_cannon_shell", () -> EntityType.Builder.<NuclearCannonShellProjectile>of(NuclearCannonShellProjectile::new, MobCategory.MISC)
                    .sized(0.8f, 0.8f)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("nuclear_cannon_shell"));

    public static final DeferredHolder<Block, NuclearCannonShellBlock> NUCLEAR_CANNON_SHELL_BLOCK =
            BLOCKS.register("nuclear_cannon_shell", () -> new NuclearCannonShellBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0f, 12.0f)
                    .noOcclusion()));

    public static final DeferredHolder<Item, NuclearCannonShellItem> NUCLEAR_CANNON_SHELL =
            ITEMS.register("nuclear_cannon_shell", () -> new NuclearCannonShellItem(NUCLEAR_CANNON_SHELL_BLOCK.get(), new Item.Properties().stacksTo(16)
                    .component(org.papiricoh.create_nuclearindustry.AllNuclearDataComponents.WARHEAD_LOAD.get(),
                            org.papiricoh.create_nuclearindustry.explosive.WarheadLoad.FULL)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NuclearCannonShellBlockEntity>> NUCLEAR_CANNON_SHELL_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("nuclear_cannon_shell", () -> BlockEntityType.Builder.of(
                    NuclearCannonShellBlockEntity::new,
                    NUCLEAR_CANNON_SHELL_BLOCK.get()
            ).build(null));

    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>,
            net.minecraft.world.item.crafting.RecipeSerializer<ThermonuclearCannonShellRecipe>> THERMONUCLEAR_CANNON_SHELL_RECIPE =
            RECIPE_SERIALIZERS.register("thermonuclear_cannon_shell",
                    () -> new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(ThermonuclearCannonShellRecipe::new));

    public static final DeferredHolder<EntityType<?>, EntityType<ThermonuclearCannonShellProjectile>> THERMONUCLEAR_CANNON_SHELL_PROJECTILE =
            ENTITY_TYPES.register("thermonuclear_cannon_shell", () -> EntityType.Builder.<ThermonuclearCannonShellProjectile>of(
                            ThermonuclearCannonShellProjectile::new, MobCategory.MISC)
                    .sized(0.8f, 0.8f)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("thermonuclear_cannon_shell"));

    public static final DeferredHolder<Block, ThermonuclearCannonShellBlock> THERMONUCLEAR_CANNON_SHELL_BLOCK =
            BLOCKS.register("thermonuclear_cannon_shell", () -> new ThermonuclearCannonShellBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(4.0f, 12.0f)
                    .noOcclusion()));

    public static final DeferredHolder<Item, ThermonuclearCannonShellItem> THERMONUCLEAR_CANNON_SHELL =
            ITEMS.register("thermonuclear_cannon_shell", () -> new ThermonuclearCannonShellItem(THERMONUCLEAR_CANNON_SHELL_BLOCK.get(),
                    new Item.Properties().stacksTo(16)
                            .component(org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry.THERMONUCLEAR_LOAD.get(),
                                    org.papiricoh.create_nuclearindustry.explosive.ThermonuclearLoad.SHELL_FULL)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThermonuclearCannonShellBlockEntity>> THERMONUCLEAR_CANNON_SHELL_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("thermonuclear_cannon_shell", () -> BlockEntityType.Builder.of(
                    ThermonuclearCannonShellBlockEntity::new,
                    THERMONUCLEAR_CANNON_SHELL_BLOCK.get()
            ).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<DepletedUraniumAPShotProjectile>> DEPLETED_URANIUM_AP_SHOT_PROJECTILE =
            ENTITY_TYPES.register("depleted_uranium_ap_shot", () -> EntityType.Builder.<DepletedUraniumAPShotProjectile>of(
                            DepletedUraniumAPShotProjectile::new, MobCategory.MISC)
                    .sized(0.8f, 0.8f)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("depleted_uranium_ap_shot"));

    public static final DeferredHolder<Block, DepletedUraniumAPShotBlock> DEPLETED_URANIUM_AP_SHOT_BLOCK =
            BLOCKS.register("depleted_uranium_ap_shot", () -> new DepletedUraniumAPShotBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0f, 12.0f)
                    .noOcclusion()));

    public static final DeferredHolder<Item, ProjectileBlockItem> DEPLETED_URANIUM_AP_SHOT =
            ITEMS.register("depleted_uranium_ap_shot", () -> new ProjectileBlockItem(DEPLETED_URANIUM_AP_SHOT_BLOCK.get(),
                    new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BigCannonProjectileBlockEntity>> DEPLETED_URANIUM_AP_SHOT_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("depleted_uranium_ap_shot", () -> BlockEntityType.Builder.<BigCannonProjectileBlockEntity>of(
                    (pos, state) -> new BigCannonProjectileBlockEntity(CBCNuclearContent.DEPLETED_URANIUM_AP_SHOT_BLOCK_ENTITY.get(), pos, state),
                    DEPLETED_URANIUM_AP_SHOT_BLOCK.get()
            ).build(null));

    private CBCNuclearContent() {}

    public static void init(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        modEventBus.addListener(CBCNuclearContent::commonSetup);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(CBCNuclearContent::registerRenderers);
        }
    }

    public static void addCreativeItems(CreativeModeTab.Output output) {
        output.accept(NUCLEAR_CANNON_SHELL.get());
        output.accept(THERMONUCLEAR_CANNON_SHELL.get());
        output.accept(DEPLETED_URANIUM_AP_SHOT.get());
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            MunitionPropertiesHandler.registerProjectileHandler(
                    NUCLEAR_CANNON_SHELL_PROJECTILE.get(),
                    CBCMunitionPropertiesHandlers.COMMON_SHELL_BIG_CANNON_PROJECTILE);
            MunitionPropertiesHandler.registerProjectileHandler(
                    THERMONUCLEAR_CANNON_SHELL_PROJECTILE.get(),
                    CBCMunitionPropertiesHandlers.COMMON_SHELL_BIG_CANNON_PROJECTILE);
            MunitionPropertiesHandler.registerProjectileHandler(
                    DEPLETED_URANIUM_AP_SHOT_PROJECTILE.get(),
                    CBCMunitionPropertiesHandlers.INERT_BIG_CANNON_PROJECTILE);
        });
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NUCLEAR_CANNON_SHELL_PROJECTILE.get(), BigCannonProjectileRenderer::new);
        event.registerEntityRenderer(THERMONUCLEAR_CANNON_SHELL_PROJECTILE.get(), BigCannonProjectileRenderer::new);
        event.registerEntityRenderer(DEPLETED_URANIUM_AP_SHOT_PROJECTILE.get(), BigCannonProjectileRenderer::new);
    }
}
