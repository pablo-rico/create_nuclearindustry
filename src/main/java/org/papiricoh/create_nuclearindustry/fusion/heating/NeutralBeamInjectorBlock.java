package org.papiricoh.create_nuclearindustry.fusion.heating;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakShellRole;

/** Neutral beam injector: a kinetic heater in the tokamak shell. */
public class NeutralBeamInjectorBlock extends PlasmaHeaterBlock<NeutralBeamInjectorBlockEntity> {
    public static final MapCodec<NeutralBeamInjectorBlock> CODEC = simpleCodec(NeutralBeamInjectorBlock::new);

    public NeutralBeamInjectorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends NeutralBeamInjectorBlock> codec() {
        return CODEC;
    }

    @Override
    public TokamakShellRole tokamakRole(BlockState state) {
        return TokamakShellRole.NBI;
    }

    @Override
    public Class<NeutralBeamInjectorBlockEntity> getBlockEntityClass() {
        return NeutralBeamInjectorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends NeutralBeamInjectorBlockEntity> getBlockEntityType() {
        return PlasmaHeatingRegistry.NEUTRAL_BEAM_INJECTOR_ENTITY.get();
    }
}
