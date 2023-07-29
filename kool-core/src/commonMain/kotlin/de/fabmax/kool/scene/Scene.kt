package de.fabmax.kool.scene

import de.fabmax.kool.KoolContext
import de.fabmax.kool.input.Pointer
import de.fabmax.kool.math.Ray
import de.fabmax.kool.pipeline.OffscreenRenderPass
import de.fabmax.kool.pipeline.RenderPass
import de.fabmax.kool.pipeline.ScreenRenderPass
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.util.Disposable

/**
 * @author fabmax
 */

inline fun scene(name: String? = null, block: Scene.() -> Unit): Scene {
    return Scene(name).apply(block)
}

open class Scene(name: String? = null) : Node(name) {

    val lighting = Lighting()
    var camera: Camera = PerspectiveCamera()

    val onRenderScene: MutableList<(KoolContext) -> Unit> = mutableListOf()

    val mainRenderPass = ScreenRenderPass(this)

    private val mutOffscreenPasses = mutableListOf<OffscreenRenderPass>()
    private val addOffscreenPasses = mutableListOf<OffscreenRenderPass>()
    private val remOffscreenPasses = mutableListOf<OffscreenRenderPass>()
    val offscreenPasses: List<OffscreenRenderPass>
        get() = mutOffscreenPasses

    private val disposables = mutableListOf<Disposable>()

    var framebufferCaptureMode = FramebufferCaptureMode.Disabled
    val capturedFramebuffer = Texture2d()

    fun addOffscreenPass(pass: OffscreenRenderPass) {
        addOffscreenPasses += pass
    }

    fun removeOffscreenPass(pass: OffscreenRenderPass) {
        addOffscreenPasses -= pass
        remOffscreenPasses += pass
    }

    private fun addOffscreenPasses() {
        if (addOffscreenPasses.isNotEmpty()) {
            addOffscreenPasses.forEach {
                if (it !in mutOffscreenPasses) {
                    mutOffscreenPasses += it
                }
            }
            addOffscreenPasses.clear()
        }
    }

    private fun removeOffscreenPasses() {
        if (remOffscreenPasses.isNotEmpty()) {
            mutOffscreenPasses.removeAll(remOffscreenPasses)
            remOffscreenPasses.clear()
        }
    }

    fun renderScene(ctx: KoolContext) {
        for (i in onRenderScene.indices) {
            onRenderScene[i](ctx)
        }

        // remove all offscreen passes that were scheduled for removal in last frame
        removeOffscreenPasses()
        addOffscreenPasses()

        mainRenderPass.update(ctx)

        for (i in offscreenPasses.indices) {
            val pass = offscreenPasses[i]
            if (pass.isEnabled) {
                pass.update(ctx)
                pass.collectDrawCommands(ctx)
            }
        }
        mainRenderPass.collectDrawCommands(ctx)
    }

    override fun update(updateEvent: RenderPass.UpdateEvent) {
        for (i in disposables.indices) {
            disposables[i].dispose(updateEvent.ctx)
        }
        disposables.clear()

        // update lights not attached
        lighting.onUpdate(updateEvent)

        super.update(updateEvent)
    }

    fun dispose(disposable: Disposable) {
        disposables += disposable
    }

    override fun checkIsVisible(cam: Camera, ctx: KoolContext): Boolean {
        // don't do frustum checking for Scene
        return isVisible
    }

    override fun dispose(ctx: KoolContext) {
        disposables.forEach { it.dispose(ctx) }
        disposables.clear()

        mainRenderPass.dispose(ctx)
        mutOffscreenPasses.removeAll(remOffscreenPasses)
        remOffscreenPasses.clear()
        for (i in offscreenPasses.indices) {
            offscreenPasses[i].dispose(ctx)
        }
        remOffscreenPasses.clear()
        mutOffscreenPasses.clear()
        capturedFramebuffer.dispose()

        super.dispose(ctx)
    }

    fun computePickRay(pointer: Pointer, ctx: KoolContext, result: Ray): Boolean {
        return camera.computePickRay(result, pointer, mainRenderPass.viewport, ctx)
    }

    enum class FramebufferCaptureMode {
        Disabled,
        BeforeRender,
        AfterRender
    }
}
