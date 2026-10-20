package org.papiricoh.create_nuclearindustry.missile.silo;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.papiricoh.create_nuclearindustry.missile.physics.PropellantModel;

import java.util.Locale;

/** A propellant tank: shows whether it holds a solid cartridge or liquids, and how full of liquids it is. */
public class RocketTankBlock extends RocketSectionBlock {
    public static final EnumProperty<Propellant> PROPELLANT = EnumProperty.create("propellant", Propellant.class);
    /** Liquids in quarters, rounded up so a drop shows: 0 empty, 4 full. Always 0 unless {@link Propellant#LIQUID}. */
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 4);

    public enum Propellant implements StringRepresentable {
        EMPTY, SOLID, LIQUID;

        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    public RocketTankBlock(Properties properties) {
        super(properties, RocketComposition.Section.TANK);
        registerDefaultState(defaultBlockState().setValue(PROPELLANT, Propellant.EMPTY).setValue(FILL, 0));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(RocketTankBlock::new); }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PROPELLANT, FILL);
    }

    @Override public BlockState look(BlockState state, RocketSectionEntity part) {
        PropellantModel.Tank tank = part.propellant();
        int liquid = part.fuel().getFluidAmount() + part.oxidizer().getFluidAmount();
        Propellant propellant = tank.cartridge() ? Propellant.SOLID : liquid > 0 ? Propellant.LIQUID : Propellant.EMPTY;
        int fill = propellant == Propellant.LIQUID ? quarters(liquid, 2 * PropellantModel.TANK_CAPACITY) : 0;
        return state.setValue(PROPELLANT, propellant).setValue(FILL, fill);
    }

    /** {@code amount} of {@code capacity} in quarters, rounded up: 0 only when empty. */
    static int quarters(int amount, int capacity) {
        return amount <= 0 ? 0 : Math.min(4, (int) Math.ceil(4.0 * amount / capacity));
    }
}
