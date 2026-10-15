package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.maintenance.MaintenanceKitItem;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitters;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.spentfuel.CoolingGrade;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelData;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelRegistry;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;
import org.papiricoh.create_nuclearindustry.wastetransport.WasteCargo;
import org.papiricoh.create_nuclearindustry.wastetransport.WasteTransportConfig;
import org.papiricoh.create_nuclearindustry.wastetransport.WasteTransportRegistry;
import org.papiricoh.create_nuclearindustry.wastetransport.breach.BreachEffects;
import org.papiricoh.create_nuclearindustry.wastetransport.breach.MountedWasteStorage;
import org.papiricoh.create_nuclearindustry.wastetransport.breach.TrainBreachMonitor;
import org.papiricoh.create_nuclearindustry.wastetransport.cask.WasteCaskBlock;
import org.papiricoh.create_nuclearindustry.wastetransport.cask.WasteCaskBlockEntity;
import org.papiricoh.create_nuclearindustry.wastetransport.cask.WasteCaskMountedStorage;
import org.papiricoh.create_nuclearindustry.wastetransport.cistern.WasteCisternBlockEntity;
import org.papiricoh.create_nuclearindustry.wastetransport.cistern.WasteCisternMountedStorage;

import java.util.List;
import java.util.Optional;

/**
 * Radioactive waste transport: forming, filtering, shielding, breaches and their repair, and the
 * containers riding contraptions. Whatever spills contaminates chunks, so those tests run in
 * batches of their own and clean up after themselves.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class WasteTransportTests {
    private static final String BATCH = ContaminationTests.BATCH + "_waste_";

    // ------------------------------------------------------------------ forming and filtering

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void wasteCisternFormsLikeATank(GameTestHelper helper) {
        BlockPos base = new BlockPos(3, 1, 3);
        for (BlockPos pos : List.of(base, base.east(), base.south(), base.south().east())) {
            helper.setBlock(pos, cistern());
        }
        helper.succeedWhen(() -> {
            WasteCisternBlockEntity cistern = cisternAt(helper, base.south().east());
            helper.assertTrue(cistern != null, "the cistern block entity should be there");
            if (cistern == null) {
                return;
            }
            WasteCisternBlockEntity controller = cistern.controllerCistern();
            helper.assertTrue(controller != null, "the four blocks should share a controller");
            if (controller == null) {
                return;
            }
            int expected = 4 * FluidTankBlockEntity.getCapacityMultiplier();
            helper.assertTrue(controller.getTankInventory().getCapacity() == expected,
                    "a 2x2 cistern should hold " + expected + " mB, got " + controller.getTankInventory().getCapacity());
            helper.assertTrue(cistern.fluidHandler() == controller.getTankInventory(), "every block should expose the shared tank");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void wasteCisternTakesOnlyRadioactiveFluids(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 1, 4);
        helper.setBlock(pos, cistern());
        IFluidHandler handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), null);
        helper.assertTrue(handler != null, "the cistern should expose a fluid handler");
        if (handler != null) {
            helper.assertTrue(handler.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "water is refused");
            helper.assertTrue(handler.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "lava is refused");
            helper.assertTrue(handler.fill(solution(1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "spent fuel solution goes in");
            helper.assertTrue(handler.fill(new FluidStack(AllNuclearFluids.PLUTONIUM_NITRATE.get(), 1000), IFluidHandler.FluidAction.EXECUTE) == 0,
                    "a second fluid does not mix in");
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void wasteCaskSharesAndFiltersItsSlots(GameTestHelper helper) {
        BlockPos front = new BlockPos(4, 1, 4);
        BlockPos back = front.south();
        helper.setBlock(front, cask(Direction.Axis.Z));
        helper.setBlock(back, cask(Direction.Axis.Z));
        long now = helper.getLevel().getGameTime();
        helper.succeedWhen(() -> {
            IItemHandler viaFront = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(front), null);
            IItemHandler viaBack = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(back), null);
            helper.assertTrue(viaFront != null && viaBack != null, "both blocks should expose an inventory");
            if (viaFront == null || viaBack == null) {
                return;
            }
            helper.assertTrue(viaFront.getSlots() == 2 * WasteTransportConfig.caskSlotsPerBlock,
                    "two blocks should share " + 2 * WasteTransportConfig.caskSlotsPerBlock + " slots, got " + viaFront.getSlots());
            ItemStack dirt = new ItemStack(Items.DIRT, 4);
            helper.assertTrue(ItemHandlerHelper.insertItem(viaFront, dirt, true).getCount() == 4, "dirt is refused");
            helper.assertTrue(ItemHandlerHelper.insertItem(viaFront, new ItemStack(WasteRegistry.HIGH_LEVEL_WASTE.get(), 3), true).isEmpty(),
                    "high-level waste goes in");
            ItemStack hot = spentFuel(CoolingGrade.HOT);
            ItemStack cooled = spentFuel(CoolingGrade.COOLED);
            helper.assertTrue(!WasteCargo.acceptsItem(hot, now) && ItemHandlerHelper.insertItem(viaFront, hot, true).getCount() == 1,
                    "hot spent fuel is refused");
            helper.assertTrue(ItemHandlerHelper.insertItem(viaFront, cooled, true).isEmpty(), "cooled spent fuel goes in");
            ItemHandlerHelper.insertItem(viaFront, new ItemStack(WasteRegistry.LOW_LEVEL_WASTE.get(), 5), false);
            int seen = 0;
            for (int slot = 0; slot < viaBack.getSlots(); slot++) {
                if (viaBack.getStackInSlot(slot).is(WasteRegistry.LOW_LEVEL_WASTE.get())) {
                    seen += viaBack.getStackInSlot(slot).getCount();
                }
            }
            helper.assertTrue(seen == 5, "what goes in through one block shows through the other, saw " + seen);
        });
    }

    // ------------------------------------------------------------------ shielding and breaches

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH + "dose", timeoutTicks = 100)
    public static void wasteCaskShieldsUntilBreached(GameTestHelper helper) {
        BlockPos caskPos = new BlockPos(6, 2, 4);
        helper.setBlock(caskPos, cask(Direction.Axis.X));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos body = helper.absolutePos(caskPos.south(3));
        player.setPos(body.getX() + 0.5, body.getY() - 0.4, body.getZ() + 0.5);
        double[] shielded = {-1};
        helper.runAfterDelay(5, () -> {
            WasteCaskBlockEntity cask = caskAt(helper, caskPos);
            helper.assertTrue(cask != null, "the cask block entity should be there");
            if (cask == null) {
                return;
            }
            cask.getInventoryOfBlock().setStackInSlot(0, new ItemStack(WasteRegistry.HIGH_LEVEL_WASTE.get(), 16));
            cask.refreshActivity();
            RadiationEmitters.reindex(helper.getLevel());
            RadiationHandler.update(player);
            shielded[0] = player.getData(AllNuclearRadiation.RATE);
            helper.assertTrue(cask.bareActivity() > 200, "16 high-level waste should be over 200 rad/s bare, got " + cask.bareActivity());
            helper.assertTrue(shielded[0] < cask.bareActivity() * 0.005,
                    "sealed, 3 blocks away should take almost nothing of " + cask.bareActivity() + " rad/s, got " + shielded[0]);
            cask.markBreached();
            RadiationHandler.update(player);
            float breached = player.getData(AllNuclearRadiation.RATE);
            helper.assertTrue(breached > 0 && breached >= 50 * shielded[0],
                    "breached it should give at least 50 times more than sealed (" + shielded[0] + "), got " + breached);
        });
        helper.runAfterDelay(10, () -> {
            empty(helper, caskPos);
            ContaminationTests.clean(helper, 1);
            helper.assertTrue(shielded[0] >= 0, "the dose should have been measured");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH + "spill")
    public static void wasteSpillScalesWithActivity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ChunkPos chunk = new ChunkPos(helper.absolutePos(new BlockPos(6, 1, 6)));
        BlockPos middle = new BlockPos(chunk.getMiddleBlockX(), helper.absolutePos(BlockPos.ZERO).getY() + 1, chunk.getMiddleBlockZ());
        try {
            BreachEffects.spill(level, middle, 100);
            float once = Contamination.get(level, chunk);
            ContaminationTests.clean(helper, 1);
            BreachEffects.spill(level, middle, 200);
            float twice = Contamination.get(level, chunk);
            helper.assertTrue(once > 0, "a spill should contaminate the chunk it falls in");
            helper.assertTrue(Math.abs(twice - 2 * once) < 0.05 * twice,
                    "twice the activity should leave twice the contamination: " + once + " then " + twice);
        } finally {
            ContaminationTests.clean(helper, 1);
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH + "break")
    public static void brokenFullCisternContaminates(GameTestHelper helper) {
        // At the middle of its chunk, where the chunk's contamination is read: a small spill reaches it.
        ChunkPos chunk = new ChunkPos(helper.absolutePos(new BlockPos(6, 1, 6)));
        BlockPos corner = helper.absolutePos(BlockPos.ZERO);
        BlockPos pos = new BlockPos(chunk.getMiddleBlockX() - corner.getX(), 1, chunk.getMiddleBlockZ() - corner.getZ());
        helper.setBlock(pos, cistern());
        WasteCisternBlockEntity cistern = cisternAt(helper, pos);
        helper.assertTrue(cistern != null, "the cistern block entity should be there");
        try {
            if (cistern != null) {
                cistern.getTankInventory().fill(solution(FluidTankBlockEntity.getCapacityMultiplier()), IFluidHandler.FluidAction.EXECUTE);
                helper.assertTrue(Contamination.get(helper.getLevel(), chunk) == 0, "the arena should start clean");
                helper.destroyBlock(pos);
                helper.assertTrue(Contamination.get(helper.getLevel(), chunk) > 0, "a full cistern broken should contaminate the ground");
            }
        } finally {
            ContaminationTests.clean(helper, 1);
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void maintenanceKitPatchesABreachInTwoUses(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 1, 4);
        helper.setBlock(pos, cask(Direction.Axis.Z));
        helper.setBlock(pos.south(), cask(Direction.Axis.Z));
        helper.succeedWhen(() -> {
            WasteCaskBlockEntity cask = caskAt(helper, pos.south());
            helper.assertTrue(cask != null && cask.controllerCask() != null, "the cask should have formed");
            if (cask == null) {
                return;
            }
            helper.assertTrue(MaintenanceKitItem.service(helper.getLevel(), helper.absolutePos(pos), 0.5) == MaintenanceKitItem.Outcome.NOTHING_TO_DO,
                    "a sealed cask needs nothing");
            cask.markBreached();
            helper.assertTrue(cask.isBreached(), "the breach belongs to the whole cask");
            BlockPos other = helper.absolutePos(pos);
            helper.assertTrue(MaintenanceKitItem.service(helper.getLevel(), other, 0.5) == MaintenanceKitItem.Outcome.MENDED, "the first use mends");
            helper.assertTrue(cask.isBreached(), "half mended is still breached");
            helper.assertTrue(MaintenanceKitItem.service(helper.getLevel(), other, 0.5) == MaintenanceKitItem.Outcome.MENDED, "the second use mends");
            helper.assertTrue(!cask.isBreached(), "two uses seal it");
            helper.assertTrue(MaintenanceKitItem.service(helper.getLevel(), other, 0.5) == MaintenanceKitItem.Outcome.NOTHING_TO_DO,
                    "then there is nothing left to do");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH + "explosion", timeoutTicks = 100)
    public static void explosionBreachesASurvivingCask(GameTestHelper helper) {
        BlockPos pos = new BlockPos(6, 2, 6);
        helper.setBlock(pos, cask(Direction.Axis.Z));
        helper.runAfterDelay(3, () -> {
            WasteCaskBlockEntity cask = caskAt(helper, pos);
            helper.assertTrue(cask != null, "the cask block entity should be there");
            if (cask == null) {
                return;
            }
            cask.getInventoryOfBlock().setStackInSlot(0, new ItemStack(WasteRegistry.HIGH_LEVEL_WASTE.get(), 16));
            Vec3 blast = Vec3.atCenterOf(helper.absolutePos(pos.east(2)));
            helper.getLevel().explode(null, blast.x, blast.y, blast.z, 3.0f, Level.ExplosionInteraction.NONE);
            helper.assertTrue(helper.getBlockState(pos).getBlock() instanceof WasteCaskBlock, "the cask survives a blast that breaks no blocks");
            helper.assertTrue(cask.isBreached(), "but the blast breaches it");
            helper.assertTrue(Contamination.get(helper.getLevel(), new ChunkPos(helper.absolutePos(pos))) > 0, "and its waste spills");
        });
        helper.runAfterDelay(6, () -> {
            empty(helper, pos);
            ContaminationTests.clean(helper, 1);
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ riding contraptions

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH + "codec")
    public static void wasteMountedStoragesRoundTrip(GameTestHelper helper) {
        BlockPos cisternPos = new BlockPos(3, 1, 3);
        BlockPos caskPos = new BlockPos(6, 1, 3);
        BlockPos cisternBack = new BlockPos(3, 1, 7);
        BlockPos caskBack = new BlockPos(6, 1, 7);
        helper.setBlock(cisternPos, cistern());
        helper.setBlock(caskPos, cask(Direction.Axis.Z));
        helper.setBlock(cisternBack, cistern());
        helper.setBlock(caskBack, cask(Direction.Axis.Z));
        helper.succeedWhen(() -> {
            ServerLevel level = helper.getLevel();
            WasteCisternBlockEntity cistern = cisternAt(helper, cisternPos);
            WasteCaskBlockEntity cask = caskAt(helper, caskPos);
            WasteCisternBlockEntity cisternTarget = cisternAt(helper, cisternBack);
            WasteCaskBlockEntity caskTarget = caskAt(helper, caskBack);
            helper.assertTrue(cistern != null && cask != null && cisternTarget != null && caskTarget != null, "all four containers should be there");
            if (cistern == null || cask == null || cisternTarget == null || caskTarget == null) {
                return;
            }
            if (cistern.getTankInventory().isEmpty()) {
                cistern.getTankInventory().fill(solution(3000), IFluidHandler.FluidAction.EXECUTE);
                cistern.markBreached();
                cask.getInventoryOfBlock().setStackInSlot(2, new ItemStack(WasteRegistry.HIGH_LEVEL_WASTE.get(), 7));
            }
            RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);

            MountedFluidStorage mountedFluid = WasteTransportRegistry.CISTERN_STORAGE.get().mount(level, cistern.getBlockState(),
                    helper.absolutePos(cisternPos), cistern);
            helper.assertTrue(mountedFluid != null, "the cistern's controller mounts");
            Optional<MountedFluidStorage> fluidBack = mountedFluid == null ? Optional.empty() : MountedFluidStorage.CODEC.encodeStart(ops, mountedFluid)
                    .result().flatMap(tag -> MountedFluidStorage.CODEC.parse(ops, tag).result());
            helper.assertTrue(fluidBack.isPresent() && fluidBack.get() instanceof WasteCisternMountedStorage, "the mounted cistern survives its codec");
            if (fluidBack.isPresent() && fluidBack.get() instanceof WasteCisternMountedStorage decoded) {
                helper.assertTrue(decoded.getFluid().getAmount() == 3000 && decoded.isBreached(), "fluid and breach survive the codec");
                decoded.unmount(level, cisternTarget.getBlockState(), helper.absolutePos(cisternBack), cisternTarget);
                helper.assertTrue(cisternTarget.getTankInventory().getFluidAmount() == 3000, "unmounting puts the fluid back");
                helper.assertTrue(cisternTarget.isBreached(), "and the breach");
            }

            MountedItemStorage mountedItems = WasteTransportRegistry.CASK_STORAGE.get().mount(level, cask.getBlockState(),
                    helper.absolutePos(caskPos), cask);
            helper.assertTrue(mountedItems != null, "every cask block mounts");
            Optional<MountedItemStorage> itemsBack = mountedItems == null ? Optional.empty() : MountedItemStorage.CODEC.encodeStart(ops, mountedItems)
                    .result().flatMap(tag -> MountedItemStorage.CODEC.parse(ops, tag).result());
            helper.assertTrue(itemsBack.isPresent() && itemsBack.get() instanceof WasteCaskMountedStorage, "the mounted cask survives its codec");
            if (itemsBack.isPresent() && itemsBack.get() instanceof WasteCaskMountedStorage decoded) {
                helper.assertTrue(decoded.getStackInSlot(2).getCount() == 7, "the slots survive the codec");
                helper.assertTrue(decoded.insertItem(0, new ItemStack(Items.DIRT), true).getCount() == 1, "the decoded slots still refuse dirt");
                decoded.unmount(level, caskTarget.getBlockState(), helper.absolutePos(caskBack), caskTarget);
                helper.assertTrue(caskTarget.getInventoryOfBlock().getStackInSlot(2).is(WasteRegistry.HIGH_LEVEL_WASTE.get()),
                        "unmounting puts the items back");
            }
            // Passed: leave nothing breached or loaded behind.
            empty(helper, cisternPos, caskPos, cisternBack, caskBack);
            ContaminationTests.clean(helper, 1);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH + "train", timeoutTicks = 100)
    public static void wasteCargoIsExposedFilteredAndBreachedOnDerailment(GameTestHelper helper) {
        BlockPos cisternPos = new BlockPos(4, 1, 6);
        BlockPos caskPos = new BlockPos(7, 1, 6);
        helper.setBlock(cisternPos, cistern());
        helper.setBlock(caskPos, cask(Direction.Axis.Z));
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            WasteCisternBlockEntity cistern = cisternAt(helper, cisternPos);
            WasteCaskBlockEntity cask = caskAt(helper, caskPos);
            helper.assertTrue(cistern != null && cask != null, "both containers should be there");
            if (cistern == null || cask == null) {
                return;
            }
            cistern.getTankInventory().fill(solution(4000), IFluidHandler.FluidAction.EXECUTE);
            cask.getInventoryOfBlock().setStackInSlot(0, new ItemStack(WasteRegistry.HIGH_LEVEL_WASTE.get(), 16));

            MountedStorageManager manager = new MountedStorageManager();
            manager.addBlock(level, cistern.getBlockState(), helper.absolutePos(cisternPos), BlockPos.ZERO, cistern);
            manager.addBlock(level, cask.getBlockState(), helper.absolutePos(caskPos), new BlockPos(3, 0, 0), cask);
            manager.initialize();

            int highLevel = 0;
            for (int slot = 0; slot < manager.getAllItems().getSlots(); slot++) {
                if (manager.getAllItems().getStackInSlot(slot).is(WasteRegistry.HIGH_LEVEL_WASTE.get())) {
                    highLevel += manager.getAllItems().getStackInSlot(slot).getCount();
                }
            }
            helper.assertTrue(highLevel == 16, "the cask's cargo should show in the contraption's items (not internal), saw " + highLevel);
            helper.assertTrue(manager.getFluids().getFluidInTank(0).getAmount() == 4000, "the cistern's fluid should show in its fluids");
            helper.assertTrue(ItemHandlerHelper.insertItem(manager.getAllItems(), new ItemStack(Items.DIRT, 2), false).getCount() == 2,
                    "a portable interface cannot load dirt into the cask");
            helper.assertTrue(manager.getFluids().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0,
                    "nor pump water into the cistern");

            Vec3 at = Vec3.atCenterOf(helper.absolutePos(cisternPos));
            helper.assertTrue(TrainBreachMonitor.breachCargo(level, at, manager) == 2, "a derailment breaches both containers");
            helper.assertTrue(MountedWasteStorage.of(manager).stream().allMatch(MountedWasteStorage::isBreached), "both are breached");
            int left = manager.getFluids().getFluidInTank(0).getAmount();
            int expectedLeft = 4000 - (int) Math.round(4000 * WasteTransportConfig.spillFluidFraction);
            helper.assertTrue(left == expectedLeft, "the cistern should lose its spill share, " + expectedLeft + " left, got " + left);
            helper.assertTrue(Contamination.get(level, new ChunkPos(BlockPos.containing(at))) > 0, "the cargo spills where the carriage is");
            helper.assertTrue(TrainBreachMonitor.breachCargo(level, at, manager) == 0, "breaching them again spills nothing more");
        });
        helper.runAfterDelay(6, () -> {
            empty(helper, cisternPos, caskPos);
            ContaminationTests.clean(helper, 1);
            helper.succeed();
        });
    }

    /**
     * A Create mechanical piston pushes a filled cistern and a loaded, breached cask two blocks and puts
     * them down again: both arrive with their contents and the cask with its breach.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH + "piston", timeoutTicks = 300)
    public static void pistonCarriesWasteContainersWithTheirCargo(GameTestHelper original) {
        CreateGameTestHelper helper = CreateGameTestHelper.of(original);
        BlockPos piston = new BlockPos(3, 1, 6);
        BlockPos cisternPos = piston.east();
        BlockPos caskPos = piston.east(2);
        Block pistonBlock = createBlock("mechanical_piston");
        Block pole = createBlock("piston_extension_pole");
        Block motor = createBlock("creative_motor");
        helper.setBlock(piston, pistonBlock.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));
        helper.setBlock(piston.west(), pole.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));
        helper.setBlock(piston.west(2), pole.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));
        helper.setBlock(cisternPos, cistern());
        helper.setBlock(caskPos, cask(Direction.Axis.Z));
        boolean shaftAlongZ = isShaftAlongZ(helper.getBlockState(piston));
        BlockPos motorPos = shaftAlongZ ? piston.north() : piston.above();
        helper.setBlock(motorPos, motor.defaultBlockState().setValue(BlockStateProperties.FACING, shaftAlongZ ? Direction.SOUTH : Direction.DOWN));
        // A creative motor starts turning at once: hold it still until the containers are loaded.
        if (helper.getLevel().getBlockEntity(helper.absolutePos(motorPos)) instanceof CreativeMotorBlockEntity idle) {
            idle.generatedSpeed.setValue(0);
        }
        helper.runAfterDelay(3, () -> {
            WasteCisternBlockEntity cistern = cisternAt(helper, cisternPos);
            WasteCaskBlockEntity cask = caskAt(helper, caskPos);
            helper.assertTrue(cistern != null && cask != null, "both containers should be there before the push");
            if (cistern == null || cask == null) {
                return;
            }
            cistern.getTankInventory().fill(solution(2500), IFluidHandler.FluidAction.EXECUTE);
            // The cask, not the cistern, carries the breach: a breached cistern would leak on the way.
            cask.markBreached();
            cask.getInventoryOfBlock().setStackInSlot(4, new ItemStack(WasteRegistry.HIGH_LEVEL_WASTE.get(), 6));
            if (helper.getBlockEntity(motorPos) instanceof CreativeMotorBlockEntity motorEntity) {
                motorEntity.generatedSpeed.setValue(64);
            }
        });
        helper.succeedWhen(() -> {
            WasteCisternBlockEntity moved = cisternAt(helper, cisternPos.east(2));
            WasteCaskBlockEntity movedCask = caskAt(helper, caskPos.east(2));
            helper.assertTrue(moved != null && movedCask != null, "the piston should have pushed both containers two blocks and put them down");
            if (moved == null || movedCask == null) {
                return;
            }
            helper.assertTrue(moved.getTankInventory().getFluidAmount() == 2500
                            && moved.getTankInventory().getFluid().is(AllNuclearFluids.SPENT_FUEL_SOLUTION.get()),
                    "the cistern keeps its fluid, got " + moved.getTankInventory().getFluid().getAmount());
            helper.assertTrue(!moved.isBreached(), "a sealed cistern arrives sealed");
            helper.assertTrue(movedCask.getInventoryOfBlock().getStackInSlot(4).getCount() == 6, "the cask keeps its cargo");
            helper.assertTrue(movedCask.isBreached(), "and its breach");
            // Passed: leave nothing breached or loaded behind.
            empty(helper, cisternPos.east(2), caskPos.east(2));
            ContaminationTests.clean(helper, 1);
        });
    }

    // ------------------------------------------------------------------ helpers

    /** Empties and seals the containers at {@code positions}, so nothing irradiates or leaks after the test. */
    private static void empty(GameTestHelper helper, BlockPos... positions) {
        for (BlockPos pos : positions) {
            if (cisternAt(helper, pos) instanceof WasteCisternBlockEntity cistern) {
                cistern.clearContent();
                cistern.repair(1);
            } else if (caskAt(helper, pos) instanceof WasteCaskBlockEntity cask) {
                cask.clearContent();
                cask.repair(1);
            }
        }
    }

    private static BlockState cistern() {
        return WasteTransportRegistry.RADIOACTIVE_WASTE_CISTERN.get().defaultBlockState();
    }

    private static BlockState cask(Direction.Axis axis) {
        return WasteTransportRegistry.SHIELDED_WASTE_CASK.get().defaultBlockState().setValue(WasteCaskBlock.HORIZONTAL_AXIS, axis);
    }

    private static FluidStack solution(int amount) {
        return new FluidStack(AllNuclearFluids.SPENT_FUEL_SOLUTION.get(), amount);
    }

    private static ItemStack spentFuel(CoolingGrade grade) {
        ItemStack stack = new ItemStack(AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get());
        stack.set(SpentFuelRegistry.SPENT_FUEL.get(), SpentFuelData.undated(grade));
        return stack;
    }

    private static WasteCisternBlockEntity cisternAt(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof WasteCisternBlockEntity cistern ? cistern : null;
    }

    private static WasteCaskBlockEntity caskAt(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof WasteCaskBlockEntity cask ? cask : null;
    }

    private static Block createBlock(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", path));
    }

    /** Whether a mechanical piston facing east turns on a shaft along z (else along y). */
    private static boolean isShaftAlongZ(BlockState piston) {
        for (var property : piston.getProperties()) {
            if (property.getName().equals("axis_along_first") && property.getValueClass() == Boolean.class) {
                return !(Boolean) piston.getValue(property);
            }
        }
        return true;
    }
}
