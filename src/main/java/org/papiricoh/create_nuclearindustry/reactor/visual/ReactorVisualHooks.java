package org.papiricoh.create_nuclearindustry.reactor.visual;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;

import java.util.function.Consumer;

/**
 * Client-side work the fission reactor and the plant's machines hand off each client tick (their
 * looping sounds), set by {@link org.papiricoh.create_nuclearindustry.reactor.visual.client.ReactorVisualsClient}
 * so the block entities never touch client classes.
 */
public final class ReactorVisualHooks {
    /** Runs every client tick for each loaded reactor controller. */
    public static Consumer<ReactorBlockEntity> reactorClientTick = reactor -> {};
    /** Runs every client tick for each loaded centrifuge, electrolyzer, vitrification melter and radioisotope generator. */
    public static Consumer<BlockEntity> machineClientTick = machine -> {};

    private ReactorVisualHooks() {}
}
