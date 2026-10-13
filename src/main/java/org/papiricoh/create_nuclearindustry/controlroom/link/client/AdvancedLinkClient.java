package org.papiricoh.create_nuclearindustry.controlroom.link.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.papiricoh.create_nuclearindustry.controlroom.link.AdvancedRedstoneLinkBlockEntity;

/** Client side of the Advanced Redstone Link, kept apart so the block never loads screen classes on a server. */
public final class AdvancedLinkClient {
    private AdvancedLinkClient() {}

    public static void openScreen(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof AdvancedRedstoneLinkBlockEntity link) {
            minecraft.setScreen(new AdvancedLinkScreen(pos, link.getCode()));
        }
    }
}
