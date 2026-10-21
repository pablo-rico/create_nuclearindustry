package org.papiricoh.create_nuclearindustry.commands;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.coolingtower.blockentity.CoolingTowerControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;
import org.papiricoh.create_nuclearindustry.gametest.AcceleratorTestBuilder;
import org.papiricoh.create_nuclearindustry.gametest.BlockPlacer;
import org.papiricoh.create_nuclearindustry.gametest.ContainmentTestBuilder;
import org.papiricoh.create_nuclearindustry.gametest.CoolingTowerTestBuilder;
import org.papiricoh.create_nuclearindustry.gametest.CryoplantTestBuilder;
import org.papiricoh.create_nuclearindustry.gametest.FissionTestBuilder;
import org.papiricoh.create_nuclearindustry.gametest.SteamGeneratorTestBuilder;
import org.papiricoh.create_nuclearindustry.gametest.TokamakTestBuilder;
import org.papiricoh.create_nuclearindustry.gametest.TurbineTestBuilder;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockText;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentMonitorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen.SteamGeneratorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.AbstractTurbineControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.TurbineStructure;
import org.papiricoh.create_nuclearindustry.turbine.TurbineTier;

/**
 * The machines {@code /nuclear spawn} places, already formed: the same ones the game tests build
 * (see the builders in {@code gametest}), so what the command gives a player is exactly what the
 * tests prove works. Each stands with its lowest north-west corner at the origin; the space it
 * needs is cleared first, and its controller is asked to form at once.
 */
