package org.papiricoh.create_nuclearindustry.ruins;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.NuclearFeature;

/**
 * Abandoned nuclear sites: an old power plant, a civil fallout shelter and a military bunker, laid out
 * by jigsaw from the templates {@code tools/structgen/worldgen_*.py} writes. The ruins are scenery and
 * loot: no machine in them forms or runs. Only the structure type is registered in code; the
 * structures, pools, processors and sets are datapack entries ({@code data/RuinsWorldgen}).
 */
public final class RuinsRegistry implements NuclearFeature {
    public static final RuinsRegistry INSTANCE = new RuinsRegistry();

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, Create_NuclearIndustry.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<NuclearRuinStructure>> NUCLEAR_RUIN =
            STRUCTURE_TYPES.register("nuclear_ruin", () -> () -> NuclearRuinStructure.CODEC);

    private RuinsRegistry() {}

    @Override
    public void register(IEventBus modEventBus) {
        STRUCTURE_TYPES.register(modEventBus);
    }
}
