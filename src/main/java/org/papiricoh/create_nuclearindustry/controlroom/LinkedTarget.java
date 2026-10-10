package org.papiricoh.create_nuclearindustry.controlroom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.coolingtower.blockentity.CoolingTowerControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockController;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.display.ReactorReadout;
import org.papiricoh.create_nuclearindustry.turbine.AbstractTurbineControllerBlockEntity;

import java.util.Optional;

/**
 * Turns what a Control Room Linker points at into a target, and a stored target back into the live
 * machine. A click on any block of a formed reactor, turbine or cooling tower links its controller;
 * a click anywhere else watches the radiation there. Server side only.
 */
public final class LinkedTarget {
    public static final String NO_LINK = "none";
    public static final String OUT_OF_RANGE = "range";
    public static final String UNLOADED = "unloaded";
    public static final String OTHER_DIMENSION = "dimension";

    private LinkedTarget() {}

    /** The live end of a link. */
    public sealed interface Resolved permits Reactor, Turbine, Tower, Area, NoSignal {
        TargetKind kind();
    }

    public record Reactor(ReactorBlockEntity reactor) implements Resolved {
        public TargetKind kind() {
            return TargetKind.REACTOR;
        }
    }

    public record Turbine(AbstractTurbineControllerBlockEntity turbine) implements Resolved {
        public TargetKind kind() {
            return TargetKind.TURBINE;
        }
    }

    public record Tower(CoolingTowerControllerBlockEntity tower) implements Resolved {
        public TargetKind kind() {
            return TargetKind.COOLING_TOWER;
        }
    }

    public record Area(ServerLevel level, BlockPos pos) implements Resolved {
        public TargetKind kind() {
            return TargetKind.AREA;
        }
    }

    /** {@code reason} is one of {@link #NO_LINK}, {@link #OUT_OF_RANGE}, {@link #UNLOADED} or {@link #OTHER_DIMENSION}. */
    public record NoSignal(String reason) implements Resolved {
        public TargetKind kind() {
            return NO_LINK.equals(reason) ? TargetKind.NONE : TargetKind.NO_SIGNAL;
        }
    }

    /** The position a linker stores for a click on {@code clicked}. */
    public static BlockPos linkPosFor(Level level, BlockPos clicked) {
        BlockEntity clickedEntity = level.getBlockEntity(clicked);
        if (isMachineController(clickedEntity)) {
            return clicked;
        }
        if (clickedEntity != null) {
            Optional<ReactorBlockEntity> reactor = ReactorReadout.reactorOf(clickedEntity);
            if (reactor.isPresent()) {
                return reactor.get().getBlockPos();
            }
        }
        for (MultiblockController controller : MultiblockRegistry.affecting(level, clicked)) {
            if (isFormedMachine(controller)) {
                return controller.getBlockPos();
            }
        }
        return clicked;
    }

    /** What a link to {@code pos} watches, judged by the block entity there. */
    public static TargetKind kindAt(Level level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof ReactorBlockEntity) {
            return TargetKind.REACTOR;
        }
        if (entity instanceof AbstractTurbineControllerBlockEntity) {
            return TargetKind.TURBINE;
        }
        if (entity instanceof CoolingTowerControllerBlockEntity) {
            return TargetKind.COOLING_TOWER;
        }
        return TargetKind.AREA;
    }

    /** Whether a device at {@code from} can reach {@code target}, or why not. */
    @Nullable
    public static String reachProblem(Level level, BlockPos from, @Nullable GlobalPos target) {
        if (target == null) {
            return NO_LINK;
        }
        if (!level.dimension().equals(target.dimension())) {
            return OTHER_DIMENSION;
        }
        if (!from.closerThan(target.pos(), Config.controlRoomLinkRange)) {
            return OUT_OF_RANGE;
        }
        if (!level.isLoaded(target.pos())) {
            return UNLOADED;
        }
        return null;
    }

    /** The live target of a device at {@code from} linked to {@code target}. */
    public static Resolved resolve(Level level, BlockPos from, @Nullable GlobalPos target) {
        String problem = reachProblem(level, from, target);
        if (problem != null) {
            return new NoSignal(problem);
        }
        BlockEntity entity = level.getBlockEntity(target.pos());
        if (entity instanceof ReactorBlockEntity reactor) {
            return new Reactor(reactor);
        }
        if (entity instanceof AbstractTurbineControllerBlockEntity turbine) {
            return new Turbine(turbine);
        }
        if (entity instanceof CoolingTowerControllerBlockEntity tower) {
            return new Tower(tower);
        }
        if (level instanceof ServerLevel serverLevel) {
            return new Area(serverLevel, target.pos());
        }
        return new NoSignal(UNLOADED);
    }

    /** The reactor a device is linked to, if it is linked to one it can reach. */
    public static Optional<ReactorBlockEntity> reactorOf(BlockEntity device, @Nullable GlobalPos target) {
        if (device.getLevel() == null) {
            return Optional.empty();
        }
        return resolve(device.getLevel(), device.getBlockPos(), target) instanceof Reactor reactor
                ? Optional.of(reactor.reactor())
                : Optional.empty();
    }

    private static boolean isMachineController(@Nullable BlockEntity entity) {
        return entity instanceof ReactorBlockEntity || entity instanceof AbstractTurbineControllerBlockEntity
                || entity instanceof CoolingTowerControllerBlockEntity;
    }

    private static boolean isFormedMachine(MultiblockController controller) {
        if (controller instanceof ReactorBlockEntity reactor) {
            return reactor.isFormed();
        }
        if (controller instanceof AbstractTurbineControllerBlockEntity turbine) {
            return turbine.getStructure().isPresent();
        }
        if (controller instanceof CoolingTowerControllerBlockEntity tower) {
            return tower.getStructure().isPresent();
        }
        return false;
    }
}
