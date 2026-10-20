package org.papiricoh.create_nuclearindustry.missile.silo;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.Locale;

/** A warhead section: its reentry vehicle shows what it is armed with; empty (or not a valid charge yet) it shows its bare bay. */
public class RocketWarheadBlock extends RocketSectionBlock {
    public static final EnumProperty<Charge> CHARGE = EnumProperty.create("charge", Charge.class);

    public enum Charge implements StringRepresentable {
        EMPTY, CONVENTIONAL, FISSION, DIRTY, THERMONUCLEAR;

        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    public RocketWarheadBlock(Properties properties) {
        super(properties, RocketComposition.Section.WARHEAD);
        registerDefaultState(defaultBlockState().setValue(CHARGE, Charge.EMPTY));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(RocketWarheadBlock::new); }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CHARGE);
    }

    @Override public BlockState look(BlockState state, RocketSectionEntity part) {
        Charge charge = switch (part.warhead().kind()) {
            case CONVENTIONAL -> Charge.CONVENTIONAL;
            case FISSION -> Charge.FISSION;
            case DIRTY -> Charge.DIRTY;
            case THERMONUCLEAR -> Charge.THERMONUCLEAR;
            case INERT -> Charge.EMPTY;
        };
        return state.setValue(CHARGE, charge);
    }
}
