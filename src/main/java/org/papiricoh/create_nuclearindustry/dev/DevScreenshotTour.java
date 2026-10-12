package org.papiricoh.create_nuclearindustry.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.fusion.heating.PlasmaHeatingRegistry;

/** Temporary screenshot tour, not committed. */
public final class DevScreenshotTour {
    private static boolean opened;
    private static boolean savedPause;
    private static int tick = -1;
    private static int shot;

    private record View(Vec3 eye, Vec3 target, boolean gui) {}

    private static final View[] VIEWS = {
            new View(new Vec3(4.5, 202.4, 0.6), new Vec3(4.5, 201.5, 4.2), false),
            new View(new Vec3(0.8, 202.6, 2.0), new Vec3(2.5, 201.5, 4.0), false),
            new View(new Vec3(4.5, 202.4, 8.6), new Vec3(4.5, 201.5, 4.8), false),
            new View(new Vec3(-0.5, 201.6, 4.5), new Vec3(2.5, 201.5, 4.5), false),
            new View(new Vec3(-550.6, 83.6, -34.8), new Vec3(-547.0, 82.5, -36.5), false),
            new View(new Vec3(80.6, 81.6, 25.6), new Vec3(83.5, 80.5, 28.0), false),
            new View(new Vec3(4.5, 202.4, 0.6), new Vec3(4.5, 201.5, 4.2), true),
    };
    private static final int START = 80;
    private static final int PER_VIEW = 90;

    private DevScreenshotTour() {}

    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!opened && mc.screen instanceof TitleScreen) {
            opened = true;
            savedPause = mc.options.pauseOnLostFocus;
            mc.options.pauseOnLostFocus = false;
            mc.createWorldOpenFlows().openWorld("nuclear_tour", () -> {});
            return;
        }
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.player == null || server == null) {
            return;
        }
        tick++;
        if (tick == 1) {
            for (String c : new String[]{"gamemode creative @a", "time set 6000", "weather clear",
                    "gamerule doDaylightCycle false", "tp @a 4.5 202 0.5",
                    "fill -2 199 -2 10 199 10 smooth_stone", "fill -2 200 -2 10 205 10 air",
                    "clear @a", "give @a create_nuclearindustry:turbine_output",
                    "give @a create_nuclearindustry:fusion_plasma_turbine",
                    "give @a create_nuclearindustry:fusion_neutral_beam_injector"}) {
                command(server, c);
            }
        }
        if (tick == 40) {
            server.execute(() -> {
                ServerLevel level = server.overworld();
                place(level, new BlockPos(2, 200, 4), AllNuclearBlocks.TURBINE_OUTPUT.get());
                place(level, new BlockPos(4, 200, 4), AllNuclearBlocks.FUSION_PLASMA_TURBINE.get());
                place(level, new BlockPos(6, 200, 4), PlasmaHeatingRegistry.NEUTRAL_BEAM_INJECTOR.get());
                place(level, new BlockPos(2, 201, 4), AllNuclearBlocks.TURBINE_OUTPUT.get());
                place(level, new BlockPos(4, 201, 4), AllNuclearBlocks.FUSION_PLASMA_TURBINE.get());
                place(level, new BlockPos(6, 201, 4), PlasmaHeatingRegistry.NEUTRAL_BEAM_INJECTOR.get());
            });
            command(server, "kill @e[type=item]");
        }
        if (tick < START) {
            return;
        }
        int index = (tick - START) / PER_VIEW;
        int step = (tick - START) % PER_VIEW;
        if (index >= VIEWS.length) {
            if (index == VIEWS.length && step == 0) {
                mc.options.pauseOnLostFocus = savedPause;
                mc.options.hideGui = false;
                mc.stop();
            }
            return;
        }
        View view = VIEWS[index];
        if (step == 0) {
            command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f", view.eye.x, view.eye.y - 1.62, view.eye.z));
            mc.options.hideGui = !view.gui;
        }
        mc.player.setDeltaMovement(Vec3.ZERO);
        mc.player.getAbilities().flying = true;
        mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, view.target);
        if (step == PER_VIEW - 1) {
            Screenshot.grab(mc.gameDirectory, "tour_" + (++shot) + ".png", mc.getMainRenderTarget(), m -> {});
        }
    }

    private static void place(ServerLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH), 3);
    }

    private static void command(MinecraftServer server, String command) {
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }
}
