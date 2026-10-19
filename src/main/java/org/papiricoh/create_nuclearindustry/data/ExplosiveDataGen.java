package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.AllNuclearTags;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.data.recipe.WeaponsRecipes;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry;
import org.papiricoh.create_nuclearindustry.integration.jei.PlantJeiInfo;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;

import static org.papiricoh.create_nuclearindustry.data.LangEntries.add;
import static org.papiricoh.create_nuclearindustry.data.LangEntries.block;
import static org.papiricoh.create_nuclearindustry.data.LangEntries.item;

import java.util.concurrent.CompletableFuture;

/**
 * Lang, models, tags, data maps and recipes for nuclear weapons: the thermonuclear bomb and its cannon
 * shell, the parts warheads are assembled from, the bombs' and launch pad's screens, what a nuclear
 * detonation leaves (trinitite, charred logs), its flash's damage type, the target designator's airburst
 * and the cannon shells' loads.
 */
final class ExplosiveDataGen extends AreaDataGen {
    static final ExplosiveDataGen INSTANCE = new ExplosiveDataGen();

    private ExplosiveDataGen() {}

    @Override
    void lang() {
        RadarLang.add();
        SiloLang.add();
        block("rocket_engine", "Rocket Engine", "Motor de cohete");
        block("rocket_tank", "Rocket Propellant Tank", "Depósito de propelente");
        block("rocket_guidance", "Rocket Guidance Module", "Módulo de guiado");
        block("rocket_warhead", "Rocket Warhead", "Ojiva de cohete");
        block("silo_controller", "Silo Controller", "Controlador de silo");
        block("silo_wall", "Silo Wall", "Pared de silo");
        item("propellant_cartridge", "Propellant Cartridge", "Cartucho de propelente");
        block("trinitite", "Trinitite", "Trinitita");
        block("charred_log", "Charred Log", "Tronco carbonizado");
        add("death.attack.create_nuclearindustry.nuclear_flash", "%1$s was burnt by a nuclear flash",
                "%1$s ardió con el destello de una explosión nuclear");
        add("death.attack.create_nuclearindustry.nuclear_flash.player", "%1$s was burnt by a nuclear flash while fighting %2$s",
                "%1$s ardió con el destello de una explosión nuclear mientras luchaba contra %2$s");
        String designator = "item.create_nuclearindustry.target_designator.";
        add(designator + "ground_burst", "Ground burst: goes off on the target", "Explosión en superficie: estalla sobre el objetivo");
        add(designator + "airburst", "Airburst: goes off above the target", "Explosión aérea: estalla por encima del objetivo");
        add(designator + "mode_hint", "Sneak-use in the air to switch", "Úsalo agachado al aire para cambiar");
        add("item.create_nuclearindustry.nuclear_cannon_shell.cores", "Warhead: %s cores, reaching %s blocks",
                "Cabeza: %s núcleos, alcance de %s bloques");
        weaponsLang();
        add(PlantJeiInfo.PREFIX + "nuclear_aftermath",
                "What a nuclear crater leaves. Trinitite is the sand and rock of the crater's floor fused into green glass by the fireball, among basalt, obsidian and, at the very centre, magma; it is faintly radioactive. Charred Logs are what the flash leaves of trees within its reach.",
                "Lo que deja un cráter nuclear. La trinitita es la arena y la roca del fondo del cráter fundidas en vidrio verde por la bola de fuego, entre basalto, obsidiana y, en el mismo centro, magma; es ligeramente radiactiva. Los Troncos carbonizados son lo que deja el destello de los árboles a su alcance.");
    }

