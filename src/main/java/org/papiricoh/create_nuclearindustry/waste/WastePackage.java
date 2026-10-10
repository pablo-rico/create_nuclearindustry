package org.papiricoh.create_nuclearindustry.waste;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/**
 * A block that holds radioactive waste (the old barrel, the treated drums and canisters), from
 * the {@code waste_package} block data map. Placed, it leaks and irradiates less the better it
 * is packed and the better the repository around it.
 *
 * @param leakFactor     share of {@code contamination.barrelLeak} it adds to its chunk per random tick
 * @param emissionFactor share of its carried dose rate it gives off once placed
 * @param form           how the waste is held, which counts towards the repository score
 */
public record WastePackage(float leakFactor, float emissionFactor, WasteForm form) {
    private static final Codec<WasteForm> FORM_CODEC = Codec.STRING.comapFlatMap(id -> {
        WasteForm form = WasteForm.byId(id);
        return form != null ? DataResult.success(form) : DataResult.error(() -> "Unknown waste form " + id);
    }, WasteForm::id);

    public static final Codec<WastePackage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("leak_factor").forGetter(WastePackage::leakFactor),
            Codec.FLOAT.fieldOf("emission_factor").forGetter(WastePackage::emissionFactor),
            FORM_CODEC.fieldOf("form").forGetter(WastePackage::form)
    ).apply(instance, WastePackage::new));

    public static final DataMapType<Block, WastePackage> DATA_MAP = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "waste_package"), Registries.BLOCK, CODEC).build();

    /** The package data of {@code state}'s block, or {@code null} for anything that is not a waste package. */
    @Nullable
    public static WastePackage of(BlockState state) {
        return state.getBlockHolder().getData(DATA_MAP);
    }
}
