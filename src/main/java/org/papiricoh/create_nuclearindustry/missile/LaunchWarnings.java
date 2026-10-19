package org.papiricoh.create_nuclearindustry.missile;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Tells players of a missile launch, as {@code missile.launchBroadcast} says. */
public final class LaunchWarnings {
    /** Players at least this close (blocks, horizontally) to where a missile comes down are always warned. */
    public static final int MIN_RADIUS = 128;

    private LaunchWarnings() {}

    /**
     * Announces a missile launched in {@code level} that comes down at {@code aim} in {@code flightTicks},
     * with a warhead that warns sirens {@code threatRadius} blocks around.
     */
    public static void announce(ServerLevel level, Vec3 aim, int threatRadius, int flightTicks) {
        LaunchBroadcast broadcast = MissileConfig.launchBroadcast;
        if (broadcast == LaunchBroadcast.NONE) {
            return;
        }
        int seconds = Mth.ceil(flightTicks / 20.0);
        int x = Mth.floor(aim.x);
        int z = Mth.floor(aim.z);
        if (broadcast == LaunchBroadcast.SERVER) {
            Component message = Component.translatable("message.create_nuclearindustry.missile_launch.server", x, z,
                    level.dimension().location().toString(), seconds).withStyle(ChatFormatting.RED);
            level.getServer().getPlayerList().getPlayers().forEach(player -> player.sendSystemMessage(message));
            return;
        }
        Component message = Component.translatable("message.create_nuclearindustry.missile_launch.area", x, z, seconds)
                .withStyle(ChatFormatting.RED);
        double radius = LaunchRules.warningRadius(threatRadius, MIN_RADIUS);
        for (ServerPlayer player : level.players()) {
            double dx = player.getX() - aim.x;
            double dz = player.getZ() - aim.z;
            if (dx * dx + dz * dz <= radius * radius) {
                player.sendSystemMessage(message);
            }
        }
    }
}
