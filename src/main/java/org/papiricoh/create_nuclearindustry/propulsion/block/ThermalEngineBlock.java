package org.papiricoh.create_nuclearindustry.propulsion.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.papiricoh.create_nuclearindustry.propulsion.PropulsionRegistry;
import org.papiricoh.create_nuclearindustry.propulsion.blockentity.ThermalEngineBlockEntity;

/** The nuclear thermal engine's controller: pipes bring it hot primary coolant and propellant. */
public class ThermalEngineBlock extends RocketEngineBlock<ThermalEngineBlockEntity> {
    public static final MapCodec<ThermalEngineBlock> CODEC = simpleCodec(ThermalEngineBlock::new);

    public ThermalEngineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public Class<ThermalEngineBlockEntity> getBlockEntityClass() {
        return ThermalEngineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ThermalEngineBlockEntity> getBlockEntityType() {
        return PropulsionRegistry.THERMAL_ENGINE_ENTITY.get();
    }
}
