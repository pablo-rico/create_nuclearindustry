package org.papiricoh.create_nuclearindustry.missile.orbit.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.create_nuclearindustry.missile.orbit.GroundStationMenu;
import org.papiricoh.create_nuclearindustry.missile.orbit.GroundStationConfigurePayload;

/** Functional code editor; the station synchronizes its status independently. */
public class GroundStationScreen extends AbstractContainerScreen<GroundStationMenu> {
    private EditBox code;
    public GroundStationScreen(GroundStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 220; imageHeight = 112;
    }
    @Override protected void init() {
        super.init();
        code = new EditBox(font, leftPos + 16, topPos + 35, 110, 20, Component.translatable("ground_station.create_nuclearindustry.code_label"));
        code.setMaxLength(16);
        code.setValue(menu.station == null ? "" : menu.station.code());
        addRenderableWidget(code);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> {
            PacketDistributor.sendToServer(new GroundStationConfigurePayload(menu.pos, code.getValue()));
            onClose();
        }).bounds(leftPos + 140, topPos + 35, 64, 20).build());
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff202830);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 12, 12, 0xffffff, false);
        if (menu.station != null) graphics.drawString(font, menu.station.statusLine(), 16, 75, 0xc0d8c0, false);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
