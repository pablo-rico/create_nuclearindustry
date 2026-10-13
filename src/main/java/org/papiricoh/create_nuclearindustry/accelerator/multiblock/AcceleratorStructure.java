package org.papiricoh.create_nuclearindustry.accelerator.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A formed particle accelerator: the controller and its ion source, the injector line, and, when the
 * injector feeds a ring, the ring and its extraction line. Also every machine beside them.
 */
public final class AcceleratorStructure {
    public final BlockPos controller;
    public final Direction facing;
    public final BlockPos source;
    public final BeamlinePath injector;
    @Nullable
    public final RingSection ring;
    @Nullable
    public final BeamlinePath extraction;
    public final Map<AttachmentRole, List<BlockPos>> attachments;
    private final BoundingBox bounds;

    public AcceleratorStructure(BlockPos controller, Direction facing, BlockPos source, BeamlinePath injector,
                                @Nullable RingSection ring, @Nullable BeamlinePath extraction,
                                Map<AttachmentRole, List<BlockPos>> attachments) {
        this.controller = controller.immutable();
        this.facing = facing;
        this.source = source.immutable();
        this.injector = injector;
        this.ring = ring;
        this.extraction = extraction;
        this.attachments = attachments;
        this.bounds = computeBounds();
    }

    /** The line whose end the beam reaches: the extraction line of a ring, or the injector line. */
    public BeamlinePath deliveryLine() {
        return extraction != null ? extraction : injector;
    }

    public BlockPos terminal() {
        return deliveryLine().terminal();
    }

    public BeamElementRole terminalRole() {
        return deliveryLine().terminalRole();
    }

    public int copperCavities() {
        return injector.count(BeamElementRole.RF_CAVITY);
    }

    public int superconductingCavities() {
        return injector.count(BeamElementRole.SC_RF_CAVITY);
    }

    public int magnets() {
        return injector.magnets() + (ring == null ? 0 : ring.magnets()) + (extraction == null ? 0 : extraction.magnets());
    }

    /** Superconducting magnets and cavities in the whole machine. */
    public int superconducting() {
        return injector.superconducting() + (ring == null ? 0 : ring.superconducting())
                + (extraction == null ? 0 : extraction.superconducting());
    }

    private int superconductingMagnets() {
        return injector.count(BeamElementRole.SC_DIPOLE) + (ring == null ? 0 : ring.count(BeamElementRole.SC_DIPOLE))
                + (extraction == null ? 0 : extraction.count(BeamElementRole.SC_DIPOLE));
    }

    /** Magnets the power supplies must feed; superconducting ones count half. */
    public double magnetLoad() {
        int sc = superconductingMagnets();
        return magnets() - sc + 0.5 * sc;
    }

    /** Beamline blocks the pumps keep at vacuum: the source, every line and the ring. */
    public int cells() {
        return 1 + injector.cells().size() + (ring == null ? 0 : ring.cells()) + (extraction == null ? 0 : extraction.cells().size());
    }

    public List<BlockPos> attachments(AttachmentRole role) {
        return attachments.getOrDefault(role, List.of());
    }

    /** Every attachment, the ion source and the terminal: the block entities the controller talks to. */
    public List<BlockPos> functionalParts() {
        List<BlockPos> parts = new ArrayList<>();
        parts.add(source);
        attachments.values().forEach(parts::addAll);
        parts.add(terminal());
        return parts;
    }

    /** Every block of the structure, for the snapshot. */
    public Set<BlockPos> allPositions() {
        Set<BlockPos> all = new LinkedHashSet<>();
        all.add(controller);
        all.add(source);
        all.addAll(injector.cells());
        if (ring != null) {
            for (int i = 0; i < ring.geometry().cellCount(); i++) {
                all.add(ring.center().offset(ring.geometry().dx(i), 0, ring.geometry().dz(i)));
            }
        }
        if (extraction != null) {
            all.addAll(extraction.cells());
        }
        attachments.values().forEach(all::addAll);
        return all;
    }

    public BoundingBox bounds() {
        return bounds;
    }

    private BoundingBox computeBounds() {
        int minX = controller.getX(), minY = controller.getY(), minZ = controller.getZ();
        int maxX = minX, maxY = minY, maxZ = minZ;
        List<BlockPos> corners = new ArrayList<>(injector.cells());
        corners.add(source);
        if (extraction != null) {
            corners.addAll(extraction.cells());
        }
        if (ring != null) {
            int r = ring.radius() + 1;
            corners.add(ring.center().offset(-r, 0, -r));
            corners.add(ring.center().offset(r, 0, r));
        }
        attachments.values().forEach(corners::addAll);
        for (BlockPos p : corners) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
            maxZ = Math.max(maxZ, p.getZ());
        }
        return new BoundingBox(minX - 1, minY - 1, minZ - 1, maxX + 1, maxY + 1, maxZ + 1);
    }

    /** Where the beam goes, for the renderer: straight segments between these points, in order. */
    public List<BlockPos> injectorWaypoints() {
        List<BlockPos> points = new ArrayList<>();
        points.add(source);
        points.addAll(injector.waypoints());
        return points;
    }

    static Map<AttachmentRole, List<BlockPos>> merge(List<Map<AttachmentRole, List<BlockPos>>> maps) {
        Map<AttachmentRole, Set<BlockPos>> merged = new EnumMap<>(AttachmentRole.class);
        for (Map<AttachmentRole, List<BlockPos>> map : maps) {
            map.forEach((role, list) -> merged.computeIfAbsent(role, r -> new LinkedHashSet<>()).addAll(list));
        }
        Map<AttachmentRole, List<BlockPos>> out = new EnumMap<>(AttachmentRole.class);
        merged.forEach((role, set) -> out.put(role, List.copyOf(set)));
        return out;
    }
}
