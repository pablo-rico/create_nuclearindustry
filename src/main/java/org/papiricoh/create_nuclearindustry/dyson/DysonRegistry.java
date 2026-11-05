package org.papiricoh.create_nuclearindustry.dyson;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearGUIs;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.NuclearFeature;
import org.papiricoh.create_nuclearindustry.creativetab.NuclearTabSection;
import org.papiricoh.create_nuclearindustry.creativetab.SectionedOutput;
import org.papiricoh.create_nuclearindustry.dyson.ejector.EjectorLaunchPayload;
import org.papiricoh.create_nuclearindustry.dyson.ejector.EjectorMenu;
import org.papiricoh.create_nuclearindustry.dyson.ejector.EjectorOrbitPayload;
import org.papiricoh.create_nuclearindustry.dyson.ejector.EmRailEjectorBlock;
import org.papiricoh.create_nuclearindustry.dyson.ejector.EmRailEjectorBlockEntity;
import org.papiricoh.create_nuclearindustry.dyson.receiver.RayReceiverBlock;
import org.papiricoh.create_nuclearindustry.dyson.receiver.RayReceiverBlockEntity;
import org.papiricoh.create_nuclearindustry.dyson.swarm.DysonSwarm;
import org.papiricoh.create_nuclearindustry.dyson.swarm.SwarmSyncPayload;
import org.papiricoh.create_nuclearindustry.dyson.terminal.ConfigureSwarmOrbitPayload;
import org.papiricoh.create_nuclearindustry.dyson.terminal.SwarmTerminalBlock;
import org.papiricoh.create_nuclearindustry.dyson.terminal.SwarmTerminalBlockEntity;
import org.papiricoh.create_nuclearindustry.dyson.terminal.SwarmTerminalMenu;

import java.util.function.Consumer;

/**
 * The Dyson swarm: solar sails, the EM rail ejector that throws them into orbit, the ray receiver that turns their light into
 * Forge Energy, and the terminal where the orbits are set. The swarm itself is kept by {@link DysonSwarm}.
 */
public final class DysonRegistry implements NuclearFeature {
    public static final DysonRegistry INSTANCE = new DysonRegistry();

    public static final DeferredHolder<net.minecraft.world.level.block.Block, EmRailEjectorBlock> EM_RAIL_EJECTOR = AllNuclearBlocks.BLOCKS.register(
            "em_rail_ejector", () -> new EmRailEjectorBlock(machine(MapColor.COLOR_ORANGE)));
    public static final DeferredHolder<net.minecraft.world.level.block.Block, RayReceiverBlock> RAY_RECEIVER = AllNuclearBlocks.BLOCKS.register(
            "ray_receiver", () -> new RayReceiverBlock(machine(MapColor.GOLD)));
    public static final DeferredHolder<net.minecraft.world.level.block.Block, SwarmTerminalBlock> SWARM_TERMINAL = AllNuclearBlocks.BLOCKS.register(
            "swarm_terminal", () -> new SwarmTerminalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(3.5f, 6)
                    .sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().lightLevel(state -> 5)));

    public static final DeferredHolder<Item, BlockItem> EM_RAIL_EJECTOR_ITEM = AllNuclearItems.ITEMS.register("em_rail_ejector",
            () -> new BlockItem(EM_RAIL_EJECTOR.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, BlockItem> RAY_RECEIVER_ITEM = AllNuclearItems.ITEMS.register("ray_receiver",
            () -> new BlockItem(RAY_RECEIVER.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, BlockItem> SWARM_TERMINAL_ITEM = AllNuclearItems.ITEMS.register("swarm_terminal",
            () -> new BlockItem(SWARM_TERMINAL.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> SOLAR_SAIL = AllNuclearItems.ITEMS.register("solar_sail",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    /** A sail part-made on its sequenced assembly. */
    public static final DeferredHolder<Item, SequencedAssemblyItem> INCOMPLETE_SOLAR_SAIL = AllNuclearItems.ITEMS.register("incomplete_solar_sail",
            () -> new SequencedAssemblyItem(new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EmRailEjectorBlockEntity>> EJECTOR_ENTITY =
            AllNuclearEntities.ENTITIES.register("em_rail_ejector", () -> BlockEntityType.Builder.of(EmRailEjectorBlockEntity::new, EM_RAIL_EJECTOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RayReceiverBlockEntity>> RECEIVER_ENTITY =
            AllNuclearEntities.ENTITIES.register("ray_receiver", () -> BlockEntityType.Builder.of(RayReceiverBlockEntity::new, RAY_RECEIVER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SwarmTerminalBlockEntity>> TERMINAL_ENTITY =
            AllNuclearEntities.ENTITIES.register("swarm_terminal", () -> BlockEntityType.Builder.of(SwarmTerminalBlockEntity::new, SWARM_TERMINAL.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<EjectorMenu>> EJECTOR_MENU =
            AllNuclearGUIs.MENUS.register("em_rail_ejector", () -> IMenuTypeExtension.create(EjectorMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<SwarmTerminalMenu>> TERMINAL_MENU =
            AllNuclearGUIs.MENUS.register("swarm_terminal", () -> IMenuTypeExtension.create(SwarmTerminalMenu::fromNetwork));

    /** Where swarm updates go on a client; set by the client side. */
    public static Consumer<SwarmSyncPayload> clientSync = p -> {
    };

    private DysonRegistry() {
    }

    private static BlockBehaviour.Properties machine(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(5, 12).sound(SoundType.NETHERITE_BLOCK)
                .requiresCorrectToolForDrops().noOcclusion();
    }

    @Override
    public void register(IEventBus modEventBus) {
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            var registrar = event.registrar("1");
            registrar.playToServer(EjectorOrbitPayload.TYPE, EjectorOrbitPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer player) EjectorOrbitPayload.apply(player, p);
            }));
            registrar.playToServer(ConfigureSwarmOrbitPayload.TYPE, ConfigureSwarmOrbitPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer player) ConfigureSwarmOrbitPayload.apply(player, p);
            }));
            registrar.playToClient(EjectorLaunchPayload.TYPE, EjectorLaunchPayload.CODEC,
                    (p, ctx) -> ctx.enqueueWork(() -> EjectorLaunchPayload.handle(ctx.player().level(), p)));
            registrar.playToClient(SwarmSyncPayload.TYPE, SwarmSyncPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> clientSync.accept(p)));
        });
        NeoForge.EVENT_BUS.addListener(DysonSwarm::onLevelTick);
        NeoForge.EVENT_BUS.addListener(DysonSwarm::onLogin);
        NeoForge.EVENT_BUS.addListener(DysonSwarm::onDimensionChange);
    }

    @Override
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, EJECTOR_ENTITY.get(), (be, side) -> be.magazine());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, RECEIVER_ENTITY.get(), (be, side) -> be.energy());
    }

    @Override
    public void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, SectionedOutput output) {
        output.section(NuclearTabSection.DYSON);
        output.accept(SOLAR_SAIL.get());
        output.accept(EM_RAIL_EJECTOR_ITEM.get());
        output.accept(RAY_RECEIVER_ITEM.get());
        output.accept(SWARM_TERMINAL_ITEM.get());
    }
}
