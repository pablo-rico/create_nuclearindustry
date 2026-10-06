package org.papiricoh.create_nuclearindustry.turbine;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;

import java.util.List;

/**
 * An output port that gives the turbine's exhaust steam instead of condensate. Works on either
 * turbine; it takes the tier of the turbine it belongs to.
 */
public class TurbineExhaustPortBlockEntity extends TurbinePortBlockEntity {
    public TurbineExhaustPortBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.TURBINE_EXHAUST_PORT.get(), pos, state);
    }

    @Override
    protected TurbineTier tier() {
        return controller().map(AbstractTurbineControllerBlockEntity::tier).orElse(TurbineTier.STEAM);
    }

    @Override
    protected ReactorFluidPortMode mode() {
        return ReactorFluidPortMode.OUTPUT;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.translatable("block.create_nuclearindustry.turbine_exhaust_port").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Exhaust steam out, for a cooling tower").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  The turbine makes twice the power").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("  Status: " + (isLinked() ? "Linked" : "No turbine"))
                .withStyle(isLinked() ? ChatFormatting.GREEN : ChatFormatting.RED));
        return true;
    }
}
