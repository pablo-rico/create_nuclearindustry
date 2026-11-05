package org.papiricoh.create_nuclearindustry.explosive.conventional;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.NuclearFeature;
import org.papiricoh.create_nuclearindustry.creativetab.NuclearTabSection;
import org.papiricoh.create_nuclearindustry.creativetab.SectionedOutput;
import org.papiricoh.create_nuclearindustry.missile.cluster.BombletEntity;

/**
 * Non-nuclear warheads: the charges a warhead bay takes besides TNT (plastic explosive, thermobaric charges, cluster
 * bomblets), what each is worth ({@link #EXPLOSIVE_CHARGE}), the bomblets a cluster warhead frees, and the damage their blasts
 * and fragments do.
 */
public final class ConventionalRegistry implements NuclearFeature {
    public static final ConventionalRegistry INSTANCE = new ConventionalRegistry();

    /** A conventional blast's fireball and its shock close by. */
    public static final ResourceKey<DamageType> CONVENTIONAL_BLAST = damage("conventional_blast");
    /** Fragments of a warhead's casing. */
    public static final ResourceKey<DamageType> SHRAPNEL = damage("shrapnel");
    /** A thermobaric charge's burning cloud; a fire damage. */
    public static final ResourceKey<DamageType> THERMOBARIC_BURN = damage("thermobaric_burn");

    /** What each item of a warhead's charge is worth; synced, so a console's slots refuse what the bay would. */
    public static final DataMapType<Item, ExplosiveCharge> EXPLOSIVE_CHARGE = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "explosive_charge"), Registries.ITEM, ExplosiveCharge.CODEC)
            .synced(ExplosiveCharge.CODEC, false).build();

    /** A military plastic explosive: four times TNT for its weight, stable until a detonator fires it. */
    public static final DeferredHolder<Item, Item> PLASTIC_EXPLOSIVE = AllNuclearItems.ITEMS.register("plastic_explosive",
            () -> new Item(new Item.Properties().stacksTo(16)));
    /** A fuel-air charge: metal powder and fuel that a small burster disperses as a cloud and lights. */
    public static final DeferredHolder<Item, Item> THERMOBARIC_CHARGE = AllNuclearItems.ITEMS.register("thermobaric_charge",
            () -> new Item(new Item.Properties().stacksTo(16)));
    /** A small bomblet a cluster warhead carries by the dozen. */
    public static final DeferredHolder<Item, Item> CLUSTER_BOMBLET = AllNuclearItems.ITEMS.register("cluster_bomblet",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<EntityType<?>, EntityType<BombletEntity>> BOMBLET = AllNuclearEntities.ENTITY_TYPES.register("bomblet",
            () -> EntityType.Builder.<BombletEntity>of(BombletEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("bomblet"));

    private ConventionalRegistry() {}

    private static ResourceKey<DamageType> damage(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, name));
    }

    @Override
    public void register(IEventBus modEventBus) {
        modEventBus.addListener(RegisterDataMapTypesEvent.class, event -> event.register(EXPLOSIVE_CHARGE));
    }

    @Override
    public void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, SectionedOutput output) {
        output.section(NuclearTabSection.WEAPONS);
        output.accept(PLASTIC_EXPLOSIVE.get());
        output.accept(THERMOBARIC_CHARGE.get());
        output.accept(CLUSTER_BOMBLET.get());
    }
}
