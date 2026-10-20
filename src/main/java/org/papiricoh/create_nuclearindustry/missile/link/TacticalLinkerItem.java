package org.papiricoh.create_nuclearindustry.missile.link;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;

/**
 * Links missile devices: click a missile radar or a launch controller (or any block of its launch site) to remember it,
 * then click a command table or a radar scope to link or unlink it there. Sneak-click a device to clear its links;
 * sneak-use in the air to forget the stored one.
 */
public class TacticalLinkerItem extends Item {
    public TacticalLinkerItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static TacticalLink getLink(ItemStack stack) {
        return stack.get(AllNuclearDataComponents.TACTICAL_LINK.get());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getLink(stack) != null;
    }
}
