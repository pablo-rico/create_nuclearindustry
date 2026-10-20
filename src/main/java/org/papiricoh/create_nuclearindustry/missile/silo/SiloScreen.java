package org.papiricoh.create_nuclearindustry.missile.silo;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.missile.flight.Warhead;
import org.papiricoh.create_nuclearindustry.missile.gui.LayoutRect;
import org.papiricoh.create_nuclearindustry.missile.physics.GuidanceTier;
import org.papiricoh.create_nuclearindustry.missile.silo.client.ClientLaunchDiagrams;
import org.papiricoh.create_nuclearindustry.missile.silo.client.ConsoleNumbers;
import org.papiricoh.create_nuclearindustry.missile.silo.client.ConsoleDraw;
import org.papiricoh.create_nuclearindustry.missile.silo.client.ConsoleStages;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.papiricoh.create_nuclearindustry.missile.gui.SiloLayout.*;

/** A live cutaway and loading console beside the launch controls. All figures come from the server's snapshot. */
public class SiloScreen extends AbstractContainerScreen<SiloMenu> {
    private static final ResourceLocation ROCKET_TEXTURE = texture("launch_controller_rocket");
    private static final ResourceLocation LAUNCH_TEXTURE = texture("launch_controller_launch");
    private static final ResourceLocation WIDGETS = texture("launch_controller_widgets");
    private static final int TEXT = 0xFFE0E9E7, MUTED = 0xFF8CA5AA, ACCENT = 0xFF78D8BE;
    private static final int[] LAMPS = {0xFFE86C57, 0xFFE0AD58, 0xFF78D8BE, 0xFFFF793F, 0xFFFF4A3A, 0xFF7DA9DD};
    private EditBox x, y, z;
    private ConsoleButton rocketTab, launchTab, set, airburst, arm, disarm;
    private boolean launch, widgetsAvailable, rocketAvailable, launchAvailable;
    private final ConsoleStages stages = new ConsoleStages();

    public SiloScreen(SiloMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath("create_nuclearindustry", "textures/gui/" + name + ".png");
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("silo.create_nuclearindustry.screen." + key, args);
    }

    private static Component sectionName(int ordinal) {
        RocketComposition.Section[] kinds = RocketComposition.Section.values();
        return text("section." + (ordinal >= 0 && ordinal < kinds.length ? kinds[ordinal].name().toLowerCase(Locale.ROOT) : "foreign"));
    }

    private static Component guidanceName(int ordinal) {
        return Component.translatable("silo.create_nuclearindustry.guidance." + GuidanceTier.byOrdinal(ordinal).name().toLowerCase(Locale.ROOT));
    }

    private @Nullable SiloControllerEntity controller() {
        return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(menu.pos) instanceof SiloControllerEntity c ? c : null;
    }

    @Override
    protected void init() {
        String oldX = x == null ? null : x.getValue(), oldY = y == null ? null : y.getValue(), oldZ = z == null ? null : z.getValue();
        super.init();
        widgetsAvailable = minecraft.getResourceManager().getResource(WIDGETS).isPresent();
        rocketAvailable = minecraft.getResourceManager().getResource(ROCKET_TEXTURE).isPresent();
        launchAvailable = minecraft.getResourceManager().getResource(LAUNCH_TEXTURE).isPresent();
        rocketTab = button(TAB_ROCKET, text("tab.rocket"), b -> tab(false));
        launchTab = button(TAB_LAUNCH, text("tab.launch"), b -> tab(true));
        x = coordinate(BOX_X, "X"); y = coordinate(BOX_Y, "Y"); z = coordinate(BOX_Z, "Z");
        SiloControllerEntity c = controller();
        BlockPos target = c == null || c.target() == null ? BlockPos.ZERO : c.target();
        x.setValue(oldX == null ? Integer.toString(target.getX()) : oldX);
        y.setValue(oldY == null ? Integer.toString(target.getY()) : oldY);
        z.setValue(oldZ == null ? Integer.toString(target.getZ()) : oldZ);
        set = button(SET, text("set"), b -> configure(false));
        airburst = button(AIRBURST, text("airburst.off"), b -> configure(true));
        arm = button(ARM, text("arm"), b -> send(SiloControlPayload.ARM, BlockPos.ZERO, 0));
        disarm = button(DISARM, text("disarm"), b -> send(SiloControlPayload.DISARM, BlockPos.ZERO, 0));
        set.setTooltip(Tooltip.create(text("set.hint")));
        arm.setTooltip(Tooltip.create(text("arm.hint")));
        tab(launch);
    }

    private EditBox coordinate(LayoutRect r, String axis) {
        EditBox box = new EditBox(font, leftPos + r.x(), topPos + r.y(), r.w(), r.h(), text("coordinate", axis));
        box.setMaxLength(11);
        box.setFilter(s -> s.matches("-?[0-9]*"));
        box.setTextColor(TEXT);
        addRenderableWidget(box);
        return box;
    }

    private ConsoleButton button(LayoutRect r, Component label, Button.OnPress action) {
        return addRenderableWidget(new ConsoleButton(r, label, action));
    }

    private void tab(boolean launch) {
        this.launch = launch;
        menu.inventoryVisible = !launch;
        for (EditBox box : List.of(x, y, z)) { box.visible = launch; box.setFocused(false); }
        setFocused(null);
        updateControls();
    }

