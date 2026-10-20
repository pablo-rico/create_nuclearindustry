package org.papiricoh.create_nuclearindustry.missile.holo;

import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.Locale;

/** Player → server: send the table's silo a new target at ({@code x, z}), ground or air burst. Checked by {@link HoloTableBlockEntity#designate}. */
public record HoloDesignatePayload(BlockPos centre, int x, int z, boolean airburst) implements CustomPacketPayload {
    public static final Type<HoloDesignatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "holo_designate"));
    public static final StreamCodec<ByteBuf, HoloDesignatePayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, HoloDesignatePayload::centre,
            ByteBufCodecs.VAR_INT, HoloDesignatePayload::x,
            ByteBufCodecs.VAR_INT, HoloDesignatePayload::z,
            ByteBufCodecs.BOOL, HoloDesignatePayload::airburst,
            HoloDesignatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(ServerPlayer player, HoloDesignatePayload p) {
        if (!(player.containerMenu instanceof HoloTableMenu menu) || !menu.centre.equals(p.centre)
                || !(player.level().getBlockEntity(p.centre) instanceof HoloTableBlockEntity table)) return;
        HoloTableBlockEntity.Designation result = table.designate(player, p.x, p.z, p.airburst);
        String key = "holo_table.create_nuclearindustry.designate." + result.name().toLowerCase(Locale.ROOT);
        player.displayClientMessage(Component.translatable(key, p.x, p.z).withStyle(result == HoloTableBlockEntity.Designation.OK ? ChatFormatting.GREEN
                : ChatFormatting.RED), true);
    }
}
