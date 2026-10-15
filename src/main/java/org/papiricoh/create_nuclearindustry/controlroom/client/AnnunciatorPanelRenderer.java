package org.papiricoh.create_nuclearindustry.controlroom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorTheme;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorBoardLayout;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorBoardLayout.Box;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorBoardLayout.Button;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorLogic;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorPanelBlock;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorPanelBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorSeverity;

import java.util.Locale;

/**
 * Draws a whole annunciator board from its origin panel, in the board's {@link MonitorTheme}: each
 * panel's six windows, lit by their alarm state (fast flash, steady, slow flash) in the colour of
 * their severity, the first-out ringed in white, the group nameplates and the board's buttons. The
 * layout comes from {@link AnnunciatorBoardLayout}, the same one clicks are read against.
 */
public class AnnunciatorPanelRenderer implements BlockEntityRenderer<AnnunciatorPanelBlockEntity> {
    /** Drawing units per face pixel. */
    private static final float UNIT = 10;
    /** Fine decoration is left out beyond this many blocks: too small to see, and it shimmers. */
    private static final double DETAIL_DISTANCE = 24;
    private static final int FIRST_OUT = 0xFFFFFFFF;
    private static final String KEY = "create_nuclearindustry.annunciator.";

    /**
     * The colours of one style.
     *
     * @param plate        the board behind the windows
     * @param bezel        the frame round each window
     * @param off          an unlit window's glass, and its text
     * @param alarm        a lit alarm window, and its text
     * @param warning      a lit warning window, and its text
     * @param status       a lit status window, and its text
     * @param nameplate    a group nameplate, its edge and its text
     * @param buttonText   the button captions on light and dark buttons
     * @param buttons      TEST, SILENCE, ACK and RESET
     * @param frostLit     the frosted strip along the top of a lit lens, 0 for none
     */
    private record Look(int plate, int bezel, int off, int offText, int alarm, int alarmText, int warning, int warningText,
                        int status, int statusText, int nameplate, int nameplateEdge, int nameplateText, int buttonDarkText,
                        int buttonLightText, int[] buttons, int frostLit, boolean screws) {
        int lit(AnnunciatorSeverity severity) {
            return switch (severity) {
                case ALARM -> alarm;
                case WARNING -> warning;
                case STATUS -> status;
            };
        }

        int litText(AnnunciatorSeverity severity) {
            return switch (severity) {
                case ALARM -> alarmText;
                case WARNING -> warningText;
                case STATUS -> statusText;
            };
        }
    }

    /** Dark housing, frosted glass windows backlit red, amber or white, black engraved nameplates. */
    private static final Look STANDARD = new Look(0xFF2A3033, 0xFF141719, 0xFF3C423F, 0xFF838B85, 0xFFE8442E, 0xFF2A0804,
            0xFFF2B33D, 0xFF2A1A04, 0xFFEDEBDF, 0xFF1E1E1E, 0xFF111416, 0xFF4A5256, 0xFFE6E6DA, 0xFF1E1E1E, 0xFFF2F0E6,
            new int[]{0xFFE6E6DA, 0xFFF0C040, 0xFF3A4044, 0xFFC8301F}, 0, false);
    /** A plant computer's alarm page: flat colour blocks on black, cyan nameplates on a blue band. */
    private static final Look PROCESS_COMPUTER = new Look(0xFF05080C, 0xFF05080C, 0xFF0E1620, 0xFF2F5A66, 0xFFFF5555, 0xFF05080C,
            0xFFFFFF55, 0xFF05080C, 0xFF55FFFF, 0xFF05080C, 0xFF0F2C82, 0xFF0F2C82, 0xFFE8F0F2, 0xFF05080C, 0xFFE8F0F2,
            new int[]{0xFFE8F0F2, 0xFFFFFF55, 0xFF55FFFF, 0xFFFF5555}, 0, false);
    /** A 1970s board: seafoam enamel, lenses in dark cases, engraved black plates held by screws. */
    private static final Look MIMIC_BOARD = new Look(0xFFB4C5B8, 0xFF2B302D, 0xFF6E7872, 0xFF2E3431, 0xFFC8301F, 0xFFFFF6EC,
            0xFFF3B23A, 0xFF1A1200, 0xFF9BE38F, 0xFF0E2A10, 0xFF1C201E, 0xFF4E5852, 0xFFF3F1E7, 0xFF1A1200, 0xFFF3F1E7,
            new int[]{0xFFE9E6D8, 0xFFF3B23A, 0xFF2B302D, 0xFFC8301F}, 0xFFCAD7CD, true);

    private final Font font;

