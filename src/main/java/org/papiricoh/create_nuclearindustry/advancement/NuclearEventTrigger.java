package org.papiricoh.create_nuclearindustry.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** One trigger for every {@link NuclearEvent}; the criterion names the event it waits for. */
public class NuclearEventTrigger extends SimpleCriterionTrigger<NuclearEventTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, NuclearEvent event) {
        trigger(player, instance -> instance.matches(event));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, String event)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.STRING.fieldOf("event").forGetter(TriggerInstance::event)
        ).apply(instance, TriggerInstance::new));

        public boolean matches(NuclearEvent event) {
            return this.event.equals(event.id());
        }

        public static Criterion<TriggerInstance> of(NuclearEvent event) {
            return NuclearAdvancements.TRIGGER.get().createCriterion(new TriggerInstance(Optional.empty(), event.id()));
        }
    }
}
