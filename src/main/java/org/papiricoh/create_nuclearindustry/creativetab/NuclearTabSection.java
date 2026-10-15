package org.papiricoh.create_nuclearindustry.creativetab;

import java.util.Locale;

/**
 * The themed sections of the creative tab, in tab order. Each opens with a banner row
 * ({@code textures/gui/creative_section/<id>.png}: {@link #FRAMES} frames of 162x18 stacked
 * vertically, drawn by {@code NuclearTabBanners}) whose title sits on a translucent box in
 * {@link #background} with {@link #text} over a {@link #shadow}. {@link #OTHER} only shows when
 * another mod adds items to the tab.
 */
public enum NuclearTabSection {
    MATERIALS(0xAA2B3326, 0xFFCFDE90, 0xFF2E3A1E, 3),
    FUEL_CYCLE(0xAA3D2B0C, 0xFFF8E07A, 0xFF5B3F12, 2),
    FISSION(0xAA10284A, 0xFFA8E4FF, 0xFF16304E, 3),
    PRIMARY_LOOP(0xAA3A2422, 0xFFEEA18A, 0xFF4C2928, 3),
    POWER(0xAA2A2F2C, 0xFFFFDA87, 0xFF613D2B, 2),
    CONTROL_ROOM(0xAA1F2824, 0xFFB8E6B0, 0xFF24302A, 4),
    SPENT_FUEL(0xAA14302F, 0xFFB3DFCF, 0xFF203B3D, 3),
    ISOTOPES(0xAA1E3A2A, 0xFFC4E793, 0xFF203C28, 3),
    SAFETY(0xAA2A2410, 0xFFF3DF92, 0xFF574023, 2),
    FUSION(0xAA2E1A36, 0xFFF6C8FF, 0xFF432344, 2),
    CRYOGENICS(0xAA22303A, 0xFFE6F6FF, 0xFF38494A, 4),
    ACCELERATOR(0xAA141D33, 0xFFC9DCF3, 0xFF1D2A45, 2),
    ANTIMATTER(0xAA221438, 0xFFD6BFFF, 0xFF2E1F45, 3),
    CIVIL_DEFENSE(0xAA262A18, 0xFFE0E6B0, 0xFF353A20, 3),
    WEAPONS(0xAA3A1A0C, 0xFFFFC080, 0xFF4C2410, 3),
    /** Nuclear engines for Create Aeronautics ships; only shown with Sable installed. */
    PROPULSION(0xAA1A2638, 0xFFFFD9A8, 0xFF2A3348, 2),
    OTHER(0xAA2A2A2A, 0xFFE0E0E0, 0xFF303030, 4);

    /** Frames in every banner sheet; tools/texturegen/tex_creative_tab.py draws the same number. */
    public static final int FRAMES = 16;
    public static final int WIDTH = 162;
    public static final int HEIGHT = 18;

    public final int background;
    public final int text;
    public final int shadow;
    /** Game ticks each frame shows while the banner is hovered. */
    public final int frametime;

    NuclearTabSection(int background, int text, int shadow, int frametime) {
        this.background = background;
        this.text = text;
        this.shadow = shadow;
        this.frametime = frametime;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String langKey() {
        return "itemGroup.create_nuclearindustry.section." + id();
    }
}
