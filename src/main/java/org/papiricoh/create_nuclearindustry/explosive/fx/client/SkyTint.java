package org.papiricoh.create_nuclearindustry.explosive.fx.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.dyson.client.ClientSun;
import org.papiricoh.create_nuclearindustry.explosive.blast.ExplosionPalette;
import org.papiricoh.create_nuclearindustry.explosive.fx.model.CloudEvolution;

/**
 * How a nuclear detonation colours the sky: the fireball's orange afterglow over the sky and the clouds while it cools, the
 * brown dust over everything near the cloud for minutes after, and the sun dimmed while the cloud stands between it and the
 * player (its cap a disc of thick smoke) or its dust hangs overhead. Worked out once a frame and read by the sky colour, the
 * cloud colour ({@code ClientLevelSkyTintMixin}) and the sun's disc ({@code SunDimming}). Off with {@code explosion.skyTint};
 * the sun is left alone under a shader pack, which draws its own.
 */
public final class SkyTint {
    private static final double[] SUN = new double[3];
    private static double glow;
    private static ExplosionPalette.Rgb glowColour = ExplosionPalette.FOG_GLOW;
    private static double dust;
    private static double sun = 1;

    private SkyTint() {}

    /** Works out this frame's tint. */
    static void update(float partial) {
        glow = 0;
        dust = 0;
        sun = 1;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (!ClientConfig.explosionSkyTint || level == null || NuclearFx.nukes().isEmpty()) return;
        Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
        ClientSun.direction(level, partial, SUN);
        double hidden = 0;
        for (NuclearFx.Nuke nuke : NuclearFx.nukes()) {
            double age = nuke.age(partial);
            CloudEvolution cloud = nuke.cloud();
            double life = cloud.life(age);
            double here = NuclearFx.glow(nuke, age, eye) * nuke.palette().fogMax();
            if (here > glow) {
                glow = here;
                glowColour = nuke.palette().fog();
            }
            // Dust hanging over the land round the cloud.
            Vec3 cap = nuke.fireball(age);
            double flat = Math.hypot(eye.x - cap.x, eye.z - cap.z);
            double reach = Math.max(cloud.capRadius(age) * 2, nuke.payload().thermalRadius());
            dust = Math.max(dust, 0.35 * life * CloudEvolution.smooth((age - 15) / 40) * Math.max(0, 1 - flat / Math.max(1, reach)));
            // The cap between the player and the sun.
            if (SUN[1] > -0.05 && age > cloud.fireballSeconds()) {
                double dx = cap.x - eye.x, dy = cap.y - eye.y, dz = cap.z - eye.z;
                double along = dx * SUN[0] + dy * SUN[1] + dz * SUN[2];
                if (along > 0) {
                    double mx = dx - SUN[0] * along, my = dy - SUN[1] * along, mz = dz - SUN[2] * along;
                    double r = cloud.capRadius(age);
                    double h = Math.max(1, cloud.capHalfThickness(age));
                    // Squash height to make the cap a sphere: an ellipsoid's shadow.
                    double miss = Math.sqrt(mx * mx + my * my * (r / h) * (r / h) + mz * mz);
                    hidden = Math.max(hidden, 0.8 * life * (1 - CloudEvolution.smooth((miss - 0.6 * r) / (0.4 * r))));
                }
            }
        }
        if (!NukeRenderTypes.pack()) sun = Math.max(0.1, (1 - hidden) * (1 - 0.6 * dust));
    }

    /** What the sun's disc is dimmed by: 1 for no change. */
    public static float sunFactor() {
        return (float) sun;
    }

    /** The sky's colour {@code colour} as tinted now, or null for no change. */
    @Nullable
    public static Vec3 sky(Vec3 colour) {
        return tint(colour, 0.7, 0.5);
    }

    /** The clouds' colour, lit by the fireball more than the sky is. */
    @Nullable
    public static Vec3 clouds(Vec3 colour) {
        return tint(colour, 0.9, 0.6);
    }

    @Nullable
    private static Vec3 tint(Vec3 colour, double glowWeight, double dustWeight) {
        if (glow <= 0.005 && dust <= 0.005) return null;
        double r = colour.x, g = colour.y, b = colour.z;
        double t = Math.min(1, glow * glowWeight);
        r += (glowColour.red() - r) * t;
        g += (glowColour.green() - g) * t;
        b += (glowColour.blue() - b) * t;
        double d = Math.min(1, dust * dustWeight);
        double grey = (r + g + b) / 3 * 0.85;
        r += (grey * 1.05 - r) * d;
        g += (grey * 0.97 - g) * d;
        b += (grey * 0.86 - b) * d;
        return new Vec3(r, g, b);
    }
}
