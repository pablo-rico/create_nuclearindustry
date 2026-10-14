package org.papiricoh.create_nuclearindustry;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Per-player display options; never read on a dedicated server. */
public class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue CONTAMINATION_PARTICLES = BUILDER
            .comment("Show dust over contaminated ground.")
            .define("contamination.particles", true);
    private static final ModConfigSpec.DoubleValue PARTICLE_DENSITY = BUILDER
            .comment("Multiplier on how much dust contaminated ground shows.")
            .defineInRange("contamination.particleDensity", 1.0, 0.0, 4.0);

    private static final ModConfigSpec.BooleanValue DOSE_HUD = BUILDER
            .comment("Show the dose HUD while you are exposed or still carry a dose.")
            .define("hud.enabled", true);
    private static final ModConfigSpec.EnumValue<HudCorner> HUD_CORNER = BUILDER
            .comment("Screen corner for the dose HUD.")
            .defineEnum("hud.corner", HudCorner.TOP_LEFT);

    private static final ModConfigSpec.BooleanValue PANEL_HUD = BUILDER
            .comment("Show the panel on the right of the screen while looking at a reactor, a tokamak, a control module, a console or an Advanced Redstone Link.")
            .define("hud.panel", true);

    private static final ModConfigSpec.DoubleValue EXPLOSION_FLASH = BUILDER
            .comment("How bright a nuclear detonation's flash is on screen, 0 (none) to 1 (full). Lower it if bright flashes bother you.")
            .defineInRange("explosion.flash", 1.0, 0.0, 1.0);
    private static final ModConfigSpec.DoubleValue EXPLOSION_CLOUD_DETAIL = BUILDER
            .comment("How many puffs a mushroom cloud is drawn with: 0.25 is light on slow machines, 2 is denser.")
            .defineInRange("explosion.cloudDetail", 1.0, 0.25, 2.0);
    private static final ModConfigSpec.BooleanValue EXPLOSION_SHAKE = BUILDER
            .comment("Shake the view when a nuclear detonation's shock wave passes.")
            .define("explosion.shake", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean contaminationParticles = true;
    public static double particleDensity = 1.0;
    public static boolean doseHud = true;
    public static HudCorner hudCorner = HudCorner.TOP_LEFT;
    public static boolean panelHud = true;
    public static double explosionFlash = 1.0;
    public static double explosionCloudDetail = 1.0;
    public static boolean explosionShake = true;

    public enum HudCorner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT;

        public boolean right() {
            return this == TOP_RIGHT || this == BOTTOM_RIGHT;
        }

        public boolean bottom() {
            return this == BOTTOM_LEFT || this == BOTTOM_RIGHT;
        }
    }

    static void onLoad(ModConfigEvent.Loading event) {
        bake(event);
    }

    static void onReload(ModConfigEvent.Reloading event) {
        bake(event);
    }

    private static void bake(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        contaminationParticles = CONTAMINATION_PARTICLES.get();
        particleDensity = PARTICLE_DENSITY.get();
        doseHud = DOSE_HUD.get();
        hudCorner = HUD_CORNER.get();
        panelHud = PANEL_HUD.get();
        explosionFlash = EXPLOSION_FLASH.get();
        explosionCloudDetail = EXPLOSION_CLOUD_DETAIL.get();
        explosionShake = EXPLOSION_SHAKE.get();
    }
}
