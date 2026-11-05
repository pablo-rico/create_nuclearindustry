package org.papiricoh.create_nuclearindustry.dyson.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.dyson.ejector.EjectorMenu;
import org.papiricoh.create_nuclearindustry.dyson.ejector.EjectorOrbitPayload;
import org.papiricoh.create_nuclearindustry.dyson.ejector.EjectorStatus;
import org.papiricoh.create_nuclearindustry.dyson.ejector.EmRailEjectorBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.gui.LayoutRect;

import java.util.List;

import static org.papiricoh.create_nuclearindustry.dyson.gui.EjectorLayout.*;

/**
 * An EM rail ejector's screen: its sail magazine with the charge gauge beside it, a button per orbit (the one it launches into lit,
 * closed ones dark), and what it is doing.
 */
public class EjectorScreen extends AbstractContainerScreen<EjectorMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "textures/gui/em_rail_ejector_screen.png");
    private static final int TEXT = 0xFFFFE6A0, FAINT = 0xFFB89A5A, GOOD = 0xFF9CFFB0, BAD = 0xFFFF8A6A;

    public EjectorScreen(EjectorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelY = -1000;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        EmRailEjectorBlockEntity ejector = menu.ejector;
        if (ejector != null && button == 0) {
            for (int i = 0; i < ORBITS.size(); i++) {
                if (ORBITS.get(i).contains(mx - leftPos, my - topPos)) {
                    PacketDistributor.sendToServer(new EjectorOrbitPayload(menu.pos, i));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partial, int mx, int my) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, WIDTH, HEIGHT, SHEET, SHEET);
        EmRailEjectorBlockEntity ejector = menu.ejector;
        for (int i = 0; i < ORBITS.size(); i++) {
            LayoutRect r = ORBITS.get(i);
            int state = ejector != null && ejector.orbit() == i ? 1 : ClientSwarm.get().shapes()[i].enabled() ? 0 : 2;
            graphics.blit(TEXTURE, leftPos + r.x(), topPos + r.y(), SPRITE_ORBIT.x(), SPRITE_ORBIT.y() + state * r.h(), r.w(), r.h(), SHEET, SHEET);
        }
        if (ejector != null) {
            int filled = Math.round(ejector.charge(partial) * CHARGE.h());
            if (filled > 0)
                graphics.blit(TEXTURE, leftPos + CHARGE.x(), topPos + CHARGE.bottom() - filled, SPRITE_CHARGE.x(),
                        SPRITE_CHARGE.y() + SPRITE_CHARGE.h() - filled, CHARGE.w(), filled, SHEET, SHEET);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mx, int my) {
        graphics.drawString(font, title, TITLE.x() + 2, TITLE.y() + 2, TEXT, false);
        for (int i = 0; i < ORBITS.size(); i++) {
            LayoutRect r = ORBITS.get(i);
            Component label = Component.translatable("create_nuclearindustry.ejector.orbit_button", i + 1);
            graphics.drawString(font, label, r.x() + (r.w() - font.width(label)) / 2 - 2, r.y() + 3, TEXT, false);
        }
        EmRailEjectorBlockEntity ejector = menu.ejector;
        if (ejector == null) return;
        List<Component> lines = ejector.statusLines();
        int colour = ejector.status() == EjectorStatus.CHARGING ? GOOD : BAD;
        graphics.drawString(font, lines.get(0), STATUS.x() + 3, STATUS.y() + 3, colour, false);
        int every = ejector.ticksPerLaunch();
        Component second = every < 0 ? Component.translatable("create_nuclearindustry.ejector.compact_idle", ejector.sailsLoaded())
                : Component.translatable("create_nuclearindustry.ejector.compact", ejector.sailsLoaded(),
                String.format(java.util.Locale.ROOT, "%.1f", every / 20f));
        graphics.drawString(font, second, STATUS.x() + 3, STATUS.y() + 13, FAINT, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mx, int my, float partial) {
        super.render(graphics, mx, my, partial);
        renderTooltip(graphics, mx, my);
        for (int i = 0; i < ORBITS.size(); i++) {
            if (ORBITS.get(i).contains(mx - leftPos, my - topPos)) {
                boolean open = ClientSwarm.get().shapes()[i].enabled();
                graphics.renderTooltip(font, List.of(Component.translatable("create_nuclearindustry.ejector.orbit", i + 1),
                        Component.translatable(open ? "create_nuclearindustry.ejector.orbit_open" : "create_nuclearindustry.ejector.orbit_closed"),
                        Component.translatable("create_nuclearindustry.swarm.orbit_sails", ClientSwarm.get().sails()[i])),
                        java.util.Optional.empty(), mx, my);
            }
        }
    }
}