    public AnnunciatorPanelRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    private static Look look(MonitorTheme theme) {
        return switch (theme) {
            case STANDARD -> STANDARD;
            case PROCESS_COMPUTER -> PROCESS_COMPUTER;
            case MIMIC_BOARD -> MIMIC_BOARD;
        };
    }

    @Override
    public void render(AnnunciatorPanelBlockEntity board, float partialTick, PoseStack ms, MultiBufferSource buffers,
                       int light, int overlay) {
        if (!board.isOrigin() || board.getLevel() == null) {
            return;
        }
        int width = board.boardWidth();
        int height = board.boardHeight();
        // Create's tick clock rather than the level's game time, which stands still in a Ponder scene.
        long time = AnimationTickHolder.getTicks(board.getLevel());
        Look look = look(board.theme());
        boolean detail = detail(board);
        AnnunciatorBoardLayout layout = new AnnunciatorBoardLayout(width, height);
        ms.pushPose();
        ScreenDraw.onFrontFace(ms, board.facing());
        // The top-left corner of the plate's face, y down, in tenths of a pixel.
        ms.translate(0, height, -AnnunciatorPanelBlock.FACE_DEPTH / 16f + 0.001f);
        ms.scale(1 / (16 * UNIT), -1 / (16 * UNIT), 1 / (16 * UNIT));
        Painter p = new Painter(ms, buffers, font, board.getLevel() != Minecraft.getInstance().level);
        try {
            p.rect(0, 0, width * 16 * UNIT, height * 16 * UNIT, look.plate());
            boolean test = board.lampTest();
            for (int bx = 0; bx < width; bx++) {
                for (int by = 0; by < height; by++) {
                    AnnunciatorPanelBlockEntity panel = board.panelAt(bx, by);
                    if (panel != null) {
                        panel(p, layout, look, panel, bx, by, time, test, detail);
                    }
                }
            }
            buttons(p, layout, look, board, detail);
        } finally {
            p.finish();
        }
        ms.popPose();
    }

    private void panel(Painter p, AnnunciatorBoardLayout layout, Look look, AnnunciatorPanelBlockEntity panel, int bx, int by,
                       long time, boolean test, boolean detail) {
        AnnunciatorLogic logic = panel.logic();
        for (int i = 0; i < AnnunciatorPanelBlockEntity.WINDOWS; i++) {
            AnnunciatorPanelBlockEntity.Window window = panel.window(i);
            Box box = scaled(layout.window(bx, by, i));
            boolean linked = window.target() != null;
            boolean lit = test || linked && AnnunciatorLogic.lit(logic.state(i), time);
            p.rect(box.x0() - 3, box.y0() - 3, box.x1() + 3, box.y1() + 3, look.bezel());
            p.rect(box.x0(), box.y0(), box.x1(), box.y1(), lit ? look.lit(window.severity()) : look.off());
            if (lit && look.frostLit() != 0 && detail && window.severity() != AnnunciatorSeverity.ALARM) {
                p.rect(box.x0(), box.y0(), box.x1(), box.y0() + 3, look.frostLit());
            }
            if (panel.isFirstOut(i)) {
                p.frame(box.x0() - 3, box.y0() - 3, box.x1() + 3, box.y1() + 3, 1.5f, FIRST_OUT);
            }
            String label = !linked ? "-" : window.label().isEmpty()
                    ? Component.translatable(KEY + "short." + window.metric().name().toLowerCase(Locale.ROOT)).getString()
                    : window.label();
            label(p, label.toUpperCase(Locale.ROOT), box, lit ? look.litText(window.severity()) : look.offText());
        }
        Box plate = layout.nameplate(bx, by);
        if (plate != null) {
            Box box = scaled(plate);
            p.rect(box.x0(), box.y0(), box.x1(), box.y1(), look.nameplateEdge());
            p.rect(box.x0() + 1.5f, box.y0() + 1.5f, box.x1() - 1.5f, box.y1() - 1.5f, look.nameplate());
            float textLeft = box.x0() + 3;
            float textRight = box.x1() - 3;
            if (look.screws() && detail) {
                screw(p, box.x0() + 4, (box.y0() + box.y1()) / 2);
                screw(p, box.x1() - 4, (box.y0() + box.y1()) / 2);
                textLeft += 5;
                textRight -= 5;
            }
            String name = panel.nameplate();
            if (!name.isEmpty()) {
                p.fitted(name.toUpperCase(Locale.ROOT), (textLeft + textRight) / 2, box.y0() + (box.height() - 8 * 0.9f) / 2 + 0.5f,
                        textRight - textLeft, look.nameplateText(), 0.9f, Painter.Align.CENTER);
            }
        }
    }

