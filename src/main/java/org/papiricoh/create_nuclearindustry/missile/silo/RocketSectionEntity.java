package org.papiricoh.create_nuclearindustry.missile.silo;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.explosive.*;
import org.papiricoh.create_nuclearindustry.missile.flight.Warhead;
import org.papiricoh.create_nuclearindustry.missile.physics.GuidanceTier;
import org.papiricoh.create_nuclearindustry.missile.physics.PropellantModel;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

/**
 * Inventories belong to physical sections, never to the console. Empty payloads are inert. A tank holds either a solid propellant
 * cartridge or rocket fuel and liquid oxygen, pumped in through the silo's fuel port; never both.
 */
public class RocketSectionEntity extends BlockEntity implements IHaveGoggleInformation {
    /** Create's precision mechanism, looked up by id (Create's item entries are not on the compile classpath). */
    public static final ResourceLocation PRECISION_MECHANISM = ResourceLocation.fromNamespaceAndPath("create", "precision_mechanism");

    private UUID identity = UUID.randomUUID();
    private final List<ItemStack> migrationDrops = new ArrayList<>();
    private final ItemStackHandler contents = new ItemStackHandler(WarheadBay.SLOTS) {
        @Override public int getSlots() { return WarheadBay.activeSlots(section()); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return accepts(slot, stack); }
        @Override protected int getStackLimit(int slot, ItemStack stack) {
            return Math.min(stack.getMaxStackSize(), WarheadBay.limit(section(), slot, stack, this));
        }
        @Override protected void onContentsChanged(int slot) { changed(); }
    };
    private final FluidTank fuel = tank(true);
    private final FluidTank oxidizer = tank(false);

    public RocketSectionEntity(BlockPos pos, BlockState state) { super(SiloRegistry.SECTION_ENTITY.get(), pos, state); }

    private FluidTank tank(boolean isFuel) {
        return new FluidTank(PropellantModel.TANK_CAPACITY, stack -> section() == RocketComposition.Section.TANK
                && contents.getStackInSlot(0).isEmpty() && stack.is(isFuel ? AllNuclearFluids.ROCKET_FUEL.get() : AllNuclearFluids.LIQUID_OXYGEN.get())) {
            @Override protected void onContentsChanged() { changed(); }
        };
    }

    /** Anything held changed: saved, shown to clients, and the silo around it checks its rocket again. */
    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            MultiblockRegistry.affecting(level, worldPosition).forEach(c -> c.requestStructureRevalidation(null));
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public UUID identity() { return identity; }
    public ItemStackHandler contents() { return contents; }
    /** Rocket fuel, for tanks. */
    public FluidTank fuel() { return fuel; }
    /** Liquid oxygen, for tanks. */
    public FluidTank oxidizer() { return oxidizer; }
    public RocketComposition.Section section() { return ((RocketSectionBlock) getBlockState().getBlock()).section; }

    private boolean accepts(int slot, ItemStack stack) {
        return WarheadBay.accepts(section(), slot, stack)
                && (section() != RocketComposition.Section.TANK || fuel.isEmpty() && oxidizer.isEmpty());
    }

    /** What a guidance module holding {@code stack} steers by. */
    public static GuidanceTier tierOf(ItemStack stack) {
        if (stack.is(SiloRegistry.STAR_TRACKER.get())) return GuidanceTier.STELLAR;
        if (!stack.isEmpty() && PRECISION_MECHANISM.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) return GuidanceTier.INERTIAL;
        return GuidanceTier.BASIC;
    }

    /** What this guidance module steers by; {@link GuidanceTier#BASIC} for any other section. */
    public GuidanceTier guidanceTier() {
        return section() == RocketComposition.Section.GUIDANCE ? tierOf(contents.getStackInSlot(0)) : GuidanceTier.BASIC;
    }

    /** What this tank holds, for the propellant model; {@link PropellantModel.Tank#EMPTY} for any other section. */
    public PropellantModel.Tank propellant() {
        if (section() != RocketComposition.Section.TANK) return PropellantModel.Tank.EMPTY;
        return new PropellantModel.Tank(contents.getStackInSlot(0).is(SiloRegistry.PROPELLANT.get()), fuel.getFluidAmount(), oxidizer.getFluidAmount());
    }

