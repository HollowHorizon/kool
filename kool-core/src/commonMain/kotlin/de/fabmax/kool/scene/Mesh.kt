package de.fabmax.kool.scene

import de.fabmax.kool.KoolSystem
import de.fabmax.kool.math.RayF
import de.fabmax.kool.math.RayTest
import de.fabmax.kool.math.spatial.BoundingBoxF
import de.fabmax.kool.pipeline.*
import de.fabmax.kool.pipeline.shading.DepthShader
import de.fabmax.kool.scene.animation.Skin
import de.fabmax.kool.scene.geometry.IndexedVertexList
import de.fabmax.kool.scene.geometry.MeshBuilder
import de.fabmax.kool.util.Time

fun Node.addMesh(attributes: List<Attribute>, name: String = makeChildName("mesh"), block: Mesh.() -> Unit): Mesh {
    val mesh = Mesh(IndexedVertexList(attributes), name)
    mesh.block()
    addNode(mesh)
    return mesh
}

fun Node.addMesh(vararg attributes: Attribute, name: String = makeChildName("mesh"), block: Mesh.() -> Unit): Mesh {
    val mesh = Mesh(IndexedVertexList(*attributes), name)
    mesh.block()
    addNode(mesh)
    return mesh
}

fun Node.addColorMesh(name: String = makeChildName("colorMesh"), block: Mesh.() -> Unit): Mesh {
    return addMesh(
        Attribute.POSITIONS, Attribute.NORMALS, Attribute.COLORS,
        name = name,
        block = block
    )
}

fun Node.addTextureMesh(name: String = makeChildName("textureMesh"), isNormalMapped: Boolean = false, block: Mesh.() -> Unit): Mesh {
    val attributes = mutableListOf(Attribute.POSITIONS, Attribute.NORMALS, Attribute.TEXTURE_COORDS)
    if (isNormalMapped) {
        attributes += Attribute.TANGENTS
    }
    val mesh = addMesh(attributes, name, block)
    if (isNormalMapped) {
        mesh.geometry.generateTangents()
    }
    return mesh
}

/**
 * Class for renderable geometry (triangles, lines, points).
 */
open class Mesh(var geometry: IndexedVertexList, name: String = geometry.name) : Node(name) {

    constructor(attributes: List<Attribute>, name: String = makeNodeName("Mesh")) :
            this(IndexedVertexList(attributes), name)
    constructor(vararg attributes: Attribute, name: String = makeNodeName("Mesh")) :
            this(IndexedVertexList(*attributes), name)

    val id = instanceId++

    var instances: MeshInstanceList? = null
        set(value) {
            field = value
            if (value != null) {
                // frustum checking does not play well with instancing -> disable it if instancing is used
                isFrustumChecked = false
            }
        }
    var morphWeights: FloatArray? = null
    var skin: Skin? = null
    var isOpaque = true

    val meshPipelineData = PipelineData(BindGroupScope.MESH)

    var shader: Shader? = null
        set(value) {
            if (field !== value) {
                field = value
                // fixme: this is not optimal in cases where the old shader is still used in other places
                pipeline?.let { discardedPipelines += it }
                pipeline = null
            }
        }

    /**
     * Optional shader used by [de.fabmax.kool.pipeline.DepthMapPass] (mainly used for rendering shadow maps). If null
     * DepthMapPass uses [depthShaderConfig] or - if this is null as well - a geometry based default config to create
     * a default depth shader.
     */
    var depthShader: Shader? = null

    /**
     * Optional shader used by [de.fabmax.kool.pipeline.NormalLinearDepthMapPass] (mainly used for rendering
     * screen-space ao maps). If null NormalLinearDepthMapPass uses [depthShaderConfig] or - if this is null as well -
     * a geometry based default config to create a default depth shader.
     */
    var normalLinearDepthShader: Shader? = null

    /**
     * Custom config for depth shader creation. If non-null, this is used to create depth shaders for shadow and ssao
     * passes. By supplying a custom depth shader config, depth shaders can consider alpha masks. If [depthShader]
     * and / or [normalLinearDepthShader] are set, these are preferred.
     */
    var depthShaderConfig: DepthShader.Config? = null

    /**
     * Optional list with lod geometry used by shadow passes. Shadow passes will use the geometry at index
     * [de.fabmax.kool.util.SimpleShadowMap.shadowMapLevel] or the last list entry in case the list has fewer entries.
     * If list is empty the regular geometry is used.
     */
    val shadowGeometry = mutableListOf<IndexedVertexList>()

    /**
     * Determines whether this node is considered during shadow pass.
     */
    var isCastingShadowLevelMask = -1
    var isCastingShadow: Boolean
        get() = isCastingShadowLevelMask != 0
        set(value) {
            isCastingShadowLevelMask = if (value) -1 else 0
        }

