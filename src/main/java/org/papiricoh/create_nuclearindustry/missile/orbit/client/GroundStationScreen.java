package org.papiricoh.create_nuclearindustry.missile.orbit.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.link.LinkCode;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.gui.LayoutRect;
import org.papiricoh.create_nuclearindustry.missile.orbit.GroundStationBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.orbit.GroundStationConfigurePayload;
import org.papiricoh.create_nuclearindustry.missile.orbit.GroundStationMenu;
import org.papiricoh.create_nuclearindustry.missile.orbit.Satellite;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;
import org.papiricoh.create_nuclearindustry.missile.radar.client.ScopeDraw;
import org.papiricoh.create_nuclearindustry.missile.silo.client.ConsoleDraw;

import java.util.Locale;

import static org.papiricoh.create_nuclearindustry.missile.gui.StationLayout.*;

/**
 * A ground station's screen, over its painted olive console: the sky over it with its network's satellites (by bearing and
 * ground distance, the ring their horizon), its link code (typed straight into the code window, sent once complete or cleared),
 * a lamp lit while it works, the dish's speed against what it needs, and its network: how many satellites and the soonest pass.
 */
public class GroundStationScreen extends AbstractContainerScreen<GroundStationMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "textures/gui/ground_station_screen.png");
    private static final String KEY = "ground_station.create_nuclearindustry.";
    private static final int GREEN = 0xFF7DFF9A, GREEN_FAINT = 0xFF3E7A4C, AMBER = 0xFFFFB347, RED = 0xFFFF5A4A, TEXT = 0xFFE8FFEC;

    private EditBox code;

    public GroundStationScreen(GroundStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
    }

    private GroundStationBlockEntity station() {
        return menu.station;
    }

    @Override
    protected void init() {
        super.init();
        code = new EditBox(font, leftPos + CODE.x() + 4, topPos + CODE.y() + 3, CODE.w() - 6, 10, Component.translatable(KEY + "code_label"));
        code.setBordered(false);
        code.setMaxLength(LinkCode.LENGTH);
        code.setTextColor(GREEN);
        code.setFilter(text -> text.chars().allMatch(c -> LinkCode.allowed((char) c)));
        if (station() != null) code.setValue(station().code());
        code.setResponder(text -> {
            String clean = LinkCode.sanitize(text);
            if (station() != null && (clean.isEmpty() || LinkCode.complete(clean)) && !clean.equals(station().code()))
                PacketDistributor.sendToServer(new GroundStationConfigurePayload(menu.pos, clean));
        });
        addRenderableWidget(code);
    }

    private boolean over(LayoutRect r, double mx, double my) {
        return r.contains(mx - leftPos, my - topPos);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (code.isMouseOver(mx, my) || over(CODE, mx, my)) {
            setFocused(code);
            return true;
        }
        setFocused(null);
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
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        if (minecraft.getResourceManager().getResource(TEXTURE).isPresent()) g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        else g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF1E2418);
        GroundStationBlockEntity station = station();
        if (station == null || minecraft.level == null) return;
        try (ScopeDraw d = ScopeDraw.begin(g, true)) {
            if (station.operational())
                d.disc(leftPos + CODE_LAMP.x() + CODE_LAMP.w() / 2f, topPos + CODE_LAMP.y() + CODE_LAMP.h() / 2f, 4.5f, GREEN, ScopeDraw.alpha(GREEN, 0.2f));
            sky(d, station, minecraft.level.getGameTime() + partial);
        }
    }

    /** The network's satellites over the station: a dot each by bearing and ground distance, its footprint the ring. */
    private void sky(ScopeDraw d, GroundStationBlockEntity station, double time) {
        if (!LinkCode.complete(station.code())) return;
        float cx = leftPos + SKY_CX + 0.5f, cy = topPos + SKY_CY + 0.5f;
        double sx = station.getBlockPos().getX() + 0.5, sz = station.getBlockPos().getZ() + 0.5;
        for (Satellite satellite : ClientSatellites.all()) {
            if (!satellite.code().equals(station.code())) continue;
            double footprint = satellite.orbit().footprint(MissileConfig.radarEarthRadius);
            if (!Double.isFinite(footprint) || footprint <= 0) footprint = 1.0E9;
            V3 at = satellite.orbit().position(time);
            double fx = (at.x() - sx) / footprint, fz = (at.z() - sz) / footprint;
            if (fx * fx + fz * fz > 1) continue;
            float x = cx + (float) fx * SKY_RADIUS, y = cy + (float) fz * SKY_RADIUS;
            d.disc(x, y, 4, ScopeDraw.alpha(GREEN, 0.5f), ScopeDraw.alpha(GREEN, 0));
            d.disc(x, y, 1.6f, 0xFFE7F9C1, GREEN);
            // Where it heads: a short tick along its ground velocity.
            V3 v = satellite.orbit().velocity(time);
            double speed = Math.hypot(v.x(), v.z());
            if (speed > 1e-6) d.line(x, y, x + (float) (v.x() / speed) * 5, y + (float) (v.z() / speed) * 5, 0.8f, GREEN, ScopeDraw.alpha(GREEN, 0.2f));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, TITLE.x() + 3, TITLE.y() + 2, TEXT, false);
        GroundStationBlockEntity station = station();
        if (station == null) return;
        if (code.getValue().isEmpty() && !code.isFocused()) g.drawString(font, "----", CODE.x() + 4, CODE.y() + 3, GREEN_FAINT, false);
        float rpm = Math.abs(station.getSpeed());
        boolean fast = rpm >= MissileConfig.minimumRpm;
        Component speed = Component.translatable(KEY + "screen.rpm", Math.round(rpm));
        ConsoleDraw.line(g, font, speed, RPM.x() + 4, RPM.y() + 3, RPM.w() - 7, fast ? GREEN : RED, ConsoleDraw.SMALL);
        boolean online = station.operational();
        ConsoleDraw.line(g, font, station.statusLine(), STATUS.x() + 5, STATUS.y() + 4, STATUS.w() - 10, online ? GREEN : AMBER, ConsoleDraw.SMALL);
        Component why = !LinkCode.complete(station.code()) ? Component.translatable(KEY + "screen.needs_code")
                : !fast ? Component.translatable(KEY + "screen.needs_rpm", Math.round(MissileConfig.minimumRpm))
                : online ? Component.translatable(KEY + "screen.tracking") : station.statusLine();
        ConsoleDraw.line(g, font, why, STATUS.x() + 5, STATUS.y() + 14, STATUS.w() - 10, online ? GREEN_FAINT : RED, ConsoleDraw.SMALL);
        ConsoleDraw.line(g, font, Component.translatable(KEY + "satellites", station.satelliteCount()), NETWORK.x() + 5, NETWORK.y() + 4, NETWORK.w() - 10,
                station.satelliteCount() > 0 ? GREEN : GREEN_FAINT, ConsoleDraw.SMALL);
        long pass = station.nextPass();
        Component next = pass < 0 ? Component.translatable(KEY + "no_pass") : pass == 0 ? Component.translatable(KEY + "screen.pass_now")
                : Component.translatable(KEY + "screen.next_pass", clock(pass));
        ConsoleDraw.line(g, font, next, NETWORK.x() + 5, NETWORK.y() + 19, NETWORK.w() - 10, pass == 0 ? GREEN : pass > 0 ? TEXT : GREEN_FAINT, ConsoleDraw.SMALL);
    }

    private static String clock(long ticks) {
        long seconds = Math.max(0, ticks) / 20;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
        GroundStationBlockEntity station = station();
        Component tip = null;
        if (over(CODE, mouseX, mouseY) || over(CODE_LAMP, mouseX, mouseY)) tip = Component.translatable(KEY + "screen.code.tooltip");
        else if (over(RPM, mouseX, mouseY) && station != null)
            tip = Component.translatable(KEY + "screen.rpm.tooltip", Math.round(Math.abs(station.getSpeed())), Math.round(MissileConfig.minimumRpm));
        else if (over(SKY, mouseX, mouseY)) tip = Component.translatable(KEY + "screen.sky.tooltip");
        if (tip != null) g.renderTooltip(font, font.split(tip, 200), mouseX, mouseY);
    }
}
