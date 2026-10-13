package org.papiricoh.create_nuclearindustry.radiation;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** While held, shows the dose rate and accumulated dose and clicks faster the hotter it gets. */
public class GeigerCounterItem extends Item {

    public GeigerCounterItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)
                || (!selected && player.getOffhandItem() != stack) || player.tickCount % 10 != 0) {
            return;
        }
        float rate = player.getData(AllNuclearRadiation.RATE);
        float dose = player.getData(AllNuclearRadiation.DOSE);
        ChatFormatting color = rate >= 1.0f ? ChatFormatting.RED : rate >= 0.1f ? ChatFormatting.GOLD : ChatFormatting.GREEN;
        player.displayClientMessage(Component.translatable("item.create_nuclearindustry.geiger_counter.reading",
                String.format("%.2f", rate), String.format("%.0f", dose)).withStyle(color), true);
        int clicks = Math.min(6, (int) Math.ceil(rate * 3));
        for (int i = 0; i < clicks; i++) {
            level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS,
                    0.35f, 1.6f + level.random.nextFloat() * 0.4f);
        }
    }
}
