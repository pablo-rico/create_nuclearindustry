package org.papiricoh.create_nuclearindustry;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.papiricoh.create_nuclearindustry.creativetab.NuclearTab;

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

    private static final ModConfigSpec.BooleanValue ROCKET_EFFECTS = BUILDER
            .comment("Draw rocket plumes, smoke trails, staging and re-entry effects, and play their sounds.")
            .define("rocket.effects", true);
    private static final ModConfigSpec.DoubleValue ROCKET_DETAIL = BUILDER
            .comment("How much smoke and how many plume layers rockets are drawn with: 0.25 is light on slow machines, 2 is denser.")
            .defineInRange("rocket.detail", 1.0, 0.25, 2.0);
    private static final ModConfigSpec.BooleanValue ROCKET_FAR_TRAILS = BUILDER
            .comment("Keep drawing a rocket's smoke trail and glow far beyond the distance entities are shown at.")
            .define("rocket.farTrails", true);
    private static final ModConfigSpec.IntValue ROCKET_TRAIL_SECONDS = BUILDER
            .comment("Seconds a rocket's smoke trail lingers in the sky.")
            .defineInRange("rocket.trailSeconds", 40, 5, 120);
    private static final ModConfigSpec.BooleanValue ROCKET_SHAKE = BUILDER
            .comment("Shake the view near a rocket lifting off.")
            .define("rocket.shake", true);
    private static final ModConfigSpec.BooleanValue RADAR_SOUNDS = BUILDER
            .comment("Play the radar's sweep pings, new-track tones and klaxon in its screen and near radar scopes.")
            .define("radar.sounds", true);
    private static final ModConfigSpec.BooleanValue RADAR_PHOSPHOR = BUILDER
            .comment("Draw radar scopes like a phosphor screen: echoes glow where the sweep passes and fade until it comes round again.")
            .define("radar.phosphor", true);

    private static final ModConfigSpec.BooleanValue CREATIVE_TAB_SECTIONS = BUILDER
            .comment("Split the mod's creative tab into themed sections with banners. Off, it is one list in the same order.",
                    "Takes effect the next time the tab is rebuilt (rejoin the world).")
            .define("creativeTab.sections", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean contaminationParticles = true;
    public static double particleDensity = 1.0;
    public static boolean doseHud = true;
    public static HudCorner hudCorner = HudCorner.TOP_LEFT;
    public static boolean panelHud = true;
    public static double explosionFlash = 1.0;
    public static double explosionCloudDetail = 1.0;
    public static boolean explosionShake = true;
    public static boolean rocketEffects = true;
    public static double rocketDetail = 1.0;
    public static boolean rocketFarTrails = true;
    public static int rocketTrailSeconds = 40;
    public static boolean rocketShake = true;
    public static boolean radarSounds = true;
    public static boolean radarPhosphor = true;

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
        rocketEffects = ROCKET_EFFECTS.get();
        rocketDetail = ROCKET_DETAIL.get();
        rocketFarTrails = ROCKET_FAR_TRAILS.get();
        rocketTrailSeconds = ROCKET_TRAIL_SECONDS.get();
        rocketShake = ROCKET_SHAKE.get();
        radarSounds = RADAR_SOUNDS.get();
        radarPhosphor = RADAR_PHOSPHOR.get();
        NuclearTab.bannersEnabled = CREATIVE_TAB_SECTIONS.get();
    }
}