    // todo: replace pipeline with pipelineInstance
    private var pipeline: Pipeline? = null
    private val discardedPipelines = mutableListOf<Pipeline>()

    var rayTest = MeshRayTest.boundsTest()

    private var lastGeomUpdateFrame = -1

    /**
     * Time the latest draw call took (in seconds).
     */
    var drawTime = 0.0
        internal set

    init {
        isFrustumChecked = true
    }

    override fun addContentToBoundingBox(localBounds: BoundingBoxF) {
        localBounds.add(geometry.bounds)
    }

    open fun generate(generator: MeshBuilder.() -> Unit) {
        geometry.batchUpdate {
            clear()
            MeshBuilder(this).generator()
        }
    }

    fun getOrCreatePipeline(updateEvent: RenderPass.UpdateEvent): Pipeline? {
        if (discardedPipelines.isNotEmpty()) {
            discardedPipelines.forEach { updateEvent.ctx.disposePipeline(it) }
            discardedPipelines.clear()
        }
        return pipeline ?: shader?.let { s ->
            s.getOrCreatePipeline(this, updateEvent).also { pipeline = it }
        }
    }

    fun setIsCastingShadow(shadowMapLevel: Int, enabled: Boolean) {
        isCastingShadowLevelMask = if (enabled) {
            isCastingShadowLevelMask or (1 shl shadowMapLevel)
        } else {
            isCastingShadowLevelMask and (1 shl shadowMapLevel).inv()
        }
    }

    fun disableShadowCastingAboveLevel(shadowMapLevel: Int) {
        isCastingShadowLevelMask = isCastingShadowLevelMask and ((2 shl shadowMapLevel) - 1)
    }

    fun isCastingShadow(shadowMapLevel: Int): Boolean {
        return (isCastingShadowLevelMask and (1 shl shadowMapLevel)) != 0
    }

    override fun rayTestLocal(test: RayTest, localRay: RayF) {
        rayTest.rayTest(test, localRay)
    }

    /**
     * Deletes all buffers associated with this mesh.
     */
    override fun release() {
        // fixme: same check as for Node
        if (!isReleased) {
            super.release()
            geometry.release()
            shadowGeometry.forEach { it.release() }
            pipeline?.let { KoolSystem.requireContext().disposePipeline(it) }
            pipeline = null
        }
    }

    override fun collectDrawCommands(updateEvent: RenderPass.UpdateEvent) {
        super.collectDrawCommands(updateEvent)

        if (!updateEvent.drawFilter(this) || !isRendered) {
            // mesh is not visible (either hidden or outside frustum)
            return
        }

        val insts = instances
        if (insts != null && insts.numInstances == 0) {
            // instanced mesh has no instances
            return
        }

        // update bounds and ray test if geometry has changed
        if (geometry.hasChanged && !geometry.isBatchUpdate && lastGeomUpdateFrame < Time.frameCount) {
            // don't clear the hasChanged flag yet, this is done by rendering backend after vertex buffers are updated
            // however, we store the frame index here to avoid doing stuff multiple times if there are multiple
            // render-passes (e.g. shadow map + normal render)
            lastGeomUpdateFrame = Time.frameCount

            if (geometry.isRebuildBoundsOnSync) {
                geometry.rebuildBounds()
            }
            rayTest.onMeshDataChanged(this)
        }
        updateEvent.view.appendMeshToDrawQueue(this, updateEvent)
    }

    companion object {
        private var instanceId = 1L
    }
}

/**
 * Mesh with default attributes for vertex color based rendering:
 * [Attribute.POSITIONS], [Attribute.NORMALS], [Attribute.COLORS]
 */
open class ColorMesh(name: String = makeNodeName("ColorMesh")) : Mesh(Attribute.POSITIONS, Attribute.NORMALS, Attribute.COLORS, name = name)

/**
 * Mesh with default attributes for texture color based rendering:
 * [Attribute.POSITIONS], [Attribute.NORMALS], [Attribute.TEXTURE_COORDS] and [Attribute.TANGENTS] if
 * isNormalMapped is true.
 */
open class TextureMesh(isNormalMapped: Boolean = false, name: String = makeNodeName("TextureMesh")) : Mesh(
    if (isNormalMapped) {
        listOf(Attribute.POSITIONS, Attribute.NORMALS, Attribute.TEXTURE_COORDS, Attribute.TANGENTS)
    } else {
        listOf(Attribute.POSITIONS, Attribute.NORMALS, Attribute.TEXTURE_COORDS)
    },
    name = name
)