    private static void weaponsLang() {
        block("thermonuclear_bomb", "Thermonuclear Bomb", "Bomba termonuclear");
        block("thermonuclear_cannon_shell", "Thermonuclear Cannon Shell", "Proyectil termonuclear de cañón");
        item("thermonuclear_cannon_shell", "Thermonuclear Cannon Shell", "Proyectil termonuclear de cañón");
        add("item.create_nuclearindustry.thermonuclear_cannon_shell.load",
                "Warhead: %s lithium deuteride, %s tamper, reaching %s blocks",
                "Cabeza: %s de deuteruro de litio, %s de camisa, alcance de %s bloques");
        item("lithium_deuteride", "Lithium-6 Deuteride", "Deuteruro de litio-6");
        item("explosive_lens", "Explosive Lens", "Lente explosiva");
        item("neutron_initiator", "Neutron Initiator", "Iniciador de neutrones");
        item("firing_set", "Firing Set", "Conjunto de disparo");
        item("implosion_assembly", "Implosion Assembly", "Conjunto de implosión");
        item("bomb_casing", "Bomb Casing", "Carcasa de bomba");
        item("thermonuclear_casing", "Thermonuclear Casing", "Carcasa termonuclear");
        item("incomplete_explosive_lens", "Incomplete Explosive Lens", "Lente explosiva incompleta");
        item("incomplete_firing_set", "Incomplete Firing Set", "Conjunto de disparo incompleto");
        item("incomplete_implosion_assembly", "Incomplete Implosion Assembly", "Conjunto de implosión incompleto");
        item("incomplete_nuclear_bomb", "Incomplete Nuclear Bomb", "Bomba nuclear incompleta");
        item("incomplete_thermonuclear_bomb", "Incomplete Thermonuclear Bomb", "Bomba termonuclear incompleta");

        String gui = "gui.create_nuclearindustry.";
        add(gui + "bomb.armed", "Armed: %ss", "Armada: %s s");
        add(gui + "bomb.waiting", "Waiting for redstone", "Esperando redstone");
        add(gui + "nuclear_bomb.cores", "Cores: %s/%s", "Núcleos: %s/%s");
        add(gui + "nuclear_bomb.enrichment", "Avg enrichment: %s%%", "Enriquecimiento medio: %s%%");
        add(gui + "nuclear_bomb.power", "Power: %s", "Potencia: %s");
        add(gui + "nuclear_bomb.radius", "Radius/crater: %s/%s", "Radio/cráter: %s/%s");
        add(gui + "nuclear_bomb.hint", "Two cores per slot", "Dos núcleos por hueco");
        String thermo = gui + "thermonuclear_bomb.";
        add(thermo + "primary", "Primary: %s", "Primario: %s");
        add(thermo + "primary.uranium", "uranium core", "núcleo de U");
        add(thermo + "primary.plutonium", "plutonium core", "núcleo de Pu");
        add(thermo + "no_primary", "NO PRIMARY: no detonation", "SIN PRIMARIO: no detona");
        add(thermo + "secondary", "Secondary: %s/%s deuteride", "Secundario: %s/%s deuteruro");
        add(thermo + "tamper", "Tamper: %s/%s depleted U", "Camisa: %s/%s de uranio");
        add(thermo + "fizzle", "Fizzle: primary only", "Fizzle: solo el primario");
        add(thermo + "fallout", "Fallout: %s blocks", "Lluvia: %s bloques");
        add(thermo + "slot.primary", "Primary", "Primario");
        add(thermo + "slot.secondary", "Secondary", "Secundario");
        add(thermo + "slot.tamper", "Tamper", "Camisa");
        String pad = gui + "launch_pad.";
        add(pad + "missile_ready", "Missile: ready", "Misil: listo");
        add(pad + "missile_missing", "Missile: missing", "Misil: falta");
        add(pad + "dirty", "Dirty: %s rad/s", "Sucia: %s rad/s");
        add(pad + "cores", "Cores: %s", "Núcleos: %s");
        add(pad + "cores_salted", "Cores: %s, salted", "Núcleos: %s, salada");
        add(pad + "target", "Target: %s %s %s", "Objetivo: %s %s %s");
        add(pad + "distance", "Distance: %s m", "Distancia: %s m");
        add(pad + "no_target", "No target set", "Sin objetivo");
        add(pad + "launching", "Launching: %ss", "Lanzamiento: %s s");
        add(pad + "not_ready", "Not ready", "No está listo");
        add(pad + "refused", "Launch refused", "Lanzamiento denegado");
        add(pad + "refused.dimension", "No launches in this dimension", "No se lanza en esta dimensión");
        add(pad + "refused.out_of_range", "Target beyond maximum range", "Fuera del alcance máximo");
        add(pad + "refused.spawn_protected", "Target too close to spawn", "Objetivo muy cerca del spawn");
        add(pad + "refused.no_permission", "Its player lacks permission", "Su jugador no tiene permiso");
        add(pad + "refused.cooldown", "Its player launched recently", "Su jugador lanzó hace poco");
        add(pad + "refused.cooldown.seconds", "Its player launched recently: %s s left", "Su jugador lanzó hace poco: faltan %s s");
        add("message.create_nuclearindustry.launch_refused", "Launch pad at %s %s %s will not launch: %s",
                "La plataforma de %s %s %s no lanza: %s");
        add("message.create_nuclearindustry.missile_launch.area", "Incoming missile! It comes down near %s, %s in %s s",
                "¡Misil en camino! Cae cerca de %s, %s dentro de %s s");
        add("message.create_nuclearindustry.missile_launch.server", "Missile launched: it comes down near %s, %s (%s) in %s s",
                "Misil lanzado: cae cerca de %s, %s (%s) dentro de %s s");
    }

