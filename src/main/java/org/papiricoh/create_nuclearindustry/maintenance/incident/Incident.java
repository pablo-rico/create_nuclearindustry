package org.papiricoh.create_nuclearindustry.maintenance.incident;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.papiricoh.create_nuclearindustry.maintenance.physics.InesScale;

/**
 * One entry of the incident log.
 *
 * @param gameTime  overworld game time it happened
 * @param dimension dimension id
 * @param pos       where
 * @param type      what
 * @param ines      its INES level, or {@link InesScale#NONE}
 * @param detail    free text for the line (a magnitude, a part), may be empty
 */
public record Incident(long gameTime, String dimension, BlockPos pos, IncidentType type, int ines, String detail) {
    public static final String KEY = "create_nuclearindustry.incident.";

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Time", gameTime);
        tag.putString("Dimension", dimension);
        tag.putLong("Pos", pos.asLong());
        tag.putString("Type", type.id());
        tag.putInt("Ines", ines);
        tag.putString("Detail", detail);
        return tag;
    }

    public static Incident load(CompoundTag tag) {
        return new Incident(tag.getLong("Time"), tag.getString("Dimension"), BlockPos.of(tag.getLong("Pos")),
                IncidentType.byId(tag.getString("Type")), tag.getInt("Ines"), tag.getString("Detail"));
    }

    /** "INES 2" or "—", coloured by severity. */
    public MutableComponent level() {
        if (ines == InesScale.NONE) {
            return Component.translatable(KEY + "off_scale").withStyle(ChatFormatting.GRAY);
        }
        ChatFormatting colour = ines >= 4 ? ChatFormatting.DARK_RED : ines >= 2 ? ChatFormatting.RED : ines == 1 ? ChatFormatting.GOLD
                : ChatFormatting.YELLOW;
        return Component.translatable(KEY + "ines", ines).withStyle(colour);
    }

    /** "Steam generator tube rupture (M4.2)". */
    public MutableComponent title() {
        MutableComponent name = Component.translatable(KEY + "type." + type.id());
        return detail.isEmpty() ? name : name.append(" (" + detail + ")");
    }

    /** "Day 12: [INES 2] Steam generator tube rupture at 120, 64, -30". */
    public MutableComponent line() {
        return Component.translatable(KEY + "line", gameTime / 24000L, level(), title(), pos.toShortString());
    }
}
