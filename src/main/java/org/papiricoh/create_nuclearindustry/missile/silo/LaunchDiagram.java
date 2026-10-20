package org.papiricoh.create_nuclearindustry.missile.silo;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * What the launch controller's screen draws its cutaway and stage table from: the column standing on the base, cell by cell
 * from the bottom (plus a ghost cell for the next block to add), its stages, its warhead, where it is wrong and what to do
 * about it. Built on the server while the screen is open and sent with {@link SiloDiagramPayload}.
 *
 * @param column     cells bottom to top, at most {@link #MAX_CELLS}
 * @param stages     one per stage, first stage first, at most 3
 * @param errorCell  the cell an error is pinned to, or -1
 * @param errorWhere where the error lies: one of {@code WHERE_*}
 * @param error      the error's lang suffix ({@code silo.create_nuclearindustry.error.*}), empty if none
 * @param advice     what to add or change next, at most {@link #MAX_ADVICE}
 * @param guidance   the {@code GuidanceTier} ordinal
 * @param site       {@code SITE_*}: no base found, an open platform or a walled silo
 * @param launchDelay ticks a salvo holds the countdown back by
 */
public record LaunchDiagram(List<Cell> column, List<StageInfo> stages, WarheadInfo warhead, int errorCell, byte errorWhere,
                            String error, List<Advice> advice, float reach, float cep, byte guidance, byte site, int launchDelay) {
    public static final int MAX_CELLS = 18, MAX_STAGES = 3, MAX_ADVICE = 4, MAX_TEXT = 64;
    public static final byte WHERE_NONE = 0, WHERE_CELL = 1, WHERE_BASE = 2, WHERE_CLEARANCE = 3, WHERE_EXIT = 4, WHERE_CONTROLLER = 5;
    public static final byte SITE_NONE = 0, SITE_PLATFORM = 1, SITE_SILO = 2;
    public static final LaunchDiagram EMPTY = new LaunchDiagram(List.of(), List.of(), WarheadInfo.NONE, -1, WHERE_NONE, "", List.of(),
            0, 0, (byte) 0, SITE_NONE, 0);

    /**
     * One block of the column.
     *
     * @param section the {@code RocketComposition.Section} ordinal, or {@link #FOREIGN} for a block that is not a section
     * @param stage   0–2 for a stage, {@link #PAYLOAD} for guidance and warhead, -1 for none
     * @param fill    propellant or load, 0–100
     * @param flags   {@code ERROR | LIQUID | LOADED | GHOST}
     */
    public record Cell(byte section, byte stage, byte fill, byte flags) {
        public static final byte FOREIGN = 4, PAYLOAD = 3;
        public static final byte ERROR = 1, LIQUID = 2, LOADED = 4, GHOST = 8;

        public boolean has(byte flag) {
            return (flags & flag) != 0;
        }
    }

    /**
     * One stage, as the rocket flies it.
     *
     * @param propellant 0 solid, 1 liquid
     * @param fill       propellant loaded, 0–100
     * @param twr        thrust to weight at ignition
     * @param deltaV     velocity it adds, blocks per tick
     * @param burnSeconds how long it burns
     * @param reachUpTo  how far the rocket reaches with this stage and those under it, blocks
     */
    public record StageInfo(byte tanks, byte propellant, byte fill, float twr, float deltaV, float burnSeconds, float reachUpTo) {
    }

    /**
     * The warhead.
     *
     * @param kind    the {@code Warhead.Kind} ordinal
     * @param power   blast power
     * @param radius  destruction radius, blocks
     * @param fallout fallout radius, blocks
     * @param mass    payload mass, rocket units
     */
    public record WarheadInfo(byte kind, float power, int radius, int fallout, float mass) {
        public static final WarheadInfo NONE = new WarheadInfo((byte) 0, 0, 0, 0, 0);
    }

    /**
     * Something to do at a cell.
     *
     * @param index        the cell, or the column's height to add on top
     * @param action       {@code ADD | REPLACE | REMOVE | INFO}
     * @param expectedMask bit per {@code RocketComposition.Section} ordinal of what belongs there
     * @param key          lang suffix ({@code silo.create_nuclearindustry.advice.*})
     * @param arg          a number the text shows (a stage, a count), or 0
     */
    public record Advice(byte index, byte action, byte expectedMask, String key, int arg) {
        public static final byte ADD = 0, REPLACE = 1, REMOVE = 2, INFO = 3;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(column.size());
        for (Cell c : column) { buf.writeByte(c.section); buf.writeByte(c.stage); buf.writeByte(c.fill); buf.writeByte(c.flags); }
        buf.writeVarInt(stages.size());
        for (StageInfo s : stages) {
            buf.writeByte(s.tanks); buf.writeByte(s.propellant); buf.writeByte(s.fill);
            buf.writeFloat(s.twr); buf.writeFloat(s.deltaV); buf.writeFloat(s.burnSeconds); buf.writeFloat(s.reachUpTo);
        }
        buf.writeByte(warhead.kind); buf.writeFloat(warhead.power); buf.writeVarInt(warhead.radius); buf.writeVarInt(warhead.fallout); buf.writeFloat(warhead.mass);
        buf.writeVarInt(errorCell + 1); buf.writeByte(errorWhere); buf.writeUtf(error, MAX_TEXT);
        buf.writeVarInt(advice.size());
        for (Advice a : advice) { buf.writeByte(a.index); buf.writeByte(a.action); buf.writeByte(a.expectedMask); buf.writeUtf(a.key, MAX_TEXT); buf.writeVarInt(a.arg); }
        buf.writeFloat(reach); buf.writeFloat(cep); buf.writeByte(guidance); buf.writeByte(site); buf.writeVarInt(launchDelay);
    }

    private static int bounded(FriendlyByteBuf buf, int max) {
        int n = buf.readVarInt();
        if (n < 0 || n > max) throw new IllegalArgumentException("launch diagram list of " + n + " (max " + max + ")");
        return n;
    }

    public static LaunchDiagram read(FriendlyByteBuf buf) {
        int cells = bounded(buf, MAX_CELLS);
        List<Cell> column = new ArrayList<>(cells);
        for (int i = 0; i < cells; i++) column.add(new Cell(buf.readByte(), buf.readByte(), buf.readByte(), buf.readByte()));
        int n = bounded(buf, MAX_STAGES);
        List<StageInfo> stages = new ArrayList<>(n);
        for (int i = 0; i < n; i++)
            stages.add(new StageInfo(buf.readByte(), buf.readByte(), buf.readByte(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat()));
        WarheadInfo warhead = new WarheadInfo(buf.readByte(), buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readFloat());
        int errorCell = buf.readVarInt() - 1;
        byte errorWhere = buf.readByte();
        String error = buf.readUtf(MAX_TEXT);
        int a = bounded(buf, MAX_ADVICE);
        List<Advice> advice = new ArrayList<>(a);
        for (int i = 0; i < a; i++) advice.add(new Advice(buf.readByte(), buf.readByte(), buf.readByte(), buf.readUtf(MAX_TEXT), buf.readVarInt()));
        return new LaunchDiagram(List.copyOf(column), List.copyOf(stages), warhead, errorCell, errorWhere, error, List.copyOf(advice),
                buf.readFloat(), buf.readFloat(), buf.readByte(), buf.readByte(), buf.readVarInt());
    }
}
