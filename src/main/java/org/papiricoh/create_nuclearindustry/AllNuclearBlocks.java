package org.papiricoh.create_nuclearindustry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.papiricoh.create_nuclearindustry.blocks.ControlRodBlock;
import org.papiricoh.create_nuclearindustry.blocks.HeatExchangerBlock;
import org.papiricoh.create_nuclearindustry.blocks.NuclearReactorControllerBlock;
import org.papiricoh.create_nuclearindustry.blocks.UraniumRodBlock;

public class AllNuclearBlocks {
    // Create a Deferred Register to hold Blocks which will all be registered under the "create_nuclearindustry" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Create_NuclearIndustry.MODID);


    // Creates a new Block with the id "create_nuclearindustry:example_block", combining the namespace and path
    public static final RegistryObject<Block> URANIUM_ORE = BLOCKS.register("uranium_ore", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> BORAX_ORE = BLOCKS.register("borax_ore", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));



    public static final RegistryObject<Block> REACTOR_CASING = BLOCKS.register("reactor_casing", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f)));



    public static final RegistryObject<Block> CONTROL_ROD = BLOCKS.register("control_rod", () -> new ControlRodBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f)));
    public static final RegistryObject<Block> URANIUM_ROD = BLOCKS.register("uranium_rod", () -> new UraniumRodBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f)));
    public static final RegistryObject<Block> HEAT_EXCHANGER = BLOCKS.register("heat_exchanger", () -> new HeatExchangerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f)));
    public static final RegistryObject<Block> REACTOR_CONTROLLER = BLOCKS.register("reactor_controller", () -> new NuclearReactorControllerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f)));




    public static void init() {}
}
