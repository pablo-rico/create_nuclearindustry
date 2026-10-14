package org.papiricoh.create_nuclearindustry.controlroom.desk;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TokamakScales;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.multiblock.ReactorStructureValidator;
import org.papiricoh.create_nuclearindustry.reactor.physics.ReactorPhysicsSimulator;
import org.papiricoh.create_nuclearindustry.reactor.visual.RodVisualSync;
import org.papiricoh.create_nuclearindustry.reactor.visual.RodVisuals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Server side: the bytes the screen modules sync for their picture, read from what the reactor and the
 * tokamak already simulate. A core map reads the reactor itself (its columns are not in the console's
 * sample); a plasma section reads the sample.
 */
final class ModuleDisplays {
    private static final byte[] NONE = new byte[0];

    private ModuleDisplays() {}

    /**
     * The map of a formed reactor's interior (see {@link CoreMap}): control channels with how far their
     * rods are in, fuel columns with how burnt their assemblies are, moderator, reflector and heat
     * exchanger columns, and empty ones.
     */
    static byte[] coreMap(ReactorBlockEntity reactor) {
        Level level = reactor.getLevel();
        Optional<ReactorStructureValidator.ReactorStructure> found = reactor.getStructure();
        if (level == null || found.isEmpty() || !reactor.isFormed()) {
            return NONE;
        }
        ReactorStructureValidator.ReactorStructure structure = found.get();
        int side = Math.min(CoreMap.MAX_SIDE, structure.width - 2);
        int west = structure.controllerPos.getX() - structure.width / 2 + 1;
        int north = structure.controllerPos.getZ() - structure.width / 2 + 1;
        byte[] data = CoreMap.blank(side);
        for (int row = 0; row < side; row++) {
            for (int column = 0; column < side; column++) {
                BlockPos bottom = new BlockPos(west + column, structure.bottomY + 1, north + row);
                CoreMap.Kind kind = structure.moderatorColumns.contains(bottom) ? CoreMap.Kind.MODERATOR
                        : structure.reflectorColumns.contains(bottom) ? CoreMap.Kind.REFLECTOR
                        : level.getBlockState(bottom).isAir() ? CoreMap.Kind.EMPTY
                        : CoreMap.Kind.EXCHANGER;
                CoreMap.set(data, column, row, kind, Float.NaN);
            }
        }
        // The channels are in the structure's order, the order the rod scan split its insertion in.
        double[] insertions = reactor.getControlRodScan().channelInsertions();
        int index = 0;
        for (BlockPos channel : structure.controlChannels) {
            float inserted = index < insertions.length ? (float) insertions[index] : Float.NaN;
            index++;
            CoreMap.set(data, channel.getX() - west, channel.getZ() - north, CoreMap.Kind.CONTROL, inserted);
        }
        Map<Long, float[]> burns = columnBurns(structure, reactor.fuelAssemblies());
        for (BlockPos column : structure.fuelColumns) {
            float[] burn = burns.get(BlockPos.asLong(column.getX(), 0, column.getZ()));
            CoreMap.set(data, column.getX() - west, column.getZ() - north, CoreMap.Kind.FUEL,
                    burn == null || burn[1] == 0 ? Float.NaN : burn[0] / burn[1]);
        }
        return data;
    }

    /**
     * Each fuel column's summed burn and assembly count, keyed by its (x, 0, z): the assemblies fill the
     * channels in {@link RodVisuals#order}, as the fuel blocks show them.
     */
    private static Map<Long, float[]> columnBurns(ReactorStructureValidator.ReactorStructure structure,
                                                  List<ReactorPhysicsSimulator.AssemblyView> assemblies) {
        List<BlockPos> rods = RodVisuals.order(RodVisualSync.rodsOf(structure), BlockPos::getX, BlockPos::getY, BlockPos::getZ);
        Map<Long, float[]> burns = new HashMap<>();
        for (int i = 0; i < Math.min(rods.size(), assemblies.size()); i++) {
            BlockPos rod = rods.get(i);
            float[] sum = burns.computeIfAbsent(BlockPos.asLong(rod.getX(), 0, rod.getZ()), key -> new float[2]);
            sum[0] += (float) assemblies.get(i).burnFraction();
            sum[1]++;
        }
        return burns;
    }

    /** A tokamak's plasma as {@link PlasmaSection} packs it, from the console's sample. */
    static byte[] plasma(MonitorReading sample) {
        return PlasmaSection.encode(TokamakScales.phase(sample.value(MonitorReading.K_PHASE)),
                sample.value(MonitorReading.K_TEMPERATURE) / TokamakScales.MAX_TEMPERATURE,
                sample.value(MonitorReading.K_DENSITY) / TokamakScales.DENSITY_MAX,
                sample.value(MonitorReading.K_INSTABILITY) / TokamakScales.DISRUPTION_INSTABILITY,
                sample.value(MonitorReading.K_FIELD),
                sample.value(MonitorReading.K_CONFINEMENT),
                sample.flag(MonitorReading.K_TRIPPED));
    }
}
