import de.fabmax.kool.editor.api.KoolScript
import de.fabmax.kool.editor.api.ScriptLoader
import de.fabmax.kool.app.SampleRotationAnimator

// GENERATED FILE! Do not edit manually ////////////////////////////

object ScriptBindings : ScriptLoader.AppScriptLoader {
    override fun newScriptInstance(scriptClassName: String): KoolScript {
        return when (scriptClassName) {
            "de.fabmax.kool.app.SampleRotationAnimator" -> SampleRotationAnimator()
            else -> throw IllegalArgumentException("$scriptClassName not mapped.")
        }
    }

    override fun getScriptProperty(script: KoolScript, propertyName: String): Any {
        return when (script) {
            is SampleRotationAnimator -> getSampleRotationAnimatorProperty(script, propertyName)
            else -> throw IllegalArgumentException("Unknown script class: ${script::class}")
        }
    }

    override fun setScriptProperty(script: KoolScript, propertyName: String, value: Any?) {
        when (script) {
            is SampleRotationAnimator -> setSampleRotationAnimatorProperty(script, propertyName, value)
            else -> throw IllegalArgumentException("Unknown script class: ${script::class}")
        }
    }

    private fun getSampleRotationAnimatorProperty(script: SampleRotationAnimator, propertyName: String): Any {
        return when (propertyName) {
            "rotationSpeed" -> script.rotationSpeed
            "speedMulti" -> script.speedMulti
            else -> throw IllegalArgumentException("Unknown parameter $propertyName for script class ${script::class}")
        }
    }

    private fun setSampleRotationAnimatorProperty(script: SampleRotationAnimator, propertyName: String, value: Any?) {
        when (propertyName) {
            "rotationSpeed" -> script.rotationSpeed = value as de.fabmax.kool.math.Vec3f
            "speedMulti" -> script.speedMulti = value as Float
            else -> throw IllegalArgumentException("Unknown parameter $propertyName for script class ${script::class}")
        }
    }
}
