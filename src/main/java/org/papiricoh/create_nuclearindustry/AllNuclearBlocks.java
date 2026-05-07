package org.papiricoh.create_nuclearindustry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.enrichment.block.CentrifugeBlock;
import org.papiricoh.create_nuclearindustry.fluids.block.DualFluidPipeBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ControlRodBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.HeatExchangerBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.NuclearReactorControllerBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.UraniumRodBlock;

public class AllNuclearBlocks {
    // Create a Deferred Register to hold Blocks which will all be registered under the "create_nuclearindustry" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, Create_NuclearIndustry.MODID);


    // Creates a new Block with the id "create_nuclearindustry:example_block", combining the namespace and path
    public static final DeferredHolder<Block, ? extends Block> URANIUM_ORE = BLOCKS.register("uranium_ore", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, ? extends Block> BORAX_ORE = BLOCKS.register("borax_ore", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));



    public static final DeferredHolder<Block, ? extends Block> REACTOR_CASING = BLOCKS.register("reactor_casing", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f)));



    public static final DeferredHolder<Block, ? extends Block> CONTROL_ROD = BLOCKS.register("control_rod", () -> new ControlRodBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f)));
    public static final DeferredHolder<Block, ? extends Block> URANIUM_ROD = BLOCKS.register("uranium_rod", () -> new UraniumRodBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f)));
    public static final DeferredHolder<Block, ? extends Block> HEAT_EXCHANGER = BLOCKS.register("heat_exchanger", () -> new HeatExchangerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f)));
    public static final DeferredHolder<Block, ? extends Block> REACTOR_CONTROLLER = BLOCKS.register("reactor_controller", () -> new NuclearReactorControllerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f)));
    public static final DeferredHolder<Block, ? extends Block> DUAL_FLUID_PIPE = BLOCKS.register("dual_fluid_pipe", () -> new DualFluidPipeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f, 6.0f)));
    public static final DeferredHolder<Block, ? extends Block> CENTRIFUGE = BLOCKS.register("centrifuge", () -> new CentrifugeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).noOcclusion()));




    public static void init() {}
}
