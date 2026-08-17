package com.mistbeyond.transport.data.model

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mistbeyond.transport.Ids
import net.minecraft.client.data.models.BlockModelGenerators
import net.minecraft.client.data.models.model.ModelInstance
import net.minecraft.resources.Identifier
import kotlin.math.sqrt

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
    /** Rail center distance: 24 px = 1.5 blocks (matches `block/track`). */
    private const val GAUGE = 24.0f

    /** Rail web cross-section: 2 px wide, 2.4 px tall (y 1.5..3.9). */
    private const val RAIL_W = 2.0f
    private const val RAIL_BOTTOM = 1.5f
    private const val RAIL_TOP = 3.9f

    /** Rail base/top caps: 3 px wide, 0.5 px tall. */
    private const val CAP_W = 3.0f
    private const val CAP_H = 0.5f

    /** Half-length of the rail elements along the local track axis, exactly the distance from the
     * block center to a corner (8*sqrt(2) px). Adjacent cells' rails then meet precisely at the
     * seam plane: the end caps coincide back-to-back and side faces abut, so the rail renders as
     * one continuous piece with no seam and no z-fighting (opposite-facing coincident faces are
     * resolved by backface culling). */
    private val TRACK_HALF_LENGTH: Float = 8f * sqrt(2f)

    /** Sleeper slab: 33 px long (overhangs into neighboring cells by design), 1 px tall, 3 px wide. */
    private const val SLEEPER_LEN = 33.0f
    private const val SLEEPER_H = 1.0f
    private const val SLEEPER_W = 3.0f

    /** Along-track spacing of the three sleepers. The diagonal cell pitch is 16*sqrt(2) px; three
     * sleepers per cell at spacing 16*sqrt(2)/3 keep the sleeper rhythm uniform across cells. The
     * straight track's 8 px spacing cannot be used here: 8 does not divide the diagonal pitch, so
     * an 8 px rhythm would leave a visible gap at every cell boundary. */
    private val SLEEPER_SPACING: Float = 16f * sqrt(2f) / 3f

    /** Rotation angle for the diagonal variant, in degrees. */
    private const val DIAGONAL_ANGLE = 45.0f

    /** Rotation origin: the block center. The y coordinate is irrelevant for a Y-axis rotation. */
    private val ORIGIN = floatArrayOf(8f, 8f, 8f)

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
        val halfGauge = GAUGE / 2f
        val halfLength = TRACK_HALF_LENGTH
        val halfSleeper = SLEEPER_LEN / 2f
        val halfSleeperW = SLEEPER_W / 2f
        for (side in listOf(-1f, 1f)) {
            val railCenter = 8f + side * halfGauge
            // Rail web, base cap, top cap.
            elements.add(
                box(
                    railCenter - RAIL_W / 2f, RAIL_BOTTOM, 8f - halfLength,
                    railCenter + RAIL_W / 2f, RAIL_TOP, 8f + halfLength, "rail"
                )
            )
            elements.add(
                box(
                    railCenter - CAP_W / 2f, RAIL_BOTTOM - CAP_H, 8f - halfLength,
                    railCenter + CAP_W / 2f, RAIL_BOTTOM, 8f + halfLength, "rail"
                )
            )
            elements.add(
                box(
                    railCenter - CAP_W / 2f, RAIL_TOP, 8f - halfLength,
                    railCenter + CAP_W / 2f, RAIL_TOP + CAP_H, 8f + halfLength, "rail"
                )
            )
        }
        for (offset in listOf(-SLEEPER_SPACING, 0f, SLEEPER_SPACING)) {
            elements.add(
                box(
                    8f - halfSleeper, 0f, 8f + offset - halfSleeperW,
                    8f + halfSleeper, SLEEPER_H, 8f + offset + halfSleeperW, "sleeper"
                )
            )
        }
        root.add("elements", elements)
        return root
    }

    private fun box(
        fromX: Float,
        fromY: Float,
        fromZ: Float,
        toX: Float,
        toY: Float,
        toZ: Float,
        texture: String,
    ): JsonObject {
        val dx = toX - fromX
        val dy = toY - fromY
        val dz = toZ - fromZ
        val element = JsonObject()
        element.add("from", vec(fromX, fromY, fromZ))
        element.add("to", vec(toX, toY, toZ))
        val rotation = JsonObject()
        rotation.add("origin", vec(ORIGIN[0], ORIGIN[1], ORIGIN[2]))
        rotation.addProperty("axis", "y")
        rotation.addProperty("angle", DIAGONAL_ANGLE)
        element.add("rotation", rotation)
        val faces = JsonObject()
        faces.add("north", face(dx, dy, texture))
        faces.add("south", face(dx, dy, texture))
        faces.add("east", face(dz, dy, texture))
        faces.add("west", face(dz, dy, texture))
        faces.add("up", face(dx, dz, texture))
        faces.add("down", face(dx, dz, texture))
        element.add("faces", faces)
        return element
    }

    private fun face(uvW: Float, uvH: Float, texture: String): JsonObject {
        val face = JsonObject()
        val uv = JsonArray()
        uv.add(0f)
        uv.add(0f)
        uv.add(uvW)
        uv.add(uvH)
        face.add("uv", uv)
        face.addProperty("texture", "#$texture")
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
