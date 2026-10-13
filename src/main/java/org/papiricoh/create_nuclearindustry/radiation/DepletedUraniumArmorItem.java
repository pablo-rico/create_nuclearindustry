package org.papiricoh.create_nuclearindustry.radiation;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Heavy depleted-uranium plate: better armour and radiation shielding than anything else,
 * paid for in speed. A full set slows the wearer by about a sixth.
 */
public class DepletedUraniumArmorItem extends ArmorItem {
    private final double slowdown;

    public DepletedUraniumArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
        this.slowdown = switch (type) {
            case HELMET, BOOTS -> 0.03;
            case CHESTPLATE -> 0.06;
            case LEGGINGS -> 0.05;
            case BODY -> 0.0;
        };
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        ItemAttributeModifiers base = super.getDefaultAttributeModifiers();
        if (slowdown <= 0) {
            return base;
        }
        return base.withModifierAdded(Attributes.MOVEMENT_SPEED,
                new AttributeModifier(AllNuclearRadiation.id("depleted_uranium_armor." + getType().getName()), -slowdown,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                EquipmentSlotGroup.bySlot(getType().getSlot()));
    }
}
