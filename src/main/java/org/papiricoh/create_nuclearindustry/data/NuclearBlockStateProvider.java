package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

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
            int x = facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0;
            int y = facing.getAxis().isHorizontal() ? ((int) facing.toYRot() + 180) % 360 : 0;
            return ConfiguredModel.builder().modelFile(file).rotationX(x).rotationY(y).build();
        });
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlockWithItem(AllNuclearBlocks.SPODUMENE_ORE.get(), cubeAll(AllNuclearBlocks.SPODUMENE_ORE.get()));
        axisBlock((net.minecraft.world.level.block.RotatedPillarBlock) AllNuclearBlocks.FUSION_CENTRAL_SOLENOID.get(),
                modLoc("block/fusion_central_solenoid_side"), modLoc("block/fusion_central_solenoid_top"));
        simpleBlockItem(AllNuclearBlocks.FUSION_CENTRAL_SOLENOID.get(), models().getExistingFile(modLoc("block/fusion_central_solenoid")));
        simpleBlockWithItem(AllNuclearBlocks.FUSION_BREEDING_BLANKET.get(), cubeAll(AllNuclearBlocks.FUSION_BREEDING_BLANKET.get()));
        simpleBlockWithItem(AllNuclearBlocks.REINFORCED_TURBINE_GLASS.get(), models()
                .cubeAll("reinforced_turbine_glass", modLoc("block/reinforced_turbine_glass"))
                .renderType("cutout"));
        rotorStates(AllNuclearBlocks.TURBINE_ROTOR.get(), "turbine_rotor");
        rotorStates(AllNuclearBlocks.FUSION_TURBINE_ROTOR.get(), "fusion_turbine_rotor");
        facingStates(AllNuclearBlocks.TURBINE_OUTPUT.get(), "turbine_output");
        simpleBlockWithItem(AllNuclearBlocks.RADIOACTIVE_WASTE_BARREL.get(), models().cubeBottomTop("radioactive_waste_barrel",
                modLoc("block/radioactive_waste_barrel_side"),
                modLoc("block/radioactive_waste_barrel_top"),
                modLoc("block/radioactive_waste_barrel_top")));
    }
}
