package org.papiricoh.create_nuclearindustry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.enrichment.block.CentrifugeBlock;
import org.papiricoh.create_nuclearindustry.explosive.block.NuclearBombBlock;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionFluidPortBlock;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionFuelInjectorBlock;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionMagnetInputBlock;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionPlasmaTurbineBlock;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionReactorControllerBlock;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionTurbineFluidPortBlock;
import org.papiricoh.create_nuclearindustry.fluids.block.DualFluidPipeBlock;
import org.papiricoh.create_nuclearindustry.missile.block.LaunchPadBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ControlRodBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.HeatExchangerBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.NuclearReactorControllerBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFuelPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortBlock;
import org.papiricoh.create_nuclearindustry.coolingtower.block.CoolingTowerControllerBlock;
import org.papiricoh.create_nuclearindustry.coolingtower.block.CoolingTowerFanBlock;
import org.papiricoh.create_nuclearindustry.coolingtower.block.CoolingTowerFluidPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorRedstonePortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorTemperatureSensorBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.TurbineCasingBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.TurbineFluidPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.TurbineOutputBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.TurbineRotorBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.UraniumRodBlock;

public class AllNuclearBlocks {
    // Create a Deferred Register to hold Blocks which will all be registered under the "create_nuclearindustry" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, Create_NuclearIndustry.MODID);


    // Creates a new Block with the id "create_nuclearindustry:example_block", combining the namespace and path
    public static final DeferredHolder<Block, ? extends Block> URANIUM_ORE = BLOCKS.register("uranium_ore", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, ? extends Block> BORAX_ORE = BLOCKS.register("borax_ore", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, ? extends Block> SPODUMENE_ORE = BLOCKS.register("spodumene_ore", () -> new net.minecraft.world.level.block.DropExperienceBlock(
            net.minecraft.util.valueproviders.UniformInt.of(1, 3),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));



    public static final DeferredHolder<Block, ? extends Block> REACTOR_CASING = BLOCKS.register("reactor_casing", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f)));



