package org.papiricoh.create_nuclearindustry.fusion.cryo.plant;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.papiricoh.create_nuclearindustry.fusion.cryo.physics.CryoplantModel;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A validated cryogenic plant: a 3x3 block of casing 3 to 5 high, its controller, ports and compressors. */
public final class CryoplantStructure {
    public static final int MIN_HEIGHT = 3;
    public static final int MAX_HEIGHT = 5;

    /** Centre of the bottom layer. */
    public final BlockPos bottomCenter;
    public final BlockPos controller;
    public final int height;
    private final Map<Long, CryoplantPortMode> ports = new HashMap<>();
    private final Map<CryoplantPortMode, Integer> modeCounts = new EnumMap<>(CryoplantPortMode.class);
    private final List<BlockPos> compressors = new ArrayList<>();

    public CryoplantStructure(BlockPos bottomCenter, BlockPos controller, int height) {
        this.bottomCenter = bottomCenter.immutable();
        this.controller = controller.immutable();
        this.height = height;
    }

    void addPort(BlockPos pos, CryoplantPortMode mode) {
        ports.put(pos.asLong(), mode);
        modeCounts.merge(mode, 1, Integer::sum);
    }

    void addCompressor(BlockPos pos) {
        compressors.add(pos.immutable());
    }

    public int ports(CryoplantPortMode mode) {
        return modeCounts.getOrDefault(mode, 0);
    }

    public boolean ownsPort(BlockPos pos) {
        return ports.containsKey(pos.asLong());
    }

    public boolean ownsCompressor(BlockPos pos) {
        return compressors.contains(pos);
    }

    public List<BlockPos> portPositions() {
        return ports.keySet().stream().map(BlockPos::of).toList();
    }

    public List<BlockPos> compressorPositions() {
        return List.copyOf(compressors);
    }

    /** A plant with a Liquid Air port draws in air; otherwise it liquefies the helium loop. */
    public CryoplantModel.Mode mode() {
        return ports(CryoplantPortMode.LIQUID_AIR_OUT) > 0 ? CryoplantModel.Mode.AIR : CryoplantModel.Mode.HELIUM;
    }

    public BoundingBox bounds() {
        return new BoundingBox(bottomCenter.getX() - 1, bottomCenter.getY(), bottomCenter.getZ() - 1,
                bottomCenter.getX() + 1, bottomCenter.getY() + height - 1, bottomCenter.getZ() + 1);
    }

    public boolean contains(BlockPos pos) {
        return bounds().isInside(pos);
    }

    /** Every block of the plant. */
    public List<BlockPos> allPositions() {
        List<BlockPos> positions = new ArrayList<>(9 * height);
        for (int y = 0; y < height; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    positions.add(bottomCenter.offset(dx, y, dz));
                }
            }
        }
        return positions;
    }
}
