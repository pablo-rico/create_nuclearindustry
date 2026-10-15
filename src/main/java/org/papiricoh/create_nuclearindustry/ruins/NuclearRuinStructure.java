package org.papiricoh.create_nuclearindustry.ruins;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

import java.util.Optional;

/**
 * A jigsaw structure that first asks its {@link RuinKind} whether it may generate at all (its
 * {@code ruins.*} switch and chance) and, with a {@code max_slope} of 0 or more, whether the ground
 * is flat and dry enough ({@link Flatness}). Vanilla's {@code JigsawStructure} is final, so the
 * placement itself is a plain call to {@link JigsawPlacement#addPieces}.
 */
public class NuclearRuinStructure extends Structure {
    /** Blocks from the chunk centre to the four corner samples of the flatness check. */
    static final int SLOPE_SAMPLE_RADIUS = 16;

    public static final MapCodec<NuclearRuinStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
            Codec.intRange(0, 20).fieldOf("size").forGetter(s -> s.size),
            HeightProvider.CODEC.fieldOf("start_height").forGetter(s -> s.startHeight),
            Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(s -> s.projectStartToHeightmap),
            Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistanceFromCenter),
            RuinKind.CODEC.fieldOf("kind").forGetter(s -> s.kind),
            Codec.intRange(-1, 256).optionalFieldOf("max_slope", -1).forGetter(s -> s.maxSlope)
    ).apply(instance, NuclearRuinStructure::new));

    private final Holder<StructureTemplatePool> startPool;
    private final int size;
    private final HeightProvider startHeight;
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    private final int maxDistanceFromCenter;
    private final RuinKind kind;
    private final int maxSlope;

    public NuclearRuinStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int size,
                                HeightProvider startHeight, Optional<Heightmap.Types> projectStartToHeightmap,
                                int maxDistanceFromCenter, RuinKind kind, int maxSlope) {
        super(settings);
        this.startPool = startPool;
        this.size = size;
        this.startHeight = startHeight;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.kind = kind;
        this.maxSlope = maxSlope;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!kind.allowed(context.random())) {
            return Optional.empty();
        }
        ChunkPos chunk = context.chunkPos();
        int slope = kind.maxSlope(maxSlope);
        if (slope >= 0 && !groundFits(context, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(), slope)) {
            return Optional.empty();
        }
        int y = startHeight.sample(context.random(), new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor()));
        return layout(context, new BlockPos(chunk.getMinBlockX(), y, chunk.getMinBlockZ()), projectStartToHeightmap);
    }

    /** Lays the pieces out from {@code start}, without the switch or ground checks (tests use it directly). */
    public Optional<GenerationStub> layout(GenerationContext context, BlockPos start, Optional<Heightmap.Types> heightmap) {
        return JigsawPlacement.addPieces(context, startPool, Optional.empty(), size, start, false, heightmap,
                maxDistanceFromCenter, PoolAliasLookup.EMPTY, DimensionPadding.ZERO, LiquidSettings.IGNORE_WATERLOGGING);
    }

    /** The centre and four corners of a square around it: level within {@code slope} blocks and with no water. */
    private static boolean groundFits(GenerationContext context, int x, int z, int slope) {
        int r = SLOPE_SAMPLE_RADIUS;
        int[][] points = {{x, z}, {x - r, z - r}, {x + r, z - r}, {x - r, z + r}, {x + r, z + r}};
        int[] surface = new int[points.length];
        int[] floor = new int[points.length];
        for (int i = 0; i < points.length; i++) {
            surface[i] = context.chunkGenerator().getFirstOccupiedHeight(points[i][0], points[i][1],
                    Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
            floor[i] = context.chunkGenerator().getFirstOccupiedHeight(points[i][0], points[i][1],
                    Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
        }
        return Flatness.isFlat(slope, surface) && Flatness.isDry(surface, floor);
    }

    public RuinKind kind() {
        return kind;
    }

    public int size() {
        return size;
    }

    @Override
    public StructureType<?> type() {
        return RuinsRegistry.NUCLEAR_RUIN.get();
    }
}
