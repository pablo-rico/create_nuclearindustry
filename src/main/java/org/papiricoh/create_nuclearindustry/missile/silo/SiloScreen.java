package org.papiricoh.create_nuclearindustry.missile.silo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class SiloScreen extends AbstractContainerScreen<SiloMenu> {
    private EditBox x, y, z;
    public SiloScreen(SiloMenu menu, Inventory inv, Component title) { super(menu, inv, title); imageWidth = 300; imageHeight = 180; }
    private MutableComponent text(String key) { return Component.translatable("silo.create_nuclearindustry." + key); }
    @Override protected void init() {
        super.init();
        x = coordinate(16, "X"); y = coordinate(106, "Y"); z = coordinate(196, "Z");
        if (minecraft.level.getBlockEntity(menu.pos) instanceof SiloControllerEntity c && c.target() != null) {
            x.setValue("" + c.target().getX()); y.setValue("" + c.target().getY()); z.setValue("" + c.target().getZ());
        }
        addRenderableWidget(Button.builder(text("target"), b -> {
            try { send(0, new BlockPos(Integer.parseInt(x.getValue()), Integer.parseInt(y.getValue()), Integer.parseInt(z.getValue()))); }
            catch (NumberFormatException ignored) { }
        }).bounds(leftPos + 16, topPos + 112, 268, 20).build());
        addRenderableWidget(Button.builder(text("arm"), b -> send(1, BlockPos.ZERO)).bounds(leftPos + 16, topPos + 144, 128, 20).build());
        addRenderableWidget(Button.builder(text("disarm"), b -> send(2, BlockPos.ZERO)).bounds(leftPos + 156, topPos + 144, 128, 20).build());
    }
    private EditBox coordinate(int offset, String axis) {
        EditBox box = new EditBox(font, leftPos + offset, topPos + 86, 86, 20, Component.literal(axis));
        box.setMaxLength(9); box.setValue("0"); addRenderableWidget(box); return box;
    }
    private void send(int action, BlockPos target) { PacketDistributor.sendToServer(new SiloControlPayload(menu.pos, action, target, 0)); }
    @Override protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff17232b);
        g.fill(leftPos+2, topPos+2, leftPos+imageWidth-2, topPos+24, 0xff32505c);
    }
    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 12, 8, 0xffeeeeee, false);
        if (minecraft.level.getBlockEntity(menu.pos) instanceof SiloControllerEntity c) {
            g.drawWordWrap(font, c.status(), 16, 32, 268, 0xffe4be72);
            if (c.rocketHeight() > 0) g.drawString(font, c.guidanceLine(), 16, 74, 0xff9fc7b8, false);
            g.drawString(font, text("timing").append(Component.literal(" " + Math.max(0, c.countdown()/20) + "s / " + (int)(c.flightTicks()/20) + "s")), 16, 62, 0xffd0d0d0, false);
        }
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) { super.render(g, mx, my, partial); renderTooltip(g, mx, my); }
}
