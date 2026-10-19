package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileFlight;
import org.papiricoh.create_nuclearindustry.missile.physics.PropellantModel;
import org.papiricoh.create_nuclearindustry.missile.silo.RocketSectionEntity;
import org.papiricoh.create_nuclearindustry.missile.silo.RocketValidator;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloControllerEntity;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry;

import java.util.List;

/**
 * Liquid propellant: pumped through a silo's fuel port into the rocket's tanks from the bottom up, refused once the silo is armed,
 * never mixed with cartridges in one stage, and flying further than solid.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PropellantTests {
    private static final String BATCH = "propellant";

    private static IFluidHandler port(GameTestHelper helper) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(SiloTestBuilder.PORT), Direction.EAST);
    }

    private static FluidStack fuel(int amount) {
        return new FluidStack(AllNuclearFluids.ROCKET_FUEL.get(), amount);
    }

    private static FluidStack oxygen(int amount) {
        return new FluidStack(AllNuclearFluids.LIQUID_OXYGEN.get(), amount);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void thePortFillsTheTanksFromTheBottomUp(GameTestHelper helper) {
        SiloControllerEntity silo = new SiloTestBuilder().tanks(2).dry().port().build(helper);
        silo.configure(helper.makeMockPlayer(GameType.SURVIVAL), SiloTests.target(helper), false);
        boolean[] pumped = {false};
        helper.runAfterDelay(5, () -> {
            IFluidHandler port = port(helper);
            helper.assertTrue(port != null, "a fuel port in the silo's wall takes fluids");
            if (port == null) return;
            helper.assertTrue(port.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0,
                    "but not water");
            helper.assertTrue(port.getTankCapacity(0) == 2 * PropellantModel.TANK_CAPACITY, "two tanks' worth of fuel, got " + port.getTankCapacity(0));
            int filled = port.fill(fuel(1500), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled == 1500, "it takes all 1500 mB, got " + filled);
            RocketSectionEntity bottom = helper.getBlockEntity(SiloTestBuilder.FLOOR.above(2));
            RocketSectionEntity top = helper.getBlockEntity(SiloTestBuilder.FLOOR.above(3));
            helper.assertTrue(bottom.fuel().getFluidAmount() == 1000, "the bottom tank fills first, got " + bottom.fuel().getFluidAmount());
            helper.assertTrue(top.fuel().getFluidAmount() == 500, "then the one above, got " + top.fuel().getFluidAmount());
            helper.assertTrue(port.fill(oxygen(5000), IFluidHandler.FluidAction.EXECUTE) == 2000, "oxygen fills its own side, up to two tanks");
            helper.assertTrue(port.fill(fuel(600), IFluidHandler.FluidAction.EXECUTE) == 500, "and fuel only what is left");
            helper.assertTrue(port.getFluidInTank(0).getAmount() == 2000, "the port reports the rocket's fuel");
            pumped[0] = true;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(pumped[0], "not pumped yet");
            helper.assertTrue(silo.state() == SiloControllerEntity.State.READY, "a liquid-fuelled rocket is ready, got " + silo.state() + " " + silo.error());
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void anArmedSiloTakesNoPropellant(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SiloControllerEntity silo = new SiloTestBuilder().tanks(2).liquid().port().build(helper);
        silo.configure(player, SiloTests.target(helper), false);
        helper.runAfterDelay(10, () -> {
            IFluidHandler port = port(helper);
            helper.assertTrue(port != null, "the port is there");
            if (port == null) return;
            helper.assertTrue(port.drain(fuel(100), IFluidHandler.FluidAction.EXECUTE).getAmount() == 100, "a ready silo can be defuelled");
            port.fill(fuel(100), IFluidHandler.FluidAction.EXECUTE);
            silo.validate();
            silo.arm(player);
            helper.assertTrue(silo.state() == SiloControllerEntity.State.ARMED, "it arms, got " + silo.state() + " " + silo.error());
            helper.assertTrue(port.drain(fuel(100), IFluidHandler.FluidAction.EXECUTE).isEmpty(), "armed, nothing drains");
            helper.assertTrue(port.fill(oxygen(100), IFluidHandler.FluidAction.EXECUTE) == 0, "nor fills");
            silo.disarm();
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aStageOfMixedPropellantIsRefused(GameTestHelper helper) {
        SiloControllerEntity silo = new SiloTestBuilder().tanks(2).build(helper);
        silo.configure(helper.makeMockPlayer(GameType.SURVIVAL), SiloTests.target(helper), false);
        BlockPos upper = SiloTestBuilder.FLOOR.above(3);
        RocketSectionEntity tank = helper.getBlockEntity(upper);
        tank.contents().setStackInSlot(0, ItemStack.EMPTY);
        SiloTestBuilder.fill(tank);
        helper.succeedWhen(() -> {
            helper.assertTrue("mixed_propellant".equals(silo.error()), "a cartridge under liquids in one stage is refused, got " + silo.error());
            helper.assertTrue(silo.errorPos().equals(helper.absolutePos(upper)), "at the tank that differs, got " + silo.errorPos());
            helper.assertTrue(silo.state() == SiloControllerEntity.State.UNPREPARED, "it can be fixed by refuelling");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aCartridgeCannotGoIntoAFuelledTank(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, SiloRegistry.TANK.get());
        RocketSectionEntity tank = helper.getBlockEntity(pos);
        tank.fuel().fill(fuel(100), IFluidHandler.FluidAction.EXECUTE);
        ItemStack rest = tank.contents().insertItem(0, new ItemStack(SiloRegistry.PROPELLANT.get()), false);
        helper.assertTrue(rest.getCount() == 1, "a tank with fuel in it takes no cartridge");
        tank.fuel().drain(100, IFluidHandler.FluidAction.EXECUTE);
        tank.contents().insertItem(0, new ItemStack(SiloRegistry.PROPELLANT.get()), false);
        helper.assertTrue(tank.oxidizer().fill(oxygen(100), IFluidHandler.FluidAction.SIMULATE) == 0, "nor a tank with a cartridge any liquid");
        helper.succeed();
    }

    /** The same rocket, solid or liquid, standing side by side: the liquid one reaches further. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aLiquidRocketReachesFurther(GameTestHelper helper) {
        BlockPos solidBase = new BlockPos(3, 1, 3), liquidBase = new BlockPos(8, 1, 8);
        column(helper, solidBase, false);
        column(helper, liquidBase, true);
        RocketValidator.Result solid = RocketValidator.validate(helper.getLevel(), helper.absolutePos(solidBase));
        RocketValidator.Result liquid = RocketValidator.validate(helper.getLevel(), helper.absolutePos(liquidBase));
        helper.assertTrue(solid.valid() && liquid.valid(), "both rockets are valid, got " + solid.error() + " / " + liquid.error());
        if (!solid.valid() || !liquid.valid()) return;
        helper.assertTrue(liquid.rocket().propellants().equals(List.of(PropellantModel.Kind.LIQUID)), "the second burns liquids");
        double solidRange = MissileFlight.maxRange(solid.rocket().design(), MissileConfig.flightParams());
        double liquidRange = MissileFlight.maxRange(liquid.rocket().design(), MissileConfig.flightParams());
        helper.assertTrue(liquidRange > solidRange, "liquid reaches " + liquidRange + ", solid " + solidRange);
        helper.succeed();
    }

    private static void column(GameTestHelper helper, BlockPos base, boolean liquid) {
        List<Block> blocks = List.of(SiloRegistry.ENGINE.get(), SiloRegistry.TANK.get(), SiloRegistry.TANK.get(), SiloRegistry.GUIDANCE.get(), SiloRegistry.WARHEAD.get());
        for (int i = 0; i < blocks.size(); i++) {
            helper.setBlock(base.above(i), blocks.get(i));
            if (blocks.get(i) == SiloRegistry.TANK.get()) {
                RocketSectionEntity tank = helper.getBlockEntity(base.above(i));
                if (liquid) SiloTestBuilder.fill(tank);
                else tank.contents().setStackInSlot(0, new ItemStack(SiloRegistry.PROPELLANT.get()));
            }
        }
        helper.setBlock(base.above(blocks.size()), Blocks.AIR);
    }
}
