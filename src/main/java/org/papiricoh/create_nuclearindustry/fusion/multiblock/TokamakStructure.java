package org.papiricoh.create_nuclearindustry.fusion.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A validated tokamak: where it is, how big it is and what its shell is made of. */
public final class TokamakStructure {
    public final BlockPos controllerPos;
    public final BlockPos center;
    public final TokamakGeometry geometry;
    public final int[] coilsPerSector;
    public final Set<BlockPos> magnetInputs = new HashSet<>();
    public final Set<BlockPos> fluidPorts = new HashSet<>();
    public final Set<BlockPos> injectors = new HashSet<>();
    /** Every shell block by role, except plain vessel blocks. */
    private final Map<TokamakShellRole, List<BlockPos>> parts = new EnumMap<>(TokamakShellRole.class);
    /** Shell blocks with a job (anything but vessel, coil and blanket), which may hold a linked block entity. */
    private final Set<BlockPos> functionalParts = new HashSet<>();
    public int coilCount;
    public int blanketCount;
    public int coolantPortCount;
    public int steamPortCount;
    public int fuelInjectorCount;
    public int tritiumOutputCount;

    TokamakStructure(BlockPos controllerPos, BlockPos center, TokamakGeometry geometry) {
        this.controllerPos = controllerPos;
        this.center = center;
        this.geometry = geometry;
        this.coilsPerSector = new int[geometry.sectorCount()];
    }

    public int majorRadius() {
        return geometry.majorRadius();
    }

    public int plasmaVolume() {
        return geometry.plasmaCount();
    }

    public int shellSize() {
        return geometry.shellCount();
    }

    public BoundingBox bounds() {
        int e = geometry.extent();
        int h = TokamakGeometry.HALF_HEIGHT;
        return new BoundingBox(center.getX() - e, center.getY() - h, center.getZ() - e,
                center.getX() + e, center.getY() + h, center.getZ() + e);
    }

    /** Every shell, plasma and solenoid position, for change tracking. */
    public List<BlockPos> allPositions() {
        List<BlockPos> positions = new ArrayList<>(geometry.shellCount() + geometry.plasmaCount() + 5);
        for (int i = 0; i < geometry.shellCount(); i++) {
            positions.add(center.offset(geometry.shellX(i), geometry.shellY(i), geometry.shellZ(i)));
        }
        for (int i = 0; i < geometry.plasmaCount(); i++) {
            positions.add(center.offset(geometry.plasmaX(i), geometry.plasmaY(i), geometry.plasmaZ(i)));
        }
        for (int dy = -TokamakGeometry.HALF_HEIGHT; dy <= TokamakGeometry.HALF_HEIGHT; dy++) {
            positions.add(center.offset(0, dy, 0));
        }
        return positions;
    }

    void addPart(TokamakShellRole role, BlockPos pos) {
        if (role == TokamakShellRole.VESSEL) {
            return;
        }
        parts.computeIfAbsent(role, r -> new ArrayList<>()).add(pos.immutable());
        if (role != TokamakShellRole.COIL && role != TokamakShellRole.BLANKET) {
            functionalParts.add(pos.immutable());
        }
    }

    /** Positions of every shell block with this role (none for {@link TokamakShellRole#VESSEL}). */
    public List<BlockPos> parts(TokamakShellRole role) {
        return Collections.unmodifiableList(parts.getOrDefault(role, List.of()));
    }

    public int count(TokamakShellRole role) {
        return parts.getOrDefault(role, List.of()).size();
    }

    /** Shell blocks with a job: ports, injectors, drives, pumps, heaters, viewports. */
    public Set<BlockPos> functionalParts() {
        return Collections.unmodifiableSet(functionalParts);
    }

    public boolean containsPart(BlockPos pos) {
        return functionalParts.contains(pos);
    }

    public boolean containsFluidPort(BlockPos pos) {
        return fluidPorts.contains(pos);
    }

    public boolean containsInjector(BlockPos pos) {
        return injectors.contains(pos);
    }

    public boolean containsMagnetInput(BlockPos pos) {
        return magnetInputs.contains(pos);
    }

    /**
     * Toroidal field coverage, 0..1: each sector counts fully once it holds
     * {@code coilsNeeded} electromagnets, partially before that.
     */
    public double coilCoverage(int coilsNeeded) {
        double sum = 0;
        for (int coils : coilsPerSector) {
            sum += Math.min(1.0, coils / (double) coilsNeeded);
        }
        return sum / coilsPerSector.length;
    }

    /** Share of the shell taken by lithium-6 breeding blankets. */
    public double blanketFraction() {
        return blanketCount / (double) geometry.shellCount();
    }

    public String dimensionsText() {
        return "R=" + majorRadius() + " (" + (2 * geometry.extent() + 1) + "x" + (2 * TokamakGeometry.HALF_HEIGHT + 1) + ")";
    }
}
