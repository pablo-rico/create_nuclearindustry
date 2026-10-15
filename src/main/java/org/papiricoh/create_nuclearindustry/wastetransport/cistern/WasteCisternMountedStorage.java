package org.papiricoh.create_nuclearindustry.wastetransport.cistern;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.contraption.storage.SyncedMountedStorage;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType;
import com.simibubi.create.api.contraption.storage.fluid.WrapperMountedFluidStorage;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.wastetransport.WasteCargo;
import org.papiricoh.create_nuclearindustry.wastetransport.WasteTransportRegistry;
import org.papiricoh.create_nuclearindustry.wastetransport.breach.MountedWasteStorage;

/**
 * A cistern riding a contraption (a train carriage, a piston...): the controller's fluid and breach.
 * Portable Fluid Interfaces and train schedule conditions see it, but its tank still only takes the
 * radioactive fluids, so nothing else can be pumped in on the way. Synced, so clients see the
 * breach drip.
 */
public class WasteCisternMountedStorage extends WrapperMountedFluidStorage<WasteCisternMountedStorage.Handler>
        implements SyncedMountedStorage, MountedWasteStorage {
    public static final MapCodec<WasteCisternMountedStorage> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("capacity").forGetter(WasteCisternMountedStorage::getCapacity),
            FluidStack.OPTIONAL_CODEC.fieldOf("fluid").forGetter(WasteCisternMountedStorage::getFluid),
            Codec.FLOAT.optionalFieldOf("breach", 0.0f).forGetter(WasteCisternMountedStorage::breachDamage)
    ).apply(i, WasteCisternMountedStorage::new));

    private float breach;
    private boolean dirty;

    public WasteCisternMountedStorage(int capacity, FluidStack fluid, float breach) {
        this(WasteTransportRegistry.CISTERN_STORAGE.get(), capacity, fluid, breach);
    }

    protected WasteCisternMountedStorage(MountedFluidStorageType<?> type, int capacity, FluidStack fluid, float breach) {
        super(type, new Handler(capacity, fluid));
        this.breach = breach;
        wrapped.onChange = () -> dirty = true;
    }

    public static WasteCisternMountedStorage fromCistern(WasteCisternBlockEntity cistern) {
        FluidTank tank = cistern.getTankInventory();
        return new WasteCisternMountedStorage(tank.getCapacity(), tank.getFluid().copy(), cistern.breachDamage());
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (be instanceof WasteCisternBlockEntity cistern) {
            cistern.applyMounted(getFluid(), breach);
        }
    }

    public FluidStack getFluid() {
        return wrapped.getFluid();
    }

    public int getCapacity() {
        return wrapped.getCapacity();
    }

    // ------------------------------------------------------------------ waste

    @Override
    public double activity() {
        return WasteCargo.activity(getFluid());
    }

    @Override
    public float breachDamage() {
        return breach;
    }

    @Override
    public boolean markBreached() {
        boolean wasSealed = breach <= 0;
        breach = 1;
        dirty = true;
        return wasSealed;
    }

    @Override
    public void loseSpilledContents(double fraction) {
        int spilled = org.papiricoh.create_nuclearindustry.wastetransport.breach.BreachModel.fluidSpill(getFluid().getAmount(), fraction);
        if (spilled > 0) {
            wrapped.drain(spilled, FluidAction.EXECUTE);
        }
    }

    @Override
    public void leak(int millibuckets) {
        if (millibuckets > 0) {
            wrapped.drain(millibuckets, FluidAction.EXECUTE);
        }
    }

    // ------------------------------------------------------------------ syncing

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void markClean() {
        dirty = false;
    }

    @Override
    public void afterSync(Contraption contraption, BlockPos localPos) {
        if (contraption.getBlockEntityClientSide(localPos) instanceof WasteCisternBlockEntity cistern) {
            cistern.getTankInventory().setFluid(getFluid());
        }
    }

    /** The mounted tank: the cistern's own fluid filter, and a hook to mark the storage for syncing. */
    public static final class Handler extends FluidTank {
        private Runnable onChange = () -> {};

        public Handler(int capacity, FluidStack stack) {
            super(capacity, WasteCargo::acceptsFluid);
            setFluid(stack);
        }

        @Override
        protected void onContentsChanged() {
            onChange.run();
        }
    }
}
