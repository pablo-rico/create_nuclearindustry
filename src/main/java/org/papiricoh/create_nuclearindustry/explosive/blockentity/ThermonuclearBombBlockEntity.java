package org.papiricoh.create_nuclearindustry.explosive.blockentity;

import net.minecraft.core.BlockPos;
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

    public ThermonuclearStats.Primary primary() {
        ItemStack core = inventory.getStackInSlot(PRIMARY_SLOT);
        if (core.is(AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE.get())) {
            return ThermonuclearStats.Primary.PLUTONIUM;
        }
        return FissileMaterial.isWarheadCore(core) ? ThermonuclearStats.Primary.URANIUM : ThermonuclearStats.Primary.NONE;
    }

    public int secondary() {
        return count(FIRST_SECONDARY_SLOT, FIRST_TAMPER_SLOT);
    }

    public int tamper() {
        return count(FIRST_TAMPER_SLOT, SLOTS);
    }

    private int count(int from, int to) {
        int count = 0;
        for (int slot = from; slot < to; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (isValidInSlot(slot, stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /** What it would go off with now, under the server's caps. */
    public ThermonuclearStats.Result stats() {
        return ThermonuclearStats.of(primary(), secondary(), tamper(), ExplosiveConfig.thermonuclearCaps());
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
