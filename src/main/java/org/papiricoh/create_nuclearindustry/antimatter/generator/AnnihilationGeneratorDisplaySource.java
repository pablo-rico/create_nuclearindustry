package org.papiricoh.create_nuclearindustry.antimatter.generator;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

/** An annihilation generator on a Display Link: what it is doing, its output and its mode. */
public class AnnihilationGeneratorDisplaySource extends SingleLineDisplaySource {
    @Override
    protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof AnnihilationGeneratorBlockEntity generator)) {
            return EMPTY_LINE;
        }
        return Component.translatable("create_nuclearindustry.display_source.annihilation_generator.line",
                Component.translatable(generator.statusKey()),
                String.format(Locale.ROOT, "%.0f", generator.outputFraction() * 100),
                Component.translatable(GeneratorMode.langKey(generator.effectiveMode())),
                String.format(Locale.ROOT, "%.3f", generator.stored()));
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
