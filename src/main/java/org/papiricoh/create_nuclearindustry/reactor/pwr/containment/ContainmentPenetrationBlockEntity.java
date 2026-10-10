package org.papiricoh.create_nuclearindustry.reactor.pwr.containment;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;

import java.util.List;

/**
 * The two buffers of a containment penetration. Each end fills the buffer that empties at the other
 * end, so fluid only ever crosses the wall; each tick a buffer also pushes what it holds into
 * whatever takes fluid on its outlet side, so a single pump can drive a line through the wall.
 */
public class ContainmentPenetrationBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final int BUFFER = 1_000;
    /** Most a buffer pushes into the block at its outlet per tick, mB. */
    private static final int PUSH_PER_TICK = 250;

    /** Filled at the negative end of the axis, emptied at the positive end. */
    private final FluidTank towardPositive = tank();
    /** Filled at the positive end, emptied at the negative end. */
    private final FluidTank towardNegative = tank();
    private final IFluidHandler negativeEnd = new End(towardPositive, towardNegative);
    private final IFluidHandler positiveEnd = new End(towardNegative, towardPositive);

    public ContainmentPenetrationBlockEntity(BlockPos pos, BlockState state) {
        super(PwrRegistry.CONTAINMENT_PENETRATION_ENTITY.get(), pos, state);
    }

    private FluidTank tank() {
        return new FluidTank(BUFFER) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    private Direction.Axis axis() {
        BlockState state = getBlockState();
        return state.hasProperty(RotatedPillarBlock.AXIS) ? state.getValue(RotatedPillarBlock.AXIS) : Direction.Axis.Y;
    }

    /** The handler of one end; the sides along the wall carry nothing. */
    @Nullable
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        if (side == null || side.getAxis() != axis()) {
            return null;
        }
        return side.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? negativeEnd : positiveEnd;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        Direction.Axis axis = axis();
        push(towardPositive, Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE));
        push(towardNegative, Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE));
    }

    private void push(FluidTank buffer, Direction outlet) {
        if (buffer.isEmpty()) {
            return;
        }
        BlockPos target = worldPosition.relative(outlet);
        if (!level.isLoaded(target)) {
            return;
        }
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, target, outlet.getOpposite());
        if (handler == null) {
            return;
        }
        FluidStack offer = buffer.drain(PUSH_PER_TICK, IFluidHandler.FluidAction.SIMULATE);
        int accepted = handler.fill(offer, IFluidHandler.FluidAction.EXECUTE);
        if (accepted > 0) {
            buffer.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    public FluidTank getTowardPositive() {
        return towardPositive;
    }

    public FluidTank getTowardNegative() {
        return towardNegative;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable("block.create_nuclearindustry.containment_penetration"))
                .withStyle(ChatFormatting.GOLD));
        Direction.Axis axis = axis();
        line(tooltip, towardPositive, Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE));
        line(tooltip, towardNegative, Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE));
        return true;
    }

    private static void line(List<Component> tooltip, FluidTank buffer, Direction outlet) {
        Component fluid = buffer.isEmpty() ? Component.translatable("create_nuclearindustry.containment.penetration.empty")
                : buffer.getFluid().getHoverName();
        tooltip.add(Component.literal("     ").append(Component.translatable("create_nuclearindustry.containment.penetration.buffer",
                Component.translatable("create_nuclearindustry.containment.direction." + outlet.getSerializedName()), fluid,
                buffer.getFluidAmount(), BUFFER)).withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("TowardPositive", towardPositive.writeToNBT(registries, new CompoundTag()));
        tag.put("TowardNegative", towardNegative.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        towardPositive.readFromNBT(registries, tag.getCompound("TowardPositive"));
        towardNegative.readFromNBT(registries, tag.getCompound("TowardNegative"));
    }

    /** One end: fills the buffer heading in, drains the buffer coming out. */
    private record End(FluidTank in, FluidTank out) implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? in.getFluid() : out.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return BUFFER;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return in.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return out.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return out.drain(maxDrain, action);
        }
    }
}
