package org.papiricoh.create_nuclearindustry.controlroom.servo;

import com.mojang.logging.LogUtils;
import com.simibubi.create.content.contraptions.elevator.ElevatorPulleyBlockEntity;
import com.simibubi.create.content.contraptions.piston.LinearActuatorBlockEntity;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlockEntity;
import com.simibubi.create.content.contraptions.piston.PistonExtensionPoleBlock;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds what a servo moves: the Mechanical Pistons and Rope Pulleys turned by its output, through
 * shafts, gearboxes, cogs and anything else Create joins, nearest first, with how fast the nearest
 * one turns for each RPM the servo puts out (negative when the gearing reverses it). The search
 * follows the joins Create itself uses, read through its propagator, so it agrees with how the
 * rotation really spreads; it runs while the output is still, from the block next to the output.
 */
public final class ServoActuators {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** Kinetic blocks the search looks at before it gives up. */
    public static final int MAX_BLOCKS = 256;
    private static final Method MODIFIER = method(RotationPropagator.class, "getRotationSpeedModifier",
            KineticBlockEntity.class, KineticBlockEntity.class);
    private static final Method NEIGHBOURS = method(RotationPropagator.class, "getPotentialNeighbourLocations", KineticBlockEntity.class);
    private static final Method RANGE = method(LinearActuatorBlockEntity.class, "getExtensionRange");
    private static final Method PASSIVE = method(LinearActuatorBlockEntity.class, "isPassive");

    /** The actuators an output turns, nearest first, and the nearest one's speed per RPM of output. */
    public record Drive(List<LinearActuatorBlockEntity> actuators, float ratio) {
        public static final Drive NONE = new Drive(List.of(), 0);

        @Nullable
        public LinearActuatorBlockEntity primary() {
            return actuators.isEmpty() ? null : actuators.get(0);
        }
    }

    private record Node(KineticBlockEntity entity, float ratio) {}

    private ServoActuators() {}

    /**
     * The actuators turned through {@code output} of {@code servo}. The servo says how fast the block
     * against its output turns for each RPM it puts out, since that join goes through its own output,
     * which may be standing still.
     */
    public static Drive find(ServoGearboxBlockEntity servo, Direction output) {
        Level level = servo.getLevel();
        if (level == null || MODIFIER == null || NEIGHBOURS == null) {
            return Drive.NONE;
        }
        BlockPos start = servo.getBlockPos().relative(output);
        if (!level.isLoaded(start) || !(level.getBlockEntity(start) instanceof KineticBlockEntity first)) {
            return Drive.NONE;
        }
        float firstRatio = servo.ratioThroughOutput(first);
        if (firstRatio == 0) {
            return Drive.NONE;
        }
        Set<BlockPos> visited = new HashSet<>();
        visited.add(servo.getBlockPos());
        visited.add(start);
        ArrayDeque<Node> queue = new ArrayDeque<>();
        queue.add(new Node(first, firstRatio));
        List<LinearActuatorBlockEntity> actuators = new ArrayList<>();
        float primaryRatio = 0;
        while (!queue.isEmpty() && visited.size() <= MAX_BLOCKS) {
            Node node = queue.poll();
            if (isActuator(node.entity()) && node.entity() instanceof LinearActuatorBlockEntity actuator) {
                if (actuators.isEmpty()) {
                    primaryRatio = node.ratio();
                }
                actuators.add(actuator);
            }
            for (BlockPos next : neighbours(node.entity())) {
                if (!visited.add(next) || !level.isLoaded(next) || !(level.getBlockEntity(next) instanceof KineticBlockEntity neighbour)) {
                    continue;
                }
                float modifier = modifier(node.entity(), neighbour);
                if (modifier == 0) {
                    // Not joined that way; another path may still reach it.
                    visited.remove(next);
                    continue;
                }
                queue.add(new Node(neighbour, node.ratio() * modifier));
            }
        }
        return actuators.isEmpty() ? Drive.NONE : new Drive(List.copyOf(actuators), primaryRatio);
    }

