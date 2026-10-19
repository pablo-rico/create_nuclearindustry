package org.papiricoh.create_nuclearindustry.missile.silo;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.AllNuclearTags;
import org.papiricoh.create_nuclearindustry.explosive.*;
import org.papiricoh.create_nuclearindustry.missile.flight.Warhead;
import org.papiricoh.create_nuclearindustry.missile.physics.GuidanceTier;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import java.util.UUID;

/** Inventories belong to physical sections, never to the console. Empty payloads are inert. */
public class RocketSectionEntity extends BlockEntity {
    private UUID identity = UUID.randomUUID();
    private final ItemStackHandler contents = new ItemStackHandler(5) {
        @Override public boolean isItemValid(int slot, ItemStack stack) { return accepts(slot, stack); }
        @Override public int getSlotLimit(int slot) { return slot < 3 ? 1 : slot == 3 ? 4 : 2; }
        @Override protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide)
                MultiblockRegistry.affecting(level, worldPosition).forEach(c -> c.requestStructureRevalidation(null));
        }
    };
    public RocketSectionEntity(BlockPos pos, BlockState state) { super(SiloRegistry.SECTION_ENTITY.get(), pos, state); }
    public UUID identity() { return identity; }
    public ItemStackHandler contents() { return contents; }
    public RocketComposition.Section section() { return ((RocketSectionBlock) getBlockState().getBlock()).section; }
    private boolean accepts(int slot, ItemStack stack) {
        if (section() == RocketComposition.Section.TANK) return slot == 0 && stack.is(SiloRegistry.PROPELLANT.get());
        if (section() == RocketComposition.Section.GUIDANCE) return slot == 0 && tierOf(stack) != GuidanceTier.BASIC;
        if (section() != RocketComposition.Section.WARHEAD) return false;
        if (slot < 3) return FissileMaterial.isWeaponsFissile(stack) || DirtyWarheadStats.isRadiological(stack);
        if (slot == 3) return stack.is(ExplosiveRegistry.LITHIUM_DEUTERIDE.get());
        return stack.is(AllNuclearTags.Items.THERMONUCLEAR_TAMPERS);
    }
    /** Create's precision mechanism, looked up by id (Create's item entries are not on the compile classpath). */
    public static final net.minecraft.resources.ResourceLocation PRECISION_MECHANISM = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", "precision_mechanism");
    /** What a guidance module holding {@code stack} steers by. */
    public static GuidanceTier tierOf(ItemStack stack) {
        if (stack.is(SiloRegistry.STAR_TRACKER.get())) return GuidanceTier.STELLAR;
        if (!stack.isEmpty() && PRECISION_MECHANISM.equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()))) return GuidanceTier.INERTIAL;
        return GuidanceTier.BASIC;
    }
    /** What this guidance module steers by; {@link GuidanceTier#BASIC} for any other section. */
    public GuidanceTier guidanceTier() {
        return section() == RocketComposition.Section.GUIDANCE ? tierOf(contents.getStackInSlot(0)) : GuidanceTier.BASIC;
    }
    public boolean fueled() { return section() != RocketComposition.Section.TANK || contents.getStackInSlot(0).is(SiloRegistry.PROPELLANT.get()); }
    public boolean validCharge() {
        return contents.getStackInSlot(3).isEmpty() && contents.getStackInSlot(4).isEmpty()
                || FissileMaterial.units(contents, 0, 3) > 0;
    }
    public Warhead warhead() {
        if (!contents.getStackInSlot(3).isEmpty()) {
            boolean plutonium = false;
            for (int i = 0; i < 3; i++) plutonium |= contents.getStackInSlot(i).is(AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE.get());
            return Warhead.thermonuclear(new ThermonuclearLoad(plutonium ? ThermonuclearStats.Primary.PLUTONIUM : ThermonuclearStats.Primary.URANIUM,
                    contents.getStackInSlot(3).getCount(), contents.getStackInSlot(4).getCount()));
        }
        int units = FissileMaterial.units(contents, 0, 3);
        float salt = DirtyWarheadStats.load(contents, 0, 3);
        if (units == 0) return salt > 0 ? Warhead.dirty(salt) : Warhead.INERT;
        float enrichment = FissileMaterial.averageEnrichment(contents, 0, 3);
        return Warhead.fission(WarheadStats.power(units, enrichment), WarheadStats.horizontalRadius(units, enrichment), WarheadStats.intensity(enrichment), salt);
    }
    public double payloadMass() {
        double mass = 1;
        for (int i = 0; i < contents.getSlots(); i++) mass += contents.getStackInSlot(i).getCount() * 0.1;
        return mass;
    }
    public void insert(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        for (int i = 0; i < contents.getSlots(); i++) {
            ItemStack rest = contents.insertItem(i, held.copyWithCount(1), false);
            if (rest.isEmpty()) { held.shrink(1); player.displayClientMessage(description(), true); return; }
        }
        player.displayClientMessage(description(), true);
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
            case WARHEAD -> warhead().kind().name();
            case GUIDANCE -> Component.translatable("silo.create_nuclearindustry.guidance." + guidanceTier().name().toLowerCase(java.util.Locale.ROOT));
            default -> fueled() ? "✓" : "0";
        };
        return Component.translatable("silo.create_nuclearindustry.section", section().name(), detail);
    }
    public void consume() { for (int i = 0; i < contents.getSlots(); i++) contents.setStackInSlot(i, ItemStack.EMPTY); }
    public void dropContents() {
        if (level != null && !level.isClientSide) for (int i = 0; i < contents.getSlots(); i++)
            Block.popResource(level, worldPosition, contents.getStackInSlot(i));
        consume();
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider); tag.putUUID("Identity", identity); tag.put("Contents", contents.serializeNBT(provider));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        identity = tag.hasUUID("Identity") ? tag.getUUID("Identity") : UUID.randomUUID();
        contents.deserializeNBT(provider, tag.getCompound("Contents"));
    }
}
