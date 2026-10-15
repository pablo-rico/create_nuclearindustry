package org.papiricoh.create_nuclearindustry.wastetransport.cask;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.contraption.storage.SyncedMountedStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.contraption.storage.item.WrapperMountedItemStorage;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.foundation.codec.CreateCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.wastetransport.WasteCargo;
import org.papiricoh.create_nuclearindustry.wastetransport.WasteTransportRegistry;
import org.papiricoh.create_nuclearindustry.wastetransport.breach.MountedWasteStorage;

/**
 * One cask block riding a contraption: its slots and the cask's breach. Not tagged internal, so
 * Portable Storage Interfaces and train schedule cargo conditions see the cargo; the slots keep the
 * cask's filter, so nothing but waste goes in on the way. It cannot be opened by hand while moving.
 */
public class WasteCaskMountedStorage extends WrapperMountedItemStorage<WasteCaskInventory>
        implements SyncedMountedStorage, MountedWasteStorage {
    public static final MapCodec<WasteCaskMountedStorage> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            CreateCodecs.ITEM_STACK_HANDLER.fieldOf("items").forGetter(storage -> storage.wrapped),
            Codec.FLOAT.optionalFieldOf("breach", 0.0f).forGetter(WasteCaskMountedStorage::breachDamage)
    ).apply(i, (ItemStackHandler items, Float breach) -> new WasteCaskMountedStorage(WasteCaskInventory.copyOf(items), breach)));

    private float breach;
    private boolean dirty;

    public WasteCaskMountedStorage(WasteCaskInventory inventory, float breach) {
        this(WasteTransportRegistry.CASK_STORAGE.get(), inventory, breach);
    }

    protected WasteCaskMountedStorage(MountedItemStorageType<?> type, WasteCaskInventory inventory, float breach) {
        super(type, inventory);
        this.breach = breach;
        inventory.setOnChange(() -> dirty = true);
    }

    public static WasteCaskMountedStorage fromCask(WasteCaskBlockEntity cask) {
        return new WasteCaskMountedStorage(WasteCaskInventory.copyOf(cask.getInventoryOfBlock()), cask.breachDamage());
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (be instanceof WasteCaskBlockEntity cask) {
            cask.applyMounted(wrapped, breach);
        }
    }

    @Override
    public boolean handleInteraction(ServerPlayer player, Contraption contraption, StructureTemplate.StructureBlockInfo info) {
        return false;
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        super.setStackInSlot(slot, stack);
        dirty = true;
    }

    // ------------------------------------------------------------------ waste

    @Override
    public double activity() {
        return WasteCargo.activity(wrapped);
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
    public void afterSync(Contraption contraption, BlockPos localPos) {}
}
