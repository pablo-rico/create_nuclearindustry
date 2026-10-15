package org.papiricoh.create_nuclearindustry.antimatter.trap;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;

/** A Penning trap on a Display Link: what it holds out of its capacity, and its state. */
public class AntimatterTrapDisplaySource extends SingleLineDisplaySource {
    @Override
    protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
        PenningTrapBlockEntity trap = trapOf(context);
        if (trap == null) {
            return EMPTY_LINE;
        }
        return Component.translatable("create_nuclearindustry.display_source.antimatter_trap.line",
                String.format(Locale.ROOT, "%.2f", trap.stored()), String.format(Locale.ROOT, "%.0f", trap.capacity()),
                Component.translatable(PenningTrapBlockEntity.stateKey(trap.state())));
    }

    /** The trap a link on any of its blocks reads. */
    private static PenningTrapBlockEntity trapOf(DisplayLinkContext context) {
        if (context.getSourceBlockEntity() instanceof PenningTrapBlockEntity trap) {
            return trap;
        }
        BlockState state = context.level().getBlockState(context.getSourcePos());
        if (state.getBlock() instanceof PenningTrapBlock block) {
            return block.master(context.level(), state, context.getSourcePos()).orElse(null);
        }
        return null;
    }

    @Override
    protected boolean allowsLabeling(DisplayLinkContext context) {
        return true;
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 20;
    }
}
