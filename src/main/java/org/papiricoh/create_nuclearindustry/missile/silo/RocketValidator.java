package org.papiricoh.create_nuclearindustry.missile.silo;

import java.util.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;

public final class RocketValidator {
    public record Result(AssembledRocket rocket, String error, BlockPos position) {
        public boolean valid() { return rocket != null; }
    }
    public static Result validate(Level level, BlockPos base) {
        List<RocketComposition.Section> sections = new ArrayList<>();
        List<String> visuals = new ArrayList<>();
        List<UUID> identities = new ArrayList<>();
        RocketSectionEntity payload = null;
        for (int i = 0; i < 18; i++) {
            BlockPos pos = base.above(i);
            if (!level.isLoaded(pos)) return new Result(null, "unloaded", pos);
            if (!(level.getBlockEntity(pos) instanceof RocketSectionEntity part)) break;
            sections.add(part.section());
            visuals.add(BuiltInRegistries.BLOCK.getKey(part.getBlockState().getBlock()).toString());
            identities.add(part.identity());
            if (!part.fueled()) return new Result(null, "fuel", pos);
            if (part.section() == RocketComposition.Section.WARHEAD) payload = part;
        }
        try {
            RocketComposition composition = RocketComposition.parse(sections);
            if (payload == null || !payload.validCharge()) return new Result(null, "charge", base.above(sections.size() - 1));
            UUID id = UUID.nameUUIDFromBytes(identities.toString().getBytes(StandardCharsets.UTF_8));
            return new Result(new AssembledRocket(id, base, composition, composition.design(payload.payloadMass()), payload.warhead(), visuals, identities), "", base);
        } catch (RocketComposition.Invalid e) { return new Result(null, e.reason, base.above(e.index)); }
    }
}
