package de.fabmax.kool.pipeline

import de.fabmax.kool.pipeline.drawqueue.DrawCommand
import de.fabmax.kool.util.MixedBuffer
import de.fabmax.kool.util.copy

class PushConstantRange private constructor(builder: Builder, val longHash: ULong, val pushConstants: List<Uniform<*>>) {

    val name = builder.name
    val instanceName = builder.instanceName
    val stages = builder.stages.copy()

    private val layout = Std140BufferLayout(pushConstants)

    /**
     * Overall size of buffer in bytes (i.e. all containing uniforms)
     */
    val size = layout.size

    private val buffer = MixedBuffer(size)

    val onUpdate: ((PushConstantRange, DrawCommand) -> Unit)? = builder.onUpdate

    fun toBuffer(): MixedBuffer {
        layout.putToBuffer(pushConstants, buffer)
        buffer.flip()
        return buffer
    }

    class Builder {
        var name = "PushConstants"
        var instanceName: String? = null
        val stages = mutableSetOf<ShaderStage>()

        val pushConstants = mutableListOf<() -> Uniform<*>>()
        var onUpdate: ((PushConstantRange, DrawCommand) -> Unit)? = null

        operator fun (() -> Uniform<*>).unaryPlus() {
            pushConstants.add(this)
        }

        fun create(): PushConstantRange {
            val pushConstants = List(pushConstants.size) { pushConstants[it]() }
            var hash = name.hashCode().toULong()
            stages.forEach {
                hash = (hash * 71023UL) + it.hashCode().toULong()
            }
            pushConstants.forEach {
                hash = (hash * 71023UL) + it::class.hashCode().toULong()
                hash = (hash * 71023UL) + it.name.hashCode().toULong()
            }
            return PushConstantRange(this, hash, pushConstants)
        }

        fun clear() {
            stages.clear()
            pushConstants.clear()
        }
    }
}