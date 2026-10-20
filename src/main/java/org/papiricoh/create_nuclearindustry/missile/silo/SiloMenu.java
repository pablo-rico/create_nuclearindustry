package org.papiricoh.create_nuclearindustry.missile.silo;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.*;
import net.neoforged.neoforge.items.wrapper.EmptyItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.missile.gui.SiloLayout;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

import java.util.UUID;

/** Live section inventories behind the controller's ten slots; selection never owns items. */
public class SiloMenu extends AbstractContainerMenu {
    public static final int SECTION_SLOTS = WarheadBay.SLOTS;
    public static final int INV_START = SECTION_SLOTS, INV_END = INV_START + 36;
    public final BlockPos pos;
    /** Client only: the screen hides inventory slots on the launch tab. */
    public boolean inventoryVisible = true;
    private final Player player;
    private final ItemStackHandler mirror = new ItemStackHandler(SECTION_SLOTS);
    private int selected = -1, selectedKind = -1;
    private boolean locked;
    private UUID identity;
    private int diagramHash;
    private long lastDiagramCheck = -5;
    private boolean diagramSent;

    public SiloMenu(int id, Inventory inv, BlockPos pos) {
        super(SiloRegistry.MENU.get(), id);
        this.pos = pos.immutable();
        this.player = inv.player;
        for (int i = 0; i < SECTION_SLOTS; i++) addSlot(new ProxySlot(i));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(inventorySlot(inv, col + row * 9 + 9, SiloLayout.INV_X + col * 18, SiloLayout.INV_Y + row * 18));
        for (int col = 0; col < 9; col++) addSlot(inventorySlot(inv, col, SiloLayout.INV_X + col * 18, SiloLayout.HOTBAR_Y));
        addDataSlot(new DataSlot() {
            public int get() { return selected; }
            public void set(int value) { selected = value; }
        });
        addDataSlot(new DataSlot() {
            public int get() { return selectedKind; }
            public void set(int value) { selectedKind = value; }
        });
        addDataSlot(new DataSlot() {
            public int get() { return locked() ? 1 : 0; }
            public void set(int value) { locked = value != 0; }
        });
    }

    private Slot inventorySlot(Inventory inv, int index, int x, int y) {
        return new Slot(inv, index, x, y) {
            @Override public boolean isActive() { return !player.level().isClientSide || inventoryVisible; }
        };
    }

    public static SiloMenu read(int id, Inventory inv, RegistryFriendlyByteBuf buf) { return new SiloMenu(id, inv, buf.readBlockPos()); }
    private @Nullable SiloControllerEntity controller() {
        return player.level().getBlockEntity(pos) instanceof SiloControllerEntity c ? c : null;
    }

    /** Checks the whole prefix, so floating sections can never be edited remotely. */
    private @Nullable RocketSectionEntity sectionAt(int index) {
        SiloControllerEntity controller = controller();
        if (controller == null || index < 0 || index >= SiloControllerEntity.MAX_ROCKET) return null;
        RocketSectionEntity result = null;
        for (int i = 0; i <= index; i++) {
            BlockPos at = controller.floor().above(i + 1);
            if (!player.level().isLoaded(at) || !(player.level().getBlockEntity(at) instanceof RocketSectionEntity part)) return null;
            result = part;
        }
        return result;
    }

    public boolean select(int index) {
        if (player.level().isClientSide) return false;
        if (index == -1) { clearSelection(); return true; }
        RocketSectionEntity section = sectionAt(index);
        if (section == null) return false;
        selected = index;
        selectedKind = section.section().ordinal();
        identity = section.identity();
        return true;
    }
    private void clearSelection() { selected = -1; selectedKind = -1; identity = null; }
    public int selected() { return selected; }
    public @Nullable RocketComposition.Section selectedSection() {
        return selectedKind >= 0 && selectedKind < RocketComposition.Section.values().length ? RocketComposition.Section.values()[selectedKind] : null;
    }
    public boolean locked() {
        if (player.level().isClientSide) return locked;
        SiloControllerEntity controller = controller();
        if (controller == null) return true;
        return switch (controller.state()) { case ARMED, COUNTDOWN, LAUNCHED -> true; default -> false; };
    }

    private IItemHandler handler() {
        if (player.level().isClientSide) return mirror;
        RocketSectionEntity section = sectionAt(selected);
        return section != null && section.identity().equals(identity) ? section.contents() : EmptyItemHandler.INSTANCE;
    }

    @Override public void broadcastChanges() {
        if (!player.level().isClientSide) {
            RocketSectionEntity section = sectionAt(selected);
            if (section == null || !section.identity().equals(identity)) clearSelection();
            locked = locked();
            long now = player.level().getGameTime();
            if (now - lastDiagramCheck >= 5 && controller() != null) {
                lastDiagramCheck = now;
                LaunchDiagram diagram = LaunchDiagrams.build(controller());
                int hash = diagram.hashCode();
                if ((!diagramSent || hash != diagramHash) && player instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer, new SiloDiagramPayload(pos, diagram));
                    diagramHash = hash;
                    diagramSent = true;
                }
            }
        }
        super.broadcastChanges();
    }

    private class ProxySlot extends Slot {
        private final int bay;
        ProxySlot(int index) { super(new SimpleContainer(0), index, SiloLayout.SLOT_X[index], SiloLayout.SLOT_Y[index]); bay = index; }
        @Override public ItemStack getItem() { return handler().getStackInSlot(bay); }
        @Override public void set(ItemStack stack) {
            if (handler() instanceof IItemHandlerModifiable inventory && (player.level().isClientSide || !locked())) {
                inventory.setStackInSlot(bay, stack);
            }
        }
        @Override public ItemStack remove(int amount) { return locked() ? ItemStack.EMPTY : handler().extractItem(bay, amount, false); }
        @Override public boolean mayPlace(ItemStack stack) {
            return isActive() && !locked() && WarheadBay.accepts(selectedSection(), bay, stack) && handler().isItemValid(bay, stack)
                    && WarheadBay.limit(selectedSection(), bay, stack, handler()) > 0;
        }
        @Override public boolean mayPickup(Player player) { return isActive() && !locked(); }
        @Override public int getMaxStackSize(ItemStack stack) { return isActive() ? WarheadBay.limit(selectedSection(), bay, stack, handler()) : 0; }
        @Override public void setChanged() {
            if (!player.level().isClientSide) {
                RocketSectionEntity section = sectionAt(selected);
                if (section != null && section.identity().equals(identity))
                    section.contents().setStackInSlot(bay, section.contents().getStackInSlot(bay));
            }
        }
        @Override public boolean isActive() { return selected >= 0 && selectedSection() != null && bay < WarheadBay.activeSlots(selectedSection()); }
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (locked() || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.isActive() || !slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < INV_START) {
            if (!moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int i = 0; i < SECTION_SLOTS && !stack.isEmpty(); i++)
                if (slots.get(i).mayPlace(stack)) moved |= moveItemStackTo(stack, i, i + 1, false);
            if (!moved) return ItemStack.EMPTY;
        }
        slot.set(stack.isEmpty() ? ItemStack.EMPTY : stack);
        slot.onTake(player, stack);
        return original;
    }

    @Override public boolean stillValid(Player player) {
        return player.level().getBlockEntity(pos) instanceof SiloControllerEntity c && ServerPayloadGuard.stillValid(c, player);
    }
}
