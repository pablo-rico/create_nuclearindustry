package org.papiricoh.create_nuclearindustry.missile.orbit;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** A satellite ground station: a dish driven by a shaft from below, which tracks its network's satellites. */
public class GroundStationBlock extends KineticBlock implements EntityBlock {
    public static final MapCodec<GroundStationBlock> CODEC = simpleCodec(GroundStationBlock::new);

    public GroundStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends GroundStationBlock> codec() {
        return CODEC;
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GroundStationBlockEntity station
                && org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard.check(player, pos))
            player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new GroundStationMenu(id, inv, station, pos),
                    net.minecraft.network.chat.Component.translatable("block.create_nuclearindustry.ground_station")), buf -> buf.writeBlockPos(pos));
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.DOWN;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GroundStationBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == OrbitRegistry.STATION_ENTITY.get() ? (l, p, s, be) -> ((GroundStationBlockEntity) be).tick() : null;
    }
}
