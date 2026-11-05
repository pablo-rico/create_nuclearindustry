package org.papiricoh.create_nuclearindustry.explosive.conventional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * What one item of a warhead's charge is worth, from the {@code create_nuclearindustry:explosive_charge} data map: how many TNT
 * blocks of blast it carries and what kind of charge it makes. Items in the {@code conventional_explosives} tag without an entry
 * count as one TNT block of high explosive.
 *
 * @param tntEquivalent blast of one item, in TNT blocks
 * @param type          what kind of warhead a bay of it makes
 */
public record ExplosiveCharge(float tntEquivalent, Type type) {
    public static final ExplosiveCharge TNT = new ExplosiveCharge(1, Type.HIGH_EXPLOSIVE);

    public enum Type implements StringRepresentable {
        /** TNT, plastic explosive: a crater, fragments and a sharp shock. */
        HIGH_EXPLOSIVE,
        /** A fuel-air charge: a wide fireball and a long, strong pressure wave, little crater. */
        THERMOBARIC,
        /** A bomblet: a cluster warhead frees one per item. */
        BOMBLET;

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final Codec<ExplosiveCharge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.floatRange(0, 100000).fieldOf("tnt_equivalent").forGetter(ExplosiveCharge::tntEquivalent),
            Type.CODEC.optionalFieldOf("type", Type.HIGH_EXPLOSIVE).forGetter(ExplosiveCharge::type)
    ).apply(instance, ExplosiveCharge::new));

    /** What {@code stack} is worth as a charge, or null if it is no conventional explosive. */
    public static @Nullable ExplosiveCharge of(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.CONVENTIONAL_EXPLOSIVES)) {
            return null;
        }
        ExplosiveCharge charge = stack.getItemHolder().getData(ConventionalRegistry.EXPLOSIVE_CHARGE);
        return charge != null ? charge : TNT;
    }
}
