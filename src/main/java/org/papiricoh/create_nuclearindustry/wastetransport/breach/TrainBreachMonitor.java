package org.papiricoh.create_nuclearindustry.wastetransport.breach;

import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.papiricoh.create_nuclearindustry.wastetransport.WasteTransportConfig;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * Watches Create's trains for derailments and crashes (a crash also derails): the moment a train
 * leaves the rails, every waste container it carries is breached and spills where its carriage is.
 * It polls the trains' state instead of hooking Create's code, twice a second; a train that is put
 * back on the rails is watched again.
 */
public final class TrainBreachMonitor {
    private static final int INTERVAL = 10;
    /** Trains already off the rails, so each derailment breaches once. */
    private static final Set<UUID> DERAILED = new HashSet<>();

    private TrainBreachMonitor() {}

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % INTERVAL != 0) {
            return;
        }
        if (!WasteTransportConfig.breachOnDerail || Create.RAILWAYS.trains.isEmpty()) {
            DERAILED.clear();
            return;
        }
        Set<UUID> present = new HashSet<>();
        for (Train train : Create.RAILWAYS.trains.values()) {
            present.add(train.id);
            if (!train.derailed) {
                DERAILED.remove(train.id);
            } else if (DERAILED.add(train.id)) {
                onDerailed(server, train);
            }
        }
        for (Iterator<UUID> iterator = DERAILED.iterator(); iterator.hasNext(); ) {
            if (!present.contains(iterator.next())) {
                iterator.remove();
            }
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        DERAILED.clear();
    }

    /** Breaches the cargo of every carriage of {@code train}, each spilling where its carriage is. */
    public static int onDerailed(MinecraftServer server, Train train) {
        int breached = 0;
        for (Carriage carriage : train.carriages) {
            if (carriage.storage == null) {
                continue;
            }
            for (ResourceKey<Level> dimension : carriage.getPresentDimensions()) {
                ServerLevel level = server.getLevel(dimension);
                Carriage.DimensionalCarriageEntity dimensional = carriage.getDimensional(dimension);
                Vec3 at = dimensional.positionAnchor;
                CarriageContraptionEntity entity = dimensional.entity.get();
                if (at == null && entity != null) {
                    at = entity.position();
                }
                if (level != null && at != null) {
                    breached += breachCargo(level, at, carriage.storage);
                    break;
                }
            }
        }
        return breached;
    }

    /**
     * Breaches every waste container in {@code storage} (a carriage's or a contraption's cargo) and
     * spills, once, the activity of those that were still sealed at {@code at}. Returns how many were
     * newly breached.
     */
    public static int breachCargo(ServerLevel level, Vec3 at, MountedStorageManager storage) {
        int breached = 0;
        double activity = 0;
        for (MountedWasteStorage waste : MountedWasteStorage.of(storage)) {
            double before = waste.activity();
            if (waste.markBreached()) {
                breached++;
                activity += before;
                waste.loseSpilledContents(WasteTransportConfig.spillFluidFraction);
            }
        }
        if (breached > 0) {
            BreachEffects.spill(level, BlockPos.containing(at), activity);
        }
        return breached;
    }
}
