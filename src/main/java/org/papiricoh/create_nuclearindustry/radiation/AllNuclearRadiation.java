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

    public static final ResourceKey<DamageType> RADIATION_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, id("radiation"));

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

    /** Durability multiplier for hazmat pieces, like leather's 5. */
    public static Item.Properties hazmatProperties(ArmorItem.Type type) {
        return new Item.Properties().durability(type.getDurability(8));
    }
}