    public static final DeferredHolder<Block, ? extends Block> CONTROL_ROD = BLOCKS.register("control_rod", () -> new ControlRodBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f)));
    public static final DeferredHolder<Block, ? extends Block> URANIUM_ROD = BLOCKS.register("uranium_rod", () -> new UraniumRodBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f)));
    public static final DeferredHolder<Block, ? extends Block> HEAT_EXCHANGER = BLOCKS.register("heat_exchanger", () -> new HeatExchangerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f)));
    public static final DeferredHolder<Block, ? extends Block> REACTOR_CONTROLLER = BLOCKS.register("reactor_controller", () -> new NuclearReactorControllerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f)));
    public static final DeferredHolder<Block, ? extends Block> REACTOR_FLUID_PORT = BLOCKS.register("reactor_fluid_port", () -> new ReactorFluidPortBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f)));
    public static final DeferredHolder<Block, ? extends Block> REACTOR_FUEL_PORT = BLOCKS.register("reactor_fuel_port", () -> new ReactorFuelPortBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f)));
    public static final DeferredHolder<Block, ? extends Block> REACTOR_TEMPERATURE_SENSOR = BLOCKS.register("reactor_temperature_sensor", () -> new ReactorTemperatureSensorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f)));
    public static final DeferredHolder<Block, ? extends Block> REACTOR_REDSTONE_PORT = BLOCKS.register("reactor_redstone_port", () -> new ReactorRedstonePortBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f)));
    public static final DeferredHolder<Block, ? extends Block> COOLING_TOWER_CASING = BLOCKS.register("cooling_tower_casing", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.5f, 9.0f).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, ? extends Block> COOLING_TOWER_CONTROLLER = BLOCKS.register("cooling_tower_controller", () -> new CoolingTowerControllerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 9.0f).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, ? extends Block> COOLING_TOWER_FLUID_PORT = BLOCKS.register("cooling_tower_fluid_port", () -> new CoolingTowerFluidPortBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 9.0f).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, ? extends Block> COOLING_TOWER_FAN = BLOCKS.register("cooling_tower_fan", () -> new CoolingTowerFanBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 9.0f).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, ? extends Block> DUAL_FLUID_PIPE = BLOCKS.register("dual_fluid_pipe", () -> new DualFluidPipeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f, 6.0f)));
    public static final DeferredHolder<Block, ? extends Block> CENTRIFUGE = BLOCKS.register("centrifuge", () -> new CentrifugeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).noOcclusion()));
    public static final DeferredHolder<Block, ? extends Block> ELECTROLYZER = BLOCKS.register("electrolyzer", () -> new org.papiricoh.create_nuclearindustry.chemistry.electrolyzer.ElectrolyzerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(3.0f, 6.0f).noOcclusion()));
    public static final DeferredHolder<Block, ? extends Block> RADIOACTIVE_WASTE_BARREL = BLOCKS.register("radioactive_waste_barrel", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(4.0f, 12.0f).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, ? extends Block> DEPLETED_URANIUM_BLOCK = BLOCKS.register("depleted_uranium_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 12.0f).sound(net.minecraft.world.level.block.SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, ? extends Block> RTG = BLOCKS.register("rtg", () -> new org.papiricoh.create_nuclearindustry.rtg.RtgBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 9.0f).noOcclusion().requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, ? extends Block> REINFORCED_TURBINE_GLASS = BLOCKS.register("reinforced_turbine_glass", () -> new org.papiricoh.create_nuclearindustry.turbine.block.TurbineGlassBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.NONE).strength(3.0f, 1200.0f).sound(net.minecraft.world.level.block.SoundType.GLASS).noOcclusion()
            .isValidSpawn((state, level, pos, type) -> false).isRedstoneConductor((state, level, pos) -> false)
            .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false)));
    public static final DeferredHolder<Block, ? extends Block> TURBINE_CASING = BLOCKS.register("turbine_casing", () -> new TurbineCasingBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f)));
    public static final DeferredHolder<Block, ? extends Block> TURBINE_ROTOR = BLOCKS.register("turbine_rotor", () -> new TurbineRotorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f, 6.0f).noOcclusion()));
    public static final DeferredHolder<Block, ? extends Block> TURBINE_OUTPUT = BLOCKS.register("turbine_output", () -> new TurbineOutputBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).noOcclusion()));
    public static final DeferredHolder<Block, ? extends Block> TURBINE_FLUID_PORT = BLOCKS.register("turbine_fluid_port", () -> new TurbineFluidPortBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).noOcclusion()));
    public static final DeferredHolder<Block, ? extends Block> NUCLEAR_BOMB = BLOCKS.register("nuclear_bomb", () -> new NuclearBombBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 12.0f).noOcclusion()));
    public static final DeferredHolder<Block, ? extends Block> LAUNCH_PAD = BLOCKS.register("launch_pad", () -> new LaunchPadBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 12.0f).noOcclusion()));

    // ---- Fusion reactor (independent ring multiblock) ----
    public static final DeferredHolder<Block, ? extends Block> FUSION_CONTROLLER = BLOCKS.register("fusion_controller", () -> new FusionReactorControllerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(4.0f, 8.0f).noOcclusion()));
    public static final DeferredHolder<Block, ? extends Block> FUSION_ACCELERATOR_SEGMENT = BLOCKS.register("fusion_accelerator_segment", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f)));
    public static final DeferredHolder<Block, ? extends Block> FUSION_ACCELERATOR_CORNER = BLOCKS.register("fusion_accelerator_corner", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f)));
    public static final DeferredHolder<Block, ? extends Block> FUSION_ELECTROMAGNET = BLOCKS.register("fusion_electromagnet", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(3.0f, 6.0f)));
    public static final DeferredHolder<Block, ? extends Block> FUSION_CRYOSTAT_CASING = BLOCKS.register("fusion_cryostat_casing", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(3.0f, 6.0f)));
    public static final DeferredHolder<Block, ? extends Block> FUSION_CENTRAL_SOLENOID = BLOCKS.register("fusion_central_solenoid", () -> new net.minecraft.world.level.block.RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(3.0f, 6.0f)));
    public static final DeferredHolder<Block, ? extends Block> FUSION_BREEDING_BLANKET = BLOCKS.register("fusion_breeding_blanket", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(3.0f, 6.0f)));
    public static final DeferredHolder<Block, ? extends Block> FUSION_MAGNET_INPUT = BLOCKS.register("fusion_magnet_input", () -> new FusionMagnetInputBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(3.0f, 6.0f).noOcclusion()));
    public static final DeferredHolder<Block, ? extends Block> FUSION_FLUID_PORT = BLOCKS.register("fusion_fluid_port", () -> new FusionFluidPortBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).noOcclusion()));
    public static final DeferredHolder<Block, ? extends Block> FUSION_FUEL_INJECTOR = BLOCKS.register("fusion_fuel_injector", () -> new FusionFuelInjectorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).noOcclusion()));
    public static final DeferredHolder<Block, ? extends Block> FUSION_TURBINE_CASING = BLOCKS.register("fusion_turbine_casing", () -> new TurbineCasingBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f)));
    public static final DeferredHolder<Block, ? extends Block> FUSION_TURBINE_ROTOR = BLOCKS.register("fusion_turbine_rotor", () -> new TurbineRotorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f, 6.0f).noOcclusion()));
    public static final DeferredHolder<Block, ? extends Block> FUSION_TURBINE_FLUID_PORT = BLOCKS.register("fusion_turbine_fluid_port", () -> new FusionTurbineFluidPortBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).noOcclusion()));
    public static final DeferredHolder<Block, ? extends Block> FUSION_PLASMA_TURBINE = BLOCKS.register("fusion_plasma_turbine", () -> new FusionPlasmaTurbineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).noOcclusion()));


    public static void init() {}
}
