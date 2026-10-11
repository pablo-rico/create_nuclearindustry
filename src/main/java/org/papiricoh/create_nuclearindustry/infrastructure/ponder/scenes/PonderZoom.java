package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.mojang.logging.LogUtils;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import org.slf4j.Logger;

import java.lang.reflect.Field;

/**
 * Camera zoom for scenes too big to read at one scale. Ponder only sets a scene's scale and
 * centre once, while it is built, but it reads them again every frame; this instruction eases
 * them to new values over a few ticks, so a scene can show a whole plant and then close in on
 * one part of it. The centre is the base plate square's middle, moved by whole blocks.
 *
 * <p>Seeking in Ponder replays the scene from its start, so a scene that zooms must call
 * {@link #start} first: that sets the opening view before any other instruction runs.
 */
final class PonderZoom extends TickingInstruction {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Field SCALE = field("scaleFactor");
    private static final Field Y_OFFSET = field("yOffset");
    private static final Field OFFSET_X = field("basePlateOffsetX");
    private static final Field OFFSET_Z = field("basePlateOffsetZ");
    private static final Field PLATE_SIZE = field("basePlateSize");

    /** Where the camera looks: scene scale, the block it centres on, and the vertical offset. */
    record View(float scale, float centerX, float centerZ, float yOffset) {}

    private final View to;
    private float fromScale;
    private float fromY;
    private float fromX;
    private float fromZ;

    private PonderZoom(View to, int ticks) {
        super(false, Math.max(1, ticks));
        this.to = to;
    }

    /** Sets the opening view at once; the first instruction of a scene that zooms. */
    static void start(SceneBuilder scene, View view) {
        scene.addInstruction(new PonderZoom(view, 1));
    }

    /** Eases to {@code view} over {@code ticks} and waits for it. */
    static void to(SceneBuilder scene, View view, int ticks) {
        scene.addInstruction(new PonderZoom(view, ticks));
        scene.idle(ticks);
    }

    @Override
    protected void firstTick(PonderScene scene) {
        super.firstTick(scene);
        fromScale = get(SCALE, scene, to.scale());
        fromY = get(Y_OFFSET, scene, to.yOffset());
        float half = plateSize(scene) / 2.0f;
        fromX = get(OFFSET_X, scene, to.centerX() - half) + half;
        fromZ = get(OFFSET_Z, scene, to.centerZ() - half) + half;
    }

    @Override
    public void tick(PonderScene scene) {
        super.tick(scene);
        float t = 1.0f - remainingTicks / (float) totalTicks;
        float eased = t * t * (3 - 2 * t);
        float half = plateSize(scene) / 2.0f;
        set(SCALE, scene, lerp(fromScale, to.scale(), eased));
        set(Y_OFFSET, scene, lerp(fromY, to.yOffset(), eased));
        set(OFFSET_X, scene, Math.round(lerp(fromX, to.centerX(), eased) - half));
        set(OFFSET_Z, scene, Math.round(lerp(fromZ, to.centerZ(), eased) - half));
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    private static int plateSize(PonderScene scene) {
        try {
            return PLATE_SIZE == null ? 0 : PLATE_SIZE.getInt(scene);
        } catch (IllegalAccessException e) {
            return 0;
        }
    }

    private static float get(Field field, PonderScene scene, float fallback) {
        if (field == null) {
            return fallback;
        }
        try {
            return field.getType() == int.class ? field.getInt(scene) : field.getFloat(scene);
        } catch (IllegalAccessException e) {
            return fallback;
        }
    }

    private static void set(Field field, PonderScene scene, float value) {
        if (field == null) {
            return;
        }
        try {
            if (field.getType() == int.class) {
                field.setInt(scene, Math.round(value));
            } else {
                field.setFloat(scene, value);
            }
        } catch (IllegalAccessException ignored) {
            // Looked up and opened once; a failure here only leaves the camera where it is.
        }
    }

    private static Field field(String name) {
        try {
            Field field = PonderScene.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("Ponder zoom unavailable: no field {} on PonderScene ({})", name, e.toString());
            return null;
        }
    }
}
