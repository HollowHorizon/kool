package de.fabmax.kool

import de.fabmax.kool.math.clamp
import de.fabmax.kool.platform.Lwjgl3Context
import de.fabmax.kool.platform.MonitorSpec
import de.fabmax.kool.util.Log
import de.fabmax.kool.util.Time
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import java.text.SimpleDateFormat
import java.util.*

/**
 * Desktop LWJGL3 platform implementation
 *
 * @author fabmax
 */

actual fun defaultKoolConfig(): KoolConfig = KoolConfigJvm()

val KoolSystem.configJvm: KoolConfigJvm get() = config as KoolConfigJvm

/**
 * Creates a new [KoolContext] based on the [KoolConfig] provided by [KoolSystem]. [KoolSystem.initialize] has to be
 * called before invoking this function.
 */
actual fun createContext(config: KoolConfig): KoolContext {
    KoolSystem.initialize(config)
    return DesktopImpl.createContext()
}

actual fun Double.toString(precision: Int): String = "%.${precision.clamp(0, 12)}f".format(Locale.ENGLISH, this)

internal object DesktopImpl {
    private var ctx: Lwjgl3Context? = null

    val monitors: MutableList<MonitorSpec> = mutableListOf()
    val primaryMonitor: MonitorSpec

    init {
        if (Log.printer == Log.DEFAULT_PRINTER) {
            val dateFmt = SimpleDateFormat("HH:mm:ss.SSS")
            Log.printer = { lvl, tag, message ->
                synchronized(dateFmt) {
                    val frmTxt = ctx?.let { "|f:${Time.frameCount}" } ?: ""
                    val txt = "${dateFmt.format(System.currentTimeMillis())}$frmTxt ${lvl.indicator}/$tag: $message"
                    if (lvl.level < Log.Level.WARN.level) {
                        println(txt)
                    } else {
                        System.err.println(txt)
                    }
                }
            }
        }

        // setup an error callback
        GLFWErrorCallback.createPrint(System.err).set()

        // initialize GLFW
        if (!GLFW.glfwInit()) {
            throw KoolException("Unable to initialize GLFW")
        }

        val primMonId = GLFW.glfwGetPrimaryMonitor()
        val mons = GLFW.glfwGetMonitors()!!
        var primMon = MonitorSpec(mons[0])
        for (i in 0 until mons.limit()) {
            val spec = MonitorSpec(mons[i])
            monitors += spec
            if (mons[i] == primMonId) {
                primMon = spec
            }
        }
        primaryMonitor = primMon
    }

    fun createContext(): KoolContext {
        synchronized(this) {
            if (ctx == null) {
                ctx = Lwjgl3Context()
            }
        }
        return ctx!!
    }
}