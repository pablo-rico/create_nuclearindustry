package org.papiricoh.create_nuclearindustry.reactor.visual.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.client.sound.MachineLoopSound;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.multiblock.ReactorStructureValidator;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.block.ReliefValveBlock;
import org.papiricoh.create_nuclearindustry.reactor.visual.ReactorSoundModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Client tick of every fission reactor controller (installed on
 * {@link org.papiricoh.create_nuclearindustry.reactor.visual.ReactorVisualHooks}): starts the
 * reactor's looping sounds when the synced readings call for them. Each sound then follows the
 * readings and fades out by itself (see {@link ReactorLoopSound}).
 */
public final class ReactorClientEffects {
    /** Sounds start only this close to the controller, in blocks. */
    private static final double EFFECT_RANGE = 48.0;

    private static final Map<ReactorBlockEntity, State> STATES = new WeakHashMap<>();

    private ReactorClientEffects() {}

    /** The sounds one controller is playing. */
    private static final class State {
        final Map<ReactorLoopSound.Kind, ReactorLoopSound> core = new HashMap<>();
        final Map<BlockPos, ReactorLoopSound> ports = new HashMap<>();
    }

    public static void tick(ReactorBlockEntity reactor) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = reactor.getLevel();
        // Ponder scenes run in their own client level; keep them quiet.
        if (level == null || level != minecraft.level || minecraft.player == null) {
            return;
        }
        if (reactor.getBlockPos().getCenter().distanceToSqr(minecraft.player.position()) > EFFECT_RANGE * EFFECT_RANGE) {
            return;
        }
        State state = STATES.computeIfAbsent(reactor, r -> new State());
        for (ReactorLoopSound.Kind kind : List.of(ReactorLoopSound.Kind.HUM, ReactorLoopSound.Kind.KLAXON, ReactorLoopSound.Kind.ALARM)) {
            if (wants(reactor, kind, null) && !playing(state.core.get(kind))) {
                ReactorLoopSound sound = new ReactorLoopSound(kind, reactor, null);
                state.core.put(kind, sound);
                minecraft.getSoundManager().play(sound);
            }
        }
        state.ports.values().removeIf(sound -> !playing(sound));
        for (Port port : ports(reactor)) {
            if (wants(reactor, port.kind(), port.pos()) && !playing(state.ports.get(port.pos()))) {
                ReactorLoopSound sound = new ReactorLoopSound(port.kind(), reactor, port.pos());
                state.ports.put(port.pos(), sound);
                minecraft.getSoundManager().play(sound);
            }
        }
    }

    /** A place on the reactor's shell that can sound: an output port or the relief valve. */
    private record Port(BlockPos pos, ReactorLoopSound.Kind kind) {}

    private static List<Port> ports(ReactorBlockEntity reactor) {
        List<Port> ports = new ArrayList<>();
        ReactorStructureValidator.ReactorStructure structure = reactor.getStructure().orElse(null);
        if (structure == null) {
            return ports;
        }
        boolean pwr = reactor.getPrimaryLoop().readings().pwr();
        for (BlockPos pos : structure.fluidPorts) {
            if (isOutputPort(reactor.getLevel(), pos)) {
                ports.add(new Port(pos, pwr ? ReactorLoopSound.Kind.BUBBLING : ReactorLoopSound.Kind.HISS));
            }
        }
        if (structure.pressurizer != null) {
            ports.add(new Port(structure.pressurizer.reliefValve(), ReactorLoopSound.Kind.RELIEF));
        }
        return ports;
    }

    private static boolean isOutputPort(@Nullable Level level, BlockPos pos) {
        if (level == null) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.is(AllNuclearBlocks.REACTOR_FLUID_PORT.get()) && state.getValue(ReactorFluidPortBlock.MODE) == ReactorFluidPortMode.OUTPUT;
    }

    /** Whether the reactor still calls for a sound of this kind (at this port, for port sounds). */
    static boolean wants(ReactorBlockEntity reactor, ReactorLoopSound.Kind kind, @Nullable BlockPos at) {
        ReactorBlockEntity.ReactorDisplaySnapshot snapshot = reactor.getDisplaySnapshot();
        boolean formed = snapshot.formed() && reactor.getStructure().isPresent();
        return switch (kind) {
            case HUM -> ReactorSoundModel.humWanted(formed, snapshot.powerFraction());
            case KLAXON -> formed && ReactorSoundModel.klaxonWanted(reactor.isScramActive(), snapshot.coreTemperature());
            case ALARM -> ReactorSoundModel.alarmWanted(formed, snapshot.coreTemperature());
            case HISS -> formed && at != null && isOutputPort(reactor.getLevel(), at) && !reactor.getPrimaryLoop().readings().pwr()
                    && ReactorSoundModel.flowWanted(steamFlowPerPort(reactor));
            case BUBBLING -> formed && at != null && isOutputPort(reactor.getLevel(), at) && reactor.getPrimaryLoop().readings().pwr()
                    && ReactorSoundModel.flowWanted(primaryFlowPerPort(reactor));
            case RELIEF -> formed && at != null && reliefOpen(reactor.getLevel(), at);
        };
    }

    private static boolean reliefOpen(@Nullable Level level, BlockPos pos) {
        if (level == null) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.is(PwrRegistry.RELIEF_VALVE.get()) && state.getValue(ReliefValveBlock.OPEN);
    }

    /** Steam boiling off the core through each output port, mB/t; nothing while the core is below boiling or the tank is empty. */
    static double steamFlowPerPort(ReactorBlockEntity reactor) {
        ReactorBlockEntity.ReactorDisplaySnapshot snapshot = reactor.getDisplaySnapshot();
        if (snapshot.coreTemperature() <= 100.0 || snapshot.steamAmount() <= 0 || !NuclearFluidHelper.isReactorOutput(reactor.getSteamFluid())) {
            return 0.0;
        }
        return snapshot.steamGenerationRate() / Math.max(1, snapshot.steamOutputPortCount());
    }

    /** Hot primary coolant pumped out through each output port, mB/t. */
    static double primaryFlowPerPort(ReactorBlockEntity reactor) {
        int ports = reactor.getDisplaySnapshot().steamOutputPortCount();
        return reactor.getPrimaryLoop().flowOut() / Math.max(1, ports);
    }

    static boolean playing(@Nullable MachineLoopSound<?> sound) {
        return sound != null && !sound.isStopped() && Minecraft.getInstance().getSoundManager().isActive(sound);
    }
}
