package org.papiricoh.create_nuclearindustry.multiblock;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * What a multiblock tells players. Two kinds of message, so routine chatter never reaches the chat:
 * <ul>
 *   <li>{@link #status}: the answer to something a player just did (right-clicking the controller,
 *       pressing a button). It goes to that player's action bar, and nowhere when nobody acted:
 *       structures revalidating after a block change say nothing; goggles and screens show why
 *       they are not formed.</li>
 *   <li>{@link #alert}: a serious event (a SCRAM, a meltdown, a trip). It goes to the chat of the
 *       player who last worked on the multiblock and of everyone nearby, once per cooldown.</li>
 * </ul>
 * Everything else goes only to the debug log ({@link #log}).
 */
public final class MultiblockMessenger {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int ALERT_COOLDOWN_TICKS = 100;
    /** Players this close to the multiblock hear its alerts. */
    public static final double ALERT_RANGE = 64.0;

    /** How the last message went out; tests read it. */
    public enum Kind { NONE, STATUS, ALERT, LOG }

    private final String logPrefix;
    private Optional<UUID> ownerUUID = Optional.empty();
    @Nullable
    private Player ownerPlayer;
    private String lastAlert = "";
    private long lastAlertGameTime = Long.MIN_VALUE;
    @Nullable
    private Component lastMessage;
    private Kind lastKind = Kind.NONE;

    public MultiblockMessenger(String logPrefix) {
        this.logPrefix = logPrefix;
    }

    public void setOwner(@Nullable Player player) {
        if (player != null) {
            ownerPlayer = player;
            ownerUUID = Optional.of(player.getUUID());
        }
    }

    /** Tells {@code actor} the outcome of what they just did, on their action bar; nothing without an actor. */
    public void status(@Nullable Level level, @Nullable Player actor, Component message) {
        lastMessage = message;
        if (actor == null || level == null || level.isClientSide) {
            lastKind = Kind.LOG;
            LOGGER.debug("[{}] {}", logPrefix, message.getString());
            return;
        }
        lastKind = Kind.STATUS;
        actor.displayClientMessage(message, true);
    }

    /**
     * Raises a serious event in the chat of the owner and of every player within {@link #ALERT_RANGE}
     * of {@code origin}. The same alert (same lang key and arguments) is not repeated within a few
     * seconds.
     */
    public void alert(@Nullable Level level, BlockPos origin, Component message) {
        lastMessage = message;
        lastKind = Kind.ALERT;
        if (level == null || level.isClientSide) {
            return;
        }
        String identity = identity(message);
        long now = level.getGameTime();
        if (identity.equals(lastAlert) && now - lastAlertGameTime < ALERT_COOLDOWN_TICKS) {
            return;
        }
        lastAlert = identity;
        lastAlertGameTime = now;
        List<Player> recipients = new ArrayList<>();
        Player owner = owner(level);
        if (owner != null && owner.level() == level) {
            recipients.add(owner);
        }
        double range = ALERT_RANGE * ALERT_RANGE;
        for (Player player : level.players()) {
            if (!recipients.contains(player) && player.distanceToSqr(origin.getCenter()) <= range) {
                recipients.add(player);
            }
        }
        if (recipients.isEmpty()) {
            LOGGER.debug("[{}] {}", logPrefix, message.getString());
        }
        recipients.forEach(player -> player.displayClientMessage(message, false));
    }

    /** Routine chatter: the debug log only. */
    public void log(Component message) {
        lastMessage = message;
        lastKind = Kind.LOG;
        LOGGER.debug("[{}] {}", logPrefix, message.getString());
    }

    @Nullable
    public Component lastMessage() {
        return lastMessage;
    }

    public Kind lastKind() {
        return lastKind;
    }

    private static String identity(Component message) {
        if (message.getContents() instanceof TranslatableContents translatable) {
            return translatable.getKey() + Arrays.toString(translatable.getArgs()) + message.getSiblings();
        }
        return message.getString();
    }

    /** The player who last worked on the multiblock, if they are still around. */
    @Nullable
    public Player owner(Level level) {
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
}
