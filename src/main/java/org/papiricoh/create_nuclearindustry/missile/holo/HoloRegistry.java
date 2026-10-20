package org.papiricoh.create_nuclearindustry.missile.holo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearGUIs;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.creativetab.SectionedOutput;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonationEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The holographic command table: nine pieces that fuse into one table showing, in 3D over a map of the land, what its linked radars
 * track and where its silo can reach, and letting a player send the silo a target. Its pictures go to players near it, and every
 * nuclear detonation flashes on the tables that see it.
 */
public final class HoloRegistry {
    public static final DeferredHolder<Block, HoloTableBlock> TABLE = AllNuclearBlocks.BLOCKS.register("holo_table",
            () -> new HoloTableBlock(BlockBehaviour.Properties.of().strength(3, 6).noOcclusion().lightLevel(state -> 6)));
    public static final DeferredHolder<Item, BlockItem> ITEM = AllNuclearItems.ITEMS.register("holo_table", () -> new BlockItem(TABLE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HoloTableBlockEntity>> ENTITY = AllNuclearEntities.ENTITIES.register("holo_table",
            () -> BlockEntityType.Builder.of(HoloTableBlockEntity::new, TABLE.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<HoloTableMenu>> MENU = AllNuclearGUIs.MENUS.register("holo_table",
            () -> IMenuTypeExtension.create(HoloTableMenu::fromNetwork));

    /** Where the pictures go on a client; set by the client side. */
    public static Consumer<HoloTracksPayload> clientTracks = p -> {
    };
    public static Consumer<HoloDetonationPayload> clientDetonation = p -> {
    };

    private HoloRegistry() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(RegisterPayloadHandlersEvent.class, e -> {
            var registrar = e.registrar("1");
            registrar.playToClient(HoloTracksPayload.TYPE, HoloTracksPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> clientTracks.accept(p)));
            registrar.playToClient(HoloDetonationPayload.TYPE, HoloDetonationPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> clientDetonation.accept(p)));
            registrar.playToServer(HoloDesignatePayload.TYPE, HoloDesignatePayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer player) HoloDesignatePayload.handle(player, p);
            }));
        });
        NeoForge.EVENT_BUS.addListener(NuclearDetonationEvent.class, HoloRegistry::onDetonation);
    }

    /** A detonation flashes on every table whose players are near: they are sent it once each. */
    private static void onDetonation(NuclearDetonationEvent event) {
        Set<ServerPlayer> told = new HashSet<>();
        HoloDetonationPayload payload = new HoloDetonationPayload(event.getCenter().x, event.getCenter().y, event.getCenter().z, event.getRadius());
        for (HoloTableBlockEntity table : HoloTableBlockEntity.loaded(event.getLevel())) {
            for (ServerPlayer player : event.getLevel().players()) {
                if (player.distanceToSqr(table.getBlockPos().getCenter()) <= HoloTableBlockEntity.SIGHT * HoloTableBlockEntity.SIGHT && told.add(player))
                    PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    public static void fillCreativeTab(SectionedOutput output) {
        output.accept(ITEM.get());
    }
}
