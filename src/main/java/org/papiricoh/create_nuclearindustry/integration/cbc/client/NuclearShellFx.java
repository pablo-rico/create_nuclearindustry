package org.papiricoh.create_nuclearindustry.integration.cbc.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.integration.cbc.NuclearCannonShellProjectile;
import org.papiricoh.create_nuclearindustry.integration.cbc.ThermonuclearCannonShellProjectile;
import org.papiricoh.create_nuclearindustry.missile.fx.RocketFxRegistry;

/**
 * A nuclear cannon shell in flight: Create Big Cannons draws the shell itself, so this only adds what a real shell leaves, a
 * thin grey trail of smoke behind it (as from a base-bleed charge), within sight. Its impact and blast are the detonation's own
 * effects. Only touched with Create Big Cannons installed.
 */
public final class NuclearShellFx {
    /** Blocks within which a shell's trail is drawn. */
    private static final double REACH = 192;

    private NuclearShellFx() {}

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;
        Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof NuclearCannonShellProjectile) && !(entity instanceof ThermonuclearCannonShellProjectile)) continue;
            if (entity.position().distanceToSqr(eye) > REACH * REACH) continue;
            if ((level.getGameTime() + entity.getId()) % 2 != 0) continue;
            Vec3 motion = entity.getDeltaMovement();
            if (motion.lengthSqr() < 0.01) continue;
            Vec3 back = entity.position().subtract(motion.normalize().scale(0.6));
            level.addParticle(RocketFxRegistry.ROCKET_SMOKE.get(), back.x, back.y, back.z, motion.x * 0.02, 0.01, motion.z * 0.02);
        }
    }
}
