package org.papiricoh.create_nuclearindustry.reactor.display;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Display Link source for one {@link ReactorReadout}, on the controller, a sensor or a redstone port. */
public final class ReactorReadoutDisplaySource {
    private ReactorReadoutDisplaySource() {}

    public static SingleLineDisplaySource of(ReactorReadout readout) {
        return readout.numeric() ? new Numeric(readout) : new Text(readout);
    }

    private static MutableComponent line(ReactorReadout readout, DisplayLinkContext context) {
        return ReactorReadout.reactorOf(context.getSourceBlockEntity())
                .map(readout::read)
                .orElseGet(() -> ReactorText.display("no_reactor"));
    }

    private static final class Numeric extends NumericSingleLineDisplaySource {
        private final ReactorReadout readout;

        private Numeric(ReactorReadout readout) {
            this.readout = readout;
        }

        @Override
        protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
            return line(readout, context);
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

    private static final class Text extends SingleLineDisplaySource {
        private final ReactorReadout readout;

        private Text(ReactorReadout readout) {
            this.readout = readout;
        }

        @Override
        protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
            return line(readout, context);
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
}