public enum StructurePreset {
    /** The smallest fission reactor: 5x5x5, two fuel columns, one heat exchanger, one rod channel, two ports. */
    REACTOR("reactor") {
        @Override
        BoundingBox bounds() {
            return box(0, 0, 0, FissionTestBuilder.SIZE - 1, FissionTestBuilder.SIZE - 1, FissionTestBuilder.SIZE - 1);
        }

        @Override
        BlockPos build(BlockPlacer placer) {
            FissionTestBuilder.build(placer, BlockPos.ZERO, true);
            return FissionTestBuilder.controllerAt(BlockPos.ZERO);
        }
    },
    /** A 3x3x8 steam turbine with its output (the controller) facing north and a port on top and below. */
    TURBINE("turbine") {
        @Override
        BoundingBox bounds() {
            return turbineBounds(TurbineTier.STEAM);
        }

        @Override
        BlockPos build(BlockPlacer placer) {
            return buildTurbine(placer, TurbineTier.STEAM);
        }
    },
    /** The same turbine for a tokamak's plasma steam. */
    FUSION_TURBINE("fusion_turbine") {
        @Override
        BoundingBox bounds() {
            return turbineBounds(TurbineTier.PLASMA);
        }

        @Override
        BlockPos build(BlockPlacer placer) {
            return buildTurbine(placer, TurbineTier.PLASMA);
        }
    },
    /** A cooling tower of base radius 4 and 12 layers, controller on its west side. */
    COOLING_TOWER("cooling_tower") {
        private static final int RADIUS = 4;
        private static final int HEIGHT = 12;

        @Override
        BoundingBox bounds() {
            return box(0, 0, 0, 2 * (RADIUS + 1), HEIGHT, 2 * (RADIUS + 1));
        }

        @Override
        BlockPos build(BlockPlacer placer) {
            return CoolingTowerTestBuilder.build(placer, new BlockPos(RADIUS + 1, 0, RADIUS + 1), RADIUS, HEIGHT).controllerPos();
        }
    },
    /** A tokamak of major radius 4 with breeding blankets, ports, injector and a creative motor on its magnet drive. */
    TOKAMAK("tokamak") {
        private static final int RADIUS = 4;

        @Override
        BoundingBox bounds() {
            int reach = TokamakGeometry.controllerToAxis(RADIUS) + 1;
            return box(0, 0, 0, 2 * reach, 2 * TokamakGeometry.HALF_HEIGHT, 2 * reach + 1);
        }

        @Override
        BlockPos build(BlockPlacer placer) {
            int reach = TokamakGeometry.controllerToAxis(RADIUS) + 1;
            BlockPos center = new BlockPos(reach, TokamakGeometry.HALF_HEIGHT, reach);
            TokamakTestBuilder.build(placer, center, RADIUS, true, 256);
            return TokamakTestBuilder.controller(center, RADIUS);
        }
    },
    /** A linac running south: ion source, eight RF cavities with quadrupoles, a beam dump, and its three machines beside it. */
    ACCELERATOR("accelerator") {
        @Override
        BoundingBox bounds() {
            return box(0, 0, 0, 1, 0, AcceleratorTestBuilder.LENGTH + 2);
        }

        @Override
        BlockPos build(BlockPlacer placer) {
            AcceleratorTestBuilder.linac(placer, BlockPos.ZERO, 8, true);
            AcceleratorTestBuilder.machines(placer, BlockPos.ZERO);
            return BlockPos.ZERO;
        }
    },
    /** A 3x3x4 steam generator with one port of each role. */
    STEAM_GENERATOR("steam_generator") {
        private static final int HEIGHT = 4;

        @Override
        BoundingBox bounds() {
            return box(0, 0, 0, 2, HEIGHT - 1, 2);
        }

        @Override
        BlockPos build(BlockPlacer placer) {
            BlockPos bottomCenter = new BlockPos(1, 0, 1);
            SteamGeneratorTestBuilder.build(placer, bottomCenter, HEIGHT);
            return SteamGeneratorTestBuilder.controller(bottomCenter);
        }
    },
    /** A 3x3x4 cryogenic plant with two compressors, each on its own creative motor. */
    CRYOPLANT("cryoplant") {
        private static final int HEIGHT = 4;

        @Override
        BoundingBox bounds() {
            return box(0, 0, 0, 2, HEIGHT - 1, 3);
        }

        @Override
        BlockPos build(BlockPlacer placer) {
            BlockPos bottomCenter = new BlockPos(1, 0, 1);
            CryoplantTestBuilder.build(placer, bottomCenter, HEIGHT, 2, 128);
            return CryoplantTestBuilder.controller(bottomCenter);
        }
    },
    /** A 7x6x7 containment room of concrete with a monitor inside; it seals once the monitor has measured it. */
    CONTAINMENT("containment") {
        private static final BlockPos OFFSET = new BlockPos(-ContainmentTestBuilder.ROOM_MIN.getX(),
                -ContainmentTestBuilder.ROOM_MIN.getY(), -ContainmentTestBuilder.ROOM_MIN.getZ());

        @Override
        BoundingBox bounds() {
            BlockPos max = ContainmentTestBuilder.ROOM_MAX.offset(OFFSET);
            return box(0, 0, 0, max.getX(), max.getY(), max.getZ());
        }

        @Override
        BlockPos build(BlockPlacer placer) {
            ContainmentTestBuilder.concreteRoom(placer, OFFSET);
            return ContainmentTestBuilder.MONITOR.offset(OFFSET);
        }
    };

    /** What a spawn left behind: the controller (or monitor) and the blocks it covers, in world coordinates. */
    public record Spawned(BlockPos controller, BoundingBox bounds) {}

    private final String id;