    public boolean fueled() { return section() != RocketComposition.Section.TANK || propellant().fill() > 0; }

    public boolean validCharge() { return WarheadBay.validate(contents) == null; }
    public Warhead warhead() {
        return section() == RocketComposition.Section.KILL_VEHICLE || section() == RocketComposition.Section.SATELLITE ? Warhead.INERT : WarheadBay.warhead(contents);
    }
    public double payloadMass() {
        return switch (section()) {
            case KILL_VEHICLE -> org.papiricoh.create_nuclearindustry.missile.defense.DefenseConfig.siloKillVehicleMass;
            case SATELLITE -> org.papiricoh.create_nuclearindustry.missile.orbit.OrbitConfig.satelliteMass;
            default -> WarheadBay.payloadMass(contents);
        };
    }

    public void insert(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        for (int i = 0; i < contents.getSlots(); i++) {
            ItemStack rest = contents.insertItem(i, held.copyWithCount(1), false);
            if (rest.isEmpty()) { held.shrink(1); player.displayClientMessage(description(), true); return; }
        }
        boolean fits = false;
        for (int i = 0; i < WarheadBay.activeSlots(section()); i++) fits |= WarheadBay.accepts(section(), i, held);
        String reason = section() == RocketComposition.Section.TANK && (!fuel.isEmpty() || !oxidizer.isEmpty()) ? "liquid"
                : fits && FissileMaterial.isWarheadCore(held) && WarheadBay.thermonuclear(contents) ? "thermo_primary"
                : fits ? "full" : "not_for_slot";
        player.displayClientMessage(Component.translatable("silo.create_nuclearindustry.refuse." + reason), true);
    }

    public void unload(Player player) {
        for (int i = contents.getSlots() - 1; i >= 0; i--) {
            if (!contents.getStackInSlot(i).isEmpty()) {
                ItemStack out = contents.extractItem(i, contents.getStackInSlot(i).getCount(), false);
                if (!player.addItem(out)) player.drop(out, false);
                return;
            }
        }
    }

    public Component description() {
        Object detail = switch (section()) {
            case WARHEAD -> Component.translatable("silo.create_nuclearindustry.warhead." + warhead().kind().name().toLowerCase(Locale.ROOT));
            case GUIDANCE -> Component.translatable("silo.create_nuclearindustry.guidance." + guidanceTier().name().toLowerCase(Locale.ROOT));
            case TANK -> fuel.isEmpty() && oxidizer.isEmpty() ? (fueled() ? "✓" : "0")
                    : Component.translatable("silo.create_nuclearindustry.tank.liquid", fuel.getFluidAmount(), oxidizer.getFluidAmount());
            default -> "✓";
        };
        return Component.translatable("silo.create_nuclearindustry.section", Component.translatable("silo.create_nuclearindustry.section." + section().name().toLowerCase(Locale.ROOT)), detail);
    }

    /** Empties it without dropping anything: what goes with a launch. */
    public void consume() {
        dropMigrationOverflow();
        for (int i = 0; i < contents.getSlots(); i++) contents.setStackInSlot(i, ItemStack.EMPTY);
        fuel.setFluid(FluidStack.EMPTY);
        oxidizer.setFluid(FluidStack.EMPTY);
        migrationDrops.clear();
    }

    /** Drops what can be held in a hand; liquids spill away. */
    public void dropContents() {
        if (level != null && !level.isClientSide) for (int i = 0; i < contents.getSlots(); i++)
            Block.popResource(level, worldPosition, contents.getStackInSlot(i));
        dropMigrationOverflow();
        consume();
    }

    /** Deserialization can happen before a level is assigned; keep excess until it can be dropped safely. */
    @Override public void onLoad() {
        super.onLoad();
        dropMigrationOverflow();
    }

