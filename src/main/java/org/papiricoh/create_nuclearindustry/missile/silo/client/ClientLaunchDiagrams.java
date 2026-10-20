package org.papiricoh.create_nuclearindustry.missile.silo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import org.papiricoh.create_nuclearindustry.missile.silo.LaunchDiagram;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloDiagramPayload;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloMenu;

import java.util.HashMap;
import java.util.Map;

/** Menu-scoped snapshots cannot leak into another dimension or a later visit to the same controller. */
public final class ClientLaunchDiagrams {
    private static final Map<BlockPos, LaunchDiagram> diagrams = new HashMap<>();
    private static ClientLevel level;
    private static Object menu;

    private ClientLaunchDiagrams() {}

    private static void checkSession() {
        Minecraft mc = Minecraft.getInstance();
        Object current = mc.player == null ? null : mc.player.containerMenu;
        if (level != mc.level || menu != current) {
            diagrams.clear();
            level = mc.level;
            menu = current;
        }
    }

    public static void accept(SiloDiagramPayload payload) {
        checkSession();
        if (menu instanceof SiloMenu silo && silo.pos.equals(payload.pos()))
            diagrams.put(payload.pos().immutable(), payload.diagram());
    }

    public static LaunchDiagram at(BlockPos pos) {
        checkSession();
        return diagrams.getOrDefault(pos, LaunchDiagram.EMPTY);
    }
}
