package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.AllNuclearTags;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalRegistry;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ExplosiveCharge;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry;

/**
 * Lang, models, tags and data maps for non-nuclear warheads and every explosion's shared effects: plastic explosive,
 * thermobaric charges and cluster bomblets with what each is worth, the bomblet entity, the damage their blasts and fragments
 * do, flash blindness and deafness, the explosions' sound subtitles, and the warhead screens' airburst modes.
 */
final class ConventionalDataGen extends AreaDataGen {
    static final ConventionalDataGen INSTANCE = new ConventionalDataGen();

    private ConventionalDataGen() {}

    @Override
    void lang() {
        LangEntries.item("plastic_explosive", "Plastic Explosive", "Explosivo plástico");
        LangEntries.item("thermobaric_charge", "Thermobaric Charge", "Carga termobárica");
        LangEntries.item("cluster_bomblet", "Cluster Bomblet", "Submunición de racimo");
        LangEntries.add("entity.create_nuclearindustry.bomblet", "Bomblet", "Submunición");
        LangEntries.add("effect.create_nuclearindustry.flash_blinded", "Flash Blinded", "Deslumbrado");
        LangEntries.add("effect.create_nuclearindustry.deafened", "Deafened", "Ensordecido");
        death("conventional_blast", "was blown up by a warhead", "voló por los aires por una cabeza explosiva",
                "was blown up by %2$s's warhead", "voló por los aires por la cabeza explosiva de %2$s");
        death("shrapnel", "was cut down by shrapnel", "cayó por la metralla",
                "was cut down by %2$s's shrapnel", "cayó por la metralla de %2$s");
        death("thermobaric_burn", "was engulfed by a thermobaric fireball", "fue engullido por una bola de fuego termobárica",
                "was engulfed by %2$s's thermobaric fireball", "fue engullido por la bola de fuego termobárica de %2$s");

        String silo = "silo.create_nuclearindustry.";
        LangEntries.add(silo + "warhead.cluster", "Cluster", "Racimo");
        LangEntries.add(silo + "warhead.thermobaric", "Thermobaric", "Termobárica");
        LangEntries.add(silo + "error.charge_mixed_types", "One kind of conventional charge per warhead (%s, %s, %s)",
                "Un solo tipo de carga convencional por cabeza (%s, %s, %s)");
        LangEntries.add(silo + "advice.charge_mixed_types", "Keep TNT and plastic explosive, thermobaric charges or bomblets apart",
                "No mezcles TNT y explosivo plástico, cargas termobáricas ni submuniciones");
        LangEntries.add(silo + "screen.airburst.always", "Airburst: always", "Aérea: siempre");
        LangEntries.add(silo + "screen.airburst.never", "Ground burst only", "Solo en superficie");

        String sub = "subtitles.create_nuclearindustry.";
        LangEntries.add(sub + "nuke_crack", "Shock wave cracks", "Restalla la onda de choque");
        LangEntries.add(sub + "nuke_boom", "Nuclear blast booms", "Retumba la explosión nuclear");
        LangEntries.add(sub + "nuke_rumble", "Blast rumbles on", "Sigue retumbando la explosión");
        LangEntries.add(sub + "nuke_rumble_far", "Distant nuclear rumble", "Retumbo nuclear lejano");
        LangEntries.add(sub + "nuke_return_wind", "Wind rushes back", "El viento vuelve con fuerza");
        LangEntries.add(sub + "seismic_rumble", "Ground trembles", "Tiembla el suelo");
        LangEntries.add(sub + "cloud_lightning", "Lightning crackles in the cloud", "Crepitan rayos en la nube");
        LangEntries.add(sub + "explosion_small", "Explosion", "Explosión");
        LangEntries.add(sub + "explosion_medium", "Explosion", "Explosión");
        LangEntries.add(sub + "explosion_large", "Large explosion", "Gran explosión");
        LangEntries.add(sub + "explosion_distant", "Distant explosion", "Explosión lejana");
        LangEntries.add(sub + "debris_rain", "Debris falls", "Cae escombro");
        LangEntries.add(sub + "thermobaric_blast", "Thermobaric blast", "Explosión termobárica");
        LangEntries.add(sub + "thermobaric_distant", "Distant thermobaric blast", "Explosión termobárica lejana");
        LangEntries.add(sub + "cluster_open", "Cluster warhead opens", "Se abre la cabeza de racimo");
        LangEntries.add(sub + "cluster_crackle", "Bomblets detonate", "Detonan las submuniciones");
        LangEntries.add(sub + "tinnitus", "Ears ring", "Pitan los oídos");
    }

    private static void death(String id, String en, String es, String enPlayer, String esPlayer) {
        String key = "death.attack.create_nuclearindustry." + id;
        LangEntries.add(key, "%1$s " + en, "%1$s " + es);
        LangEntries.add(key + ".player", "%1$s " + enPlayer, "%1$s " + esPlayer);
    }

    @Override
    void itemModels(NuclearItemModelProvider provider) {
        // The thermobaric charge and the bomblet have hand-written models (the bomblet's is 3D).
        provider.basicItem(ConventionalRegistry.PLASTIC_EXPLOSIVE.get());
    }

    @Override
    void itemTags(NuclearItemTagsProvider provider, HolderLookup.Provider lookup) {
        provider.tagFor(SiloRegistry.CONVENTIONAL_EXPLOSIVES).add(ConventionalRegistry.PLASTIC_EXPLOSIVE.get(),
                ConventionalRegistry.THERMOBARIC_CHARGE.get(), ConventionalRegistry.CLUSTER_BOMBLET.get());
        provider.tagFor(AllNuclearTags.Items.FLASH_PROTECTION).add(AllNuclearItems.HAZMAT_HELMET.get());
    }

    @Override
    void dataMaps(NuclearDataMapProvider provider) {
        var charges = provider.builderFor(ConventionalRegistry.EXPLOSIVE_CHARGE);
        charges.add(holder(Items.TNT), ExplosiveCharge.TNT, false);
        charges.add(holder(ConventionalRegistry.PLASTIC_EXPLOSIVE.get()), new ExplosiveCharge(4, ExplosiveCharge.Type.HIGH_EXPLOSIVE), false);
        charges.add(holder(ConventionalRegistry.THERMOBARIC_CHARGE.get()), new ExplosiveCharge(4, ExplosiveCharge.Type.THERMOBARIC), false);
        charges.add(holder(ConventionalRegistry.CLUSTER_BOMBLET.get()), new ExplosiveCharge(1.5f, ExplosiveCharge.Type.BOMBLET), false);
    }

    @SuppressWarnings("deprecation")
    private static net.minecraft.core.Holder<Item> holder(Item item) {
        return item.builtInRegistryHolder();
    }
}
