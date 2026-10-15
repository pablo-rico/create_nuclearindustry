package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterRegistry;
import org.papiricoh.create_nuclearindustry.antimatter.trap.PenningTrapBlockEntity;
import org.papiricoh.create_nuclearindustry.energy.CreativeRfGeneratorBlockEntity;
import org.papiricoh.create_nuclearindustry.energy.EnergyRegistry;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonationEvent;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationStyle;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Small pieces for the antimatter tests: Penning traps, creative generators beside them, and a
 * record of every annihilation (its {@link NuclearDetonationEvent}), so a test can tell a trap or a
 * vial really went off rather than just vanished. Annihilations go off at the end of the level's
 * tick, so a test waits at least one more tick for one.
 *
 * <p>Tests that load a trap and then let it be must leave it empty or fed: a loaded trap without
 * energy quenches and blows up later, beside other tests.
 */
public final class AntimatterTestBuilder {
    /** Middle of {@link NuclearGameTests#SMALL_ARENA}, where a small annihilation stays inside the arena. */
    public static final BlockPos MIDDLE = new BlockPos(6, 2, 6);

    private static final List<Blast> BLASTS = new CopyOnWriteArrayList<>();
    private static boolean listening;

    /** One annihilation seen. */
    public record Blast(Vec3 centre, float radius, DetonationStyle style) {}

    private AntimatterTestBuilder() {}

    /** A small trap at {@code pos} facing north. */
    public static void smallTrap(GameTestHelper helper, BlockPos pos) {
        listen();
        helper.setBlock(pos, AntimatterRegistry.SMALL_PENNING_TRAP.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
    }

    /** A creative generator at {@code pos}, feeding whatever is around it. */
    public static void generator(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, EnergyRegistry.CREATIVE_RF_GENERATOR.get());
    }

    /** A small trap at {@code pos} with a creative generator below it. */
    public static void fedTrap(GameTestHelper helper, BlockPos pos) {
        smallTrap(helper, pos);
        generator(helper, pos.below());
    }

    @Nullable
    public static PenningTrapBlockEntity trap(GameTestHelper helper, BlockPos pos) {
        return AcceleratorTestBuilder.entity(helper, pos, PenningTrapBlockEntity.class);
    }

    @Nullable
    public static CreativeRfGeneratorBlockEntity generatorAt(GameTestHelper helper, BlockPos pos) {
        return AcceleratorTestBuilder.entity(helper, pos, CreativeRfGeneratorBlockEntity.class);
    }

    // ------------------------------------------------------------------ annihilations seen

    /** Starts recording annihilations; every test that may set one off calls it first. */
    public static synchronized void listen() {
        if (!listening) {
            listening = true;
            NeoForge.EVENT_BUS.addListener(NuclearDetonationEvent.class, event -> {
                if (event.getStyle() == DetonationStyle.ANTIMATTER) {
                    BLASTS.add(new Blast(event.getCenter(), event.getPower(), event.getStyle()));
                }
            });
        }
    }

    /** Annihilations seen within {@code reach} blocks of {@code pos} (a test position). */
    public static int blastsNear(GameTestHelper helper, BlockPos pos, double reach) {
        Vec3 at = Vec3.atCenterOf(helper.absolutePos(pos));
        int count = 0;
        for (Blast blast : BLASTS) {
            if (blast.centre().distanceTo(at) <= reach) {
                count++;
            }
        }
        return count;
    }
}
