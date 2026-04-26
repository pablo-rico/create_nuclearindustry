package org.papiricoh.create_nuclearindustry.network;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

@EventBusSubscriber(modid = Create_NuclearIndustry.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkEvents {

    @SubscribeEvent
    public static void onRegisterPayloadHandler(RegisterPayloadHandlerEvent event) {
        final IPayloadRegistrar registrar = event.registrar(Create_NuclearIndustry.MODID)
                .versioned("1.0.0");

        registrar.playToClient(
                SyncReactorIsFormedPacket.ID,
                SyncReactorIsFormedPacket.STREAM_CODEC,
                SyncReactorIsFormedPacket::handle
        );
    }
}
