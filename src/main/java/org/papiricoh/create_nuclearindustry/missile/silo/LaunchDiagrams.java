package org.papiricoh.create_nuclearindustry.missile.silo;

import net.minecraft.core.BlockPos;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveConfig;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonation;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.flight.Warhead;
import org.papiricoh.create_nuclearindustry.missile.physics.*;

import java.util.*;
import static org.papiricoh.create_nuclearindustry.missile.silo.LaunchDiagram.*;

/** Server snapshot for an open controller; expensive reach estimates live only as long as the rocket. */
public final class LaunchDiagrams {
    private record ReachKey(MissileDesign design, BallisticModel.Params params) {}
    private record CachedReach(ReachKey key, List<Double> ranges) {}
    private static final Map<AssembledRocket, CachedReach> REACH = new WeakHashMap<>();

    private LaunchDiagrams() {}

    public static LaunchDiagram build(SiloControllerEntity controller) {
        var level = controller.getLevel();
        if (level == null) return EMPTY;
        BlockPos base = controller.floor().above();
        List<RocketSectionEntity> parts = new ArrayList<>();
        List<RocketComposition.Section> kinds = new ArrayList<>();
        for (int i = 0; i < MAX_CELLS; i++) {
            BlockPos at = base.above(i);
            if (!level.isLoaded(at) || !(level.getBlockEntity(at) instanceof RocketSectionEntity part)) break;
            parts.add(part);
            kinds.add(part.section());
        }
        String error = controller.error();
        BlockPos errorPos = controller.errorPos();
        int errorCell = -1;
        byte where = WHERE_NONE;
        if (!error.isEmpty()) {
            int index = errorPos.getY() - base.getY();
            where = switch (error) {
                case "base", "wall" -> WHERE_BASE;
                case "interior" -> WHERE_CLEARANCE;
                case "exit" -> WHERE_EXIT;
                default -> WHERE_CONTROLLER;
            };
            if (where == WHERE_CONTROLLER && errorPos.getX() == base.getX() && errorPos.getZ() == base.getZ()
                    && index >= 0 && index < MAX_CELLS) {
                errorCell = index;
                where = WHERE_CELL;
            }
        }
        RocketComposition composition = null;
        try { composition = RocketComposition.parse(kinds); } catch (RocketComposition.Invalid ignored) {}
        List<Integer> starts = composition == null ? List.of() : composition.stageStarts();
        List<Cell> cells = new ArrayList<>();
        WarheadInfo warhead = WarheadInfo.NONE;
        int stage = -1;
        for (int i = 0; i < parts.size(); i++) {
            RocketSectionEntity part = parts.get(i);
            if (part.section() == RocketComposition.Section.ENGINE) stage++;
            int cellStage = part.section() == RocketComposition.Section.GUIDANCE || part.section() == RocketComposition.Section.WARHEAD
                    ? Cell.PAYLOAD : Math.min(stage, 2);
            byte flags = (byte) (i == errorCell ? Cell.ERROR : 0);
            int fill = 0;
            if (part.section() == RocketComposition.Section.TANK) {
                fill = (int) Math.round(part.propellant().fill() * 100);
                if (part.propellant().liquid()) flags |= Cell.LIQUID;
                if (fill > 0) flags |= Cell.LOADED;
            } else {
                for (int slot = 0; slot < part.contents().getSlots(); slot++) if (!part.contents().getStackInSlot(slot).isEmpty()) flags |= Cell.LOADED;
                if ((flags & Cell.LOADED) != 0) fill = 100;
            }
            cells.add(new Cell((byte) part.section().ordinal(), (byte) cellStage, (byte) fill, flags));
            if (part.section() == RocketComposition.Section.WARHEAD) warhead = warhead(part);
        }
        AssembledRocket rocket = controller.rocket();
        MissileDesign design = controller.currentDesign();
        List<StageInfo> stages = new ArrayList<>();
        if (rocket != null && design != null && composition != null) {
            List<StageStats> stats = StageStats.of(design, MissileConfig.gravity);
            List<Double> ranges = ranges(rocket, design);
            for (int s = 0; s < Math.min(starts.size(), stats.size()); s++) {
                int tanks = composition.tanksPerStage().get(s);
                double fill = 0;
                boolean liquid = false;
                for (int t = 0; t < tanks; t++) {
                    var tank = parts.get(starts.get(s) + 1 + t).propellant();
                    fill += tank.fill();
                    liquid |= tank.liquid();
                }
                StageStats stat = stats.get(s);
                stages.add(new StageInfo((byte) tanks, (byte) (liquid ? 1 : 0), (byte) Math.round(fill * 100 / tanks),
                        (float) stat.twr(), (float) stat.deltaV(), (float) (stat.burnTicks() / 20), ranges.get(s).floatValue()));
            }
        }
        boolean hasBase = true;
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            BlockPos at = controller.floor().offset(x, 0, z);
            hasBase &= level.isLoaded(at) && level.getBlockState(at).is(SiloRegistry.LAUNCH_BASES);
        }
        byte site = !hasBase ? SITE_NONE
                : !parts.isEmpty() && controller.shaftHeight() >= parts.size() ? SITE_SILO : SITE_PLATFORM;
        return new LaunchDiagram(List.copyOf(cells), List.copyOf(stages), warhead, errorCell, where, error,
                RocketAdvisor.advise(kinds), (float) controller.reach(), (float) controller.cep(), (byte) controller.guidance().ordinal(),
                site, controller.launchDelay());
    }

    private static List<Double> ranges(AssembledRocket rocket, MissileDesign design) {
        ReachKey key = new ReachKey(design, MissileConfig.flightParams());
        CachedReach cached = REACH.get(rocket);
        if (cached != null && cached.key.equals(key)) return cached.ranges;
        List<Double> ranges = new ArrayList<>();
        for (int k = 1; k <= design.stages().size(); k++) ranges.add(MissileFlight.maxRange(
                new MissileDesign(design.stages().subList(0, k), design.payloadMass(), design.cep()), key.params));
        List<Double> result = List.copyOf(ranges);
        REACH.put(rocket, new CachedReach(key, result));
        return result;
    }

    private static WarheadInfo warhead(RocketSectionEntity part) {
        Warhead warhead = part.warhead();
        float power = warhead.power();
        int radius = warhead.destructionRadius();
        int fallout = 0;
        switch (warhead.kind()) {
            case FISSION -> fallout = Math.max(NuclearDetonation.falloutRadius(radius), warhead.salting() > 0
                    ? org.papiricoh.create_nuclearindustry.explosive.DirtyWarheadStats.falloutRadius(warhead.salting()) : 0);
            case DIRTY -> fallout = warhead.threatRadius();
            case THERMONUCLEAR -> {
                var stats = warhead.thermo().stats(ExplosiveConfig.thermonuclearCaps());
                power = stats.power();
                fallout = stats.falloutRadius();
            }
            case CONVENTIONAL -> radius = warhead.threatRadius();
            default -> {}
        }
        return new WarheadInfo((byte) warhead.kind().ordinal(), power, radius, fallout, (float) part.payloadMass());
    }
}
