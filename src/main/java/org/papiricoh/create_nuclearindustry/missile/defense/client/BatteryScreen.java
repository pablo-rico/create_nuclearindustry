package org.papiricoh.create_nuclearindustry.missile.defense.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.link.LinkCode;
import org.papiricoh.create_nuclearindustry.missile.defense.DefenseConfig;
import org.papiricoh.create_nuclearindustry.missile.defense.DefenseSettings;
import org.papiricoh.create_nuclearindustry.missile.defense.Doctrine;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.BatteryConfigurePayload;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.InterceptorBatteryEntity;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.InterceptorBatteryMenu;
import org.papiricoh.create_nuclearindustry.missile.gui.LayoutRect;

import java.util.Locale;

import static org.papiricoh.create_nuclearindustry.missile.gui.BatteryLayout.*;

/**
 * An interceptor battery's screen, over its painted console: the turret seen from above on the scope (where it points, where it
 * is turning to and each tube's lamp), the tubes' state, what it is doing, and its radar link code, automatic mode, doctrine and
 * defended zone, changed by clicking the painted keys.
 */
public class BatteryScreen extends AbstractContainerScreen<InterceptorBatteryMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "textures/gui/interceptor_battery_screen.png");
    private static final String KEY = "battery.create_nuclearindustry.screen.";
    private static final int GREEN = 0xFF7DFF9A, GREEN_FAINT = 0xFF3E7A4C, AMBER = 0xFFFFB347, RED = 0xFFFF5A4A, TEXT = 0xFFE8FFEC;
    /** The scope painted in the schematic: its centre and outer ring, texture pixels. */
    private static final int SCOPE_X = 56, SCOPE_Y = 81, SCOPE_R = 44;
    /** Blocks one press of the zone keys changes the defended radius by. */
    private static final int ZONE_STEP = 50;

    private EditBox code;

    public BatteryScreen(InterceptorBatteryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
    }

    private InterceptorBatteryEntity battery() {
        return menu.battery;
    }

    @Override
    protected void init() {
        super.init();
        code = new EditBox(font, leftPos + CODE.x() + 4, topPos + CODE.y() + 3, CODE.w() - 6, 10, Component.translatable(KEY + "code"));
        code.setBordered(false);
        code.setMaxLength(LinkCode.LENGTH);
        code.setTextColor(GREEN);
        code.setFilter(text -> text.chars().allMatch(c -> LinkCode.allowed((char) c)));
        if (battery() != null) code.setValue(battery().code());
        code.setResponder(text -> {
            String clean = LinkCode.sanitize(text);
            if (battery() != null && (clean.isEmpty() || LinkCode.complete(clean)) && !clean.equals(battery().code()))
                send(BatteryConfigurePayload.SET_CODE, 0, clean);
        });
        addRenderableWidget(code);
    }

    private void send(byte action, int arg, String text) {
        PacketDistributor.sendToServer(new BatteryConfigurePayload(menu.pos, action, arg, text));
    }

    private boolean over(LayoutRect r, double mx, double my) {
        return r.contains(mx - leftPos, my - topPos);
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (code.isMouseOver(mx, my) || over(CODE, mx, my)) {
            setFocused(code);
            return true;
        }
        setFocused(null);
        InterceptorBatteryEntity battery = battery();
        if (button == 0 && battery != null) {
            DefenseSettings settings = battery.settings();
            if (over(AUTO, mx, my)) {
                send(BatteryConfigurePayload.SET_AUTO, settings.auto() ? 0 : 1, "");
            } else if (over(DOCTRINE, mx, my)) {
                send(BatteryConfigurePayload.SET_DOCTRINE, (settings.doctrine().ordinal() + 1) % Doctrine.values().length, "");
            } else if (over(ZONE_DOWN, mx, my)) {
                send(BatteryConfigurePayload.SET_ZONE_RADIUS, Math.max(0, settings.zoneRadius() - ZONE_STEP), "");
            } else if (over(ZONE_UP, mx, my)) {
                send(BatteryConfigurePayload.SET_ZONE_RADIUS, settings.zoneRadius() + ZONE_STEP, "");
            } else if (over(ZONE_HERE, mx, my)) {
                send(BatteryConfigurePayload.ZONE_HERE, 0, "");
            } else {
                return super.mouseClicked(mx, my, button);
            }
            click();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (code.isFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
                setFocused(null);
                return true;
            }
            return code.keyPressed(key, scan, modifiers) || true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
        InterceptorBatteryEntity battery = battery();
        if (battery == null) return;
        Component tip = null;
        if (over(AUTO, mouseX, mouseY)) tip = Component.translatable(KEY + "auto.tooltip");
        else if (over(DOCTRINE, mouseX, mouseY)) tip = Component.translatable(KEY + "doctrine.tooltip");
        else if (over(ZONE, mouseX, mouseY) || over(ZONE_DOWN, mouseX, mouseY) || over(ZONE_UP, mouseX, mouseY))
            tip = Component.translatable(KEY + "zone.tooltip");
        else if (over(ZONE_HERE, mouseX, mouseY)) tip = Component.translatable(KEY + "zone_here.tooltip");
        else if (over(CODE, mouseX, mouseY) || over(CODE_LAMP, mouseX, mouseY)) tip = Component.translatable(KEY + "code.tooltip");
        if (tip != null) g.renderTooltip(font, font.split(tip, 200), mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, TITLE.x() + 2, TITLE.y() + 2, TEXT, false);
        InterceptorBatteryEntity battery = battery();
        if (battery == null) return;
        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        drawScope(g, battery, partial);
        drawTubes(g, battery);
        drawStatus(g, battery);
        DefenseSettings settings = battery.settings();
        if (code.getValue().isEmpty() && !code.isFocused()) g.drawString(font, Component.translatable(KEY + "code"), CODE.x() + 4, CODE.y() + 3, GREEN_FAINT, false);
        if (LinkCode.complete(battery.code())) g.fill(CODE_LAMP.x() + 3, CODE_LAMP.y() + 3, CODE_LAMP.x() + 11, CODE_LAMP.y() + 11, GREEN);
        centred(g, Component.translatable(KEY + (settings.auto() ? "auto.on" : "auto.off")), AUTO, settings.auto() ? GREEN : GREEN_FAINT);
        centred(g, Component.translatable("battery.create_nuclearindustry.doctrine." + settings.doctrine().name().toLowerCase(Locale.ROOT)), DOCTRINE, TEXT);
        centred(g, settings.zoneRadius() == 0 ? Component.translatable(KEY + "zone.default", Math.round(DefenseConfig.batteryRange))
                : Component.translatable(KEY + "zone.blocks", settings.zoneRadius()), ZONE, TEXT);
    }

    /** The turret from above, north up: where it points, where it is turning to, and a lamp per tube. */
    private void drawScope(GuiGraphics g, InterceptorBatteryEntity battery, float partial) {
        float yaw = battery.yaw(partial);
        if (battery.status() == InterceptorBatteryEntity.Status.SLEWING || battery.status() == InterceptorBatteryEntity.Status.TRACKING)
            ray(g, battery.targetYaw(), SCOPE_R - 2, AMBER);
        ray(g, yaw, SCOPE_R - 10, GREEN);
        float s = Mth.sin(yaw * Mth.DEG_TO_RAD), c = Mth.cos(yaw * Mth.DEG_TO_RAD);
        for (int i = 0; i < InterceptorBatteryEntity.TUBES; i++) {
            // Tubes in two columns across the turret, two rows along it; north of the turret is its muzzle.
            float across = (i & 1) == 0 ? -4 : 4, along = (i >> 1) == 0 ? -4 : 0;
            int x = Math.round(SCOPE_X + across * c - along * s), y = Math.round(SCOPE_Y + across * s + along * c);
            g.fill(x - 1, y - 1, x + 2, y + 2, tubeColour(battery, i));
        }
    }

    /** A line from the scope's centre {@code length} pixels out at bearing {@code yaw}. */
    private static void ray(GuiGraphics g, float yaw, int length, int colour) {
        float s = Mth.sin(yaw * Mth.DEG_TO_RAD), c = -Mth.cos(yaw * Mth.DEG_TO_RAD);
        for (int i = 3; i <= length; i++) {
            int x = Math.round(SCOPE_X + s * i), y = Math.round(SCOPE_Y + c * i);
            g.fill(x, y, x + 1, y + 1, colour);
        }
    }

    private static int tubeColour(InterceptorBatteryEntity battery, int tube) {
        if (battery.tubeInventory().getStackInSlot(tube).isEmpty()) return RED;
        return battery.loadingTicks(tube) > 0 ? AMBER : GREEN;
    }

    private void drawTubes(GuiGraphics g, InterceptorBatteryEntity battery) {
        for (int i = 0; i < InterceptorBatteryEntity.TUBES; i++) {
            Component state = battery.tubeInventory().getStackInSlot(i).isEmpty() ? Component.translatable(KEY + "tube.empty")
                    : battery.loadingTicks(i) > 0 ? Component.translatable(KEY + "tube.loading", (battery.loadingTicks(i) + 19) / 20)
                    : Component.translatable(KEY + "tube.ready");
            g.drawString(font, Component.literal((i + 1) + " ").append(state), TUBE_STATUS.x() + 3, TUBE_STATUS.y() + 1 + i * 10, tubeColour(battery, i), false);
        }
    }

    private void drawStatus(GuiGraphics g, InterceptorBatteryEntity battery) {
        InterceptorBatteryEntity.Status status = battery.status();
        int colour = switch (status) {
            case UNPOWERED, ON_SHIP, EMPTY -> RED;
            case HOLD, RELOADING, SLEWING -> AMBER;
            default -> GREEN;
        };
        g.drawString(font, Component.translatable("battery.create_nuclearindustry." + status.name().toLowerCase(Locale.ROOT)),
                STATUS.x() + 4, STATUS.y() + 3, colour, false);
        Component detail = status == InterceptorBatteryEntity.Status.UNPOWERED ? Component.translatable(KEY + "needs_rpm")
                : status == InterceptorBatteryEntity.Status.HOLD ? Component.translatable(KEY + "held")
                : !LinkCode.complete(battery.code()) ? Component.translatable(KEY + "no_code")
                : Component.translatable(KEY + "on_network", battery.code());
        g.drawString(font, font.plainSubstrByWidth(detail.getString(), STATUS.w() - 8), STATUS.x() + 4, STATUS.y() + 13, GREEN_FAINT, false);
    }

    private void centred(GuiGraphics g, Component text, LayoutRect r, int colour) {
        String fitted = font.plainSubstrByWidth(text.getString(), r.w() - 4);
        g.drawString(font, fitted, r.x() + (r.w() - font.width(fitted)) / 2, r.y() + (r.h() - 8) / 2, colour, false);
    }
}
