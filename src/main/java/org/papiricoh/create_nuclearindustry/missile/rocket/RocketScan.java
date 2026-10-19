package org.papiricoh.create_nuclearindustry.missile.rocket;

import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.flight.RocketShape;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.physics.PropellantModel;
import org.papiricoh.create_nuclearindustry.missile.silo.AssembledRocket;
import org.papiricoh.create_nuclearindustry.missile.silo.RocketSectionEntity;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A dry run of assembling a silo's rocket, without touching the world: what Create would capture (the column and everything
 * glued or attached to it), whether it all fits the silo's 3×3 shaft, nothing is glued to the silo itself and nothing it carries
 * holds items or fluids of its own. Blocks beyond the column add {@code missile.rocket.extraBlockMass} each to the stage they
 * fall with; extra engines at the foot of a stage are extra nozzles. The launch assembles again for real.
 *
 * @param contraption what was captured, stages assigned, or null if it could not be assembled
 * @param captured    the world positions captured
 * @param extraMass   extra dry mass per stage, the payload last
 * @param error       why it cannot fly as a silo error suffix, or empty
 * @param message     for {@code "assembly"}, Create's own reason
 */
public record RocketScan(@Nullable RocketContraption contraption, Set<BlockPos> captured, double[] extraMass,
                         List<List<RocketShape.Nozzle>> extraNozzles, float radius, String error, BlockPos errorPos,
                         @Nullable Component message) {

    public boolean valid() {
        return error.isEmpty();
    }

    private static RocketScan failed(String error, BlockPos pos, @Nullable Component message) {
        return new RocketScan(null, Set.of(), new double[0], List.of(), 0.5f, error, pos.immutable(), message);
    }

    /** Assembles {@code rocket}, standing on {@code floor}, into a contraption: a dry run, or the real thing before removal. */
    public static RocketScan scan(Level level, BlockPos floor, AssembledRocket rocket) {
        RocketContraption contraption = new RocketContraption(rocket.height());
        try {
            if (!contraption.assemble(level, floor)) return failed("assembly", floor, null);
        } catch (AssemblyException e) {
            return failed("assembly", e.hasPosition() ? e.getPosition() : floor, e.component);
        }
        // Create initialises a contraption's storage when an entity takes it; this one may be written out before that.
        contraption.getStorage().initialize();
        List<Integer> starts = rocket.composition().stageStarts();
        int stages = starts.size();
        int payloadBottom = rocket.height() - 2;
        contraption.assignStages(starts, payloadBottom);
        Set<BlockPos> captured = new HashSet<>();
        for (BlockPos local : contraption.blocks().keySet()) captured.add(floor.offset(local));

        double[] extra = new double[stages + 1];
        List<List<RocketShape.Nozzle>> nozzles = new ArrayList<>();
        for (int s = 0; s < stages; s++) nozzles.add(new ArrayList<>());
        float radius = 0.5f;
        for (Map.Entry<BlockPos, StructureBlockInfo> entry : contraption.blocks().entrySet()) {
            BlockPos local = entry.getKey();
            BlockPos world = floor.offset(local);
            if (Math.abs(local.getX()) > 1 || Math.abs(local.getZ()) > 1 || local.getY() < 1 || local.getY() > rocket.height())
                return failed("oversize", world, null);
            for (Direction direction : Direction.values()) {
                BlockPos next = world.relative(direction);
                if (captured.contains(next)) continue;
                BlockState neighbour = level.getBlockState(next);
                if (RocketContraption.isSilo(neighbour) && SuperGlueEntity.isGlued(level, world, direction, null))
                    return failed("glued_to_silo", next, null);
            }
            if (!(level.getBlockEntity(world) instanceof RocketSectionEntity) && holdsSomething(level, world))
                return failed("cargo_not_empty", world, null);
            if (local.getX() == 0 && local.getZ() == 0) continue;
            int stage = contraption.stageOf(local, stages);
            extra[stage] += MissileConfig.extraBlockMass;
            radius = Math.max(radius, Math.max(Math.abs(local.getX()), Math.abs(local.getZ())) + 0.5f);
            if (entry.getValue().state().is(SiloRegistry.ENGINE.get()) && stage < stages && local.getY() - 1 == starts.get(stage))
                nozzles.get(stage).add(new RocketShape.Nozzle(local.getX(), local.getY() - 1, local.getZ()));
        }
        return new RocketScan(contraption, captured, extra, nozzles, radius, "", floor, null);
    }

    /** Whether the block at {@code pos} keeps items or fluids of its own, which a rocket may not carry off. */
    private static boolean holdsSomething(Level level, BlockPos pos) {
        IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (items != null) for (int i = 0; i < items.getSlots(); i++) if (!items.getStackInSlot(i).isEmpty()) return true;
        IFluidHandler fluids = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        if (fluids != null) for (int i = 0; i < fluids.getTanks(); i++) if (!fluids.getFluidInTank(i).isEmpty()) return true;
        return false;
    }

    /** {@code design} carrying the extra blocks: each stage's dry mass, and the payload's, grows by what rides on it. */
    public MissileDesign apply(MissileDesign design) {
        if (extraMass.length == 0) return design;
        List<MissileDesign.Stage> stages = new ArrayList<>();
        for (int s = 0; s < design.stages().size(); s++) {
            MissileDesign.Stage stage = design.stages().get(s);
            double extra = s < extraMass.length - 1 ? extraMass[s] : 0;
            stages.add(new MissileDesign.Stage(stage.dryMass() + extra, stage.propellant(), stage.thrust(), stage.exhaustVelocity()));
        }
        return new MissileDesign(stages, design.payloadMass() + extraMass[extraMass.length - 1], design.cep());
    }

    /** The outline the effects draw: one stage per stage, a nozzle under the column and one under each extra engine. */
    public RocketShape shape(AssembledRocket rocket) {
        List<Integer> starts = rocket.composition().stageStarts();
        int payload = rocket.height() - 2;
        List<RocketShape.Stage> stages = new ArrayList<>();
        for (int s = 0; s < starts.size(); s++) {
            int bottom = starts.get(s);
            int top = (s + 1 < starts.size() ? starts.get(s + 1) : payload) - 1;
            List<RocketShape.Nozzle> stageNozzles = new ArrayList<>();
            stageNozzles.add(new RocketShape.Nozzle(0, bottom, 0));
            if (s < extraNozzles.size()) stageNozzles.addAll(extraNozzles.get(s));
            PropellantModel.Kind kind = s < rocket.propellants().size() ? rocket.propellants().get(s) : PropellantModel.Kind.SOLID;
            stages.add(new RocketShape.Stage(bottom, top, stageNozzles,
                    kind == PropellantModel.Kind.LIQUID ? RocketShape.Propellant.LIQUID : RocketShape.Propellant.SOLID));
        }
        return new RocketShape(rocket.height(), radius, stages, payload);
    }
}
