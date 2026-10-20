package org.papiricoh.create_nuclearindustry.missile.silo;

import java.util.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.missile.physics.GuidanceTier;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.physics.PropellantModel;

/** Reads the rocket standing on a silo floor: its column of sections, what each stage burns, its guidance and its warhead. */
public final class RocketValidator {
    public record Result(AssembledRocket rocket, String error, BlockPos position) {
        public boolean valid() { return rocket != null; }
    }
    public static Result validate(Level level, BlockPos base) {
        List<RocketSectionEntity> parts = new ArrayList<>();
        List<RocketComposition.Section> sections = new ArrayList<>();
        List<String> visuals = new ArrayList<>();
        List<UUID> identities = new ArrayList<>();
        RocketSectionEntity payload = null;
        GuidanceTier guidance = GuidanceTier.BASIC;
        for (int i = 0; i < 18; i++) {
            BlockPos pos = base.above(i);
            if (!level.isLoaded(pos)) return new Result(null, "unloaded", pos);
            if (!(level.getBlockEntity(pos) instanceof RocketSectionEntity part)) break;
            parts.add(part);
            sections.add(part.section());
            visuals.add(BuiltInRegistries.BLOCK.getKey(part.getBlockState().getBlock()).toString());
            identities.add(part.identity());
            if (part.section() == RocketComposition.Section.WARHEAD) payload = part;
            if (part.section() == RocketComposition.Section.GUIDANCE) guidance = part.guidanceTier();
        }
        RocketComposition composition;
        try {
            composition = RocketComposition.parse(sections);
        } catch (RocketComposition.Invalid e) { return new Result(null, e.reason, base.above(e.index)); }
        List<PropellantModel.Stage> stages = new ArrayList<>();
        List<Integer> starts = composition.stageStarts();
        for (int s = 0; s < starts.size(); s++) {
            int first = starts.get(s) + 1;
            List<PropellantModel.Tank> tanks = new ArrayList<>();
            for (int t = 0; t < composition.tanksPerStage().get(s); t++) tanks.add(parts.get(first + t).propellant());
            try {
                stages.add(PropellantModel.stage(tanks));
            } catch (PropellantModel.Invalid e) { return new Result(null, e.reason, base.above(first + e.tank)); }
        }
        String why = payload == null ? "warhead" : WarheadBay.validate(payload.contents());
        if (why != null) return new Result(null, why, payload == null ? base.above(sections.size() - 1) : payload.getBlockPos());
        UUID id = UUID.nameUUIDFromBytes(identities.toString().getBytes(StandardCharsets.UTF_8));
        MissileDesign design = new MissileDesign(stages.stream().map(PropellantModel.Stage::design).toList(), payload.payloadMass(), 0);
        return new Result(new AssembledRocket(id, base, composition, design, payload.warhead(), visuals, identities, guidance,
                stages.stream().map(PropellantModel.Stage::kind).toList()), "", base);
    }
}
