package org.papiricoh.create_nuclearindustry.maintenance.incident;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.papiricoh.create_nuclearindustry.maintenance.Earthquakes;
import org.papiricoh.create_nuclearindustry.maintenance.MaintenanceKitItem;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockController;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.accident.PlantAlarm;
import org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen.SteamGeneratorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.AbstractTurbineControllerBlockEntity;

import java.util.List;
import java.util.Locale;

/**
 * {@code /nuclear incident}: {@code list [count]} reads the log; {@code drill [pos]} sounds the
 * plant alarm with no harm done and logs a drill; {@code trigger <kind> <pos>} sets off a fault in
 * the machine at {@code pos} (a stuck rod, a tube rupture, a turbine trip) or an earthquake there.
 */
public final class IncidentCommands {
    private static final String PREFIX = "commands.create_nuclearindustry.incident.";

    private IncidentCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> incident() {
        var list = Commands.literal("list").executes(ctx -> list(ctx.getSource(), 10))
                .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                        .executes(ctx -> list(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count"))));
        var drill = Commands.literal("drill").executes(ctx -> drill(ctx.getSource(), BlockPos.containing(ctx.getSource().getPosition())))
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(ctx -> drill(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "pos"))));
        var trigger = Commands.literal("trigger")
                .then(Commands.literal("earthquake").then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("magnitude", FloatArgumentType.floatArg(1, 9))
                                .executes(ctx -> earthquake(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "pos"),
                                        FloatArgumentType.getFloat(ctx, "magnitude"))))))
                .then(fault("stuck_rod"))
                .then(fault("sg_tube_rupture"))
                .then(fault("turbine_trip"));
        return Commands.literal("incident").then(list).then(drill).then(trigger);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> fault(String kind) {
        return Commands.literal(kind).then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(ctx -> fault(ctx.getSource(), kind, BlockPosArgument.getLoadedBlockPos(ctx, "pos"))));
    }

    private static int list(CommandSourceStack source, int count) {
        List<Incident> incidents = IncidentLog.get(source.getServer()).incidents();
        if (incidents.isEmpty()) {
            source.sendSuccess(() -> Component.translatable(Incident.KEY + "none"), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(PREFIX + "list", Math.min(count, incidents.size()), incidents.size()), false);
        for (int i = 0; i < Math.min(count, incidents.size()); i++) {
            Incident incident = incidents.get(i);
            source.sendSuccess(incident::line, false);
        }
        return incidents.size();
    }

    private static int drill(CommandSourceStack source, BlockPos pos) {
        ServerLevel level = source.getLevel();
        PlantAlarm.raise(level, pos);
        IncidentLog.record(level, pos, IncidentType.DRILL);
        source.sendSuccess(() -> Component.translatable(PREFIX + "drill", pos.toShortString()), true);
        return 1;
    }

    private static int earthquake(CommandSourceStack source, BlockPos pos, float magnitude) {
        int scrammed = Earthquakes.strike(source.getLevel(), pos, magnitude);
        source.sendSuccess(() -> Component.translatable(PREFIX + "earthquake", String.format(Locale.ROOT, "%.1f", magnitude),
                pos.toShortString(), scrammed), true);
        return 1;
    }

    private static int fault(CommandSourceStack source, String kind, BlockPos pos) {
        MultiblockController machine = MaintenanceKitItem.machineAt(source.getLevel(), pos);
        boolean done = switch (kind) {
            case "stuck_rod" -> {
                if (machine instanceof ReactorBlockEntity reactor && reactor.isFormed()) {
                    reactor.forceStuckRod(source.getLevel().random.nextInt(8));
                    yield true;
                }
                yield false;
            }
            case "sg_tube_rupture" -> {
                if (machine instanceof SteamGeneratorControllerBlockEntity generator) {
                    generator.rupture();
                    yield true;
                }
                yield false;
            }
            case "turbine_trip" -> {
                if (machine instanceof AbstractTurbineControllerBlockEntity turbine) {
                    turbine.trip();
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
        if (!done) {
            source.sendFailure(Component.translatable(PREFIX + "no_machine", Component.translatable(PREFIX + "kind." + kind)));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(PREFIX + "triggered", Component.translatable(PREFIX + "kind." + kind),
                pos.toShortString()), true);
        return 1;
    }
}
