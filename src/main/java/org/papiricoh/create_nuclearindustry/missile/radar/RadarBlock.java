package org.papiricoh.create_nuclearindustry.missile.radar;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class RadarBlock extends KineticBlock implements EntityBlock {
    public static final MapCodec<RadarBlock> CODEC = simpleCodec(RadarBlock::new);
    public RadarBlock(Properties p) { super(p); }
    @Override protected MapCodec<? extends RadarBlock> codec() { return CODEC; }
    @Override public Direction.Axis getRotationAxis(BlockState state) { return Direction.Axis.Y; }
    @Override public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) { return face == Direction.DOWN; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RadarBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == RadarRegistry.ENTITY.get() ? (l, p, s, be) -> ((RadarBlockEntity) be).tick() : null;
    }
    @Override protected boolean isSignalSource(BlockState state) { return true; }
    @Override protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return level.getBlockEntity(pos) instanceof RadarBlockEntity radar && radar.alarm() ? 15 : 0;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RadarBlockEntity radar
                && org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard.check(player, pos)) {
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new RadarMenu(id, inv, radar, pos),
                    Component.translatable("block.create_nuclearindustry.missile_radar")), b -> b.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
