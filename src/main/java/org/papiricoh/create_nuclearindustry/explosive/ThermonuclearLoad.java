package org.papiricoh.create_nuclearindustry.explosive;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The parts a thermonuclear warhead carries ({@link ThermonuclearStats}): what a thermonuclear cannon
 * shell goes off with.
 *
 * @param primary   the fission primary
 * @param secondary lithium deuteride in the secondary
 * @param tamper    depleted uranium blocks in the tamper
 */
public record ThermonuclearLoad(ThermonuclearStats.Primary primary, int secondary, int tamper) {
    public static final ThermonuclearLoad EMPTY = new ThermonuclearLoad(ThermonuclearStats.Primary.NONE, 0, 0);
    /** Most lithium deuteride and tamper a cannon shell has room for. */
    public static final int SHELL_SECONDARY = 4;
    public static final int SHELL_TAMPER = 2;
    /** A shell with all it can carry, round a plutonium primary. */
    public static final ThermonuclearLoad SHELL_FULL = new ThermonuclearLoad(ThermonuclearStats.Primary.PLUTONIUM, SHELL_SECONDARY, SHELL_TAMPER);

    private static final Codec<ThermonuclearStats.Primary> PRIMARY_CODEC = Codec.INT.xmap(ThermonuclearStats.Primary::byId, Enum::ordinal);
    public static final Codec<ThermonuclearLoad> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PRIMARY_CODEC.fieldOf("primary").forGetter(ThermonuclearLoad::primary),
            Codec.INT.fieldOf("secondary").forGetter(ThermonuclearLoad::secondary),
            Codec.INT.fieldOf("tamper").forGetter(ThermonuclearLoad::tamper)).apply(instance, ThermonuclearLoad::new));
    public static final StreamCodec<ByteBuf, ThermonuclearLoad> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(ThermonuclearStats.Primary::byId, Enum::ordinal), ThermonuclearLoad::primary,
            ByteBufCodecs.VAR_INT, ThermonuclearLoad::secondary,
            ByteBufCodecs.VAR_INT, ThermonuclearLoad::tamper,
            ThermonuclearLoad::new);

    public ThermonuclearStats.Result stats(ThermonuclearStats.Caps caps) {
        return ThermonuclearStats.of(primary, secondary, tamper, caps);
    }

    public void write(CompoundTag tag) {
        tag.putInt("Primary", primary.ordinal());
        tag.putInt("Secondary", secondary);
        tag.putInt("Tamper", tamper);
    }

    /** The load saved in {@code tag}, or a full shell's if none was saved. */
    public static ThermonuclearLoad read(CompoundTag tag) {
        if (!tag.contains("Primary")) {
            return SHELL_FULL;
        }
        return new ThermonuclearLoad(ThermonuclearStats.Primary.byId(tag.getInt("Primary")), tag.getInt("Secondary"), tag.getInt("Tamper"));
    }
}
