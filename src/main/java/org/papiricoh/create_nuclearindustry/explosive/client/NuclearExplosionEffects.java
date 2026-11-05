package org.papiricoh.create_nuclearindustry.explosive.client;

import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Camera;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.explosive.DetonationPayload;
import org.papiricoh.create_nuclearindustry.explosive.fx.client.ConventionalFx;
import org.papiricoh.create_nuclearindustry.explosive.fx.client.NuclearFx;

/**
 * The client's nuclear detonations, for whoever needs to know of them outside the effects themselves: how many are being shown
 * (tests, the screenshot tour) and the far part of their clouds for Distant Horizons' pass. What they look, sound and feel like
 * is {@link NuclearFx}'s.
 */
public final class NuclearExplosionEffects {
    private NuclearExplosionEffects() {}

    /** A detonation the server told us of. */
    public static void add(DetonationPayload payload) {
        org.papiricoh.create_nuclearindustry.explosive.fx.client.ExplosionFxClient.onNuclear(payload);
    }

    /** How many detonations, nuclear or conventional, are being shown. */
    public static int active() {
        return NuclearFx.active() + ConventionalFx.active();
    }

    /**
     * For Distant Horizons' pass, which comes before the vanilla terrain: the clouds wholly past {@code from} blocks from the
     * camera, textured and camera-relative, far to near, or null if there are none. The vanilla pass then leaves them out.
     */
    @Nullable
    public static MeshData farMesh(float partial, Camera camera, double from) {
        return NuclearFx.farMesh(partial, camera, from);
    }
}
