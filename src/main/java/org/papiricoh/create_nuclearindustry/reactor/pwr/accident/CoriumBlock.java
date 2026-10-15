package org.papiricoh.create_nuclearindustry.reactor.pwr.accident;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrConfig;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrTags;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentMonitorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Molten core. All of its state is in the block state (its remaining heat as {@code HEAT} and
 * {@code STEP}), and it acts only on scheduled ticks every {@link #TICK_INTERVAL} ticks, so it is
 * cheap, saved with its chunk and simply waits while the chunk is unloaded. Each tick
 * {@link CoriumModel} decides whether it falls, melts the block below, spreads over a core catcher,
 * rests or meets water; when its heat runs out it freezes into {@link SolidifiedCoriumBlock}.
 * Melting concrete inside a containment gives off hydrogen and steam there.
 */
public class CoriumBlock extends Block implements IWrenchable {
    public static final IntegerProperty HEAT = IntegerProperty.create("heat", 0, CoriumModel.MAX_HEAT);
    public static final IntegerProperty STEP = IntegerProperty.create("step", 0, CoriumModel.MAX_STEP);
    public static final int TICK_INTERVAL = 40;
    /** Gas a melted block gives off inside a containment: hydrogen and steam from the concrete's water, mol. */
    private static final double CONCRETE_HYDROGEN = 30.0;
    private static final double CONCRETE_STEAM = 60.0;
    private static final double GAS_TEMPERATURE = 1500.0;

    public CoriumBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HEAT, CoriumModel.MAX_HEAT).setValue(STEP, CoriumModel.MAX_STEP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HEAT, STEP);
    }

    /** Fresh corium with its whole heat budget. */
    public BlockState fresh() {
        return defaultBlockState();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, TICK_INTERVAL);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int energy = CoriumModel.energy(state.getValue(HEAT), state.getValue(STEP));
        // Aboard a ship it runs towards the real ground, whichever way the ship is tilted.
        Direction down = ShipSpace.localDown(level, pos);
        BlockPos belowPos = pos.relative(down);
        CoriumModel.Below below = classify(level, belowPos);
        if (below == CoriumModel.Below.OPEN && leavesTheShip(level, pos, down)) {
            dropOutOfTheShip(level, pos, state);
            return;
        }
        BlockPos spread = below == CoriumModel.Below.CATCHER ? spreadTarget(level, pos, random) : null;
        CoriumModel.Decision decision = CoriumModel.decide(energy, below, touchesWater(level, pos), spread != null,
                PwrConfig.blockDestruction);

        BlockPos destination = pos;
        switch (decision.action()) {
            case FALL -> destination = belowPos;
            case MELT -> {
                meltConcrete(level, belowPos, level.getBlockState(belowPos));
                destination = belowPos;
            }
            case SPREAD -> destination = spread;
            case QUENCH -> SteamExplosion.quenchCorium(level, pos);
            case REST -> {
            }
        }
        if (!level.getBlockState(pos).is(this)) {
            // the steam explosion took it away
            return;
        }
        BlockState next = decision.solidifies()
                ? PwrRegistry.SOLIDIFIED_CORIUM.get().defaultBlockState()
                : defaultBlockState().setValue(HEAT, CoriumModel.heat(decision.energy())).setValue(STEP, CoriumModel.step(decision.energy()));
        if (!destination.equals(pos)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_ALL);
            level.sendParticles(ParticleTypes.LAVA, destination.getX() + 0.5, destination.getY() + 1.0, destination.getZ() + 0.5,
                    3, 0.3, 0.1, 0.3, 0.0);
        }
        level.setBlock(destination, next, UPDATE_ALL);
        if (below == CoriumModel.Below.CATCHER && decision.solidifies()) org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements.awardNearby(level, destination, 64, org.papiricoh.create_nuclearindustry.advancement.NuclearEvent.CORIUM_CAUGHT);
        if (next.is(this)) {
            level.scheduleTick(destination, this, TICK_INTERVAL);
        }
    }

    /**
     * Through the hull: the corium leaves the ship where the ship really is and falls on to whatever
     * lies under it, or straight on to the ground if the ship rests on it.
     */
    private static void dropOutOfTheShip(ServerLevel level, BlockPos pos, BlockState state) {
        BlockPos target = BlockPos.containing(ShipSpace.toWorld(level, pos));
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_ALL);
        if (!level.getBlockState(target).isAir()) {
            target = target.above();
            if (!level.getBlockState(target).isAir()) {
                return;
            }
        }
        level.setBlock(target, state, UPDATE_ALL);
        if (level.getBlockState(target.below()).isAir()) {
            FallingBlockEntity falling = FallingBlockEntity.fall(level, target, state);
            falling.dropItem = false;
        }
    }

    /** Whether the corium is aboard a ship with nothing of it left between it and the open air below. */
    private static boolean leavesTheShip(Level level, BlockPos pos, Direction down) {
        ShipSpace.Ship ship = ShipSpace.shipAt(level, pos);
        if (ship == null) {
            return false;
        }
        net.minecraft.world.level.levelgen.structure.BoundingBox hull = ship.localBounds();
        for (BlockPos at = pos.relative(down); hull.isInside(at); at = at.relative(down)) {
            if (!level.getBlockState(at).isAir()) {
                return false;
            }
        }
        return true;
    }

    /** What the corium would meet going down into {@code pos}. */
    public static CoriumModel.Below classify(Level level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) {
            return CoriumModel.Below.RESISTANT;
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof CoriumBlock || state.getBlock() instanceof SolidifiedCoriumBlock) {
            return CoriumModel.Below.CORIUM;
        }
        if (state.isAir() || !state.getFluidState().isEmpty() && !state.isCollisionShapeFullBlock(level, pos)) {
            return CoriumModel.Below.OPEN;
        }
        if (state.canBeReplaced()) {
            // grass, snow: it burns through them, unless blocks are to be left alone
            return PwrConfig.blockDestruction ? CoriumModel.Below.OPEN : CoriumModel.Below.MELTABLE;
        }
        if (state.is(PwrRegistry.CORE_CATCHER.get())) {
            return CoriumModel.Below.CATCHER;
        }
        if (state.is(PwrTags.CORIUM_RESISTANT) || state.is(Blocks.BEDROCK) || state.getDestroySpeed(level, pos) < 0
                || state.getBlock().getExplosionResistance() > 1200.0f) {
            return CoriumModel.Below.RESISTANT;
        }
        return CoriumModel.Below.MELTABLE;
    }

    private static boolean touchesWater(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            FluidState fluid = level.getFluidState(pos.relative(direction));
            if (!fluid.isEmpty() && !fluid.is(FluidTags.LAVA)) {
                return true;
            }
        }
        return false;
    }

    /** Free space beside the corium with something under it to flow onto. */
    private static BlockPos spreadTarget(Level level, BlockPos pos, RandomSource random) {
        List<BlockPos> options = new ArrayList<>(4);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(direction);
            BlockState state = level.getBlockState(side);
            if ((state.isAir() || PwrConfig.blockDestruction && state.canBeReplaced()) && state.getFluidState().isEmpty()
                    && classify(level, side.relative(ShipSpace.localDown(level, pos))) != CoriumModel.Below.OPEN) {
                options.add(side);
            }
        }
        return options.isEmpty() ? null : options.get(random.nextInt(options.size()));
    }

    /** Molten core attacking concrete gives off hydrogen and steam into the containment around it. */
    private static void meltConcrete(ServerLevel level, BlockPos pos, BlockState melted) {
        ContainmentMonitorBlockEntity containment = ContainmentRegistry.at(level, pos);
        if (containment == null) {
            return;
        }
        double scale = melted.is(PwrRegistry.CONTAINMENT_CONCRETE.get()) ? 2.0 : 1.0;
        if (PwrConfig.hydrogen) {
            containment.addHydrogen(CONCRETE_HYDROGEN * scale, GAS_TEMPERATURE);
        }
        containment.addSteam(CONCRETE_STEAM * scale, GAS_TEMPERATURE);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living && !living.fireImmune()) {
            living.hurt(level.damageSources().hotFloor(), 6.0f);
            living.igniteForSeconds(4.0f);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.LAVA, pos.getX() + random.nextDouble(), pos.getY() + 1.0, pos.getZ() + random.nextDouble(),
                    0.0, 0.0, 0.0);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX() + random.nextDouble(), pos.getY() + 1.05, pos.getZ() + random.nextDouble(),
                    0.0, 0.05, 0.0);
        }
    }

    /** Molten fuel cannot be turned or picked up with a wrench. */
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }
}