    @Override
    void blockStates(NuclearBlockStateProvider provider) {
        ModelFile thermonuclear = provider.models().withExistingParent("thermonuclear_bomb", provider.modLoc("block/nuclear_bomb"))
                .texture("body", provider.modLoc("block/thermonuclear_bomb_body"))
                .texture("top", provider.modLoc("block/thermonuclear_bomb_top"))
                .texture("panel", provider.modLoc("block/thermonuclear_bomb_panel"))
                .texture("particle", provider.modLoc("block/thermonuclear_bomb_body"));
        provider.getVariantBuilder(ExplosiveRegistry.THERMONUCLEAR_BOMB.get())
                .forAllStates(state -> ConfiguredModel.builder().modelFile(thermonuclear).build());
        provider.simpleBlockItem(ExplosiveRegistry.THERMONUCLEAR_BOMB.get(), thermonuclear);
        provider.simpleBlockWithItem(ExplosiveRegistry.TRINITITE.get(), provider.models().cubeAll("trinitite", provider.modLoc("block/trinitite")));
        provider.axisBlock(ExplosiveRegistry.CHARRED_LOG.get(), provider.modLoc("block/charred_log"), provider.modLoc("block/charred_log_top"));
        provider.simpleBlockItem(ExplosiveRegistry.CHARRED_LOG.get(), new ModelFile.UncheckedModelFile(provider.modLoc("block/charred_log")));
    }

    @Override
    void itemModels(NuclearItemModelProvider provider) {
        ExplosiveRegistry.items().stream()
                .filter(item -> item != ExplosiveRegistry.THERMONUCLEAR_BOMB_ITEM && item != ExplosiveRegistry.TRINITITE_ITEM
                        && item != ExplosiveRegistry.CHARRED_LOG_ITEM)
                .forEach(item -> provider.basicItem(item.get()));
        ExplosiveRegistry.incompleteItems().forEach(item -> provider.basicItem(item.get()));
    }

    @Override
    void itemTags(NuclearItemTagsProvider provider, HolderLookup.Provider lookup) {
        provider.tagFor(AllNuclearTags.Items.THERMONUCLEAR_TAMPERS).add(AllNuclearItems.DEPLETED_URANIUM_BLOCK.get());
        provider.tagFor(AllNuclearTags.Items.THERMONUCLEAR_COMPONENTS)
                .add(ExplosiveRegistry.LITHIUM_DEUTERIDE.get(), ExplosiveRegistry.THERMONUCLEAR_BOMB_ITEM.get())
                .addOptional(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "thermonuclear_cannon_shell"));
    }

    @Override
    void addProviders(GatherDataEvent event, DataGenerator generator, PackOutput output,
                      CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        generator.addProvider(event.includeServer(), new WeaponsRecipes(output, lookup));
    }

    @Override
    void blockTags(NuclearBlockTagsProvider provider, HolderLookup.Provider lookup) {
        provider.tagFor(BlockTags.MINEABLE_WITH_PICKAXE).add(ExplosiveRegistry.TRINITITE.get(),
                org.papiricoh.create_nuclearindustry.missile.radar.RadarRegistry.RADAR.get());
        for (var block : java.util.List.of(
                org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.ENGINE,
                org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.TANK,
                org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.GUIDANCE,
                org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.WARHEAD,
                org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.CONTROLLER,
                org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.WALL))
            provider.tagFor(BlockTags.MINEABLE_WITH_PICKAXE).add(block.get());
        provider.tagFor(BlockTags.MINEABLE_WITH_AXE).add(ExplosiveRegistry.CHARRED_LOG.get());
    }

    @Override
    void dataMaps(NuclearDataMapProvider provider) {
        // Fused with the fission products of the fireball: barely, but measurably, active.
        NuclearDataMapProvider.add(provider.builderFor(AllNuclearRadiation.RADIOACTIVITY), ExplosiveRegistry.TRINITITE_ITEM, 0.03f, false);
    }
}