    private void dropMigrationOverflow() {
        if (level == null || level.isClientSide || migrationDrops.isEmpty()) return;
        for (ItemStack stack : migrationDrops) Block.popResource(level, worldPosition, stack);
        migrationDrops.clear();
        setChanged();
    }

    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()).withStyle(ChatFormatting.GOLD));
        if (section() == RocketComposition.Section.TANK) {
            PropellantModel.Tank tank = propellant();
            if (tank.cartridge()) tooltip.add(line(Component.translatable("silo.create_nuclearindustry.tank.solid"), ChatFormatting.GREEN));
            else {
                tooltip.add(line(Component.translatable("silo.create_nuclearindustry.tank.fuel", fuel.getFluidAmount(), fuel.getCapacity()), ChatFormatting.GRAY));
                tooltip.add(line(Component.translatable("silo.create_nuclearindustry.tank.oxidizer", oxidizer.getFluidAmount(), oxidizer.getCapacity()), ChatFormatting.GRAY));
                tooltip.add(line(Component.translatable("silo.create_nuclearindustry.tank.fill", Math.round(tank.fill() * 100)),
                        tank.fill() >= 1 ? ChatFormatting.GREEN : tank.fill() > 0 ? ChatFormatting.YELLOW : ChatFormatting.RED));
            }
        } else tooltip.add(line(description(), ChatFormatting.GRAY));
        return true;
    }

    private static Component line(Component text, ChatFormatting style) {
        return Component.literal("     ").append(text).withStyle(style);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putUUID("Identity", identity);
        tag.put("Contents", contents.serializeNBT(provider));
        tag.putInt("Layout", 2);
        if (!migrationDrops.isEmpty()) {
            ItemStackHandler overflow = new ItemStackHandler(migrationDrops.size());
            for (int i = 0; i < migrationDrops.size(); i++) overflow.setStackInSlot(i, migrationDrops.get(i));
            tag.put("MigrationDrops", overflow.serializeNBT(provider));
        }
        if (!fuel.isEmpty()) tag.put("Fuel", fuel.writeToNBT(provider, new CompoundTag()));
        if (!oxidizer.isEmpty()) tag.put("Oxidizer", oxidizer.writeToNBT(provider, new CompoundTag()));
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        identity = tag.hasUUID("Identity") ? tag.getUUID("Identity") : UUID.randomUUID();
        // Never deserialize into the live handler: old saves resize it back to five slots.
        ItemStackHandler old = new ItemStackHandler();
        old.deserializeNBT(provider, tag.getCompound("Contents"));
        for (int i = 0; i < contents.getSlots(); i++) contents.setStackInSlot(i, ItemStack.EMPTY);
        migrationDrops.clear();
        if (tag.contains("MigrationDrops")) {
            ItemStackHandler overflow = new ItemStackHandler();
            overflow.deserializeNBT(provider, tag.getCompound("MigrationDrops"));
            for (int i = 0; i < overflow.getSlots(); i++) if (!overflow.getStackInSlot(i).isEmpty())
                migrationDrops.add(overflow.getStackInSlot(i).copy());
        }
        boolean v1 = tag.getInt("Layout") < 2 && section() == RocketComposition.Section.WARHEAD;
        for (int i = 0; i < old.getSlots(); i++) {
            int to = v1 ? (i < 3 ? i : i == 3 ? 3 : i == 4 ? 7 : -1) : i;
            ItemStack stack = old.getStackInSlot(i).copy();
            if (to < 0 || to >= WarheadBay.SLOTS) continue;
            if (v1 && i == 3 && stack.is(ExplosiveRegistry.LITHIUM_DEUTERIDE.get())) {
                for (int target : WarheadBay.SECONDARY) {
                    int count = Math.min(stack.getCount(), WarheadBay.limit(section(), target, stack));
                    contents.setStackInSlot(target, stack.copyWithCount(count));
                    stack.shrink(count);
                    if (stack.isEmpty()) break;
                }
                if (!stack.isEmpty()) migrationDrops.add(stack);
            } else contents.setStackInSlot(to, stack);
        }
        fuel.setFluid(FluidStack.EMPTY);
        oxidizer.setFluid(FluidStack.EMPTY);
        if (tag.contains("Fuel")) fuel.readFromNBT(provider, tag.getCompound("Fuel"));
        if (tag.contains("Oxidizer")) oxidizer.readFromNBT(provider, tag.getCompound("Oxidizer"));
        dropMigrationOverflow();
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider) { return saveWithoutMetadata(provider); }

    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
