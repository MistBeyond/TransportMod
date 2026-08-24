package com.mistbeyond.transport.client.rail.model;

import com.mistbeyond.transport.Ids;
import com.mistbeyond.transport.api.rail.graph.TrackCellData;
import com.mistbeyond.transport.block.rail.RailTrackCellBlock;
import com.mistbeyond.transport.block.rail.RailTrackCellBlockEntity;
import com.mojang.math.OctahedralGroup;
import com.mojang.math.Quadrant;
import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Baked-model route for track cells (ADR 0006): a custom unbaked block-state model whose geometry is resolved per
 * block entity from {@link TrackCellData} via per-block-entity {@link ModelData}. On a meshing worker thread the
 * level-aware {@code collectParts} overload reads the cell data from the level's model data and emits quads merged
 * into the chunk mesh. Because one BlockState cannot distinguish a simple cell from a complex one (both store the
 * same four axes), the same model is used for all states: complex cells render their placement union, simple cells
 * fall back to the matching axis geometry generated with the same {@link RailGeometryParams}, so visuals stay
 * identical to the datagen models.
 *
 * <p>Quads are produced by the very same {@link FaceBakery} the datagen models go through, with the same {@code
 * ModelState} conventions (90-degree multiples use {@link BlockModelRotation}, exactly like a variant rotation,
 * including its UV transform and vertex winding recalculation; 45/135 degrees use an arbitrary-angle model state,
 * mirroring the datagen diagonal elements' element-level rotation: no winding recalculation, untransformed UVs).
 * Lighting, ambient occlusion, normals and face directions are therefore guaranteed to match the datagen rendering.
 */
public class TrackCellDynamicModel implements CustomUnbakedBlockStateModel {
    public static final TrackCellDynamicModel INSTANCE = new TrackCellDynamicModel();
    public static final MapCodec<TrackCellDynamicModel> CODEC = MapCodec.unit(INSTANCE);

    private static final Material RAIL_MATERIAL = new Material(Identifier.withDefaultNamespace("block/iron_block"));
    private static final Material SLEEPER_MATERIAL = new Material(Identifier.withDefaultNamespace("block/spruce_planks"));
    private static final Material SIGNAL_BLOCK_MATERIAL = new Material(Identifier.withDefaultNamespace("block/redstone_block"));
    private static final Material SIGNAL_PATH_MATERIAL = new Material(Identifier.withDefaultNamespace("block/lapis_block"));
    private static final ModelDebugName DEBUG_NAME = () -> Ids.MOD_ID + ":track_cell";

    /**
     * Per-face UVs in the same convention the datagen models use ({@code [0, 0, width, height]} with the face's
     * extent in its two tangent axes, rather than absolute model coordinates): this samples the same side of the
     * 16px-texture as the old models. {@code FaceBakery} further rotates these UVs by the model rotation's UV
     * transform for 90-degree states, exactly like the datagen variant rendering.
     *
     * <p>Sleepers use one convention for every orientation: the hand-made straight model ({@code track.json}) maps
     * them with one 16px texture period along the 33px long axis and a 1.5px v offset on the side faces. The
     * diagonal geometry applies the same UVs (the 45/135-degree model states do not transform UVs, so the plank
     * texture then runs along the sleeper's long axis exactly like on the straight track). Using the box-extent UVs
     * (33px) for diagonals would sample two plank periods per sleeper and make the diagonal sleepers look denser
     * than the straight ones.
     */
    static CuboidFace.UVs faceUvs(TrackCellGeometry.Element element, Direction face) {
        float dx = element.toX() - element.fromX();
        float dy = element.toY() - element.fromY();
        float dz = element.toZ() - element.fromZ();
        if ("sleeper".equals(element.texture())) {
            return switch (face.getAxis()) {
                case X -> new CuboidFace.UVs(0.0F, 1.5F, 3.0F, 2.0F);
                case Y -> new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 3.0F);
                case Z -> new CuboidFace.UVs(0.0F, 1.5F, 16.0F, 2.0F);
            };
        }
        return switch (face.getAxis()) {
            case X -> new CuboidFace.UVs(0.0F, 0.0F, dz, dy);
            case Y -> new CuboidFace.UVs(0.0F, 0.0F, dx, dz);
            case Z -> new CuboidFace.UVs(0.0F, 0.0F, dx, dy);
        };
    }

    private TrackCellDynamicModel() {
    }

    @Override
    public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
        return CODEC;
    }

    @Override
    public void resolveDependencies(ResolvableModel.Resolver resolver) {
        // The two block textures live in the block atlas, which is always loaded; there are no model-file dependencies.
    }

    @Override
    public BlockStateModel bake(ModelBaker baker) {
        Material.Baked rail = baker.materials().get(RAIL_MATERIAL, DEBUG_NAME);
        Material.Baked sleeper = baker.materials().get(SLEEPER_MATERIAL, DEBUG_NAME);
        Material.Baked signalBlock = baker.materials().get(SIGNAL_BLOCK_MATERIAL, DEBUG_NAME);
        Material.Baked signalPath = baker.materials().get(SIGNAL_PATH_MATERIAL, DEBUG_NAME);
        return new Baked(rail, sleeper, signalBlock, signalPath, baker);
    }

    /**
     * The per-cell baked model. Immutable and thread-safe once baked; the baker's interner is a Guava strong interner
     * (thread-safe) so baking quads on the meshing threads is safe. The only per-position state enters through the
     * {@code collectParts} level parameter on the meshing thread.
     */
    private record Baked(Material.Baked rail, Material.Baked sleeper, Material.Baked signalBlock,
                         Material.Baked signalPath, ModelBaker baker) implements BlockStateModel {
        @Override
        public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
                                 List<BlockStateModelPart> parts) {
            List<TrackCellGeometry.Element> elements = elementsFor(level, pos, state);
            if (elements.isEmpty()) {
                return;
            }
            // Batch quads by texture to preserve material correctness; placeholder signal textures use distinct materials.
            // For simplicity, we bake all quads with per-element material selection but still return a single part holding
            // all quads: the part's particleMaterial is rail, but actual quads carry their own material via the baked quad's sprite.
            // NeoForge's quad baking embeds the sprite, so using a single part is acceptable for this MVP.
            parts.add(new BakedPart(bakeQuads(elements), rail));
        }

        @Override
        public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
            // Deprecated fallback without level context; the chunk mesher uses the level-aware overload.
        }

        @Override
        public Material.Baked particleMaterial() {
            return rail;
        }

        @Override
        public int materialFlags() {
            return 0;
        }

        private static List<TrackCellGeometry.Element> elementsFor(BlockAndTintGetter level, BlockPos pos,
                                                                   BlockState state) {
            TrackCellData data = level.getModelData(pos).get(RailTrackCellBlockEntity.TRACK_CELL_MODEL_DATA);
            if (data != null && !data.placements().isEmpty()) {
                return TrackCellGeometry.elementsFor(data);
            }
            return TrackCellGeometry.elementsForAxis(state.getValue(RailTrackCellBlock.DIRECTION));
        }

        /**
         * Bakes the axis-aligned element boxes with the vanilla cuboid baker. This intentionally reuses the datagen
         * rendering path instead of hand-constructing quads: {@code FaceBakery} derives face directions from the
         * rotated vertices, applies the same UV transformation rules, recalculates the vertex winding for 90-degree
         * rotations (the order the lighters expect for ambient occlusion) and computes the face normals, so the
         * runtime geometry is pixel-identical to the datagen models under every lighting path.
         */
        private List<BakedQuad> bakeQuads(List<TrackCellGeometry.Element> elements) {
            List<BakedQuad> quads = new ArrayList<>(elements.size() * 6);
            for (TrackCellGeometry.Element element : elements) {
                Material.Baked material = switch (element.texture()) {
                    case "sleeper" -> sleeper;
                    case "signal_block" -> signalBlock;
                    case "signal_path" -> signalPath;
                    default -> rail;
                };
                Vector3f from = new Vector3f(element.fromX(), element.fromY(), element.fromZ());
                Vector3f to = new Vector3f(element.toX(), element.toY(), element.toZ());
                ModelState state = modelState(element.rotationDegrees());
                for (Direction face : Direction.values()) {
                    CuboidFace cuboidFace = new CuboidFace(null, -1, "mtm:track", faceUvs(element, face), Quadrant.R0);
                    quads.add(FaceBakery.bakeQuad(baker, from, to, cuboidFace, material, face, state, null, true, 0));
                }
            }
            return quads;
        }

        /**
         * The model state used for the element rotation. 90-degree multiples map to {@link BlockModelRotation},
         * the exact same model state a vanilla variant rotation produces (with its UV transform and winding
         * recalculation); other angles use a pure Y rotation about the block center, matching the element-level
         * rotations of the datagen diagonal models (no UV transform, no winding recalculation).
         */
        private static ModelState modelState(float degrees) {
            int rounded = Math.round(degrees) % 360;
            if (rounded < 0) {
                rounded += 360;
            }
            return switch (rounded) {
                case 0 -> BlockModelRotation.IDENTITY;
                case 90 -> BlockModelRotation.get(OctahedralGroup.ROT_90_Y_POS);
                case 180 -> BlockModelRotation.get(OctahedralGroup.ROT_180_FACE_XZ);
                case 270 -> BlockModelRotation.get(OctahedralGroup.ROT_90_Y_NEG);
                default -> new ModelState() {
                    @Override
                    public Transformation transformation() {
                        return new Transformation(new Matrix4f().rotationY((float) Math.toRadians(degrees)));
                    }
                };
            };
        }
    }

    private record BakedPart(List<BakedQuad> quads, Material.Baked particle) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction side) {
            if (side == null) {
                return quads;
            }
            List<BakedQuad> result = new ArrayList<>(4);
            for (BakedQuad quad : quads) {
                if (quad.direction() == side) {
                    result.add(quad);
                }
            }
            return result;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }

        @Override
        public Material.Baked particleMaterial() {
            return particle;
        }

        @Override
        public int materialFlags() {
            return 0;
        }
    }
}