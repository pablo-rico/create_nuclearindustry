package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.papiricoh.create_nuclearindustry.dyson.DysonRegistry;
import org.papiricoh.create_nuclearindustry.dyson.ejector.EjectorStatus;
import org.papiricoh.create_nuclearindustry.dyson.receiver.ReceiverStatus;
import org.papiricoh.create_nuclearindustry.data.recipe.DysonRecipes;
import org.papiricoh.create_nuclearindustry.integration.jei.PlantJeiInfo;

import java.util.concurrent.CompletableFuture;

/**
 * Lang, blockstates, item models, tags and recipes of the Dyson swarm. The machines' block models and their partials are
 * hand-made (textures in {@code tex_dyson.py}); so are the ejector's and receiver's item models, which put their parts together.
 */
final class DysonDataGen extends AreaDataGen {
    static final DysonDataGen INSTANCE = new DysonDataGen();

    private DysonDataGen() {
    }

    @Override
    void lang() {
        LangEntries.block("em_rail_ejector", "EM Rail Ejector", "Eyector de raíl EM");
        LangEntries.block("ray_receiver", "Ray Receiver", "Receptor de rayos");
        LangEntries.block("swarm_terminal", "Dyson Swarm Terminal", "Terminal del enjambre Dyson");
        LangEntries.item("solar_sail", "Solar Sail", "Vela solar");
        LangEntries.item("incomplete_solar_sail", "Incomplete Solar Sail", "Vela solar incompleta");
        LangEntries.add("create_nuclearindustry.display_source.dyson_swarm", "Dyson Swarm", "Enjambre Dyson");
        LangEntries.add("create_nuclearindustry.display_source.ray_receiver", "Ray Receiver", "Receptor de rayos");

        String e = "create_nuclearindustry.ejector.";
        LangEntries.add(EjectorStatus.CHARGING.key(), "Charging the rails", "Cargando los raíles");
        LangEntries.add(EjectorStatus.WRONG_DIMENSION.key(), "No swarm under this sky: only in the Overworld",
                "No hay enjambre bajo este cielo: solo en el mundo normal");
        LangEntries.add(EjectorStatus.TOO_SLOW.key(), "Too slow: turn it at %s RPM or more", "Demasiado lento: gíralo a %s RPM o más");
        LangEntries.add(EjectorStatus.OVERSTRESSED.key(), "Overstressed", "Sobrecargado");
        LangEntries.add(EjectorStatus.NO_SKY.key(), "Blocked: it needs the open sky above it", "Tapado: necesita cielo abierto encima");
        LangEntries.add(EjectorStatus.SUN_DOWN.key(), "Waiting for the sun to rise", "Esperando a que salga el sol");
        LangEntries.add(EjectorStatus.ORBIT_CLOSED.key(), "Its orbit is closed: open it at a terminal",
                "Su órbita está cerrada: ábrela en un terminal");
        LangEntries.add(EjectorStatus.SWARM_FULL.key(), "The swarm is full", "El enjambre está lleno");
        LangEntries.add(EjectorStatus.NO_SAILS.key(), "No sails loaded", "Sin velas cargadas");
        LangEntries.add(e + "orbit", "Orbit %s", "Órbita %s");
        LangEntries.add(e + "orbit_button", "Orb. %s", "Órb. %s");
        LangEntries.add(e + "orbit_open", "Open", "Abierta");
        LangEntries.add(e + "orbit_closed", "Closed: open it at a terminal", "Cerrada: ábrela en un terminal");
        LangEntries.add(e + "loaded", "%s sails loaded", "%s velas cargadas");
        LangEntries.add(e + "idle", "Not turning", "Parado");
        LangEntries.add(e + "rate", "One every %s s", "Una cada %s s");
        LangEntries.add(e + "compact", "%s sails · one every %s s", "%s velas · una cada %s s");
        LangEntries.add(e + "compact_idle", "%s sails · not turning", "%s velas · parado");

        String r = "create_nuclearindustry.receiver.";
        LangEntries.add(ReceiverStatus.RECEIVING.key(), "Receiving", "Recibiendo");
        LangEntries.add(ReceiverStatus.WRONG_DIMENSION.key(), "No swarm under this sky: only in the Overworld",
                "No hay enjambre bajo este cielo: solo en el mundo normal");
        LangEntries.add(ReceiverStatus.NO_SKY.key(), "Blocked: it needs the open sky above it", "Tapado: necesita cielo abierto encima");
        LangEntries.add(ReceiverStatus.NO_SUN.key(), "No sunlight: night, or the sun too low", "Sin luz: de noche o con el sol muy bajo");
        LangEntries.add(ReceiverStatus.NO_SWARM.key(), "No sails in orbit", "No hay velas en órbita");
        LangEntries.add(ReceiverStatus.FULL.key(), "Full: nothing takes its energy", "Lleno: nada se lleva su energía");
        LangEntries.add(ReceiverStatus.WAITING.key(), "Waiting for a share of the swarm", "Esperando su parte del enjambre");
        LangEntries.add(r + "share", "Taking %s FE/t", "Recibe %s FE/t");
        LangEntries.add(r + "sharing", "%s receivers share the swarm", "%s receptores comparten el enjambre");
        LangEntries.add(r + "stored", "Stored: %s / %s FE", "Almacenado: %s / %s FE");

        String s = "create_nuclearindustry.swarm.";
        LangEntries.add(s + "sails", "Swarm: %s sails", "Enjambre: %s velas");
        LangEntries.add(s + "pool", "Beaming down %s FE/t", "Envía %s FE/t");
        LangEntries.add(s + "sun", "Sunlight: %s%%", "Luz solar: %s%%");
        LangEntries.add(s + "receivers", "%s receivers take %s FE/t", "%s receptores toman %s FE/t");
        LangEntries.add(s + "open", "Open", "Abierta");
        LangEntries.add(s + "closed", "Closed", "Cerrada");
        LangEntries.add(s + "radius", "Radius %s°", "Radio %s°");
        LangEntries.add(s + "inclination", "Inclination %s°", "Inclinación %s°");
        LangEntries.add(s + "node", "Node %s°", "Nodo %s°");
        LangEntries.add(s + "orbit_sails", "%s sails", "%s velas");
        LangEntries.add(s + "expiring", "%s wear out today", "%s se gastan hoy");
        LangEntries.add(s + "summary", "%s sails · %s%% sunlight · %s FE/t", "%s velas · %s%% de luz · %s FE/t");
        LangEntries.add(s + "beamed", "%s receivers take %s FE/t", "%s receptores toman %s FE/t");

        LangEntries.add(PlantJeiInfo.PREFIX + "dyson_swarm",
                "A Dyson swarm is solar sails orbiting the sun, beaming its light down to Ray Receivers that turn it into Forge Energy. Load Solar Sails into an EM Rail Ejector, drive it with a shaft from below at 64 RPM or more and leave the sky above it open: by day it charges its rails and throws a sail into its orbit every few seconds, faster the faster it turns. A Ray Receiver under the open sky takes power while the sun is up, most at noon, less in rain and none at night, and pushes it into the blocks around it. All the receivers share what the swarm sends: each sail gives 64 FE/t in full sun and a receiver takes up to 32,768 FE/t, so building more receivers than the swarm can feed makes nothing. Sails wear out after twenty days, so keep launching. A Dyson Swarm Terminal opens and shapes the four orbits, which change only how the swarm looks in the sky. The swarm is the whole world's and only shines on the Overworld.",
                "Un enjambre Dyson son velas solares orbitando el sol que envían su luz a Receptores de rayos, que la convierten en Forge Energy. Carga Velas solares en un Eyector de raíl EM, muévelo con un eje desde abajo a 64 RPM o más y deja el cielo abierto encima: de día carga sus raíles y lanza una vela a su órbita cada pocos segundos, más rápido cuanto más rápido gira. Un Receptor de rayos con cielo abierto recibe energía mientras hay sol, más a mediodía, menos con lluvia y nada de noche, y la empuja a los bloques de alrededor. Todos los receptores se reparten lo que envía el enjambre: cada vela da 64 FE/t a pleno sol y un receptor toma hasta 32.768 FE/t, así que poner más receptores de los que el enjambre puede alimentar no produce nada. Las velas se gastan a los veinte días: sigue lanzando. Un Terminal del enjambre Dyson abre y da forma a las cuatro órbitas, que solo cambian cómo se ve el enjambre en el cielo. El enjambre es de todo el mundo y solo brilla sobre el mundo normal.");
    }

