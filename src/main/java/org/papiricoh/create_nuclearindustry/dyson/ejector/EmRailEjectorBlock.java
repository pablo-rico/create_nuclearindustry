package org.papiricoh.create_nuclearindustry.dyson.ejector;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.Containers;
import org.papiricoh.create_nuclearindustry.dyson.DysonRegistry;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

/**
 * An EM rail ejector: a plinth driven by a shaft from below, carrying a turntable and a pair of launch rails that aim at the sun
 * (drawn by its renderer). Its outline is the plinth; the rails overhang it.
 */
public class EmRailEjectorBlock extends KineticBlock implements EntityBlock {
    public static final MapCodec<EmRailEjectorBlock> CODEC = simpleCodec(EmRailEjectorBlock::new);
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 12, 16);

    public EmRailEjectorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends EmRailEjectorBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof EmRailEjectorBlockEntity ejector && ServerPayloadGuard.check(player, pos))
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new EjectorMenu(id, inventory, ejector, pos),
                    Component.translatable("block.create_nuclearindustry.em_rail_ejector")), buf -> buf.writeBlockPos(pos));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof EmRailEjectorBlockEntity ejector) {
            for (int i = 0; i < EmRailEjectorBlockEntity.SLOTS; i++)
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), ejector.magazine().getStackInSlot(i));
        }
        super.onRemove(state, level, pos, newState, moving);
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
        return new EmRailEjectorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DysonRegistry.EJECTOR_ENTITY.get() ? (l, p, s, be) -> ((EmRailEjectorBlockEntity) be).tick() : null;
    }
}
