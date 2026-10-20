package org.papiricoh.create_nuclearindustry.explosive.blockentity;

import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.items.IItemHandler;
import org.papiricoh.create_nuclearindustry.explosive.ThermonuclearLoad;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.AllNuclearTags;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveConfig;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry;
import org.papiricoh.create_nuclearindustry.explosive.FissileMaterial;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonation;
import org.papiricoh.create_nuclearindustry.explosive.ThermonuclearStats;

/**
 * A Thermonuclear Bomb, loaded as a Teller-Ulam device ({@link ThermonuclearStats}): one weapons-grade
 * core for its primary, lithium deuteride for its secondary and depleted uranium blocks for its tamper.
 * Without a primary it cannot count down; without a secondary it fizzles as the core alone.
 */
public class ThermonuclearBombBlockEntity extends NuclearBombBlockEntity {
    public static final int PRIMARY_SLOT = 0;
    public static final int FIRST_SECONDARY_SLOT = 1;
    public static final int SECONDARY_SLOTS = 4;
    public static final int FIRST_TAMPER_SLOT = FIRST_SECONDARY_SLOT + SECONDARY_SLOTS;
    public static final int TAMPER_SLOTS = 2;
    public static final int SLOTS = FIRST_TAMPER_SLOT + TAMPER_SLOTS;
    public static final int DEUTERIDE_PER_SLOT = ThermonuclearStats.SECONDARY_FULL / SECONDARY_SLOTS;
    public static final int TAMPER_PER_SLOT = ThermonuclearStats.TAMPER_FULL / TAMPER_SLOTS;

    public ThermonuclearBombBlockEntity(BlockPos pos, BlockState state) {
        super(ExplosiveRegistry.THERMONUCLEAR_BOMB_ENTITY.get(), pos, state, SLOTS);
    }

    /** Which part {@code slot} holds. */
    public static Part part(int slot) {
        if (slot == PRIMARY_SLOT) {
            return Part.PRIMARY;
        }
        return slot < FIRST_TAMPER_SLOT ? Part.SECONDARY : Part.TAMPER;
    }

    public enum Part {
        PRIMARY,
        SECONDARY,
        TAMPER;

        public boolean takes(ItemStack stack) {
            return switch (this) {
                case PRIMARY -> FissileMaterial.isWarheadCore(stack);
                case SECONDARY -> stack.is(ExplosiveRegistry.LITHIUM_DEUTERIDE.get());
                case TAMPER -> stack.is(AllNuclearTags.Items.THERMONUCLEAR_TAMPERS);
            };
        }
    }

    @Override
    public boolean isValidInSlot(int slot, ItemStack stack) {
        return slot >= 0 && slot < SLOTS && part(slot).takes(stack);
    }

    @Override
    public boolean isValidCharge(ItemStack stack) {
        for (Part part : Part.values()) {
            if (part.takes(stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int stackLimit(int slot, ItemStack stack) {
        return switch (part(slot)) {
            case PRIMARY -> 1;
            case SECONDARY -> DEUTERIDE_PER_SLOT;
            case TAMPER -> TAMPER_PER_SLOT;
        };
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    /** Shared by placed bombs and the identical inventory saved on bomb items. */
    public static ThermonuclearLoad load(IItemHandler contents) {
        ItemStack core = contents.getStackInSlot(PRIMARY_SLOT);
        ThermonuclearStats.Primary primary = !FissileMaterial.isWarheadCore(core) ? ThermonuclearStats.Primary.NONE
                : core.is(AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE.get()) ? ThermonuclearStats.Primary.PLUTONIUM
                : ThermonuclearStats.Primary.URANIUM;
        int secondary = 0, tamper = 0;
        for (int i = FIRST_SECONDARY_SLOT; i < Math.min(SLOTS, contents.getSlots()); i++) {
            ItemStack stack = contents.getStackInSlot(i);
            if (part(i).takes(stack)) {
                if (i < FIRST_TAMPER_SLOT) secondary += stack.getCount();
                else tamper += stack.getCount();
            }
        }
        return new ThermonuclearLoad(primary, secondary, tamper);
    }

    public ThermonuclearStats.Primary primary() { return load(inventory).primary(); }
    public int secondary() { return load(inventory).secondary(); }
    public int tamper() { return load(inventory).tamper(); }

    /** What it would go off with now, under the server's caps. */
    public ThermonuclearStats.Result stats() {
        return load(inventory).stats(ExplosiveConfig.thermonuclearCaps());
    }

    @Override
    public boolean hasCharge() {
        return primary() != ThermonuclearStats.Primary.NONE;
    }

    @Override
    public float getEstimatedPower() {
        return stats().power();
    }

    @Override
    public int getEstimatedRadius() {
        return stats().radius();
    }

    @Override
    public int getEstimatedCraterRadius() {
        ThermonuclearStats.Result stats = stats();
        return (int) Math.round(Math.min(stats.maxCrater(), stats.radius() * 0.3));
    }

    @Override
    protected int countdownTicks() {
        return ExplosiveConfig.thermonuclearCountdownSeconds * 20;
    }

    @Override
    protected int threatRadius() {
        return stats().falloutRadius();
    }

    @Override
    protected void explode(ServerLevel level, BlockPos center) {
        ThermonuclearStats.Result stats = stats();
        clearContent();
        level.removeBlock(center, false);
        NuclearDetonation.detonateThermonuclear(level, center.getCenter(), stats);
    }
}
