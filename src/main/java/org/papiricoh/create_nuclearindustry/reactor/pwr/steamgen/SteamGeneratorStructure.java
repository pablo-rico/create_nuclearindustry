package org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A validated steam generator: a 3x3 column of casing around a tube bundle, its controller and ports. */
public final class SteamGeneratorStructure {
    public static final int MIN_HEIGHT = 4;
    public static final int MAX_HEIGHT = 8;

    /** Centre of the bottom layer. */
    public final BlockPos bottomCenter;
    public final BlockPos controller;
    public final int height;
    public int tubes;
    private final Map<Long, SteamGeneratorPortRole> ports = new HashMap<>();
    private final Map<SteamGeneratorPortRole, Integer> roleCounts = new EnumMap<>(SteamGeneratorPortRole.class);

    public SteamGeneratorStructure(BlockPos bottomCenter, BlockPos controller, int height) {
        this.bottomCenter = bottomCenter;
        this.controller = controller;
        this.height = height;
    }

    void addPort(BlockPos pos, SteamGeneratorPortRole role) {
        ports.put(pos.asLong(), role);
        roleCounts.merge(role, 1, Integer::sum);
    }

    public int ports(SteamGeneratorPortRole role) {
        return roleCounts.getOrDefault(role, 0);
    }

    public boolean ownsPort(BlockPos pos) {
        return ports.containsKey(pos.asLong());
    }

    public List<BlockPos> portPositions() {
        return ports.keySet().stream().map(BlockPos::of).toList();
    }

    public BoundingBox bounds() {
        return new BoundingBox(bottomCenter.getX() - 1, bottomCenter.getY(), bottomCenter.getZ() - 1,
                bottomCenter.getX() + 1, bottomCenter.getY() + height - 1, bottomCenter.getZ() + 1);
    }

    /** Every block of the generator. */
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

    public String dimensionsText() {
        return "3x3x" + height;
    }
}
