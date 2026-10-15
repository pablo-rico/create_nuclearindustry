package org.papiricoh.create_nuclearindustry.wastetransport.breach;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.wastetransport.WasteTransportConfig;

/**
 * Keeps a moving waste container irradiating and, once breached, leaking: once a second it
 * refreshes its contraption's {@link ContraptionWasteEmitter} and lets a breached container leak
 * into the ground it passes over (a cistern also loses fluid). Clients see a breached one drip.
 * It ticks even when the train's controls disable actors: shielding does not switch off.
 */
public class WasteContainerMovementBehaviour implements MovementBehaviour {
    private static final int INTERVAL = 20;

    @Override
    public boolean mustTickWhileDisabled() {
        return true;
    }

    /** Never offered as a switchable actor on train controls. */
    @Nullable
    @Override
    public ItemStack canBeDisabledVia(MovementContext context) {
        return null;
    }

    @Override
    public void tick(MovementContext context) {
        MountedWasteStorage storage = storageOf(context);
        if (context.world.isClientSide) {
            if (storage != null && storage.isBreached() && context.position != null) {
                BreachEffects.drip(context.world, context.position, context.world.random);
            }
            return;
        }
        if (!(context.world instanceof ServerLevel level) || Math.floorMod(level.getGameTime() + context.localPos.hashCode(), INTERVAL) != 0) {
            return;
        }
        AbstractContraptionEntity entity = context.contraption.entity;
        if (entity != null) {
            ContraptionWasteEmitter.update(entity);
        }
        if (storage != null && storage.isBreached() && context.position != null) {
            BlockPos where = BlockPos.containing(context.position);
            BreachEffects.leak(level, where, storage.activity());
            if (storage instanceof MountedFluidStorage fluid) {
                int amount = fluid.getFluidInTank(0).getAmount();
                storage.leak(BreachModel.fluidLeakPerSecond(amount, WasteTransportConfig.breachLeakFactor));
            }
        }
    }

    @Override
    public void stopMoving(MovementContext context) {
        if (!context.world.isClientSide && context.contraption.entity != null) {
            ContraptionWasteEmitter.forget(context.contraption.entity);
        }
    }

    @Nullable
    private static MountedWasteStorage storageOf(MovementContext context) {
        try {
            MountedItemStorage items = context.getItemStorage();
            if (items instanceof MountedWasteStorage waste) {
                return waste;
            }
            MountedFluidStorage fluids = context.getFluidStorage();
            return fluids instanceof MountedWasteStorage waste ? waste : null;
        } catch (IllegalStateException notYetSynced) {
            // A client contraption whose storage has not arrived yet.
            return null;
        }
    }
}