    @Override
    void blockStates(NuclearBlockStateProvider provider) {
        provider.simpleBlock(DysonRegistry.EM_RAIL_EJECTOR.get(), provider.models().getExistingFile(provider.modLoc("block/em_rail_ejector")));
        provider.simpleBlock(DysonRegistry.RAY_RECEIVER.get(), provider.models().getExistingFile(provider.modLoc("block/ray_receiver")));
        ModelFile terminal = provider.models().getExistingFile(provider.modLoc("block/swarm_terminal"));
        provider.horizontalBlock(DysonRegistry.SWARM_TERMINAL.get(), terminal);
        provider.simpleBlockItem(DysonRegistry.SWARM_TERMINAL.get(), terminal);
    }

    @Override
    void itemModels(NuclearItemModelProvider provider) {
        provider.basicItem(DysonRegistry.SOLAR_SAIL.get());
        provider.basicItem(DysonRegistry.INCOMPLETE_SOLAR_SAIL.get());
    }

    @Override
    void blockTags(NuclearBlockTagsProvider provider, HolderLookup.Provider lookup) {
        provider.tagFor(BlockTags.MINEABLE_WITH_PICKAXE).add(DysonRegistry.EM_RAIL_EJECTOR.get(), DysonRegistry.RAY_RECEIVER.get(),
                DysonRegistry.SWARM_TERMINAL.get());
    }

    @Override
    void addProviders(GatherDataEvent event, DataGenerator generator, PackOutput output,
                      CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        generator.addProvider(event.includeServer(), new Named("Dyson swarm recipes", new DysonRecipes(output, lookup)));
        generator.addProvider(event.includeServer(), new Named("Solar sail assembly", DysonRecipes.assembly(output, lookup)));
    }

    private record Named(String name, DataProvider provider) implements DataProvider {
        @Override
        public CompletableFuture<?> run(CachedOutput output) {
            return provider.run(output);
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
