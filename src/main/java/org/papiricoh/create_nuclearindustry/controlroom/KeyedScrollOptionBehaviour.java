package org.papiricoh.create_nuclearindustry.controlroom;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * A scroll option that can sit next to a {@link com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour}
 * on the same block entity. Create keeps one behaviour per type and every scroll value shares one
 * type and one NBT key, so a second one would silently replace the first: never drawn, never
 * clickable, never saved. This one brings its own type and its own key.
 */
public class KeyedScrollOptionBehaviour<E extends Enum<E> & INamedIconOptions> extends ScrollOptionBehaviour<E> {
    private final BehaviourType<KeyedScrollOptionBehaviour<E>> type;
    private final String key;
    private final int options;

    public KeyedScrollOptionBehaviour(BehaviourType<KeyedScrollOptionBehaviour<E>> type, String key, Class<E> enum_,
                                      Component label, SmartBlockEntity be, ValueBoxTransform slot) {
        super(enum_, label, be, slot);
        this.type = type;
        this.key = key;
        this.options = enum_.getEnumConstants().length;
    }

    @Override
    public BehaviourType<?> getType() {
        return type;
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.putInt(key, value);
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        value = Mth.clamp(tag.getInt(key), 0, options - 1);
    }
}
