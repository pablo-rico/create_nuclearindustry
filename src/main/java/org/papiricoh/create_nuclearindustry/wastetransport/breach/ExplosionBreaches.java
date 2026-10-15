package org.papiricoh.create_nuclearindustry.wastetransport.breach;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonationEvent;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.wastetransport.WasteTransportConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Explosions breach the waste containers they reach but do not blow apart (those spill as they
 * break): placed containers within the blast and the waste riding contraptions caught in it.
 */
public final class ExplosionBreaches {
    /** Blast radius assumed when an explosion does not say. */
    static final float DEFAULT_RADIUS = 4;
    /** Furthest a blast is searched for containers, so a huge one does not walk thousands of chunks. */
    static final double MAX_REACH = 48;

    private ExplosionBreaches() {}

    public static void onDetonate(ExplosionEvent.Detonate event) {
        if (!WasteTransportConfig.breachOnExplosion || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        float radius = event.getExplosion().radius();
        breachAround(level, event.getExplosion().center(), radius > 0 ? radius : DEFAULT_RADIUS, event.getAffectedBlocks());
    }

    public static void onNuclearDetonation(NuclearDetonationEvent event) {
        if (WasteTransportConfig.breachOnExplosion) {
            breachAround(event.getLevel(), event.getCenter(), event.getRadius(), List.of());
        }
    }

    /**
     * Breaches every container within {@code radius} of {@code center} except those at
     * {@code destroyed} (they break and spill anyway). Returns how many were newly breached.
     */
    public static int breachAround(ServerLevel level, Vec3 center, double radius, Collection<BlockPos> destroyed) {
        double reach = Math.min(radius, MAX_REACH);
        double reachSqr = reach * reach;
        Set<BlockPos> skip = destroyed.isEmpty() ? Set.of() : new HashSet<>(destroyed);
        Set<BlockPos> controllers = new HashSet<>();
        List<WasteContainer> found = new ArrayList<>();
        int minX = (int) Math.floor(center.x - reach) >> 4, maxX = (int) Math.floor(center.x + reach) >> 4;
        int minZ = (int) Math.floor(center.z - reach) >> 4, maxZ = (int) Math.floor(center.z + reach) >> 4;
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                LevelChunk chunk = Contamination.loaded(level, cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof WasteContainer container) || be.isRemoved() || skip.contains(be.getBlockPos())
                            || Vec3.atCenterOf(be.getBlockPos()).distanceToSqr(center) > reachSqr) {
                        continue;
                    }
                    BlockPos key = controllerOf(be);
                    if (controllers.add(key)) {
                        found.add(container);
                    }
                }
            }
        }
        int breached = 0;
        for (WasteContainer container : found) {
            if (container.breach(level, ((BlockEntity) container).getBlockPos())) {
                breached++;
            }
        }
        AABB box = new AABB(center, center).inflate(reach);
        for (AbstractContraptionEntity entity : level.getEntitiesOfClass(AbstractContraptionEntity.class, box)) {
            Contraption contraption = entity.getContraption();
            if (contraption != null && entity.position().distanceToSqr(center) <= reachSqr * 4) {
                breached += TrainBreachMonitor.breachCargo(level, entity.position(), contraption.getStorage());
            }
        }
        return breached;
    }

    private static BlockPos controllerOf(BlockEntity be) {
        if (be instanceof com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer multi && multi.getController() != null) {
            return multi.getController();
        }
        return be.getBlockPos();
    }
}
