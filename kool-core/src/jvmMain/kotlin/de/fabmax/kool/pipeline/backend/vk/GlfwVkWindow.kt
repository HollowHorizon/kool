package de.fabmax.kool.pipeline.backend.vk

import de.fabmax.kool.KoolSystem
import de.fabmax.kool.configJvm
import de.fabmax.kool.platform.GlfwWindow
import de.fabmax.kool.platform.Lwjgl3Context
import de.fabmax.kool.util.logD
import de.fabmax.kool.util.memStack
import org.lwjgl.glfw.GLFW.glfwDestroyWindow
import org.lwjgl.glfw.GLFW.glfwTerminate
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.vulkan.KHRSurface

class GlfwVkWindow(val sys: VkSystem, ctx: Lwjgl3Context) : GlfwWindow(ctx) {

    val onResize = mutableListOf<OnWindowResizeListener>()

    lateinit var surface: Surface
        private set

    init {
        // make the window visible
        if (KoolSystem.configJvm.showWindowOnStart) {
            isVisible = true
        }
    }

    override fun onFramebufferSizeChanged(width: Int, height: Int) {
        super.onFramebufferSizeChanged(width, height)
        for (listener in onResize) {
            listener.onResize(this, width, height)
        }
    }

    fun createSurface() {
        surface = Surface()
    }

    interface OnWindowResizeListener {
        fun onResize(window: GlfwVkWindow, newWidth: Int, newHeight: Int)
    }

    inner class Surface : VkResource() {
        var surfaceHandle = 0L
            private set

        init {
            memStack {
                val lp = mallocLong(1)
                checkVk(GLFWVulkan.glfwCreateWindowSurface(sys.instance.vkInstance, windowPtr, null, lp))
                surfaceHandle = lp[0]
            }
            sys.instance.addDependingResource(this)
            logD { "Created surface" }
        }

        override fun freeResources() {
            KHRSurface.vkDestroySurfaceKHR(sys.instance.vkInstance, surfaceHandle, null)
            logD { "Destroyed surface" }

            glfwDestroyWindow(windowPtr)
            glfwTerminate()
            logD { "Destroyed GLFW window" }
        }
    }
}