    StructurePreset(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** The space the structure takes, relative to its origin, cleared before it is built. */
    abstract BoundingBox bounds();

    /** Builds the structure and returns its controller, relative to the origin. */
    abstract BlockPos build(BlockPlacer placer);

    /**
     * Clears the space, builds the structure with its corner at {@code origin} and asks it to form.
     * A fuelled reactor gets six assemblies of 5% uranium and a full water tank.
     */
    public Spawned spawn(ServerLevel level, BlockPos origin, boolean fuelled) {
        BoundingBox space = bounds().moved(origin.getX(), origin.getY(), origin.getZ());
        for (BlockPos pos : BlockPos.betweenClosed(space.minX(), space.minY(), space.minZ(), space.maxX(), space.maxY(), space.maxZ())) {
            if (!level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        BlockPos controller = origin.offset(build(BlockPlacer.of(level, origin)));
        BlockEntity entity = level.getBlockEntity(controller);
        form(entity);
        if (fuelled && entity instanceof ReactorBlockEntity reactor) {
            for (int i = 0; i < 6; i++) {
                reactor.getPhysicsSimulator().loadFuelAssembly(5.0f);
            }
            reactor.fillCoolant(new FluidStack(Fluids.WATER, reactor.getTankCapacity()), IFluidHandler.FluidAction.EXECUTE);
        }
        return new Spawned(controller, space);
    }

    private static void form(BlockEntity entity) {
        if (entity instanceof ReactorBlockEntity reactor) {
            reactor.attemptFormation(null);
        } else if (entity instanceof AbstractTurbineControllerBlockEntity turbine) {
            turbine.attemptFormation(null);
        } else if (entity instanceof CoolingTowerControllerBlockEntity tower) {
            tower.attemptFormation(null);
        } else if (entity instanceof FusionReactorBlockEntity tokamak) {
            tokamak.attemptFormation(null);
        } else if (entity instanceof AcceleratorControllerBlockEntity accelerator) {
            accelerator.attemptFormation(null);
        } else if (entity instanceof SteamGeneratorControllerBlockEntity generator) {
            generator.attemptFormation(null);
        } else if (entity instanceof CryoplantControllerBlockEntity plant) {
            plant.attemptFormation(null);
        }
    }

    /** Whether the structure at {@code controller} is formed (a containment room: sealed). */
    public static boolean formed(ServerLevel level, BlockPos controller) {
        BlockEntity entity = level.getBlockEntity(controller);
        if (entity instanceof ReactorBlockEntity reactor) {
            return reactor.isFormed();
        } else if (entity instanceof AbstractTurbineControllerBlockEntity turbine) {
            return turbine.getStructure().isPresent();
        } else if (entity instanceof CoolingTowerControllerBlockEntity tower) {
            return tower.getStructure().isPresent();
        } else if (entity instanceof FusionReactorBlockEntity tokamak) {
            return tokamak.isFormed();
        } else if (entity instanceof AcceleratorControllerBlockEntity accelerator) {
            return accelerator.isFormed();
        } else if (entity instanceof SteamGeneratorControllerBlockEntity generator) {
            return generator.isFormed();
        } else if (entity instanceof CryoplantControllerBlockEntity plant) {
            return plant.isFormed();
        } else if (entity instanceof ContainmentMonitorBlockEntity monitor) {
            return monitor.sealed();
        }
        return false;
    }

    /** Why the structure at {@code controller} is not formed, as its controller reports it. */
    public static Component problem(ServerLevel level, BlockPos controller) {
        BlockEntity entity = level.getBlockEntity(controller);
        if (entity instanceof ReactorBlockEntity reactor) {
            return MultiblockText.text(reactor.getLastValidation().primaryMessage());
        } else if (entity instanceof AbstractTurbineControllerBlockEntity turbine) {
            return MultiblockText.text(turbine.getValidationMessage());
        } else if (entity instanceof CoolingTowerControllerBlockEntity tower) {
            return MultiblockText.text(tower.getValidationMessage());
        } else if (entity instanceof FusionReactorBlockEntity tokamak) {
            return MultiblockText.text(tokamak.getValidationMessage());
        } else if (entity instanceof AcceleratorControllerBlockEntity accelerator) {
            return accelerator.validationText();
        } else if (entity instanceof CryoplantControllerBlockEntity plant) {
            return MultiblockText.text(plant.getValidationMessage());
        } else if (entity instanceof ContainmentMonitorBlockEntity) {
            return Component.translatable("commands.create_nuclearindustry.spawn.measuring");
        }
        return Component.literal("?");
    }

    private static BoundingBox turbineBounds(TurbineTier tier) {
        BoundingBox probe = TurbineTestBuilder.shape(tier, BlockPos.ZERO, 3, 3, 8).bounds();
        return probe.moved(-probe.minX(), -probe.minY(), -probe.minZ());
    }

    private static BlockPos buildTurbine(BlockPlacer placer, TurbineTier tier) {
        BoundingBox probe = TurbineTestBuilder.shape(tier, BlockPos.ZERO, 3, 3, 8).bounds();
        BlockPos controller = new BlockPos(-probe.minX(), -probe.minY(), -probe.minZ());
        TurbineStructure structure = TurbineTestBuilder.build(placer, tier, controller, 3, 3, 8);
        return structure.controllerPos;
    }

    private static BoundingBox box(int x0, int y0, int z0, int x1, int y1, int z1) {
        return new BoundingBox(x0, y0, z0, x1, y1, z1);
    }
}
