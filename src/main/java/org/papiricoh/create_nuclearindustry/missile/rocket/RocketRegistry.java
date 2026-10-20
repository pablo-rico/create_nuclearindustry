package org.papiricoh.create_nuclearindustry.missile.rocket;

import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.api.registry.CreateRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightEvents;

/**
 * The rocket as a Create contraption: its contraption type, the entity that shows it in flight and the one that shows a spent
 * stage falling away. Neither entity is ever saved: the flight is the truth, and the tracker shows them near players.
 */
public final class RocketRegistry {
    public static final DeferredRegister<ContraptionType> CONTRAPTION_TYPES =
            DeferredRegister.create(CreateRegistries.CONTRAPTION_TYPE, Create_NuclearIndustry.MODID);

    public static final DeferredHolder<ContraptionType, ContraptionType> ROCKET_CONTRAPTION =
            CONTRAPTION_TYPES.register("rocket", () -> new ContraptionType(RocketContraption::new));

    public static final DeferredHolder<EntityType<?>, EntityType<RocketContraptionEntity>> ROCKET =
            AllNuclearEntities.ENTITY_TYPES.register("rocket", () -> EntityType.Builder.<RocketContraptionEntity>of(RocketContraptionEntity::new, MobCategory.MISC)
                    .sized(1, 1).noSave().fireImmune().clientTrackingRange(32).updateInterval(1).build("rocket"));

    public static final DeferredHolder<EntityType<?>, EntityType<RocketDebrisEntity>> ROCKET_DEBRIS =
            AllNuclearEntities.ENTITY_TYPES.register("rocket_debris", () -> EntityType.Builder.<RocketDebrisEntity>of(RocketDebrisEntity::new, MobCategory.MISC)
                    .sized(1, 1).noSave().fireImmune().clientTrackingRange(32).updateInterval(1).build("rocket_debris"));

    private RocketRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        CONTRAPTION_TYPES.register(modEventBus);
        FlightEvents.addListener(RocketDebrisEntity::onFlightEvent);
    }
}
