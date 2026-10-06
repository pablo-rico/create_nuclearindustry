package org.papiricoh.create_nuclearindustry.multiblock;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

/**
 * Sends status messages to the player who last interacted with a multiblock, with a
 * cooldown so automatic messages (revalidation, scrams) don't spam chat.
 */
public final class MultiblockMessenger {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int AUTOMATIC_MESSAGE_COOLDOWN_TICKS = 100;

    private final String logPrefix;
    private Optional<UUID> ownerUUID = Optional.empty();
    @Nullable
    private Player ownerPlayer;
    private String lastAutomaticMessage = "";
    private long lastAutomaticMessageGameTime = Long.MIN_VALUE;

    public MultiblockMessenger(String logPrefix) {
        this.logPrefix = logPrefix;
    }

    public void setOwner(@Nullable Player player) {
        if (player != null) {
            ownerPlayer = player;
            ownerUUID = Optional.of(player.getUUID());
        }
    }

    /**
     * @param force send even if the same automatic message was sent recently
     *              (use for messages caused directly by a player action)
     */
    public void send(@Nullable Level level, String message, boolean force) {
        if (level == null) {
            return;
        }
        if (!force && !shouldSendAutomatic(level, message)) {
            return;
        }
        Player player = owner(level);
        if (player != null) {
            player.displayClientMessage(Component.literal(message), false);
        } else {
            LOGGER.debug("[{}] {}", logPrefix, message);
        }
    }

    @Nullable
    private Player owner(Level level) {
        if (ownerPlayer != null && ownerPlayer.isAlive()) {
            return ownerPlayer;
        }
        if (ownerUUID.isPresent()) {
            Player player = level.getPlayerByUUID(ownerUUID.get());
            if (player != null && player.isAlive()) {
                ownerPlayer = player;
                return player;
            }
        }
        return null;
    }

    private boolean shouldSendAutomatic(Level level, String message) {
        long gameTime = level.getGameTime();
        if (message.equals(lastAutomaticMessage)
                && gameTime - lastAutomaticMessageGameTime < AUTOMATIC_MESSAGE_COOLDOWN_TICKS) {
            return false;
        }
        lastAutomaticMessage = message;
        lastAutomaticMessageGameTime = gameTime;
        return true;
    }
}