    /** Create's own join factor from one kinetic block to the next: 0 when they do not turn each other. */
    public static float modifier(KineticBlockEntity from, KineticBlockEntity to) {
        if (MODIFIER == null) {
            return 0;
        }
        try {
            return (float) MODIFIER.invoke(null, from, to);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return 0;
        }
    }

    /** Pistons and rope pulleys, not elevator pulleys (which answer their own contacts) nor passive pulley mirrors. */
    public static boolean isActuator(KineticBlockEntity entity) {
        return entity instanceof LinearActuatorBlockEntity actuator && !(entity instanceof ElevatorPulleyBlockEntity)
                && !passive(actuator);
    }

    private static boolean passive(LinearActuatorBlockEntity actuator) {
        if (PASSIVE == null) {
            return false;
        }
        try {
            return (boolean) PASSIVE.invoke(actuator);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /** How far the actuator reaches: a piston's poles, a pulley's rope down to the ground or Create's rope limit. */
    public static int range(LinearActuatorBlockEntity actuator) {
        if (actuator instanceof MechanicalPistonBlockEntity && !actuator.running && actuator.getLevel() != null) {
            // Create only counts a piston's poles when it assembles; count them as it will.
            return pistonPoles(actuator.getLevel(), actuator.getBlockPos(), actuator.getBlockState());
        }
        if (RANGE == null) {
            return 0;
        }
        try {
            return Math.max(0, (int) RANGE.invoke(actuator));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return 0;
        }
    }

    /**
     * A standing piston's reach, counted as Create counts it when the piston assembles: the poles
     * behind it, and when it stands extended the poles and the head in front of it.
     */
    public static int pistonPoles(Level level, BlockPos pos, BlockState piston) {
        if (!MechanicalPistonBlock.isPiston(piston) || !piston.hasProperty(BlockStateProperties.FACING)) {
            return 0;
        }
        Direction facing = piston.getValue(BlockStateProperties.FACING);
        int limit = MechanicalPistonBlock.maxAllowedPistonPoles();
        int count = 0;
        if (piston.getValue(MechanicalPistonBlock.STATE) == MechanicalPistonBlock.PistonState.EXTENDED) {
            BlockPos cursor = pos.relative(facing);
            while (count <= limit) {
                BlockState next = level.getBlockState(cursor);
                boolean head = MechanicalPistonBlock.isPistonHead(next) && next.getValue(BlockStateProperties.FACING) == facing;
                if (!head && !PistonExtensionPoleBlock.PlacementHelper.get().matchesAxis(next, facing.getAxis())) {
                    break;
                }
                count++;
                if (head) {
                    break;
                }
                cursor = cursor.relative(facing);
            }
        }
        BlockPos cursor = pos.relative(facing.getOpposite());
        while (count <= limit && PistonExtensionPoleBlock.PlacementHelper.get().matchesAxis(level.getBlockState(cursor), facing.getAxis())) {
            count++;
            cursor = cursor.relative(facing.getOpposite());
        }
        return Math.min(count, limit);
    }

    /**
     * Which way the actuator's offset moves for a positive speed: +1 out, -1 in. A pulley pays out
     * rope with a positive speed; a piston turns that over for the directions Create counts backwards.
     */
    public static int extensionSign(LinearActuatorBlockEntity actuator) {
        if (actuator instanceof MechanicalPistonBlockEntity && actuator.getBlockState().hasProperty(BlockStateProperties.FACING)) {
            Direction facing = actuator.getBlockState().getValue(BlockStateProperties.FACING);
            int modifier = facing.getAxisDirection().getStep() * (facing.getAxis() == Direction.Axis.Z ? -1 : 1);
            return -modifier;
        }
        return 1;
    }

    @SuppressWarnings("unchecked")
    private static List<BlockPos> neighbours(KineticBlockEntity entity) {
        try {
            return (List<BlockPos>) NEIGHBOURS.invoke(null, entity);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return List.of();
        }
    }

    @Nullable
    private static Method method(Class<?> owner, String name, Class<?>... parameters) {
        try {
            Method method = owner.getDeclaredMethod(name, parameters);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("Servo gearbox: no {}.{} in this version of Create; servos will find no actuators ({})",
                    owner.getSimpleName(), name, e.toString());
            return null;
        }
    }
}
