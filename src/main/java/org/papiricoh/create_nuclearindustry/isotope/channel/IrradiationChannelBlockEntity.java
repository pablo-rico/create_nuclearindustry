package org.papiricoh.create_nuclearindustry.isotope.channel;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements;
import org.papiricoh.create_nuclearindustry.advancement.NuclearEvent;
import org.papiricoh.create_nuclearindustry.isotope.IsotopeConfig;
import org.papiricoh.create_nuclearindustry.isotope.IsotopeRegistry;
import org.papiricoh.create_nuclearindustry.isotope.Isotopes;
import org.papiricoh.create_nuclearindustry.isotope.physics.NeutronActivation;
import org.papiricoh.create_nuclearindustry.multiblock.ControllerLink;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;

import java.util.List;
import java.util.Locale;

/**
 * The irradiation channel's capsule: up to sixteen cobalt ingots irradiated together. Every tick
 * of a running reactor adds its neutron level to the capsule's fluence; once the fluence activates
 * the batch ({@link NeutronActivation}), the cobalt comes out as cobalt-60 sources. Cobalt added to
 * a capsule already in the core shares its fluence, like a mixed batch. Automation inserts targets
 * on any side and takes the sources out.
 */
public class IrradiationChannelBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final int CAPACITY = 16;

    private final ControllerLink<ReactorBlockEntity> link = new ControllerLink<>(this, ReactorBlockEntity.class,
            (reactor, pos) -> reactor.getStructure().map(structure -> structure.isShell(pos)).orElse(false));
    private final ItemStackHandler targets = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return CAPACITY;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isTarget(stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            int before = getStackInSlot(0).getCount();
            ItemStack rest = super.insertItem(slot, stack, simulate);
            int added = stack.getCount() - rest.getCount();
            if (!simulate && added > 0 && before > 0) {
                fluence *= before / (double) (before + added);
            }
            return rest;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (getStackInSlot(0).isEmpty()) {
                fluence = 0;
            }
            setChanged();
        }
    };
    private final ItemStackHandler sources = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return CAPACITY;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** Targets go in, sources come out: what pipes and funnels see. */
    private final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? targets.getStackInSlot(0) : sources.getStackInSlot(0);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == 0 ? targets.insertItem(0, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 1 ? sources.extractItem(0, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return CAPACITY;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && isTarget(stack);
        }
    };
    private double fluence;
    private double neutronLevel;

    public IrradiationChannelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** What the channel activates: cobalt (Co-59) into cobalt-60. */
    public static boolean isTarget(ItemStack stack) {
        return stack.is(IsotopeRegistry.COBALT_INGOT.get());
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        link.tick();
        ReactorBlockEntity reactor = link.find().orElse(null);
        neutronLevel = reactor == null ? 0 : Math.max(0, reactor.getNeutronLevel());
        ItemStack batch = targets.getStackInSlot(0);
        if (!batch.isEmpty() && sources.getStackInSlot(0).isEmpty() && neutronLevel > 0) {
            NeutronActivation.Params p = IsotopeConfig.channel();
            fluence = NeutronActivation.fluenceAfter(fluence, neutronLevel, 1);
            if (NeutronActivation.done(p, fluence)) {
                discharge(batch.getCount(), NeutronActivation.outputActivity(p, fluence));
            }
            setChanged();
        }
        updateStatus();
    }

    /** The whole batch comes out as cobalt-60 sources, dated now. */
    private void discharge(int count, double activity) {
        targets.setStackInSlot(0, ItemStack.EMPTY);
        fluence = 0;
        sources.setStackInSlot(0, Isotopes.fresh(IsotopeRegistry.COBALT_60.get(), count, level.getGameTime(), Math.min(1.0, activity)));
        NuclearAdvancements.awardNearby(level, worldPosition, NuclearAdvancements.WITNESS_RADIUS, NuclearEvent.FIRST_ISOTOPE);
        sendData();
    }

    private void updateStatus() {
        ChannelStatus status = !sources.getStackInSlot(0).isEmpty() ? ChannelStatus.READY
                : targets.getStackInSlot(0).isEmpty() ? ChannelStatus.EMPTY
                : neutronLevel > 0 ? ChannelStatus.IRRADIATING : ChannelStatus.LOADED;
        BlockState state = getBlockState();
        if (state.hasProperty(IrradiationChannelBlock.STATUS) && state.getValue(IrradiationChannelBlock.STATUS) != status) {
            level.setBlock(worldPosition, state.setValue(IrradiationChannelBlock.STATUS, status), Block.UPDATE_ALL);
        }
    }

    // ------------------------------------------------------------------ hands

    public void load(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        ItemStack rest = targets.insertItem(0, held.copy(), false);
        if (!player.getAbilities().instabuild) {
            player.setItemInHand(hand, rest);
        }
    }

    public void unload(Player player) {
        ItemStack taken = sources.extractItem(0, CAPACITY, false);
        if (taken.isEmpty() && player.isShiftKeyDown()) {
            taken = targets.extractItem(0, CAPACITY, false);
        }
        if (!taken.isEmpty() && !player.getInventory().add(taken)) {
            player.drop(taken, false);
        }
    }

    public void dropContents() {
        if (level == null) {
            return;
        }
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 0.5, z = worldPosition.getZ() + 0.5;
        Containers.dropItemStack(level, x, y, z, targets.getStackInSlot(0));
        Containers.dropItemStack(level, x, y, z, sources.getStackInSlot(0));
    }

    public IItemHandler automation() {
        return automation;
    }

    /** Share of the required fluence the batch has had, 0..1. */
    public double progress() {
        return NeutronActivation.progress(IsotopeConfig.channel(), fluence);
    }

    public double fluence() {
        return fluence;
    }

    /** Game tests: sets the batch's fluence. */
    public void setFluence(double fluence) {
        this.fluence = fluence;
    }

    public int comparatorSignal() {
        if (!sources.getStackInSlot(0).isEmpty()) {
            return 15;
        }
        return targets.getStackInSlot(0).isEmpty() ? 0 : Mth.clamp((int) Math.floor(progress() * 14) + 1, 1, 14);
    }

    @Nullable
    public ReactorBlockEntity reactor() {
        return link.find().orElse(null);
    }

    // ------------------------------------------------------------------ goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        String k = "create_nuclearindustry.irradiation_channel.goggles.";
        tooltip.add(Component.literal("    ").append(Component.translatable("block.create_nuclearindustry.reactor_irradiation_channel"))
                .withStyle(ChatFormatting.GOLD));
        boolean linked = link.find().isPresent();
        tooltip.add(line(Component.translatable(linked ? k + "linked" : k + "not_linked"), linked ? ChatFormatting.GREEN : ChatFormatting.RED));
        ItemStack batch = targets.getStackInSlot(0);
        ItemStack ready = sources.getStackInSlot(0);
        if (!ready.isEmpty()) {
            tooltip.add(line(Component.translatable(k + "ready", ready.getCount()), ChatFormatting.AQUA));
        } else if (batch.isEmpty()) {
            tooltip.add(line(Component.translatable(k + "empty"), ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(line(Component.translatable(k + "batch", batch.getCount(), Math.round(progress() * 100)), ChatFormatting.GRAY));
            long ticks = NeutronActivation.ticksLeft(IsotopeConfig.channel(), fluence, neutronLevel);
            tooltip.add(ticks == Long.MAX_VALUE ? line(Component.translatable(k + "no_neutrons"), ChatFormatting.YELLOW)
                    : line(Component.translatable(k + "time_left", String.format(Locale.ROOT, "%.0f", ticks / 20.0)), ChatFormatting.GREEN));
        }
        return true;
    }

    private static Component line(Component text, ChatFormatting style) {
        return Component.literal("     ").append(text).withStyle(style);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Targets", targets.serializeNBT(registries));
        tag.put("Sources", sources.serializeNBT(registries));
        tag.putDouble("Fluence", fluence);
        tag.putDouble("Neutrons", neutronLevel);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        targets.deserializeNBT(registries, tag.getCompound("Targets"));
        sources.deserializeNBT(registries, tag.getCompound("Sources"));
        fluence = tag.getDouble("Fluence");
        neutronLevel = tag.getDouble("Neutrons");
    }
}
