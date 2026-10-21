package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlock;
import org.papiricoh.create_nuclearindustry.controlroom.desk.DeskShape;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.coolingtower.block.CoolingTowerFluidPortBlock;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionTurbineFluidPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;
import org.papiricoh.create_nuclearindustry.reactor.block.TurbineFluidPortBlock;
import org.papiricoh.create_nuclearindustry.turbine.block.TurbineFormedLook;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorRedstonePortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.RedstonePortMode;

/** Block states and models for blocks added by the rewrite; older blocks keep hand-written ones. */
public class NuclearBlockStateProvider extends BlockStateProvider {

    public NuclearBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Create_NuclearIndustry.MODID, existingFileHelper);
    }

    /** Rotor stages keep their model in both states; a formed rotor is hidden by its render shape. */
    private void rotorStates(Block block, String model) {
        ModelFile file = models().getExistingFile(modLoc("block/" + model));
        getVariantBuilder(block).forAllStates(state -> {
            Direction.Axis axis = state.getValue(RotatedPillarBlock.AXIS);
            return ConfiguredModel.builder().modelFile(file)
                    .rotationX(axis == Direction.Axis.Y ? 0 : 90)
                    .rotationY(axis == Direction.Axis.X ? 90 : 0)
                    .build();
        });
    }

    /** For orientable models whose front texture faces north. */
    private void facingStates(Block block, String model) {
        ModelFile file = models().getExistingFile(modLoc("block/" + model));
        getVariantBuilder(block).forAllStates(state -> {
            Direction facing = state.getValue(BlockStateProperties.FACING);
            return ConfiguredModel.builder().modelFile(file).rotationX(northToX(facing)).rotationY(northToY(facing)).build();
        });
    }

    /** Blockstate rotations that turn a model built facing north to {@code facing}. */
    private static int northToX(Direction facing) {
        return facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0;
    }

    private static int northToY(Direction facing) {
        return facing.getAxis().isHorizontal() ? ((int) facing.toYRot() + 180) % 360 : 0;
    }

    /** A turbine controller: the one-block engine, or once formed only what the bezel leaves to show. */
    private void turbineControllerStates(Block block, String model, String formedModel) {
        ModelFile unformed = models().getExistingFile(modLoc("block/" + model));
        ModelFile formed = models().getExistingFile(modLoc("block/" + formedModel));
        getVariantBuilder(block).forAllStates(state -> {
            Direction facing = state.getValue(BlockStateProperties.FACING);
            return ConfiguredModel.builder().modelFile(state.getValue(TurbineFormedLook.FORMED) ? formed : unformed)
                    .rotationX(northToX(facing)).rotationY(northToY(facing)).build();
        });
    }

    /** A turbine wall without a facing: the full block, or thin plates on its outer faces once formed. */
    private void turbineWallStates(Block block, String model, String panelModel) {
        MultiPartBlockStateBuilder builder = getMultipartBuilder(block);
        unpanelled(builder.part().modelFile(models().getExistingFile(modLoc("block/" + model))).addModel()).end();
        panelParts(builder, panelModel, part -> {});
    }

    /** Turbine fluid ports: the orientable port, or a port plate per outer face once formed. */
    private void turbinePortStates(Block block, EnumProperty<ReactorFluidPortMode> modeProperty, String model, String panelDir) {
        MultiPartBlockStateBuilder builder = getMultipartBuilder(block);
        for (ReactorFluidPortMode mode : ReactorFluidPortMode.values()) {
            ModelFile file = models().getExistingFile(modLoc("block/" + model + "_" + mode.getSerializedName()));
            for (Direction facing : Direction.values()) {
                unpanelled(builder.part().modelFile(file).rotationX(northToX(facing)).rotationY(northToY(facing)).addModel()
                        .condition(BlockStateProperties.FACING, facing).condition(modeProperty, mode)).end();
            }
            panelParts(builder, panelDir + "/fluid_port_" + mode.getSerializedName() + "_panel", part -> part.condition(modeProperty, mode));
        }
    }

    private void turbineExhaustStates(Block block, String model, String panelModel) {
        MultiPartBlockStateBuilder builder = getMultipartBuilder(block);
        ModelFile file = models().getExistingFile(modLoc("block/" + model));
        for (Direction facing : Direction.values()) {
            unpanelled(builder.part().modelFile(file).rotationX(northToX(facing)).rotationY(northToY(facing)).addModel()
                    .condition(BlockStateProperties.FACING, facing)).end();
        }
        panelParts(builder, panelModel, part -> {});
    }

    private static MultiPartBlockStateBuilder.PartBuilder unpanelled(MultiPartBlockStateBuilder.PartBuilder part) {
        TurbineFormedLook.PANELS.values().forEach(panel -> part.condition(panel, false));
        return part;
    }

    /**
     * One plate model (built on the north face) turned onto every face whose panel is set. UV-locked
     * so connected textures, which are picked in world orientation, keep their borders on the right
     * edges of the top and bottom plates.
     */
    private void panelParts(MultiPartBlockStateBuilder builder, String panelModel,
                            java.util.function.Consumer<MultiPartBlockStateBuilder.PartBuilder> extra) {
        ModelFile panel = models().getExistingFile(modLoc("block/" + panelModel));
        TurbineFormedLook.PANELS.forEach((face, property) -> {
            MultiPartBlockStateBuilder.PartBuilder part = builder.part().modelFile(panel)
                    .rotationX(northToX(face)).rotationY(northToY(face)).uvLock(true).addModel().condition(property, true);
            extra.accept(part);
            part.end();
        });
    }

    /** Reactor-casing block with a lamp panel on its output face; the SCRAM mode gets a striped panel. */
    private void redstonePortStates() {
        ModelFile normal = models().orientable("reactor_redstone_port", modLoc("block/reactor_casing"),
                modLoc("block/reactor_redstone_port"), modLoc("block/reactor_casing"));
        ModelFile scram = models().orientable("reactor_redstone_port_scram", modLoc("block/reactor_casing"),
                modLoc("block/reactor_redstone_port_scram"), modLoc("block/reactor_casing"));
        getVariantBuilder(AllNuclearBlocks.REACTOR_REDSTONE_PORT.get()).forAllStatesExcept(state -> {
            Direction facing = state.getValue(BlockStateProperties.FACING);
            int x = facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0;
            int y = facing.getAxis().isHorizontal() ? ((int) facing.toYRot() + 180) % 360 : 0;
            ModelFile model = state.getValue(ReactorRedstonePortBlock.MODE) == RedstonePortMode.SCRAM ? scram : normal;
            return ConfiguredModel.builder().modelFile(model).rotationX(x).rotationY(y).build();
        }, BlockStateProperties.POWER);
        simpleBlockItem(AllNuclearBlocks.REACTOR_REDSTONE_PORT.get(), normal);
    }

    /** Monitors and panels face the player; the alarm swaps to its lit textures while it sounds. */
    private void controlRoomStates() {
        ResourceLocation housing = modLoc("block/control_monitor_side");
        ModelFile monitor = models().orientable("control_monitor", housing, modLoc("block/control_monitor_front"), housing);
        horizontalBlock(AllNuclearBlocks.CONTROL_MONITOR.get(), monitor);
        simpleBlockItem(AllNuclearBlocks.CONTROL_MONITOR.get(), monitor);
        controlDeskStates();
        ModelFile alarm = models().cubeBottomTop("nuclear_alarm", modLoc("block/nuclear_alarm_side"), housing, modLoc("block/nuclear_alarm_top"));
        ModelFile alarmLit = models().cubeBottomTop("nuclear_alarm_lit", modLoc("block/nuclear_alarm_side_lit"), housing,
                modLoc("block/nuclear_alarm_top_lit"));
        getVariantBuilder(AllNuclearBlocks.NUCLEAR_ALARM.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(BlockStateProperties.LIT) ? alarmLit : alarm).build());
        simpleBlockItem(AllNuclearBlocks.NUCLEAR_ALARM.get(), alarm);
    }

    /**
     * The control desk is multipart: a body per shape plus an end cheek on each end no neighbouring
     * desk closes, so joined desks show no seam. The models are hand-made in models/block/control_desk;
     * native orientation has the operator to the north.
     */
    private void controlDeskStates() {
        Block desk = AllNuclearBlocks.REACTOR_CONTROL_PANEL.get();
        ModelFile straight = models().getExistingFile(modLoc("block/control_desk/straight"));
        ModelFile inner = models().getExistingFile(modLoc("block/control_desk/inner"));
        ModelFile outer = models().getExistingFile(modLoc("block/control_desk/outer"));
        ModelFile capLeft = models().getExistingFile(modLoc("block/control_desk/cap_left"));
        ModelFile capRight = models().getExistingFile(modLoc("block/control_desk/cap_right"));
        var builder = getMultipartBuilder(desk);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int base = ((int) facing.toYRot() + 180) % 360;
            for (DeskShape shape : DeskShape.values()) {
                ModelFile body = shape == DeskShape.STRAIGHT ? straight : shape.isInner() ? inner : outer;
                builder.part().modelFile(body).rotationY((base + shape.bodyExtraRotation()) % 360).addModel()
                        .condition(BlockStateProperties.HORIZONTAL_FACING, facing).condition(ControlDeskBlock.SHAPE, shape).end();
                builder.part().modelFile(capLeft).rotationY((base + shape.leftCapRotation()) % 360).addModel()
                        .condition(BlockStateProperties.HORIZONTAL_FACING, facing).condition(ControlDeskBlock.SHAPE, shape)
                        .condition(ControlDeskBlock.LEFT, false).end();
                builder.part().modelFile(capRight).rotationY((base + shape.rightCapRotation()) % 360).addModel()
                        .condition(BlockStateProperties.HORIZONTAL_FACING, facing).condition(ControlDeskBlock.SHAPE, shape)
                        .condition(ControlDeskBlock.RIGHT, false).end();
            }
        }
    }

    /** Concrete casing for the tower, with a front texture on the faces that have one. */
    private void coolingTowerStates() {
        ResourceLocation concrete = modLoc("block/cooling_tower_casing");
        simpleBlockWithItem(AllNuclearBlocks.COOLING_TOWER_CASING.get(), cubeAll(AllNuclearBlocks.COOLING_TOWER_CASING.get()));

        ModelFile controller = models().orientable("cooling_tower_controller", concrete, modLoc("block/cooling_tower_controller"), concrete);
        horizontalBlock(AllNuclearBlocks.COOLING_TOWER_CONTROLLER.get(), controller);
        simpleBlockItem(AllNuclearBlocks.COOLING_TOWER_CONTROLLER.get(), controller);

        ModelFile fan = models().orientable("cooling_tower_fan", concrete, modLoc("block/cooling_tower_fan"), concrete);
        horizontalBlock(AllNuclearBlocks.COOLING_TOWER_FAN.get(), fan);
        simpleBlockItem(AllNuclearBlocks.COOLING_TOWER_FAN.get(), fan);

        ModelFile input = models().orientable("cooling_tower_fluid_port_input", concrete,
                modLoc("block/cooling_tower_fluid_port_input"), concrete);
        ModelFile output = models().orientable("cooling_tower_fluid_port_output", concrete,
                modLoc("block/cooling_tower_fluid_port_output"), concrete);
        getVariantBuilder(AllNuclearBlocks.COOLING_TOWER_FLUID_PORT.get()).forAllStates(state -> {
            Direction facing = state.getValue(BlockStateProperties.FACING);
            int x = facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0;
            int y = facing.getAxis().isHorizontal() ? ((int) facing.toYRot() + 180) % 360 : 0;
            ModelFile model = state.getValue(CoolingTowerFluidPortBlock.MODE) == ReactorFluidPortMode.INPUT ? input : output;
            return ConfiguredModel.builder().modelFile(model).rotationX(x).rotationY(y).build();
        });
        simpleBlockItem(AllNuclearBlocks.COOLING_TOWER_FLUID_PORT.get(), input);
    }

    @Override
    protected void registerStatesAndModels() {
        // Fluid blocks render from their fluid type; the model only needs a particle texture.
        simpleBlock(org.papiricoh.create_nuclearindustry.AllNuclearFluids.CONTAMINATED_WATER_BLOCK.get(),
                models().getBuilder("contaminated_water").texture("particle", mcLoc("block/water_still")));
        simpleBlockWithItem(AllNuclearBlocks.SPODUMENE_ORE.get(), cubeAll(AllNuclearBlocks.SPODUMENE_ORE.get()));
        axisBlock((net.minecraft.world.level.block.RotatedPillarBlock) AllNuclearBlocks.FUSION_CENTRAL_SOLENOID.get(),
                modLoc("block/fusion_central_solenoid_side"), modLoc("block/fusion_central_solenoid_top"));
        simpleBlockItem(AllNuclearBlocks.FUSION_CENTRAL_SOLENOID.get(), models().getExistingFile(modLoc("block/fusion_central_solenoid")));
        simpleBlockWithItem(AllNuclearBlocks.FUSION_BREEDING_BLANKET.get(), cubeAll(AllNuclearBlocks.FUSION_BREEDING_BLANKET.get()));
        turbineWallStates(AllNuclearBlocks.REINFORCED_TURBINE_GLASS.get(), "reinforced_turbine_glass", "turbine/glass_panel");
        simpleBlockItem(AllNuclearBlocks.REINFORCED_TURBINE_GLASS.get(), models().getExistingFile(modLoc("block/reinforced_turbine_glass")));
        rotorStates(AllNuclearBlocks.TURBINE_ROTOR.get(), "turbine_rotor");
        rotorStates(AllNuclearBlocks.FUSION_TURBINE_ROTOR.get(), "fusion_turbine_rotor");
        turbineControllerStates(AllNuclearBlocks.TURBINE_OUTPUT.get(), "turbine_output", "turbine/output_formed");
        turbineControllerStates(AllNuclearBlocks.FUSION_PLASMA_TURBINE.get(), "fusion_plasma_turbine", "fusion_turbine/output_formed");
        turbineWallStates(AllNuclearBlocks.TURBINE_CASING.get(), "turbine_casing", "turbine/casing_panel");
        turbineWallStates(AllNuclearBlocks.FUSION_TURBINE_CASING.get(), "fusion_turbine_casing", "fusion_turbine/casing_panel");
        turbinePortStates(AllNuclearBlocks.TURBINE_FLUID_PORT.get(), TurbineFluidPortBlock.MODE, "turbine_fluid_port", "turbine");
        turbinePortStates(AllNuclearBlocks.FUSION_TURBINE_FLUID_PORT.get(), FusionTurbineFluidPortBlock.MODE, "fusion_turbine_fluid_port", "fusion_turbine");
        simpleBlockWithItem(AllNuclearBlocks.DEPLETED_URANIUM_BLOCK.get(), cubeAll(AllNuclearBlocks.DEPLETED_URANIUM_BLOCK.get()));
        simpleBlockWithItem(AllNuclearBlocks.GRAPHITE_MODERATOR.get(), cubeAll(AllNuclearBlocks.GRAPHITE_MODERATOR.get()));
        simpleBlockWithItem(AllNuclearBlocks.BERYLLIUM_REFLECTOR.get(), cubeAll(AllNuclearBlocks.BERYLLIUM_REFLECTOR.get()));
        redstonePortStates();
        controlRoomStates();
        ModelFile dirtyBomb = models().withExistingParent("dirty_bomb", modLoc("block/nuclear_bomb"))
                .texture("body", modLoc("block/dirty_bomb_body")).texture("top", modLoc("block/dirty_bomb_top"))
                .texture("particle", modLoc("block/dirty_bomb_body"));
        getVariantBuilder(AllNuclearBlocks.DIRTY_BOMB.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(dirtyBomb).build());
        simpleBlockItem(AllNuclearBlocks.DIRTY_BOMB.get(), dirtyBomb);
        simpleBlockWithItem(AllNuclearBlocks.RADIATION_SCRUBBER.get(), models().cubeBottomTop("radiation_scrubber",
                modLoc("block/radiation_scrubber_side"), modLoc("block/radiation_scrubber_bottom"), modLoc("block/radiation_scrubber_top")));
        // The RTG's model faces north with its shaft opening; the shaft points along FACING.
        ModelFile rtg = models().getExistingFile(modLoc("block/rtg"));
        directionalBlock(AllNuclearBlocks.RTG.get(), rtg);
        simpleBlockItem(AllNuclearBlocks.RTG.get(), rtg);
        coolingTowerStates();
        turbineExhaustStates(AllNuclearBlocks.TURBINE_EXHAUST_PORT.get(), "turbine_exhaust_port", "turbine/exhaust_port_panel");
        simpleBlockItem(AllNuclearBlocks.TURBINE_EXHAUST_PORT.get(), models().getExistingFile(modLoc("block/turbine_exhaust_port")));
        simpleBlockWithItem(AllNuclearBlocks.RADIOACTIVE_WASTE_BARREL.get(), models().cubeBottomTop("radioactive_waste_barrel",
                modLoc("block/radioactive_waste_barrel_side"),
                modLoc("block/radioactive_waste_barrel_top"),
                modLoc("block/radioactive_waste_barrel_top")));
        AreaDataGen.ALL.forEach(area -> area.blockStates(this));
    }
}
