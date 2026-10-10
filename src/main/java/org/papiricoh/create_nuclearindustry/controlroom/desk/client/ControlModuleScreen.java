package org.papiricoh.create_nuclearindustry.controlroom.desk.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.AlarmMetric;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ConfigureModulePayload;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModule;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModuleMenu;
import org.papiricoh.create_nuclearindustry.controlroom.desk.GaugeReading;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleFunction;
import org.papiricoh.create_nuclearindustry.controlroom.desk.OutputMode;

import java.util.List;

/**
 * Sets up one control module: what it does, its label, the reading or alarm it follows, and where
 * its redstone goes. Edits a local copy and sends the whole setup to the server on every change.
 */
public class ControlModuleScreen extends AbstractContainerScreen<ControlModuleMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "textures/gui/control_module.png");
    private static final int WIDTH = 200;
    private static final int HEIGHT = 222;
    private static final int FREQUENCY_Y = 92;
    private static final int FREQUENCY_FIRST_X = 152;
    private static final int FREQUENCY_LAST_X = 174;
    private static final String KEY = "create_nuclearindustry.control_module.";

    private ControlModule module;
    private Button functionButton;
    private EditBox labelBox;
    private Button parameterButton;
    private Button minusButton;
    private Button plusButton;
    private Button outputButton;

    public ControlModuleScreen(ControlModuleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelX = ControlModuleMenu.INVENTORY_X;
        inventoryLabelY = ControlModuleMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        ControlModule live = menu.module();
        if (live == null || minecraft == null || minecraft.level == null) {
            onClose();
            return;
        }
        module = ControlModule.read(live.write(minecraft.level.registryAccess()), minecraft.level.registryAccess());

        functionButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            List<ModuleFunction> functions = module.type().functions();
            module.withFunction(functions.get((functions.indexOf(module.function()) + 1) % functions.size()));
            changed();
        }).bounds(leftPos + 10, topPos + 20, 180, 20).build());

        labelBox = new EditBox(font, leftPos + 11, topPos + 46, 178, 14, Component.translatable(KEY + "label"));
        labelBox.setMaxLength(ControlModule.LABEL_MAX);
        labelBox.setValue(module.label());
        labelBox.setHint(Component.translatable(KEY + "label.hint"));
        labelBox.setResponder(text -> {
            if (!text.equals(module.label())) {
                module.withLabel(text);
                changed();
            }
        });
        addRenderableWidget(labelBox);

        parameterButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            if (module.function() == ModuleFunction.LAMP_ALARM) {
                AlarmMetric[] metrics = AlarmMetric.values();
                module.withMetric(metrics[(module.metric().ordinal() + 1) % metrics.length]);
            } else if (module.function() == ModuleFunction.READING) {
                GaugeReading[] readings = GaugeReading.values();
                module.withReading(readings[(module.reading().ordinal() + 1) % readings.length]);
            } else if (module.isController()) {
                module.withEngaged(!module.engaged());
            }
            changed();
        }).bounds(leftPos + 10, topPos + 66, 120, 20).build());
        minusButton = addRenderableWidget(Button.builder(Component.literal("-"), b -> step(-1))
                .bounds(leftPos + 134, topPos + 66, 18, 20).build());
        plusButton = addRenderableWidget(Button.builder(Component.literal("+"), b -> step(1))
                .bounds(leftPos + 172, topPos + 66, 18, 20).build());

        outputButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            module.withOutput(module.output().next());
            changed();
        }).bounds(leftPos + 10, topPos + 90, 136, 20).build());
        refreshWidgets();
    }

    private void step(int direction) {
        int amount = module.function() == ModuleFunction.LAMP_ALARM || module.isController() ? (hasShiftDown() ? 1 : 5) : 1;
        module.withSetting(module.setting() + direction * amount);
        changed();
    }

    private void changed() {
        refreshWidgets();
        if (minecraft != null && minecraft.level != null) {
            PacketDistributor.sendToServer(new ConfigureModulePayload(menu.deskPos(), menu.slot(),
                    module.writeConfig(minecraft.level.registryAccess())));
        }
    }

    private void refreshWidgets() {
        functionButton.setMessage(Component.translatable(KEY + "function", Component.translatable(module.function().translationKey())));
        boolean alarm = module.function() == ModuleFunction.LAMP_ALARM;
        boolean gauge = module.function() == ModuleFunction.READING;
        boolean level = module.function() == ModuleFunction.SIGNAL_LEVEL;
        boolean controller = module.isController();
        parameterButton.visible = alarm || gauge || controller;
        parameterButton.setMessage(alarm ? Component.translatable(module.metric().getTranslationKey())
                : gauge ? Component.translatable(module.reading().translationKey())
                : controller ? Component.translatable(KEY + (module.engaged() ? "mode.auto" : "mode.standby"))
                : Component.empty());
        minusButton.visible = alarm || level || controller;
        plusButton.visible = alarm || level || controller;
        outputButton.setMessage(Component.translatable(KEY + "output", Component.translatable(module.output().translationKey())));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (module != null && module.output() == OutputMode.LINK) {
            boolean first = inFrequency(mouseX, mouseY, FREQUENCY_FIRST_X);
            boolean last = inFrequency(mouseX, mouseY, FREQUENCY_LAST_X);
            if (first || last) {
                ItemStack carried = menu.getCarried();
                module.withFrequencies(first ? carried : module.frequencyFirst(), last ? carried : module.frequencyLast());
                changed();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean inFrequency(double mouseX, double mouseY, int x) {
        double localX = mouseX - leftPos - x;
        double localY = mouseY - topPos - FREQUENCY_Y;
        return localX >= 0 && localX < 18 && localY >= 0 && localY < 18;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (labelBox != null && labelBox.isFocused() && keyCode != InputConstants.KEY_ESCAPE) {
            return labelBox.keyPressed(keyCode, scanCode, modifiers) || labelBox.canConsumeInput();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
        if (module == null) {
            return;
        }
        boolean link = module.output() == OutputMode.LINK;
        for (int x : new int[]{FREQUENCY_FIRST_X, FREQUENCY_LAST_X}) {
            graphics.fill(leftPos + x, topPos + FREQUENCY_Y, leftPos + x + 18, topPos + FREQUENCY_Y + 18, link ? 0xFF2B3A33 : 0xFF1A1F1C);
            graphics.fill(leftPos + x + 1, topPos + FREQUENCY_Y + 1, leftPos + x + 17, topPos + FREQUENCY_Y + 17, link ? 0xFF3E5246 : 0xFF222824);
        }
        if (link) {
            graphics.renderItem(module.frequencyFirst(), leftPos + FREQUENCY_FIRST_X + 1, topPos + FREQUENCY_Y + 1);
            graphics.renderItem(module.frequencyLast(), leftPos + FREQUENCY_LAST_X + 1, topPos + FREQUENCY_Y + 1);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (module == null) {
            return;
        }
        graphics.drawString(font, Component.translatable(module.type().translationKey()), 10, 7, 0xFFE8C26A, false);
        if (module.function() == ModuleFunction.LAMP_ALARM || module.function() == ModuleFunction.SIGNAL_LEVEL) {
            String value = module.function() == ModuleFunction.LAMP_ALARM ? module.metric().format(module.setting())
                    : Integer.toString(module.setting());
            graphics.drawCenteredString(font, value, 162, 72, 0xFFFFFFFF);
        }
        if (module.isController()) {
            // Just the number between the buttons; the units go in the line below.
            graphics.drawCenteredString(font, String.format(java.util.Locale.ROOT, "%.0f", module.setpoint()), 162, 72, 0xFFFFFFFF);
        }
        if (module.output() == OutputMode.LINK) {
            graphics.drawString(font, Component.translatable(KEY + "frequency"), 10, 114, 0xFFB0B8B0, false);
        } else if (module.isController()) {
            ControlModule live = menu.module();
            float demand = live != null ? live.demand() : -1;
            Component state = demand >= 0 && module.engaged()
                    ? Component.translatable(KEY + "controller_demand", Math.round(demand * 100))
                    : Component.translatable(KEY + "standby");
            graphics.drawString(font, Component.translatable(KEY + "setpoint", module.formatSetpoint(), state), 10, 114, 0xFFB0B8B0, false);
        }
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFCCCCCC, false);
    }
}
