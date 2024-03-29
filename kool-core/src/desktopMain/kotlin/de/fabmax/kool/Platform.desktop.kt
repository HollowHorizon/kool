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

actual fun Double.toString(precision: Int): String = "%.${precision.clamp(0, 12)}f".format(Locale.ENGLISH, this)

val KoolSystem.configJvm: KoolConfigJvm get() = config as KoolConfigJvm

/**
 * Creates a new [KoolContext] with the given [KoolConfigJvm]. Notice that there can only be one [KoolContext], calling
 * this method multiple times is an error.
 */
fun createContext(config: KoolConfigJvm = KoolConfigJvm()): Lwjgl3Context {
    KoolSystem.initialize(config)
    return DesktopImpl.createContext()
}

fun KoolApplication(config: KoolConfigJvm, appBlock: (KoolContext) -> Unit) = KoolApplication(createContext(config), appBlock)

fun KoolApplication(ctx: Lwjgl3Context = createContext(), appBlock: (KoolContext) -> Unit) {
    appBlock(ctx)
    ctx.run()
}

internal object DesktopImpl {
    private var ctx: Lwjgl3Context? = null

    val monitors: MutableList<MonitorSpec> = mutableListOf()
    val primaryMonitor: MonitorSpec

    init {
        if (Log.printer == Log.DEFAULT_PRINTER) {
            val dateFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
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
        check(GLFW.glfwInit()) { "Unable to initialize GLFW" }

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

    fun createContext(): Lwjgl3Context {
        synchronized(this) {
            if (ctx == null) {
                ctx = Lwjgl3Context()
            }
        }
        return ctx!!
    }
}