package org.papiricoh.create_nuclearindustry.missile.silo;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.Locale;

/**
 * A launch base block. Alone it is a concrete pad; a launch controller that finds a 3×3 of them sets each one's {@link #PART}, so
 * the nine draw one launch table between them (flame grate in the middle, hold-down clamps round it), and clears it again when
 * the base is given up or the controller goes.
 */
public class LaunchBaseBlock extends SiloRoleBlock {
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    /** Which ninth of a formed base a block is, row by row from the north-west corner; {@link #NONE} when not formed. */
    public enum Part implements StringRepresentable {
        NW, N, NE, W, C, E, SW, S, SE, NONE;

        /** The part {@link LaunchSiteGeometry#part} numbers {@code index}, or {@link #NONE}. */
        public static Part of(int index) {
            return index < 0 || index > 8 ? NONE : values()[index];
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public LaunchBaseBlock(Properties properties) {
        super(properties, "launch_base");
        registerDefaultState(defaultBlockState().setValue(PART, Part.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }
}