    private static void screw(Painter p, float cx, float cy) {
        p.rect(cx - 2, cy - 2, cx + 2, cy + 2, 0xFF9AA39D);
        p.rect(cx - 2, cy - 0.5f, cx + 2, cy + 0.5f, 0xFF3A403C);
    }

    /** The label in a window: on one line if it reads there, else on two, shrunk to fit. */
    private void label(Painter p, String text, Box box, int color) {
        float room = box.width() - 6;
        float width = font.width(text);
        float cx = (box.x0() + box.x1()) / 2;
        float cy = (box.y0() + box.y1()) / 2;
        int split = text.indexOf(' ') < 0 ? -1 : splitAt(text);
        if (width * 0.85f <= room || split < 0) {
            float scale = Math.min(1, Math.max(Painter.MIN_SCALE, room / Math.max(1, width)));
            p.fitted(text, cx, cy - 4 * scale, room, color, scale, Painter.Align.CENTER);
            return;
        }
        String top = text.substring(0, split).trim();
        String bottom = text.substring(split).trim();
        float scale = Math.min(0.85f, room / Math.max(1, Math.max(font.width(top), font.width(bottom))));
        scale = Math.max(Painter.MIN_SCALE, Math.min(scale, (box.height() - 2) / 17));
        p.fitted(top, cx, cy - 8.5f * scale, room, color, scale, Painter.Align.CENTER);
        p.fitted(bottom, cx, cy + 0.5f * scale, room, color, scale, Painter.Align.CENTER);
    }

    /** The space nearest the middle of {@code text}. */
    private static int splitAt(String text) {
        int middle = text.length() / 2;
        int best = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == ' ' && (best < 0 || Math.abs(i - middle) < Math.abs(best - middle))) {
                best = i;
            }
        }
        return best;
    }

    private void buttons(Painter p, AnnunciatorBoardLayout layout, Look look, AnnunciatorPanelBlockEntity board, boolean detail) {
        String[] captions = {"test", "silence", "ack", "reset"};
        for (Button button : Button.values()) {
            Box box = scaled(layout.button(button));
            int color = look.buttons()[button.ordinal()];
            boolean pressed = button == Button.TEST && board.lampTest() || button == Button.SILENCE && board.silenced();
            p.rect(box.x0() - 1.5f, box.y0() - 1.5f, box.x1() + 1.5f, box.y1() + 1.5f, look.bezel());
            p.rect(box.x0(), box.y0(), box.x1(), box.y1(), color);
            if (detail && !pressed) {
                // The cap's lit top edge: a pressed (latched) button sits flush.
                p.rect(box.x0(), box.y0(), box.x1(), box.y0() + 2, mix(color, 0xFFFFFFFF, 0.3f));
            }
            int text = luminance(color) > 0.5 ? look.buttonDarkText() : look.buttonLightText();
            float room = box.width() - 3;
            String caption = Component.translatable(KEY + captions[button.ordinal()]).getString();
            float scale = Math.min(0.8f, Math.max(Painter.MIN_SCALE, room / Math.max(1, font.width(caption))));
            p.fitted(caption, (box.x0() + box.x1()) / 2, (box.y0() + box.y1()) / 2 - 4 * scale, room, text, scale, Painter.Align.CENTER);
        }
    }

    private static Box scaled(Box box) {
        return new Box(box.x0() * UNIT, box.y0() * UNIT, box.x1() * UNIT, box.y1() * UNIT);
    }

    private static double luminance(int argb) {
        return (0.2126 * (argb >> 16 & 0xFF) + 0.7152 * (argb >> 8 & 0xFF) + 0.0722 * (argb & 0xFF)) / 255;
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
        int g = Math.round((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
        int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    /** Close enough for fine decoration; always in a Ponder scene, which shows boards up close in a level of its own. */
    private static boolean detail(AnnunciatorPanelBlockEntity board) {
        Minecraft mc = Minecraft.getInstance();
        if (board.getLevel() != mc.level) {
            return true;
        }
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        Direction right = board.right();
        Vec3 centre = Vec3.atCenterOf(board.getBlockPos()).add(right.getStepX() * (board.boardWidth() - 1) / 2.0,
                (board.boardHeight() - 1) / 2.0, right.getStepZ() * (board.boardWidth() - 1) / 2.0);
        return camera.distanceToSqr(centre) < DETAIL_DISTANCE * DETAIL_DISTANCE;
    }

    @Override
    public AABB getRenderBoundingBox(AnnunciatorPanelBlockEntity board) {
        BlockPos pos = board.getBlockPos();
        BlockPos far = pos.relative(board.right(), board.boardWidth() - 1).above(board.boardHeight() - 1);
        return new AABB(pos).minmax(new AABB(far));
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}
