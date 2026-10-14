package org.papiricoh.create_nuclearindustry.controlroom.servo;

import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;

import java.util.Locale;

/**
 * The Servo Gearbox: an encased shaft that puts a piston or pulley where a redstone signal says (see
 * {@link ServoGearboxBlockEntity}). Either end can take the rotation; the other drives. It takes
 * redstone on any face, its sides hold the mode and the stroke, and a comparator reads where the
 * actuator stands. MOTION only lights the arrow on its sides.
 */
public class ServoGearboxBlock extends AbstractEncasedShaftBlock implements IBE<ServoGearboxBlockEntity> {
    public static final EnumProperty<Motion> MOTION = EnumProperty.create("motion", Motion.class);

    /** Which arrow the sides light. */
    public enum Motion implements StringRepresentable {
        IDLE, EXTENDING, RETRACTING;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public ServoGearboxBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(MOTION, Motion.IDLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MOTION);
        super.createBlockStateDefinition(builder);
    }

    /** Redstone dust turns towards it, as to a repeater. */
    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return true;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ServoGearboxBlockEntity servo ? servo.comparatorSignal() : 0;
    }

    @Override
    public Class<ServoGearboxBlockEntity> getBlockEntityClass() {
        return ServoGearboxBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ServoGearboxBlockEntity> getBlockEntityType() {
        return ControlRoom2Registry.SERVO_GEARBOX_ENTITY.get();
    }
}
