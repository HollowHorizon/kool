package de.fabmax.kool.modules.ksl.blocks

import de.fabmax.kool.KoolContext
import de.fabmax.kool.modules.ksl.KslShader
import de.fabmax.kool.modules.ksl.KslShaderListener
import de.fabmax.kool.modules.ksl.lang.KslDataBlock
import de.fabmax.kool.modules.ksl.lang.KslProgram
import de.fabmax.kool.pipeline.Pipeline
import de.fabmax.kool.pipeline.Uniform1i
import de.fabmax.kool.pipeline.Uniform4fv
import de.fabmax.kool.pipeline.drawqueue.DrawCommand
import kotlin.math.min

fun KslProgram.sceneLightData(maxLights: Int) = SceneLightData(this, maxLights)

class SceneLightData(program: KslProgram, val maxLightCount: Int) : KslDataBlock, KslShaderListener {
    override val name = NAME

    val encodedPositions = program.uniformFloat4Array(UNIFORM_NAME_LIGHT_POSITIONS, maxLightCount)
    val encodedDirections = program.uniformFloat4Array(UNIFORM_NAME_LIGHT_DIRECTIONS, maxLightCount)
    val encodedColors = program.uniformFloat4Array(UNIFORM_NAME_LIGHT_COLORS, maxLightCount)
    val lightCount = program.uniformInt1(UNIFORM_NAME_LIGHT_COUNT)

    private var uLightPositions: Uniform4fv? = null
    private var uLightDirections: Uniform4fv? = null
    private var uLightColors: Uniform4fv? = null
    private var uLightCount: Uniform1i? = null

    init {
        program.dataBlocks += this
        program.shaderListeners += this
    }

    override fun onShaderCreated(shader: KslShader, pipeline: Pipeline, ctx: KoolContext) {
        uLightPositions = shader.uniforms[UNIFORM_NAME_LIGHT_POSITIONS] as? Uniform4fv
        uLightDirections = shader.uniforms[UNIFORM_NAME_LIGHT_DIRECTIONS] as? Uniform4fv
        uLightColors = shader.uniforms[UNIFORM_NAME_LIGHT_COLORS] as? Uniform4fv
        uLightCount = shader.uniforms[UNIFORM_NAME_LIGHT_COUNT] as? Uniform1i
    }

    override fun onUpdate(cmd: DrawCommand) {
        val lighting = cmd.queue.renderPass.lighting
        if (lighting != null) {
            val lightPos = uLightPositions ?: return
            val lightDir = uLightDirections ?: return
            val lightCol = uLightColors ?: return
            val lightCnt = uLightCount ?: return

            lightCnt.value = min(lighting.lights.size, maxLightCount)
            for (i in 0 until lightCnt.value) {
                val light = lighting.lights[i]
                light.updateEncodedValues()
                lightPos.value[i].set(light.encodedPosition)
                lightDir.value[i].set(light.encodedDirection)
                lightCol.value[i].set(light.encodedColor)
            }
        } else {
            uLightCount?.value = 0
        }
    }

    companion object {
        const val NAME = "SceneLightData"

        const val UNIFORM_NAME_LIGHT_POSITIONS = "uLightPositions"
        const val UNIFORM_NAME_LIGHT_DIRECTIONS = "uLightDirections"
        const val UNIFORM_NAME_LIGHT_COLORS = "uLightColors"
        const val UNIFORM_NAME_LIGHT_COUNT = "uLightCount"
    }
}