    private @Nullable BlockPos enteredTarget() {
        Integer tx = ConsoleNumbers.coordinate(x.getValue()), ty = ConsoleNumbers.coordinate(y.getValue()), tz = ConsoleNumbers.coordinate(z.getValue());
        return tx == null || ty == null || tz == null ? null : new BlockPos(tx, ty, tz);
    }

    private void configure(boolean toggle) {
        SiloControllerEntity c = controller();
        BlockPos target = enteredTarget();
        if (c != null && target != null && !menu.locked())
            send(SiloControlPayload.CONFIGURE, target, (toggle != c.airburst()) ? 1 : 0);
    }

    private void send(int action, BlockPos target, int arg) {
        PacketDistributor.sendToServer(new SiloControlPayload(menu.pos, action, target, arg));
    }

    private void updateControls() {
        SiloControllerEntity c = controller();
        boolean editable = c != null && !menu.locked() && !c.interceptorMode() && !c.satelliteMode();
        boolean satellite = c != null && c.satelliteMode();
        for (EditBox box : List.of(x, y, z)) { box.setEditable(editable); box.visible = launch && !satellite; }
        for (ConsoleButton button : List.of(set, airburst, arm, disarm)) button.visible = launch;
        // A satellite's fairing has no target and is never armed: the table aims and launches it.
        set.visible = airburst.visible = arm.visible = disarm.visible = launch && !satellite;
        set.active = airburst.active = editable && enteredTarget() != null;
        airburst.setMessage(text(c != null && c.airburst() ? "airburst.on" : "airburst.off"));
        arm.active = c != null && !c.interceptorMode() && !c.satelliteMode() && c.state() == SiloControllerEntity.State.READY;
        disarm.active = c != null && !c.interceptorMode() && !c.satelliteMode() && (c.state() == SiloControllerEntity.State.ARMED || c.state() == SiloControllerEntity.State.COUNTDOWN);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        g.pose().pushPose();
        g.pose().translate(leftPos, topPos, 0);
        g.fill(0, 0, WIDTH, HEIGHT, 0xFF131F25);
        if (launch ? launchAvailable : rocketAvailable)
            g.blit(launch ? LAUNCH_TEXTURE : ROCKET_TEXTURE, 0, 0, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
        else {
            for (LayoutRect r : COMMON) panel(g, r);
            for (LayoutRect r : launch ? LAUNCH : ROCKET) panel(g, r);
        }
        line(g, title, TITLE.x() + 4, TITLE.y() + 2, TITLE.w() - 32, TEXT);
        SiloControllerEntity c = controller();
        int state = c == null ? 0 : c.state().ordinal();
        g.fill(STATUS.x() + 2, STATUS.y() + 2, STATUS.x() + 12, STATUS.y() + 12, LAMPS[state]);
        sprite(g, STATUS.x() + 2, STATUS.y() + 2, SPRITE_LAMPS.x() + state * 10, SPRITE_LAMPS.y(), 10, 10);
        small(g, c == null ? text("waiting") : c.status(), STATUS.x() + 16, STATUS.y() + 4, STATUS.w() - 19, TEXT);
        LaunchDiagram diagram = ClientLaunchDiagrams.at(menu.pos);
        drawDiagram(g, diagram);
        if (launch) drawLaunch(g, diagram, c);
        else drawRocket(g, diagram);
        g.pose().popPose();
    }

    private void drawDiagram(GuiGraphics g, LaunchDiagram d) {
        boolean siteError = d.errorWhere() != LaunchDiagram.WHERE_NONE && d.errorWhere() != LaunchDiagram.WHERE_CELL;
        line(g, siteError ? text("where." + d.errorWhere()) : text("diagram"), DIAGRAM.x() + 4, DIAGRAM.y() + 3, DIAGRAM.w() - 8, siteError ? ERROR_COLOUR : MUTED);
        for (int i = 0; i < Math.min(d.column().size(), CELLS); i++) drawCell(g, d.column().get(i), i, d);
        // Some producers include the ghost in the column; others describe it only through ADD advice.
        for (LaunchDiagram.Advice a : d.advice()) {
            int i = a.index();
            if (a.action() == LaunchDiagram.Advice.ADD && i >= d.column().size() && i < CELLS) {
                int section = Integer.numberOfTrailingZeros(a.expectedMask() & 15);
                drawCell(g, new LaunchDiagram.Cell((byte) Math.min(3, section), (byte) -1, (byte) 0, LaunchDiagram.Cell.GHOST), i, d);
            }
        }
        int bx = COL_CX - BASE_W / 2;
        if (d.site() != LaunchDiagram.SITE_NONE) {
            boolean error = d.errorWhere() == LaunchDiagram.WHERE_BASE;
            g.fill(bx, baseY(), bx + BASE_W, baseY() + BASE_H, error && blink() ? ERROR_COLOUR : 0xFF6A8087);
            LayoutRect base = d.site() == LaunchDiagram.SITE_SILO ? SPRITE_BASE_SILO : SPRITE_BASE_PLATFORM;
            sprite(g, bx, baseY(), base.x(), base.y(), BASE_W, BASE_H);
            if (error && blink()) g.fill(bx, baseY(), bx + BASE_W, baseY() + 2, ERROR_COLOUR);
        }
    }

    private static boolean blink() { return Util.getMillis() / 400 % 2 == 0; }

    private void drawCell(GuiGraphics g, LaunchDiagram.Cell c, int index, LaunchDiagram d) {
        int x = cellX(), y = cellY(index);
        boolean ghost = c.has(LaunchDiagram.Cell.GHOST);
        boolean error = c.has(LaunchDiagram.Cell.ERROR) || d.errorWhere() == LaunchDiagram.WHERE_CELL && d.errorCell() == index;
        // Charge errors must stay visible even when that cell is also selected.
        error |= d.error().startsWith("charge_") && c.section() == RocketComposition.Section.WARHEAD.ordinal();
        int state = error && blink() ? CELL_ERROR : ghost ? CELL_GHOST : menu.selected() == index ? CELL_SELECTED : CELL_NORMAL;
        int colour = c.stage() >= 0 && c.stage() < STAGE_COLOURS.length ? STAGE_COLOURS[c.stage()] : PAYLOAD_COLOUR;
        if (!ghost) {
            g.fill(BRACKET_X, y, BRACKET_X + BRACKET_W, y + CELL_H, colour);
            g.fill(BRACKET_X, y, BRACKET_X + 9, y + 1, colour);
            boolean first = index == 0 || d.column().get(index - 1).stage() != c.stage();
            if (first) small(g, c.stage() >= 0 && c.stage() < STAGE_COLOURS.length ? text("stage.short", c.stage() + 1) : text("payload.short"),
                    BRACKET_X + 10, y + 2, cellX() - BRACKET_X - 11, colour);
        }
        int border = state == CELL_ERROR ? ERROR_COLOUR : state == CELL_SELECTED ? ACCENT : ghost ? 0xFF577077 : colour;
        g.fill(x, y, x + CELL_W, y + CELL_H, border);
        g.fill(x + 1, y + 1, x + CELL_W - 1, y + CELL_H - 1, ghost ? 0xFF17262D : 0xFF30434D);
        if (c.section() >= 0 && c.section() < RocketComposition.Section.values().length) {
            int tint = state == CELL_ERROR || state == CELL_SELECTED ? 0xFFFFFFFF : ghost ? GHOST_COLOUR : colour;
            if (c.section() == RocketComposition.Section.KILL_VEHICLE.ordinal()) tintedSprite(g, x, y,
                    SPRITE_CELL_KILL_VEHICLE.x() + state * CELL_W, SPRITE_CELL_KILL_VEHICLE.y(), CELL_W, CELL_H, tint);
            else if (c.section() == RocketComposition.Section.SATELLITE.ordinal()) tintedSprite(g, x, y,
                    SPRITE_CELL_SATELLITE.x() + state * CELL_W, SPRITE_CELL_SATELLITE.y(), CELL_W, CELL_H, tint);
            else tintedSprite(g, x, y, SPRITE_CELLS.x() + state * CELL_W, SPRITE_CELLS.y() + c.section() * CELL_H, CELL_W, CELL_H, tint);
            if (!widgetsAvailable) {
                // Distinct silhouettes keep the cutaway legible before the sprite sheet arrives.
                int inset = c.section() == 3 ? 6 : c.section() == 0 ? 3 : 4;
                g.fill(x + inset, y + 2, x + CELL_W - inset, y + CELL_H - 2, border);
                if (c.section() == 2) g.fill(x + 2, y + 4, x + CELL_W - 2, y + 5, border);
            }
        }
        if (!ghost && (c.section() == 1 || c.has(LaunchDiagram.Cell.LOADED))) {
            int fill = Math.clamp((int) c.fill(), 0, 100);
            g.fill(LABEL_X, y + 3, LABEL_X + 24, y + 7, 0xFF30434D);
            g.fill(LABEL_X, y + 3, LABEL_X + 24 * fill / 100, y + 7, c.has(LaunchDiagram.Cell.LIQUID) ? 0xFF4FA3D9 : colour);
        }
    }

    private void drawRocket(GuiGraphics g, LaunchDiagram d) {
        int sx = STAGES.x(), sy = STAGES.y();
        int[] columns = {C_STAGE, C_TANKS, C_TWR, C_DV, C_BURN, C_REACH};
        String[] headers = {"stage", "tanks", "twr", "dv", "burn", "reach"};
        for (int i = 0; i < columns.length; i++)
            dense(g, text("table." + headers[i]), sx + columns[i], sy + 1, i + 1 < columns.length ? columns[i + 1] - columns[i] - 2 : 45, MUTED);
        List<LaunchDiagram.StageInfo> rows = stages.rows(d);
        for (int i = 0; i < Math.min(MAX_TABLE_STAGES, rows.size()); i++) {
            LaunchDiagram.StageInfo s = rows.get(i);
            int yy = sy + STAGE_HEADER + i * STAGE_ROW;
            g.fill(sx + 1, yy, STAGES.right() - 1, yy + STAGE_ROW, i % 2 == 0 ? 0x552F4650 : 0x222F4650);
            String[] values = {Integer.toString(i + 1), s.tanks() + "× " + s.fill() + "%", ConsoleNumbers.number(s.twr()), ConsoleNumbers.number(s.deltaV()), ConsoleNumbers.number(s.burnSeconds()), ConsoleNumbers.number(s.reachUpTo())};
            for (int j = 0; j < columns.length; j++)
                dense(g, Component.literal(values[j]), sx + columns[j], yy + 1, j + 1 < columns.length ? columns[j + 1] - columns[j] - 2 : 45, j == 0 ? STAGE_COLOURS[i] : TEXT);
            int barWidth = C_TWR - C_TANKS - 3;
            g.fill(sx + C_TANKS, yy + 9, sx + C_TANKS + barWidth, yy + 11, 0xFF30434D);
            g.fill(sx + C_TANKS, yy + 9, sx + C_TANKS + barWidth * Math.clamp((int) s.fill(), 0, 100) / 100, yy + 11, s.propellant() == 1 ? 0xFF4FA3D9 : 0xFFE0883C);
        }
        if (rows.isEmpty()) small(g, text("stages.empty"), sx + 5, sy + 20, STAGES.w() - 10, MUTED);
        RocketComposition.Section selected = menu.selectedSection();
        if (selected == null) small(g, text("select"), SECTION.x() + 4, SECTION.y() + 2, SECTION.w() - 34, ACCENT);
        else line(g, text("selected", menu.selected() + 1, sectionName(selected.ordinal())), SECTION.x() + 4, SECTION.y() + 2, SECTION.w() - 34, ACCENT);
        int active = selected == null ? 0 : WarheadBay.activeSlots(selected);
        for (int i = active; i < SLOT_X.length; i++)
            g.fill(SLOT_X[i] - 1, SLOT_Y[i] - 1, SLOT_X[i] + 17, SLOT_Y[i] + 17, 0xFF2A3221);
        if (selected != null) {
            for (int i = 0; i < WarheadBay.activeSlots(selected); i++) {
                int xx = SLOT_X[i], yy = SLOT_Y[i];
                g.fill(xx - 1, yy - 1, xx + 17, yy + 17, menu.locked() ? 0xFF765E4A : 0xFF617881);
                g.fill(xx, yy, xx + 16, yy + 16, 0xFF101C22);
                if (i < menu.slots.size() && menu.slots.get(i).getItem().isEmpty()) drawGhost(g, selected, i, xx, yy);
            }
            drawSectionStats(g, d, selected);
        }
        ConsoleDraw.wrap(g, font, d.advice().isEmpty() ? text(menu.locked() ? "locked" : "advice.ready") : adviceText(d.advice().getFirst()),
                ADVICE.x() + 3, ADVICE.y() + 1, ADVICE.w() - 6, d.advice().isEmpty() ? MUTED : 0xFFE7BE79, ConsoleDraw.DENSE, 2);
        for (int row = 0; row < 4; row++) for (int col = 0; col < 9; col++) {
            int xx = INV_X + col * 18, yy = row == 3 ? HOTBAR_Y : INV_Y + row * 18;
            g.fill(xx - 1, yy - 1, xx + 17, yy + 17, 0xFF40545C);
            g.fill(xx, yy, xx + 16, yy + 16, 0xFF17262D);
        }
    }

    private static final int MAX_TABLE_STAGES = 3;

    private void drawGhost(GuiGraphics g, RocketComposition.Section section, int slot, int x, int y) {
        WarheadBay.SlotType type = WarheadBay.type(section, slot);
        if (type == null) return;
        int ghost = switch (type) {
            case CHARGE -> (int) (Util.getMillis() / 1800 % 3);
            case SECONDARY -> GHOST_LITHIUM;
            case TAMPER -> GHOST_TAMPER;
            case BOMB -> GHOST_BOMB;
            case PROPELLANT -> GHOST_PROPELLANT;
            case GUIDANCE -> GHOST_TRACKER;
            case SATELLITE -> GHOST_SATELLITE;
        };
        if (widgetsAvailable) tintedSprite(g, x, y, SPRITE_GHOSTS.x() + ghost * 16, SPRITE_GHOSTS.y(), 16, 16, 0x4D888888);
        else {
            g.fill(x + 4, y + 4, x + 12, y + 12, 0xFF354C55);
            line(g, text("slot.short." + type.name().toLowerCase(Locale.ROOT)), x + 3, y + 4, 10, MUTED);
        }
    }

    private void drawSectionStats(GuiGraphics g, LaunchDiagram d, RocketComposition.Section selected) {
        List<Component> lines = new ArrayList<>();
        SiloControllerEntity c = controller();
        if (selected == RocketComposition.Section.SATELLITE) {
            lines.add(sectionName(selected.ordinal()));
            lines.add(fairing(c));
            lines.add(orbitReach(c));
            for (int i = 0; i < lines.size(); i++) small(g, lines.get(i), SECTION_STATS.x() + 2, SECTION_STATS.y() + 2 + i * 8, SECTION_STATS.w() - 4, i == 0 ? ACCENT : TEXT);
            ConsoleDraw.wrap(g, font, satelliteText("launch_hint"), SECTION_STATS.x() + 2, SECTION_STATS.y() + 2 + lines.size() * 8, SECTION_STATS.w() - 4, MUTED,
                    ConsoleDraw.DENSE, 3);
            return;
        }
        if (selected == RocketComposition.Section.KILL_VEHICLE) {
            lines.add(text("kill_vehicle"));
            lines.add(text("mass", ConsoleNumbers.number(d.warhead().mass())));
            lines.add(guidanceName(d.guidance()));
            lines.add(text("exo_only"));
        } else if (selected == RocketComposition.Section.WARHEAD) {
            LaunchDiagram.WarheadInfo w = d.warhead();
            Warhead.Kind[] kinds = Warhead.Kind.values();
            int kind = w.kind() >= 0 && w.kind() < kinds.length ? w.kind() : 0;
            lines.add(text("warhead." + kinds[kind].name().toLowerCase(Locale.ROOT)));
            lines.add(text("power", ConsoleNumbers.number(w.power())));
            lines.add(text("radius", w.radius()));
            lines.add(text("fallout", w.fallout()));
            lines.add(text("mass", ConsoleNumbers.number(w.mass())));
        } else if (selected == RocketComposition.Section.GUIDANCE) {
            lines.add(guidanceName(d.guidance()));
            lines.add(text("cep", ConsoleNumbers.number(d.cep())));
        } else if (selected == RocketComposition.Section.TANK && menu.selected() >= 0 && menu.selected() < d.column().size()) {
            LaunchDiagram.Cell cell = d.column().get(menu.selected());
            lines.add(text(cell.has(LaunchDiagram.Cell.LIQUID) ? "liquid.short" : "solid"));
            lines.add(text("fill", cell.fill()));
        } else {
            ConsoleDraw.wrap(g, font, text("engine.hint"), SECTION_STATS.x() + 2, SECTION_STATS.y() + 2, SECTION_STATS.w() - 4, TEXT, ConsoleDraw.SMALL, 5);
        }
        for (int i = 0; i < lines.size(); i++) small(g, lines.get(i), SECTION_STATS.x() + 2, SECTION_STATS.y() + 2 + i * 8, SECTION_STATS.w() - 4, i == 0 ? ACCENT : TEXT);
    }

    private static Component satelliteText(String key, Object... args) {
        return text("satellite." + key, args);
    }

    /** What the fairing holds: the satellite's kind, or empty. */
    private static Component fairing(@Nullable SiloControllerEntity c) {
        org.papiricoh.create_nuclearindustry.missile.orbit.SatelliteKind kind = c == null ? null : c.satelliteKind();
        return satelliteText("fairing", kind == null ? satelliteText("empty")
                : Component.translatable("satellite.create_nuclearindustry.kind." + kind.name().toLowerCase(Locale.ROOT)));
    }

    /** The highest orbit the rocket reaches. */
    private static Component orbitReach(@Nullable SiloControllerEntity c) {
        org.papiricoh.create_nuclearindustry.missile.orbit.OrbitType reach = c == null ? null : c.orbitReach();
        return satelliteText("reach", reach == null ? satelliteText("reach_none")
                : Component.translatable("satellite.create_nuclearindustry.orbit." + reach.name().toLowerCase(Locale.ROOT)));
    }

    /** The rocket's delta-v against what each orbit needs, so a player sees how far off it is. */
    private static Component deltaV(SiloControllerEntity c) {
        return satelliteText("delta_v", String.format(Locale.ROOT, "%.1f", c.orbitDeltaV()), String.format(Locale.ROOT, "%.1f", c.leoDeltaV()),
                String.format(Locale.ROOT, "%.1f", c.geoDeltaV()));
    }

    private static Component adviceText(LaunchDiagram.Advice a) {
        return Component.translatable("silo.create_nuclearindustry.advice." + a.key(), a.arg());
    }

    private void drawLaunch(GuiGraphics g, LaunchDiagram d, @Nullable SiloControllerEntity c) {
        if (c != null && c.satelliteMode()) {
            // No target here: the fairing's satellite goes up from a command table, which picks its orbit and where. The
            // coordinate boxes, the arm keys and the redstone strip are painted over: none of them launches a satellite.
            g.fill(TARGET.x() + 3, TARGET.y() + 13, TARGET.right() - 3, TARGET.bottom() - 3, PANEL);
            small(g, Component.translatable("silo.create_nuclearindustry.satellite.controlled"), TARGET.x() + 5, TARGET.y() + 3, TARGET.w() - 10, MUTED);
            boolean networked = org.papiricoh.create_nuclearindustry.controlroom.link.LinkCode.complete(c.network());
            small(g, networked ? satelliteText("network", c.network()) : satelliteText("no_network"), TARGET.x() + 5, TARGET.y() + 17,
                    TARGET.w() - 10, networked ? ACCENT : 0xFFFFB347);
            ConsoleDraw.wrap(g, font, satelliteText(networked ? "launch_hint" : "network_hint"), TARGET.x() + 5, TARGET.y() + 30,
                    TARGET.w() - 10, TEXT, ConsoleDraw.SMALL, 3);
            checklist(g, c, networked);
            g.fill(REDSTONE.x() + 2, REDSTONE.y() + 2, REDSTONE.right() - 2, REDSTONE.bottom() - 2, SCREEN);
            ConsoleDraw.wrap(g, font, satelliteText("launch_steps"), REDSTONE.x() + 5, REDSTONE.y() + 4, REDSTONE.w() - 10, TEXT, ConsoleDraw.SMALL, 4);
            List<Component> lines = List.of(c.satelliteStatus(), fairing(c), orbitReach(c), deltaV(c), text("reach", ConsoleNumbers.number(d.reach())));
            for (int i = 0; i < lines.size(); i++)
                small(g, lines.get(i), READOUT.x() + 5, READOUT.y() + 5 + i * 11, READOUT.w() - 10, i == 0 ? ACCENT : c.orbitReach() == null && i == 2 ? ERROR_COLOUR : TEXT);
            countdown(g, c);
            return;
        }
        if (c != null && c.interceptorMode()) {
            ConsoleDraw.wrap(g, font, text("interceptor_controlled"), TARGET.x() + 5, TARGET.y() + 3, TARGET.w() - 10, MUTED, ConsoleDraw.SMALL, 2);
            small(g, c.pendingTarget() == null ? text("target.none") : Component.translatable(
                    "silo.create_nuclearindustry.goggles.engaging", org.papiricoh.create_nuclearindustry.missile.radar.RadarModel.designation(c.pendingTarget())),
                    READOUT.x() + 5, READOUT.y() + 5, READOUT.w() - 10, ACCENT);
            return;
        }
        small(g, text("target"), TARGET.x() + 5, TARGET.y() + 3, TARGET.w() - GPS_W - 8, MUTED);
        LayoutRect[] boxes = {BOX_X, BOX_Y, BOX_Z};
        String[] axes = {"X", "Y", "Z"};
        for (int i = 0; i < boxes.length; i++) line(g, Component.literal(axes[i]), boxes[i].right() - 9, TARGET.y() + 4, 8, ACCENT);
        List<Component> lines = List.of(text("reach", ConsoleNumbers.number(d.reach())),
                text("guidance", guidanceName(d.guidance()), ConsoleNumbers.number(d.cep())),
                text("flight", c == null ? "—" : ConsoleNumbers.number(c.flightTicks() / 20)),
                c == null || c.target() == null ? text("target.none") : text("target.xyz", c.target().getX(), c.target().getY(), c.target().getZ()),
                text("delay", ConsoleNumbers.number(d.launchDelay() / 20.0)));
        for (int i = 0; i < lines.size(); i++) small(g, lines.get(i), READOUT.x() + 5, READOUT.y() + 5 + i * 11, READOUT.w() - 10, i == 0 ? ACCENT : TEXT);
        if (c != null && c.gpsGuided()) gps(g);
        countdown(g, c);
        if (!launchAvailable) g.renderItem(Items.LEVER.getDefaultInstance(), REDSTONE.x() + 4, REDSTONE.y() + 10);
        ConsoleDraw.wrap(g, font, text("redstone"), REDSTONE.x() + 80, REDSTONE.y() + 3, REDSTONE.w() - 85, TEXT, ConsoleDraw.SMALL, 4);
    }

    /** Olive panel and dark screen of the console sheet, to paint over the parts a satellite launch does not use. */
    private static final int PANEL = 0xFF38422D, SCREEN = 0xFF071D0C;

    /** What a satellite launch still needs, where the arm keys sit: each item ticked green or crossed red. */
    private void checklist(GuiGraphics g, SiloControllerEntity c, boolean networked) {
        g.fill(ARM.x(), ARM.y(), DISARM.right(), DISARM.bottom(), 0xFF2A3221);
        g.fill(ARM.x() + 1, ARM.y() + 1, DISARM.right() - 1, DISARM.bottom() - 1, SCREEN);
        boolean[] done = {c.satelliteKind() != null, c.orbitReach() != null, networked,
                c.state() == SiloControllerEntity.State.READY || c.state() == SiloControllerEntity.State.COUNTDOWN};
        String[] keys = {"check.fairing", "check.orbit", "check.network", "check.ready"};
        for (int i = 0; i < keys.length; i++) {
            int y = ARM.y() + 3 + i * 9;
            small(g, Component.literal(done[i] ? "✔" : "✘"), ARM.x() + 4, y, 8, done[i] ? ACCENT : ERROR_COLOUR);
            small(g, satelliteText(keys[i]), ARM.x() + 13, y, DISARM.right() - ARM.x() - 16, done[i] ? TEXT : MUTED);
        }
    }

    /** Where the GPS badge sits: the target panel's top right corner, beside its heading. */
    private static final int GPS_W = 26;

    /** The GPS badge: a lit lamp and the word, while the network's communications satellites narrow the spread. */
    private void gps(GuiGraphics g) {
        int x = TARGET.right() - GPS_W - 2, y = TARGET.y() + 2;
        g.fill(x, y, x + 10, y + 10, LAMPS[2]);
        sprite(g, x, y, SPRITE_LAMPS.x() + 2 * 10, SPRITE_LAMPS.y(), 10, 10);
        small(g, satelliteText("gps"), x + 12, y + 2, GPS_W - 12, ACCENT);
    }

    private void countdown(GuiGraphics g, @Nullable SiloControllerEntity c) {
        small(g, text("countdown"), COUNTDOWN.x() + 5, COUNTDOWN.y() + 3, COUNTDOWN.w() - 10, MUTED);
        boolean counting = c != null && c.state() == SiloControllerEntity.State.COUNTDOWN;
        // The synced countdown already includes the salvo delay; do not add it twice.
        Component seconds = counting ? text("seconds", String.format(Locale.ROOT, "%.1f", Math.max(0, c.countdown()) / 20.0)) : text("standby");
        g.pose().pushPose();
        g.pose().translate(COUNTDOWN.x() + 6, COUNTDOWN.y() + 16, 0);
        g.pose().scale(2, 2, 1);
        line(g, seconds, 0, 0, (COUNTDOWN.w() - 12) / 2, counting ? ERROR_COLOUR : 0xFFFFB347);
        g.pose().popPose();
    }

    private void panel(GuiGraphics g, LayoutRect r) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), 0xFF425961);
        g.fill(r.x() + 1, r.y() + 1, r.right() - 1, r.bottom() - 1, 0xFF1B2B33);
    }

    private void sprite(GuiGraphics g, int x, int y, int u, int v, int w, int h) {
        if (widgetsAvailable) g.blit(WIDGETS, x, y, u, v, w, h, WIDGETS_SIZE, WIDGETS_SIZE);
    }

    private void line(GuiGraphics g, Component value, int x, int y, int width, int colour) {
        ConsoleDraw.line(g, font, value, x, y, width, colour, 1);
    }

    private void small(GuiGraphics g, Component value, int x, int y, int width, int colour) {
        ConsoleDraw.line(g, font, value, x, y, width, colour, ConsoleDraw.SMALL);
    }

    private void dense(GuiGraphics g, Component value, int x, int y, int width, int colour) {
        ConsoleDraw.line(g, font, value, x, y, width, colour, ConsoleDraw.DENSE);
    }

    private void tintedSprite(GuiGraphics g, int x, int y, int u, int v, int w, int h, int tint) {
        // Text is buffered, whereas sheet blits are immediate. Flush before changing the shared shader colour.
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor((tint >> 16 & 255) / 255f, (tint >> 8 & 255) / 255f, (tint & 255) / 255f, (tint >>> 24) / 255f);
        sprite(g, x, y, u, v, w, h);
        g.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {}

    // The server's section slots stay active on both tabs; hide their rendering and hit boxes together on Launch.
    @Override protected void renderSlot(GuiGraphics g, Slot slot) { if (!launch) super.renderSlot(g, slot); }
    @Override protected boolean isHovering(int x, int y, int w, int h, double mx, double my) {
        return !launch && super.isHovering(x, y, w, h, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            LaunchDiagram d = ClientLaunchDiagrams.at(menu.pos);
            int index = ConsoleNumbers.cellAt(mx - leftPos, my - topPos, d.column().size());
            if (index >= 0) {
                LaunchDiagram.Cell cell = d.column().get(index);
                if (!cell.has(LaunchDiagram.Cell.GHOST) && cell.section() >= 0 && cell.section() < RocketComposition.Section.values().length)
                    send(SiloControlPayload.SELECT, BlockPos.ZERO, index);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        updateControls();
        super.render(g, mx, my, partial);
        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mx, int my) {
        List<Component> tooltip = tooltipAt(mx - leftPos, my - topPos);
        if (tooltip.isEmpty()) {
            super.renderTooltip(g, mx, my);
        } else if (menu.getCarried().isEmpty() && hoveredSlot != null && hoveredSlot.hasItem()) {
            List<Component> combined = new ArrayList<>(getTooltipFromContainerItem(hoveredSlot.getItem()));
            combined.addAll(tooltip);
            g.renderTooltip(font, combined, hoveredSlot.getItem().getTooltipImage(), hoveredSlot.getItem(), mx, my);
        } else {
            g.renderComponentTooltip(font, tooltip, mx, my);
        }
    }

    private List<Component> tooltipAt(int mx, int my) {
        LaunchDiagram d = ClientLaunchDiagrams.at(menu.pos);
        List<Component> lines = new ArrayList<>();
        if (STATUS.contains(mx, my) && controller() != null) lines.add(controller().status());
        if (launch && controller() != null && controller().gpsGuided() && !controller().satelliteMode()
                && mx >= TARGET.right() - GPS_W - 2 && mx < TARGET.right() && my >= TARGET.y() && my < TARGET.y() + 14)
            lines.add(Component.translatable("silo.create_nuclearindustry.satellite.gps"));
        int index = ConsoleNumbers.cellAt(mx, my, d.column().size());
        if (index >= 0) {
            LaunchDiagram.Cell cell = d.column().get(index);
            lines.add(text("selected", index + 1, sectionName(cell.section())));
            if (cell.stage() >= 0 && cell.stage() < MAX_TABLE_STAGES) lines.add(text("stage.number", cell.stage() + 1));
            if (cell.section() == 1) { lines.add(text(cell.has(LaunchDiagram.Cell.LIQUID) ? "liquid" : "solid")); lines.add(text("fill", cell.fill())); }
            if (d.errorCell() == index && controller() != null) lines.add(controller().status());
            for (LaunchDiagram.Advice a : d.advice()) if (a.index() == index) lines.add(adviceText(a));
            if (!cell.has(LaunchDiagram.Cell.GHOST)) lines.add(text("cell.hint"));
        } else if (DIAGRAM.contains(mx, my)) {
            for (LaunchDiagram.Advice a : d.advice()) if (a.action() == LaunchDiagram.Advice.ADD && ConsoleNumbers.cellAt(mx, my, a.index() + 1) == a.index()) lines.add(adviceText(a));
            if (my >= baseY()) lines.add(text("site." + d.site()));
        }
        if (!launch && STAGES.contains(mx, my)) {
            int row = (my - STAGES.y() - STAGE_HEADER) / STAGE_ROW;
            lines.add(text("table.hint"));
            List<LaunchDiagram.StageInfo> rows = stages.rows(d);
            if (d.stages().isEmpty() && !rows.isEmpty()) lines.add(text("stages.estimated"));
            if (my >= STAGES.y() + STAGE_HEADER && row < rows.size()) {
                LaunchDiagram.StageInfo s = rows.get(row);
                lines.add(text("stage.number", row + 1));
                lines.add(text(s.propellant() == 1 ? "liquid" : "solid"));
                lines.add(text("fill", s.fill()));
                lines.add(text("stage.stats", ConsoleNumbers.number(s.twr()), ConsoleNumbers.number(s.deltaV()), ConsoleNumbers.number(s.burnSeconds()), ConsoleNumbers.number(s.reachUpTo())));
            }
        }
        if (!launch && ADVICE.contains(mx, my)) for (LaunchDiagram.Advice a : d.advice()) lines.add(adviceText(a));
        if (!launch && menu.selectedSection() != null) for (int i = 0; i < WarheadBay.activeSlots(menu.selectedSection()); i++) {
            if (mx >= SLOT_X[i] && mx < SLOT_X[i] + 16 && my >= SLOT_Y[i] && my < SLOT_Y[i] + 16) {
                WarheadBay.SlotType type = WarheadBay.type(menu.selectedSection(), i);
                if (type != null) lines.add(text("slot." + type.name().toLowerCase(Locale.ROOT)));
                if (menu.locked()) lines.add(text("locked"));
                else if (!menu.getCarried().isEmpty()) {
                    String reason = WarheadBay.refusal(menu.selectedSection(), i, menu.getCarried());
                    if (reason != null) lines.add(Component.translatable("silo.create_nuclearindustry." + (reason.startsWith("refuse.") ? reason : "refuse." + reason)));
                    else if (i < menu.slots.size()) {
                        Slot slot = menu.slots.get(i);
                        if (!slot.mayPlace(menu.getCarried()) || slot.getItem().getCount() >= slot.getMaxStackSize(menu.getCarried()))
                            lines.add(text("slot.full"));
                    }
                }
            }
        }
        return lines;
    }

    private final class ConsoleButton extends Button {
        private final LayoutRect region;

        ConsoleButton(LayoutRect r, Component label, OnPress action) {
            super(leftPos + r.x(), topPos + r.y(), r.w(), r.h(), label, action, DEFAULT_NARRATION);
            region = r;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
            g.pose().pushPose();
            g.pose().translate(leftPos, topPos, 0);
            boolean tab = region == TAB_ROCKET || region == TAB_LAUNCH;
            boolean selected = region == TAB_LAUNCH ? launch : region == TAB_ROCKET && !launch;
            boolean armed = region == ARM && controller() != null && (controller().state() == SiloControllerEntity.State.ARMED || controller().state() == SiloControllerEntity.State.COUNTDOWN);
            int colour = !active ? 0xFF29383E : isHoveredOrFocused() ? 0xFF49666D : 0xFF304850;
            g.fill(region.x(), region.y(), region.right(), region.bottom(), selected ? 0xFF345C59 : colour);
            if (tab) {
                LayoutRect sprite = selected ? SPRITE_TAB_ACTIVE : SPRITE_TAB_IDLE;
                sprite(g, region.x(), region.y(), sprite.x(), sprite.y(), region.w(), region.h());
            } else if (region == AIRBURST) {
                boolean on = controller() != null && controller().airburst();
                sprite(g, region.x(), region.y(), SPRITE_TOGGLES.x(), SPRITE_TOGGLES.y() + (on ? region.h() : 0), region.w(), region.h());
            } else if (region == ARM || region == DISARM) {
                int state = armed ? 3 : !active ? 2 : isHoveredOrFocused() ? 1 : 0;
                sprite(g, region.x(), region.y(), SPRITE_BUTTONS.x(), SPRITE_BUTTONS.y() + state * ARM.h(), region.w(), region.h());
            }
            if (selected || isHoveredOrFocused()) g.fill(region.x(), region.bottom() - 1, region.right(), region.bottom(), ACCENT);
            Component label = armed ? text("armed") : getMessage();
            int width = font.width(label);
            if (region == AIRBURST) line(g, label, region.x() + 24, region.y() + (region.h() - 8) / 2, region.w() - 28, active ? TEXT : MUTED);
            else if (width > region.w() - 8) line(g, label, region.x() + 3, region.y() + (region.h() - 8) / 2, region.w() - 6, active || armed ? TEXT : MUTED);
            else line(g, label, region.x() + (region.w() - width) / 2, region.y() + (region.h() - 8) / 2, region.w() - 6, active || armed ? TEXT : MUTED);
            g.pose().popPose();
        }
    }
}
