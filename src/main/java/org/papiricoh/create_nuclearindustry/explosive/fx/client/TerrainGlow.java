package org.papiricoh.create_nuclearindustry.explosive.fx.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.explosive.blast.ExplosionPalette;

/**
 * The light a detonation throws on the world: in the flash, the lightmap brightens towards white everywhere the sky reaches
 * (deep indoors less), and after it the fireball's own light, orange and fading as it cools, then the afterglow. Read by the
 * lightmap ({@code LightTextureGlowMixin}) as the game rebuilds it each tick. Off with {@code explosion.terrainLight} and under a
 * shader pack, whose lighting would double it.
 */
public final class TerrainGlow {
    private static float flash;
    private static float glowR, glowG, glowB;
    private static boolean on;

    private TerrainGlow() {}

    static void tick(Minecraft mc) {
        on = false;
        flash = 0;
        glowR = glowG = glowB = 0;
        if (!ClientConfig.explosionTerrainLight || NukeRenderTypes.pack() || mc.level == null || NuclearFx.nukes().isEmpty()) return;
        Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
        double strongest = 0;
        for (NuclearFx.Nuke nuke : NuclearFx.nukes()) {
            double age = nuke.age(0);
            flash = (float) Math.max(flash, NuclearFx.skyFlash(nuke, age, eye) * ClientConfig.explosionFlash);
            // Lit by the fireball as big as it looks from here: strong close by, faint far off, gone as it cools.
            double near = NuclearFx.apparent(nuke, age, eye);
            double heat = nuke.cloud().fireballHeat(age);
            double light = near * (0.55 * heat + 0.25 * NuclearFx.afterglow(nuke, age));
            if (light > strongest) {
                strongest = light;
                ExplosionPalette.Rgb c = heat > 0.05 ? ExplosionPalette.fire(Math.max(0.3, heat)) : nuke.palette().glow();
                glowR = (float) (c.red() * light * 0.5);
                glowG = (float) (c.green() * light * 0.5);
                glowB = (float) (c.blue() * light * 0.5);
            }
        }
        on = flash > 0.005f || strongest > 0.005;
    }

    /** Adds the glow to one lightmap texel, lit by sky light {@code sky} and block light {@code block} (0-15 each). */
    public static void apply(int sky, int block, Vector3f colours) {
        if (!on) return;
        float s = sky / 15f;
        float reach = 0.15f + 0.85f * s;
        float white = flash * reach;
        colours.x = Math.min(1, colours.x + white + glowR * reach);
        colours.y = Math.min(1, colours.y + white + glowG * reach);
        colours.z = Math.min(1, colours.z + white + glowB * reach);
    }

    static void reset() {
        on = false;
    }
}
