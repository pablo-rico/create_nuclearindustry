package org.papiricoh.create_nuclearindustry.spentfuel;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Spent fuel matching {@code base} whose cooling grade lies in {@code [min_grade, max_grade]}.
 * The base can itself be an enrichment ingredient, so a recipe can ask for "cooled LEU fuel".
 * Fuel without a history counts as Warm. Recipes for the same fuel give disjoint grade ranges, so
 * Create, which takes the first recipe that matches, always picks the one meant for the grade.
 */
public record SpentFuelGradeIngredient(Ingredient base, CoolingGrade minGrade, CoolingGrade maxGrade) implements ICustomIngredient {
    public static final MapCodec<SpentFuelGradeIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("base").forGetter(SpentFuelGradeIngredient::base),
            SpentFuelData.GRADE_CODEC.optionalFieldOf("min_grade", CoolingGrade.HOT).forGetter(SpentFuelGradeIngredient::minGrade),
            SpentFuelData.GRADE_CODEC.optionalFieldOf("max_grade", CoolingGrade.AGED).forGetter(SpentFuelGradeIngredient::maxGrade)
    ).apply(instance, SpentFuelGradeIngredient::new));

    public static SpentFuelGradeIngredient of(ICustomIngredient base, CoolingGrade minGrade, CoolingGrade maxGrade) {
        return new SpentFuelGradeIngredient(base.toVanilla(), minGrade, maxGrade);
    }

    public static SpentFuelGradeIngredient of(Ingredient base, CoolingGrade minGrade, CoolingGrade maxGrade) {
        return new SpentFuelGradeIngredient(base, minGrade, maxGrade);
    }

    public boolean acceptsGrade(CoolingGrade grade) {
        return !grade.isHotterThan(minGrade) && !maxGrade.isHotterThan(grade);
    }

    @Override
    public boolean test(ItemStack stack) {
        return base.test(stack) && acceptsGrade(SpentFuel.gradeOf(stack));
    }

    /** Each base item at every grade the range takes, for recipe viewers. */
    @Override
    public Stream<ItemStack> getItems() {
        return Arrays.stream(base.getItems()).flatMap(stack -> Arrays.stream(CoolingGrade.values())
                .filter(this::acceptsGrade)
                .map(grade -> {
                    ItemStack copy = stack.copy();
                    copy.set(SpentFuelRegistry.SPENT_FUEL.get(), SpentFuelData.undated(grade));
                    return copy;
                }));
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return SpentFuelRegistry.GRADE_INGREDIENT.get();
    }
}
