package org.papiricoh.create_nuclearindustry.controlroom.desk.client;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.client.model.IModelBuilder;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.SimpleUnbakedGeometry;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A corner control desk: its hand-made body elements plus a slope baked here, since model elements
 * only make rectangles. The slopes of the two rows the corner joins meet on the diagonal in a mitre,
 * each carrying the surface of its row, so the mimic lines turn the corner. Inner corners meet in a
 * valley, outer corners in a hip. Native orientation as the other desk models: operator to the north.
 */
public class DeskCornerModel extends SimpleUnbakedGeometry<DeskCornerModel> {
    public static final ResourceLocation LOADER_ID =
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "control_desk_corner");
    private static final float TAN = (float) Math.tan(Math.toRadians(22.5));
    private static final float COS = (float) Math.cos(Math.toRadians(22.5));
    /** Where the 15 px slope of a straight desk ends, under its back rail. */
    private static final float END = 1 + 15 * COS;

    private final List<BlockElement> elements;
    private final boolean inner;

    public DeskCornerModel(List<BlockElement> elements, boolean inner) {
        this.elements = elements;
        this.inner = inner;
    }

    /** The slope of a straight desk whose operator stands to the north or to the west, in px. */
    private enum Slope {
        OPERATOR_NORTH, OPERATOR_WEST;

        /** Distance across the desk from the operator edge, which runs 1 px in from the operator side. */
        float depth(float x, float z) {
            return (this == OPERATOR_NORTH ? z : x) - 1;
        }

        /** Texture u along the row, matching a straight desk turned by its blockstate. */
        float u(float x, float z) {
            return this == OPERATOR_NORTH ? 16 - x : z;
        }

        /** Texture v: the surface texture has its back edge on row 0 and its front edge on row 15. */
        float v(float x, float z) {
            return 15 - depth(x, z) / COS;
        }
    }

    @Override
    protected void addQuads(IGeometryBakingContext context, IModelBuilder<?> modelBuilder, ModelBaker baker,
                            Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState) {
        ModelState state = context.getRootTransform().isIdentity() ? modelState
                : UnbakedGeometryHelper.composeRootTransformIntoModelState(modelState, context.getRootTransform());
        for (BlockElement element : elements) {
            for (Direction direction : element.faces.keySet()) {
                var face = element.faces.get(direction);
                BakedQuad quad = BlockModel.bakeFace(element, face, spriteGetter.apply(context.getMaterial(face.texture())), direction, state);
                if (face.cullForDirection() == null) {
                    modelBuilder.addUnculledFace(quad);
                } else {
                    modelBuilder.addCulledFace(state.getRotation().rotateTransform(face.cullForDirection()), quad);
                }
            }
        }

        TextureAtlasSprite surface = spriteGetter.apply(context.getMaterial("surface"));
        Matrix4f pose = state.getRotation().blockCenterToCorner().getMatrix();
        boolean ao = context.useAmbientOcclusion();
        if (inner) {
            // Rows carry on to the north (operator west) and to the west (operator north) round a notch at the north-west.
            modelBuilder.addUnculledFace(slope(Slope.OPERATOR_WEST, new float[][]{{1, 0}, {END, 0}, {END, END}, {1, 1}}, surface, pose, ao));
            modelBuilder.addUnculledFace(slope(Slope.OPERATOR_NORTH, new float[][]{{0, 1}, {1, 1}, {END, END}, {0, END}}, surface, pose, ao));
        } else {
            // Rows carry on to the east (operator north) and to the south (operator west) from a post at the south-east.
            modelBuilder.addUnculledFace(slope(Slope.OPERATOR_NORTH, new float[][]{{1, 1}, {16, 1}, {16, END}, {END, END}}, surface, pose, ao));
            modelBuilder.addUnculledFace(slope(Slope.OPERATOR_WEST, new float[][]{{1, 1}, {END, END}, {END, 16}, {1, 16}}, surface, pose, ao));
        }
    }

    /** One flat piece of slope over the given (x, z) px corners, facing up. */
    private static BakedQuad slope(Slope slope, float[][] corners, TextureAtlasSprite sprite, Matrix4f pose, boolean ao) {
        Vector3f[] positions = new Vector3f[4];
        float[][] uvs = new float[4][];
        for (int i = 0; i < 4; i++) {
            float x = corners[i][0];
            float z = corners[i][1];
            float depth = slope.depth(x, z);
            positions[i] = pose.transformPosition(new Vector3f(x / 16, (9 + TAN * depth) / 16, z / 16));
            uvs[i] = new float[]{sprite.getU(slope.u(x, z) / 16), sprite.getV(slope.v(x, z) / 16)};
        }
        Vector3f normal = new Vector3f(positions[2]).sub(positions[0])
                .cross(new Vector3f(positions[3]).sub(positions[1])).normalize();
        boolean flip = normal.y() < 0;
        if (flip) {
            normal.negate();
        }

        QuadBakingVertexConsumer baker = new QuadBakingVertexConsumer();
        baker.setSprite(sprite);
        baker.setDirection(Direction.getNearest(normal.x(), normal.y(), normal.z()));
        baker.setShade(true);
        baker.setHasAmbientOcclusion(ao);
        for (int i = 0; i < 4; i++) {
            int index = flip ? 3 - i : i;
            Vector3f position = positions[index];
            baker.addVertex(position.x(), position.y(), position.z())
                    .setColor(255, 255, 255, 255)
                    .setUv(uvs[index][0], uvs[index][1])
                    .setUv2(0, 0)
                    .setNormal(normal.x(), normal.y(), normal.z());
        }
        return baker.bakeQuad();
    }

    /** Reads {@code "corner": "inner" | "outer"} and the usual {@code elements} for the body. */
    public static final class Loader implements IGeometryLoader<DeskCornerModel> {
        public static final Loader INSTANCE = new Loader();

        private Loader() {}

        @Override
        public DeskCornerModel read(JsonObject json, JsonDeserializationContext context) throws JsonParseException {
            String corner = GsonHelper.getAsString(json, "corner");
            if (!corner.equals("inner") && !corner.equals("outer")) {
                throw new JsonParseException("A control desk corner is \"inner\" or \"outer\", not \"" + corner + "\"");
            }
            List<BlockElement> elements = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "elements")) {
                elements.add(context.deserialize(element, BlockElement.class));
            }
            return new DeskCornerModel(elements, corner.equals("inner"));
        }
    }
}
