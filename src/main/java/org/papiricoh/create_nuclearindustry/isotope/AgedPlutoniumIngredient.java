package org.papiricoh.create_nuclearindustry.isotope;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import org.papiricoh.create_nuclearindustry.isotope.physics.AmericiumIngrowth;
import org.papiricoh.create_nuclearindustry.isotope.physics.IsotopeDecay;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Plutonium old enough to have grown {@code isotopes.americium.minYield} of americium-241 (from the
 * decay of its plutonium-241). Plutonium starts ageing when it is first dated: the first time a
 * player carries it. Matched against the server's clock, so plutonium aged in a chest counts.
 */
public record AgedPlutoniumIngredient(Ingredient base) implements ICustomIngredient {
    public static final MapCodec<AgedPlutoniumIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("base").forGetter(AgedPlutoniumIngredient::base)
    ).apply(instance, AgedPlutoniumIngredient::new));

    @Override
    public boolean test(ItemStack stack) {
        if (!base.test(stack)) {
            return false;
        }
        long now = Isotopes.serverTime();
        double share = now >= 0 ? Isotopes.americiumShare(stack, now) : Isotopes.storedActivity(stack);
        return share + 1.0e-9 >= IsotopeConfig.americiumMinYield;
    }

    /** The base items, dated far enough back to qualify, for recipe viewers. */
    @Override
    public Stream<ItemStack> getItems() {
        double days = AmericiumIngrowth.daysFor(IsotopeConfig.americium(), IsotopeConfig.americiumMinYield);
        int percent = (int) Math.ceil(IsotopeConfig.americiumMinYield * 100);
        return Arrays.stream(base.getItems()).map(stack -> {
            ItemStack copy = stack.copy();
            long age = Double.isFinite(days) ? (long) Math.ceil(days * IsotopeDecay.TICKS_PER_DAY / IsotopeConfig.timeScale) : 0;
            copy.set(IsotopeRegistry.ISOTOPE.get(), new IsotopeData(-age, 0.0f, percent));
            return copy;
        });
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return IsotopeRegistry.AGED_PLUTONIUM.get();
    }
}
