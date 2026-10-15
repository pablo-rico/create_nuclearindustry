package org.papiricoh.create_nuclearindustry.controlroom.annunciator;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Display Link readout of a whole annunciator board, from any of its panels: how many windows wait
 * for an acknowledgement and whether the horn is silenced, the first-out, then every lit window, the
 * most urgent first. Every line is translated.
 */
public class AnnunciatorDisplaySource extends DisplaySource {
    public static final String KEY = "create_nuclearindustry.display_source.annunciator.";

    /** A lit window, for sorting. */
    private record Lit(AnnunciatorSeverity severity, AnnunciatorLogic.State state, Component label, boolean firstOut) {}

    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof AnnunciatorPanelBlockEntity panel)) {
            return List.of(Component.translatable(KEY + "none"));
        }
        return lines(panel);
    }

    /** The lines shown for the board {@code panel} is part of. */
    public static List<MutableComponent> lines(AnnunciatorPanelBlockEntity panel) {
        List<Lit> lit = new ArrayList<>();
        int waiting = 0;
        for (AnnunciatorPanelBlockEntity member : panel.boardMembers()) {
            for (int i = 0; i < AnnunciatorPanelBlockEntity.WINDOWS; i++) {
                AnnunciatorLogic.State state = member.logic().state(i);
                if (state == AnnunciatorLogic.State.NORMAL) {
                    continue;
                }
                if (state == AnnunciatorLogic.State.ALERT) {
                    waiting++;
                }
                lit.add(new Lit(member.window(i).severity(), state, label(member.window(i)), member.isFirstOut(i)));
            }
        }
        List<MutableComponent> lines = new ArrayList<>();
        if (lit.isEmpty()) {
            lines.add(Component.translatable(KEY + "normal"));
            return lines;
        }
        lines.add(Component.translatable(panel.silenced() ? KEY + "summary.silenced" : KEY + "summary", lit.size(), waiting));
        lit.stream().filter(Lit::firstOut).findFirst()
                .ifPresent(first -> lines.add(Component.translatable(KEY + "first_out", first.label())));
        lit.sort(Comparator.comparing(Lit::severity).thenComparing(Lit::state));
        for (Lit window : lit) {
            lines.add(Component.translatable(KEY + "window", Component.translatable(window.severity().translationKey()), window.label(),
                    Component.translatable("create_nuclearindustry.annunciator.state." + window.state().name().toLowerCase(Locale.ROOT))));
        }
        return lines;
    }

    private static Component label(AnnunciatorPanelBlockEntity.Window window) {
        return window.label().isEmpty()
                ? Component.translatable("create_nuclearindustry.annunciator.short." + window.metric().name().toLowerCase(Locale.ROOT))
                : Component.literal(window.label());
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 20;
    }
}
