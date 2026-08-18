package com.mistbeyond.transport.data.model

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mistbeyond.transport.Ids
import com.mistbeyond.transport.client.rail.model.RailGeometryParams
import net.minecraft.client.data.models.BlockModelGenerators
import net.minecraft.client.data.models.model.ModelInstance
import net.minecraft.resources.Identifier

/**
 * Programmatic track model generation.
 *
 * Simple track cells (straight and diagonal 45) share one parametric geometry. The straight model
 * (`block/track`) is the hand-made reference model and stays untouched; this generator emits the
 * diagonal model (`block/track_diagonal`) that replaces the previous handwritten Blockbench
 * export, which was asymmetric because its rotation origin was (6, 6) instead of the block center
 * and its sleeper positions were not symmetric about z = 8.
 *
 * Geometry is defined in a local frame (x across the track, y up, z along the track) and rotated
 * 45 degrees around Y about the block center (8, 8). All positions and the rotation origin are
 * computed in code, so the result is symmetric by construction and matches the straight model's
 * proportions: gauge 24 px (1.5 blocks), rails 2 x 2.4 px with 3 px base/top caps, sleepers
 * 33 x 1 x 3 px. Two diagonal-specific rules keep adjacent cells visually seamless:
 *
 *  - Rail elements span exactly the block diagonal (half-length 8*sqrt(2) px). Adjacent cells'
 *    rails therefore meet exactly at the seam plane: their end caps coincide back-to-back
 *    (opposite normals, so backface culling renders only one of them — no z-fighting) and their
 *    side faces abut, so the rail reads as one continuous piece. Any epsilon would leave a
 *    visible hairline, and any overlap would make coplanar same-facing faces z-fight.
 *  - Three sleepers per cell at spacing 16*sqrt(2)/3 px: the diagonal cell pitch is 16*sqrt(2) px,
 *    so an 8 px sleeper spacing (as on the straight track) would leave a visible gap at every
 *    cell boundary. 16*sqrt(2)/3 divides the pitch exactly and keeps the rhythm uniform.
 *
 * The emitted model uses the same +45 degree Y rotation as the old export, so the blockstate
 * mapping in [ModModelProvider] (NW_SE unrotated, NE_SW rotated 90 degrees) stays valid.
 */
object TrackModelGenerator {
    /** Emits the diagonal 45 straight track model (`block/track_diagonal`). */
    fun generateDiagonal(blockModels: BlockModelGenerators) {
        val id = Identifier.fromNamespaceAndPath(Ids.MOD_ID, "block/track_diagonal")
        blockModels.modelOutput.accept(id, ModelInstance { diagonalModel() })
    }

    private fun diagonalModel(): JsonObject {
        val root = JsonObject()
        val textures = JsonObject()
        textures.addProperty("rail", "block/iron_block")
        textures.addProperty("sleeper", "block/spruce_planks")
        textures.addProperty("particle", "block/iron_block")
        root.add("textures", textures)

        val elements = JsonArray()
        val halfGauge = RailGeometryParams.GAUGE / 2f
        val halfLength = RailGeometryParams.TRACK_HALF_LENGTH
        for (side in listOf(-1f, 1f)) {
            val railCenter = 8f + side * halfGauge
            // Rail web, base cap, top cap.
            elements.add(
                box(
                    railCenter - RailGeometryParams.RAIL_W / 2f, RailGeometryParams.RAIL_BOTTOM, 8f - halfLength,
                    railCenter + RailGeometryParams.RAIL_W / 2f, RailGeometryParams.RAIL_TOP, 8f + halfLength
                )
            )
            elements.add(
                box(
                    railCenter - RailGeometryParams.CAP_W / 2f,
                    RailGeometryParams.RAIL_BOTTOM - RailGeometryParams.CAP_H,
                    8f - halfLength,
                    railCenter + RailGeometryParams.CAP_W / 2f,
                    RailGeometryParams.RAIL_BOTTOM,
                    8f + halfLength
                )
            )
            elements.add(
                box(
                    railCenter - RailGeometryParams.CAP_W / 2f, RailGeometryParams.RAIL_TOP, 8f - halfLength,
                    railCenter + RailGeometryParams.CAP_W / 2f, RailGeometryParams.RAIL_TOP + RailGeometryParams.CAP_H,
                    8f + halfLength
                )
            )
        }
        for (offset in listOf(-RailGeometryParams.SLEEPER_SPACING, 0f, RailGeometryParams.SLEEPER_SPACING)) {
            // Sleepers use the same 16px-period UV convention as the hand-made straight model (block/track): one
            // texture period along the 33px long axis, a 1.5px v offset on the side faces. Using the box-extent UVs
            // (33px) here would sample two plank periods per sleeper and make the diagonal sleepers look denser than
            // the straight ones. The runtime dynamic model (TrackCellDynamicModel.faceUvs) mirrors exactly this.
            elements.add(sleeperBox(offset))
        }
        root.add("elements", elements)
        return root
    }

