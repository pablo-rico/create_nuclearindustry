package org.papiricoh.create_nuclearindustry.missile.orbit;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.creativetab.SectionedOutput;

import java.util.function.Consumer;

/**
 * Satellites: the two kinds a rocket's satellite fairing carries ({@link SatelliteItem}), their part-made form, and the ground station
 * that links a command table to them. The fairing itself is a rocket section, in {@code SiloRegistry}; the satellites in orbit are
 * kept by {@link SatelliteTracker}.
 */
public final class OrbitRegistry {
    public static final DeferredHolder<Block, GroundStationBlock> GROUND_STATION = AllNuclearBlocks.BLOCKS.register("ground_station",
            () -> new GroundStationBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4, 12).noOcclusion().requiresCorrectToolForDrops()));
    public static final DeferredHolder<Item, BlockItem> GROUND_STATION_ITEM = AllNuclearItems.ITEMS.register("ground_station",
            () -> new BlockItem(GROUND_STATION.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GroundStationBlockEntity>> STATION_ENTITY =
            AllNuclearEntities.ENTITIES.register("ground_station", () -> BlockEntityType.Builder.of(GroundStationBlockEntity::new, GROUND_STATION.get()).build(null));
    public static final DeferredHolder<Item, SatelliteItem> EARLY_WARNING_SATELLITE = AllNuclearItems.ITEMS.register("early_warning_satellite",
            () -> new SatelliteItem(SatelliteKind.EARLY_WARNING, new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, SatelliteItem> COMMS_SATELLITE = AllNuclearItems.ITEMS.register("comms_satellite",
            () -> new SatelliteItem(SatelliteKind.COMMS, new Item.Properties().stacksTo(1)));
    /** Both satellites are put together on a sequenced assembly: the bus, then the payload. */
    public static final DeferredHolder<Item, SequencedAssemblyItem> INCOMPLETE_SATELLITE = AllNuclearItems.ITEMS.register("incomplete_satellite",
            () -> new SequencedAssemblyItem(new Item.Properties().stacksTo(1)));

    /** Where satellite lists go on a client; set by the client side. */
    public static Consumer<OrbitSyncPayload> clientSync = p -> {
    };

    private OrbitRegistry() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(RegisterPayloadHandlersEvent.class, e -> {
            var registrar = e.registrar("1");
            registrar.playToClient(OrbitSyncPayload.TYPE, OrbitSyncPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> clientSync.accept(p)));
        });
        NeoForge.EVENT_BUS.addListener(SatelliteTracker::onLevelTick);
        NeoForge.EVENT_BUS.addListener(SatelliteTracker::onLogin);
        NeoForge.EVENT_BUS.addListener(SatelliteTracker::onDimensionChange);
    }

    public static void fillCreativeTab(SectionedOutput output) {
        output.accept(GROUND_STATION_ITEM.get());
        output.accept(EARLY_WARNING_SATELLITE.get());
        output.accept(COMMS_SATELLITE.get());
    }
}
