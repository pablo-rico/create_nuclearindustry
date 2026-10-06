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
 */
public final class ControllerLink<C extends MultiblockController> {
    private static final int RESCAN_INTERVAL = 20;

    private final BlockEntity part;
    private final Class<C> controllerType;
    private final BiPredicate<C, BlockPos> ownsPart;
    private Optional<BlockPos> linked = Optional.empty();
    private int rescanCounter;

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
        Optional<C> controller = find();
        linked = controller.map(MultiblockController::getBlockPos);
        return controller;
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
        return changed;
    }

    /** Clears the link if it points at {@code controllerPos}. Returns true if it changed. */
    public boolean unlink(BlockPos controllerPos) {
        if (linked.map(controllerPos::equals).orElse(false)) {
            linked = Optional.empty();
            return true;
        }
        return false;
    }

    private Optional<C> cached(Level level) {
        if (linked.isEmpty()) {
            return Optional.empty();
        }
        BlockEntity blockEntity = level.getBlockEntity(linked.get());
        if (controllerType.isInstance(blockEntity)) {
            C controller = controllerType.cast(blockEntity);
            if (ownsPart.test(controller, part.getBlockPos())) {
                return Optional.of(controller);
            }
        }
        linked = Optional.empty();
        return Optional.empty();
    }
}
