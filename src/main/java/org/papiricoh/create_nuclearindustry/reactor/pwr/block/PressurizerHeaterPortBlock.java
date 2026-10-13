package org.papiricoh.create_nuclearindustry.reactor.pwr.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A pressurizer block with electric heaters. In automatic mode the reactor switches them to hold the
 * pressure setpoint; in manual mode (wrench toggles) they run while the block gets a redstone signal.
 */
public class PressurizerHeaterPortBlock extends Block {
    public static final BooleanProperty AUTOMATIC = BooleanProperty.create("automatic");
    private static final TagKey<Item> WRENCHES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

    public PressurizerHeaterPortBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AUTOMATIC, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AUTOMATIC);
    }

    /** Whether the heaters of the port at {@code pos} are forced on by hand. */
    public static boolean manualOn(Level level, BlockPos pos, BlockState state) {
        return !state.getValue(AUTOMATIC) && level.hasNeighborSignal(pos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(WRENCHES)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            boolean automatic = !state.getValue(AUTOMATIC);
            level.setBlock(pos, state.setValue(AUTOMATIC, automatic), UPDATE_ALL);
            player.displayClientMessage(Component.translatable(automatic
                    ? "create_nuclearindustry.pwr.heaters.automatic" : "create_nuclearindustry.pwr.heaters.manual"), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