    private fun box(fromX: Float, fromY: Float, fromZ: Float, toX: Float, toY: Float, toZ: Float): JsonObject {
        val dx = toX - fromX
        val dy = toY - fromY
        val dz = toZ - fromZ
        val element = JsonObject()
        element.add("from", vec(fromX, fromY, fromZ))
        element.add("to", vec(toX, toY, toZ))
        val rotation = JsonObject()
        rotation.add(
            "origin",
            vec(RailGeometryParams.ORIGIN_X, RailGeometryParams.ORIGIN_Y, RailGeometryParams.ORIGIN_Z),
        )
        rotation.addProperty("axis", "y")
        rotation.addProperty("angle", RailGeometryParams.DIAGONAL_ANGLE)
        element.add("rotation", rotation)
        val faces = JsonObject()
        faces.add("north", railFace(dx, dy))
        faces.add("south", railFace(dx, dy))
        faces.add("east", railFace(dz, dy))
        faces.add("west", railFace(dz, dy))
        faces.add("up", railFace(dx, dz))
        faces.add("down", railFace(dx, dz))
        element.add("faces", faces)
        return element
    }

    /** Rail face: box-extent UVs on the rail texture. */
    private fun railFace(uvW: Float, uvH: Float): JsonObject {
        val face = JsonObject()
        val uv = JsonArray()
        uv.add(0f)
        uv.add(0f)
        uv.add(uvW)
        uv.add(uvH)
        face.add("uv", uv)
        face.addProperty("texture", "#rail")
        return face
    }

    /** Sleeper slab element (long axis X, 33px) with the straight-model UV convention; centered at z = 8 + offset. */
    private fun sleeperBox(offset: Float): JsonObject {
        val halfSleeper = RailGeometryParams.SLEEPER_LEN / 2f
        val halfSleeperW = RailGeometryParams.SLEEPER_W / 2f
        val element = JsonObject()
        element.add("from", vec(8f - halfSleeper, 0f, 8f + offset - halfSleeperW))
        element.add("to", vec(8f + halfSleeper, RailGeometryParams.SLEEPER_H, 8f + offset + halfSleeperW))
        val rotation = JsonObject()
        rotation.add(
            "origin",
            vec(RailGeometryParams.ORIGIN_X, RailGeometryParams.ORIGIN_Y, RailGeometryParams.ORIGIN_Z),
        )
        rotation.addProperty("axis", "y")
        rotation.addProperty("angle", RailGeometryParams.DIAGONAL_ANGLE)
        element.add("rotation", rotation)
        val faces = JsonObject()
        // Long axis is X (33px), width is Z (3px), height is Y (1px); UVs match block/track's sleeper slabs.
        faces.add("north", sleeperFace(16f, 0.5f, 1.5f))
        faces.add("south", sleeperFace(16f, 0.5f, 1.5f))
        faces.add("east", sleeperFace(3f, 0.5f, 1.5f))
        faces.add("west", sleeperFace(3f, 0.5f, 1.5f))
        faces.add("up", sleeperFace(16f, 3f, 0f))
        faces.add("down", sleeperFace(16f, 3f, 0f))
        element.add("faces", faces)
        return element
    }

    /** Sleeper face: {@code [0, vOffset, uvW, vOffset + uvH]} on the sleeper texture, matching block/track. */
    private fun sleeperFace(uvW: Float, uvH: Float, vOffset: Float): JsonObject {
        val face = JsonObject()
        val uv = JsonArray()
        uv.add(0f)
        uv.add(vOffset)
        uv.add(uvW)
        uv.add(vOffset + uvH)
        face.add("uv", uv)
        face.addProperty("texture", "#sleeper")
        return face
    }

    private fun vec(x: Float, y: Float, z: Float): JsonArray {
        val array = JsonArray()
        array.add(x)
        array.add(y)
        array.add(z)
        return array
    }
}
