package org.papiricoh.create_nuclearindustry.infrastructure.ponder;

import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * The guide's Ponder scenes: civil defense, weapons, the fuel cycle from ore to pellet and the two
 * fan treatments. Registered by {@link AllNuclearPonderScenes} after its own scenes.
 */
public final class GuidePonderScenes {

    private GuidePonderScenes() {}

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {}

    public static void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {}
}
