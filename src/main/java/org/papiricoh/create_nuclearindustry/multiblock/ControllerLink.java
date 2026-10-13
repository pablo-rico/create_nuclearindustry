package org.papiricoh.create_nuclearindustry.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * Links a multiblock part (port, sensor, injector...) to the controller that owns it.
 * The controller pushes the link when it forms; the part re-checks it periodically and
 * falls back to a registry lookup, so a stale or missing link heals by itself.
 *
 * <p>Ports ask for their controller on every fill and drain, several times a tick. The controller
 * found is kept between re-checks and handed out again while it is still in the level (not removed)
 * and still owns the part, instead of being looked up in the level each time.
 */
public final class ControllerLink<C extends MultiblockController> {
    private static final int RESCAN_INTERVAL = 20;

    private final BlockEntity part;
    private final Class<C> controllerType;
    private final BiPredicate<C, BlockPos> ownsPart;
    private Optional<BlockPos> linked = Optional.empty();
    private int rescanCounter;
    /** The controller at {@link #linked}, as last found; checked for removal and ownership before use. */
    private C controller;
    private int levelLookups;

    public ControllerLink(BlockEntity part, Class<C> controllerType, BiPredicate<C, BlockPos> ownsPart) {
        this.part = part;
        this.controllerType = controllerType;
        this.ownsPart = ownsPart;
    }

    /** Server tick. Returns true on the ticks the link was refreshed. */
    public boolean tick() {
        if (++rescanCounter < RESCAN_INTERVAL) {
            return false;
        }
        rescanCounter = 0;
        refresh();
        return true;
    }

    public Optional<C> refresh() {
        // A re-check looks the controller up in the level again, whatever is kept.
        controller = null;
        Optional<C> found = find();
        linked = found.map(MultiblockController::getBlockPos);
        controller = found.orElse(null);
        return found;
    }

    public Optional<C> find() {
        Level level = part.getLevel();
        if (level == null) {
            return Optional.empty();
        }
        Optional<C> cached = cached(level);
        if (cached.isPresent()) {
            return cached;
        }
        BlockPos partPos = part.getBlockPos();
        return MultiblockRegistry.affecting(level, partPos, controllerType).stream()
                .filter(controller -> ownsPart.test(controller, partPos))
                .min(Comparator.comparingLong(controller -> controller.getBlockPos().asLong()));
    }

    public Optional<BlockPos> linkedPos() {
        return linked;
    }

    /** Returns true if the link changed. */
    public boolean link(BlockPos controllerPos) {
        Optional<BlockPos> next = Optional.of(controllerPos.immutable());
        boolean changed = !next.equals(linked);
        linked = next;
        if (changed) {
            controller = null;
        }
        return changed;
    }

    /** Clears the link if it points at {@code controllerPos}. Returns true if it changed. */
    public boolean unlink(BlockPos controllerPos) {
        if (linked.map(controllerPos::equals).orElse(false)) {
            linked = Optional.empty();
            controller = null;
            return true;
        }
        return false;
    }

    /** Times the linked controller was looked up in the level, for tests. */
    public int levelLookups() {
        return levelLookups;
    }

    private Optional<C> cached(Level level) {
        if (linked.isEmpty()) {
            controller = null;
            return Optional.empty();
        }
        C kept = controller;
        if (kept != null && !kept.isRemoved() && kept.getLevel() == level && kept.getBlockPos().equals(linked.get())) {
            if (ownsPart.test(kept, part.getBlockPos())) {
                return Optional.of(kept);
            }
        } else {
            levelLookups++;
            BlockEntity blockEntity = level.getBlockEntity(linked.get());
            if (controllerType.isInstance(blockEntity)) {
                C found = controllerType.cast(blockEntity);
                if (ownsPart.test(found, part.getBlockPos())) {
                    controller = found;
                    return Optional.of(found);
                }
            }
        }
        linked = Optional.empty();
        controller = null;
        return Optional.empty();
    }
}
