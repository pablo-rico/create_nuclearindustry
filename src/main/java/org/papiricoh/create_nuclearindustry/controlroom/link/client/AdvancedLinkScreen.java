package org.papiricoh.create_nuclearindustry.controlroom.link.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.papiricoh.create_nuclearindustry.controlroom.ConfigureDevicePayload;
import org.papiricoh.create_nuclearindustry.controlroom.link.LinkCode;

/**
 * Types an Advanced Redstone Link's code: four letters or digits, shown in capitals. Enter or Done
 * sends it; the server sanitises it again.
 */
public class AdvancedLinkScreen extends Screen {
    private static final String KEY = "create_nuclearindustry.advanced_link.";
    private final BlockPos pos;
    private final String initial;
    private EditBox codeBox;

    public AdvancedLinkScreen(BlockPos pos, String code) {
        super(Component.translatable("block.create_nuclearindustry.advanced_redstone_link"));
        this.pos = pos;
        this.initial = code;
    }

    @Override
    protected void init() {
        int centre = width / 2;
        int y = height / 2 - 20;
        codeBox = new EditBox(font, centre - 30, y, 60, 20, Component.translatable(KEY + "code_box"));
        codeBox.setMaxLength(LinkCode.LENGTH);
        codeBox.setValue(initial);
        codeBox.setFilter(text -> text.chars().allMatch(c -> LinkCode.allowed((char) c)));
        codeBox.setResponder(text -> {
            String upper = LinkCode.sanitize(text);
            if (!upper.equals(text)) {
                codeBox.setValue(upper);
            }
        });
        codeBox.setHint(Component.literal("----"));
        addRenderableWidget(codeBox);
        setInitialFocus(codeBox);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(centre - 50, y + 30, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 52, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable(KEY + "hint"), width / 2, height / 2 - 36, 0xA0A0A0);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        String code = LinkCode.sanitize(codeBox.getValue());
        if (!code.equals(initial)) {
            CompoundTag config = new CompoundTag();
            config.putString("Code", code);
            PacketDistributor.sendToServer(new ConfigureDevicePayload(pos, 0, config));
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
