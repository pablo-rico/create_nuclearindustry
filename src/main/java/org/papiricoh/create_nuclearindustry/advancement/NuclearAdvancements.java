package org.papiricoh.create_nuclearindustry.advancement;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/** Registers the event trigger and awards events to the players involved. */
public final class NuclearAdvancements {
    private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, Create_NuclearIndustry.MODID);
    public static final DeferredHolder<CriterionTrigger<?>, NuclearEventTrigger> TRIGGER =
            TRIGGERS.register("nuclear_event", NuclearEventTrigger::new);
    /** How close a player must be to a machine to share in what it does. */
    public static final double WITNESS_RADIUS = 24.0;

    private NuclearAdvancements() {}

    public static void register(IEventBus modEventBus) {
        TRIGGERS.register(modEventBus);
    }

    public static void award(@Nullable Player player, NuclearEvent event) {
        if (player instanceof ServerPlayer serverPlayer) {
            TRIGGER.get().trigger(serverPlayer, event);
        }
    }

    /** Every player within {@code radius} of {@code pos}. */
    public static void awardNearby(@Nullable Level level, BlockPos pos, double radius, NuclearEvent event) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        double radiusSqr = radius * radius;
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(pos.getCenter()) <= radiusSqr) {
                TRIGGER.get().trigger(player, event);
            }
        }
    }

    /** The player who last worked on a machine, wherever they are, and anyone standing near it. */
    public static void awardMachine(@Nullable Level level, BlockPos pos, @Nullable Player owner, NuclearEvent event) {
        award(owner, event);
        awardNearby(level, pos, WITNESS_RADIUS, event);
    }
}
