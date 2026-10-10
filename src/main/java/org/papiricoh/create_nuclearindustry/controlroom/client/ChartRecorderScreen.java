package org.papiricoh.create_nuclearindustry.controlroom.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.ConfigureDevicePayload;
import org.papiricoh.create_nuclearindustry.controlroom.DeviceMenu;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.controlroom.chart.ChartBuffer;
import org.papiricoh.create_nuclearindustry.controlroom.chart.ChartRecorderBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.GaugeReading;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Chooses what each pen of a chart recorder draws, from the readings that suit what it is linked to,
 * and how fast the paper runs. Click to step forwards, sneak-click to step back.
 */
public class ChartRecorderScreen extends AbstractContainerScreen<DeviceMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "textures/gui/chart_recorder.png");
    private static final int WIDTH = 200;
    private static final int HEIGHT = 124;
    /** Top of the first pen row; rows are {@link #ROW} apart. Swatches sit at x 10, buttons from x 24. */
    private static final int PEN_Y = 22;
    private static final int ROW = 24;
    private static final int SPEED_Y = 96;
    private static final String KEY = "create_nuclearindustry.chart_recorder.";

    private final GaugeReading[] pens = new GaugeReading[ChartBuffer.MAX_PENS];
    private final Button[] penButtons = new Button[ChartBuffer.MAX_PENS];
    private Button speedButton;
    private int speed;
    private List<GaugeReading> choices = List.of();

    public ChartRecorderScreen(DeviceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        if (!(menu.blockEntity() instanceof ChartRecorderBlockEntity recorder)) {
            onClose();
            return;
        }
        for (int pen = 0; pen < pens.length; pen++) {
            pens[pen] = recorder.pen(pen);
        }
        speed = recorder.speed();
        TargetKind kind = recorder.kind();
        choices = kind == TargetKind.NONE || kind == TargetKind.NO_SIGNAL
                ? Arrays.asList(GaugeReading.values())
                : GaugeReading.forKind(kind);
        for (int pen = 0; pen < pens.length; pen++) {
            int index = pen;
            penButtons[pen] = addRenderableWidget(Button.builder(Component.empty(), b -> cyclePen(index))
                    .bounds(leftPos + 24, topPos + PEN_Y + pen * ROW, 166, 20).build());
        }
        speedButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            int count = ChartRecorderBlockEntity.SPEEDS.length;
            speed = Math.floorMod(speed + (hasShiftDown() ? -1 : 1), count);
            send();
        }).bounds(leftPos + 10, topPos + SPEED_Y, 180, 20).build());
        refresh();
    }

    /** Off, then each suitable reading in turn. */
    private void cyclePen(int pen) {
        List<GaugeReading> options = new ArrayList<>();
        options.add(null);
        options.addAll(choices);
        int at = options.indexOf(pens[pen]);
        pens[pen] = options.get(Math.floorMod(at + (hasShiftDown() ? -1 : 1), options.size()));
        send();
    }

    private void send() {
        refresh();
        CompoundTag config = new CompoundTag();
        for (int pen = 0; pen < pens.length; pen++) {
            config.putString("Pen" + pen, pens[pen] == null ? "" : pens[pen].name());
        }
        config.putInt("Speed", speed);
        PacketDistributor.sendToServer(new ConfigureDevicePayload(menu.pos(), 0, config));
    }

    private void refresh() {
        for (int pen = 0; pen < pens.length; pen++) {
            Component reading = pens[pen] == null ? Component.translatable(KEY + "pen.off")
                    : Component.translatable(pens[pen].translationKey());
            penButtons[pen].setMessage(Component.translatable(KEY + "pen", pen + 1, reading));
        }
        speedButton.setMessage(Component.translatable(KEY + "speed", ChartRecorderBlockEntity.SPEEDS[speed]));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
        // Ink swatches in the wells beside each pen.
        for (int pen = 0; pen < pens.length; pen++) {
            int y = topPos + PEN_Y + pen * ROW + 4;
            int color = pens[pen] == null ? 0xFF3A403C : ChartRecorderBlockEntity.PEN_COLORS[pen];
            graphics.fill(leftPos + 11, y + 1, leftPos + 19, y + 11, color);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 10, 7, 0xFFE8C26A, false);
    }
}
