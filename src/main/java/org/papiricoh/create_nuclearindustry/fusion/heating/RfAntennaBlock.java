package org.papiricoh.create_nuclearindustry.fusion.heating;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakShellRole;

/** Radio-frequency heating antenna: a kinetic heater in the tokamak shell. */
public class RfAntennaBlock extends PlasmaHeaterBlock<RfAntennaBlockEntity> {
    public static final MapCodec<RfAntennaBlock> CODEC = simpleCodec(RfAntennaBlock::new);

    public RfAntennaBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends RfAntennaBlock> codec() {
        return CODEC;
    }

    @Override
    public TokamakShellRole tokamakRole(BlockState state) {
        return TokamakShellRole.RF_ANTENNA;
    }

    @Override
    public Class<RfAntennaBlockEntity> getBlockEntityClass() {
        return RfAntennaBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RfAntennaBlockEntity> getBlockEntityType() {
        return PlasmaHeatingRegistry.RF_ANTENNA_ENTITY.get();
    }
}
