package org.papiricoh.create_nuclearindustry.radiation;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import org.papiricoh.create_nuclearindustry.radiation.contamination.ChunkContamination;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.EnumMap;
import java.util.List;

/** Registrations for the radiation system. */
public final class AllNuclearRadiation {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Create_NuclearIndustry.MODID);
    private static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Create_NuclearIndustry.MODID);
    private static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, Create_NuclearIndustry.MODID);

    /** Accumulated dose. Not copied on death, so dying clears it. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Float>> DOSE = ATTACHMENTS.register("radiation_dose",
            () -> AttachmentType.builder(() -> 0.0f).serialize(Codec.FLOAT).build());
    /** Dose rate measured on the last update, for the Geiger counter. Not saved. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Float>> RATE = ATTACHMENTS.register("radiation_rate",
            () -> AttachmentType.builder(() -> 0.0f).build());

    /** Every rad ever received. Never decays and survives death; only a command resets it. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Float>> TOTAL_DOSE = ATTACHMENTS.register("radiation_total_dose",
            () -> AttachmentType.builder(() -> 0.0f).serialize(Codec.FLOAT).copyOnDeath().build());
    /**
     * The dose HUD's reading, synced to its own player only, and only when the rounded values
     * change. Removed (one last sync) when there is nothing left to show.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<DoseReading>> DOSE_HUD = ATTACHMENTS.register("dose_hud",
            () -> AttachmentType.builder(() -> DoseReading.NONE)
                    .sync((holder, to) -> holder == to, DoseReading.STREAM_CODEC).build());

    /** Ground contamination of a chunk. Server only; clients get just the severity below. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ChunkContamination>> CHUNK_CONTAMINATION =
            ATTACHMENTS.register("chunk_contamination", () -> AttachmentType.builder(() -> ChunkContamination.EMPTY)
                    .serialize(ChunkContamination.CODEC).build());
    /**
     * Severity (1-7) of a contaminated chunk, synced to the clients that see it: one byte, sent
     * with the chunk and again only when the severity changes. Absent when clean.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Byte>> CONTAMINATION_SEVERITY =
            ATTACHMENTS.register("contamination_severity", () -> AttachmentType.builder(() -> (byte) 0)
                    .serialize(Codec.BYTE).sync(ByteBufCodecs.BYTE).build());

    public static final DataMapType<net.minecraft.world.item.Item, Radioactivity> RADIOACTIVITY = DataMapType.builder(
            id("radioactivity"), Registries.ITEM, Radioactivity.CODEC).synced(Radioactivity.CODEC, false).build();

    /** Fraction of radiation that gets through a block. */
    public static final DataMapType<net.minecraft.world.level.block.Block, RadiationTransmission> TRANSMISSION = DataMapType.builder(
            id("radiation_transmission"), Registries.BLOCK, RadiationTransmission.CODEC).build();

    /** Fraction of radiation a worn armour piece stops. Synced so the Geiger counter can show it. */
    public static final DataMapType<net.minecraft.world.item.Item, RadiationShielding> SHIELDING = DataMapType.builder(
            id("radiation_shielding"), Registries.ITEM, RadiationShielding.CODEC).synced(RadiationShielding.CODEC, false).build();

    public static final DeferredHolder<MobEffect, RadiationSicknessEffect> RADIATION_SICKNESS =
            EFFECTS.register("radiation_sickness", RadiationSicknessEffect::new);
    /** Potassium iodide: cuts the dose taken while it lasts. */
    public static final DeferredHolder<MobEffect, RadiationMedicineEffect> RADIOPROTECTION =
            EFFECTS.register("radioprotection", () -> new RadiationMedicineEffect(0x5B8FD0));
    /** Prussian blue: flushes dose already taken, every second while it lasts. */
    public static final DeferredHolder<MobEffect, RadiationMedicineEffect> RADIATION_PURGE =
            EFFECTS.register("radiation_purge", () -> new RadiationMedicineEffect(0x2E4FA8));

    public static final ResourceKey<DamageType> RADIATION_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, id("radiation"));
    /** Decay heat of plutonium carried without protection. */
    public static final ResourceKey<DamageType> PLUTONIUM_BURN = ResourceKey.create(Registries.DAMAGE_TYPE, id("plutonium_burn"));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> HAZMAT_MATERIAL = ARMOR_MATERIALS.register("hazmat", () -> {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, 1);
        defense.put(ArmorItem.Type.CHESTPLATE, 3);
        defense.put(ArmorItem.Type.LEGGINGS, 2);
        defense.put(ArmorItem.Type.BOOTS, 1);
        defense.put(ArmorItem.Type.BODY, 3);
        return new ArmorMaterial(defense, 10, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.YELLOW_WOOL),
                List.of(new ArmorMaterial.Layer(id("hazmat"))), 0.0f, 0.0f);
    });

    /** Heavy plate pressed from depleted uranium: diamond-like armour, netherite-like toughness. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> DEPLETED_URANIUM_MATERIAL = ARMOR_MATERIALS.register("depleted_uranium", () -> {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, 3);
        defense.put(ArmorItem.Type.CHESTPLATE, 8);
        defense.put(ArmorItem.Type.LEGGINGS, 6);
        defense.put(ArmorItem.Type.BOOTS, 3);
        defense.put(ArmorItem.Type.BODY, 11);
        return new ArmorMaterial(defense, 9, SoundEvents.ARMOR_EQUIP_NETHERITE,
                () -> org.papiricoh.create_nuclearindustry.enrichment.recipe.MinEnrichmentIngredient.tier(
                        org.papiricoh.create_nuclearindustry.AllNuclearItems.URANIUM.get(),
                        org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier.DEPLETED).toVanilla(),
                List.of(new ArmorMaterial.Layer(id("depleted_uranium"))), 2.0f, 0.1f);
    });

    private AllNuclearRadiation() {}

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
        EFFECTS.register(modEventBus);
        ARMOR_MATERIALS.register(modEventBus);
        modEventBus.addListener(AllNuclearRadiation::registerDataMaps);
    }

    private static void registerDataMaps(RegisterDataMapTypesEvent event) {
        event.register(RADIOACTIVITY);
        event.register(TRANSMISSION);
        event.register(SHIELDING);
    }

    public static Holder<ArmorMaterial> hazmat() {
        return HAZMAT_MATERIAL;
    }

    static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }

    /** Durability multiplier 30 for depleted uranium plate, between diamond's 33 and iron's 15. */
    public static Item.Properties depletedUraniumProperties(ArmorItem.Type type) {
        return new Item.Properties().durability(type.getDurability(30));
    }

    /** Durability multiplier for hazmat pieces, like leather's 5. */
    public static Item.Properties hazmatProperties(ArmorItem.Type type) {
        return new Item.Properties().durability(type.getDurability(8));
    }
}
