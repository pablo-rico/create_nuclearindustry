package org.papiricoh.create_nuclearindustry.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;

import java.util.Collection;
import java.util.function.Function;

/**
 * {@code /nuclear}: debug and admin tools for ground contamination and player dose. Needs
 * permission level 2 (cheats or operator).
 */
public final class NuclearCommands {
    private static final String PREFIX = "commands.create_nuclearindustry.";

    private NuclearCommands() {}

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("nuclear")
                .requires(source -> source.hasPermission(2))
                .then(contamination())
                .then(dose()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> contamination() {
        var query = Commands.literal("query")
                .executes(ctx -> query(ctx.getSource(), BlockPos.containing(ctx.getSource().getPosition())))
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(ctx -> query(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "pos"))));
        var add = Commands.literal("add").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                        .executes(ctx -> add(ctx, 0))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(0, 32))
                                .executes(ctx -> add(ctx, IntegerArgumentType.getInteger(ctx, "radius"))))));
        var set = Commands.literal("set").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                        .executes(NuclearCommands::set)));
        var clear = Commands.literal("clear")
                .then(Commands.literal("all").executes(ctx -> clearAll(ctx.getSource())))
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(ctx -> clear(ctx, 0))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(0, 32))
                                .executes(ctx -> clear(ctx, IntegerArgumentType.getInteger(ctx, "radius")))));
        var fraction = Commands.argument("fraction", FloatArgumentType.floatArg(0, 1))
                .executes(ctx -> expand(ctx, IntegerArgumentType.getInteger(ctx, "steps"), FloatArgumentType.getFloat(ctx, "fraction")));
        var steps = Commands.argument("steps", IntegerArgumentType.integer(1, 100))
                .executes(ctx -> expand(ctx, IntegerArgumentType.getInteger(ctx, "steps"), 0.25f))
                .then(fraction);
        var expand = Commands.literal("expand").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 32))
                        .executes(ctx -> expand(ctx, 1, 0.25f))
                        .then(steps)));
        var fallout = Commands.literal("fallout").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 1024))
                        .then(Commands.argument("intensity", FloatArgumentType.floatArg(0))
                                .executes(NuclearCommands::fallout))));
        return Commands.literal("contamination").then(query).then(add).then(set).then(clear).then(expand).then(fallout);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dose() {
        var get = Commands.literal("get").then(Commands.argument("targets", EntityArgument.players())
                .executes(ctx -> forPlayers(ctx, player -> Component.translatable(PREFIX + "dose.get",
                        player.getDisplayName(), format(player.getData(AllNuclearRadiation.DOSE)),
                        format(player.getData(AllNuclearRadiation.RATE))))));
        var set = Commands.literal("set").then(Commands.argument("targets", EntityArgument.players())
                .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                        .executes(ctx -> {
                            float amount = FloatArgumentType.getFloat(ctx, "amount");
                            return forPlayers(ctx, player -> {
                                player.setData(AllNuclearRadiation.DOSE, amount);
                                return Component.translatable(PREFIX + "dose.set", player.getDisplayName(), format(amount));
                            });
                        })));
        var reset = Commands.literal("reset").then(Commands.argument("targets", EntityArgument.players())
                .executes(ctx -> forPlayers(ctx, player -> {
                    player.setData(AllNuclearRadiation.DOSE, 0.0f);
                    return Component.translatable(PREFIX + "dose.reset", player.getDisplayName());
                })));
        return Commands.literal("dose").then(get).then(set).then(reset);
    }

    private static int query(CommandSourceStack source, BlockPos pos) {
        ServerLevel level = source.getLevel();
        ChunkPos chunk = new ChunkPos(pos);
        float value = Contamination.get(level, chunk);
        source.sendSuccess(() -> Component.translatable(PREFIX + "contamination.query",
                chunk.x, chunk.z, format(value), Contamination.severity(value), Contamination.MAX_SEVERITY), false);
        return Math.round(value * 100);
    }

    private static int add(CommandContext<CommandSourceStack> ctx, int radius) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        ChunkPos center = new ChunkPos(BlockPosArgument.getBlockPos(ctx, "pos"));
        float amount = FloatArgumentType.getFloat(ctx, "amount");
        int touched = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius) {
                    continue;
                }
                LevelChunk chunk = Contamination.loaded(level, center.x + dx, center.z + dz);
                if (chunk != null) {
                    Contamination.add(chunk, (float) (amount * (1.0 - distance / (radius + 1))));
                    touched++;
                }
            }
        }
        int result = touched;
        ctx.getSource().sendSuccess(() -> Component.translatable(PREFIX + "contamination.add", format(amount), result), true);
        return touched;
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        ChunkPos chunkPos = new ChunkPos(BlockPosArgument.getLoadedBlockPos(ctx, "pos"));
        float amount = FloatArgumentType.getFloat(ctx, "amount");
        LevelChunk chunk = Contamination.loaded(level, chunkPos.x, chunkPos.z);
        if (chunk == null) {
            return 0;
        }
        Contamination.set(chunk, amount);
        float value = Contamination.read(chunk);
        ctx.getSource().sendSuccess(() -> Component.translatable(PREFIX + "contamination.set", chunkPos.x, chunkPos.z, format(value)), true);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx, int radius) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        ChunkPos center = new ChunkPos(BlockPosArgument.getBlockPos(ctx, "pos"));
        int cleared = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                LevelChunk chunk = Contamination.loaded(level, center.x + dx, center.z + dz);
                if (chunk != null && Contamination.read(chunk) > 0) {
                    Contamination.set(chunk, 0);
                    cleared++;
                }
            }
        }
        int result = cleared;
        ctx.getSource().sendSuccess(() -> Component.translatable(PREFIX + "contamination.clear", result), true);
        return cleared;
    }

    private static int clearAll(CommandSourceStack source) {
        int cleared = Contamination.clearAll(source.getLevel());
        source.sendSuccess(() -> Component.translatable(PREFIX + "contamination.clear_all", cleared), true);
        return cleared;
    }

    private static int expand(CommandContext<CommandSourceStack> ctx, int steps, float fraction) throws CommandSyntaxException {
        ChunkPos center = new ChunkPos(BlockPosArgument.getBlockPos(ctx, "pos"));
        int radius = IntegerArgumentType.getInteger(ctx, "radius");
        int touched = Contamination.expand(ctx.getSource().getLevel(), center, radius, steps, fraction);
        ctx.getSource().sendSuccess(() -> Component.translatable(PREFIX + "contamination.expand", touched, steps), true);
        return touched;
    }

    private static int fallout(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        int radius = IntegerArgumentType.getInteger(ctx, "radius");
        float intensity = FloatArgumentType.getFloat(ctx, "intensity");
        Contamination.fallout(ctx.getSource().getLevel(), pos, radius, intensity);
        ctx.getSource().sendSuccess(() -> Component.translatable(PREFIX + "contamination.fallout", radius, format(intensity)), true);
        return 1;
    }

    private static int forPlayers(CommandContext<CommandSourceStack> ctx, Function<ServerPlayer, Component> action) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer player : players) {
            Component message = action.apply(player);
            ctx.getSource().sendSuccess(() -> message, false);
        }
        return players.size();
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
