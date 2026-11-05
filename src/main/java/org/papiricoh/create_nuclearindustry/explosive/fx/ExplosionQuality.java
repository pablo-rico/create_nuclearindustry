package org.papiricoh.create_nuclearindustry.explosive.fx;

/**
 * How much a player's client spends drawing explosions: how many puffs and layers a cloud has, how often its mesh is rebuilt,
 * how far its detail fades with distance and how many particles a blast throws. Read only on the client.
 */
public enum ExplosionQuality {
    LOW,
    MEDIUM,
    HIGH,
    ULTRA
}
