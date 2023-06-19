package de.fabmax.kool.pipeline.ibl

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.*
import de.fabmax.kool.modules.atmosphere.OpticalDepthLutPass
import de.fabmax.kool.modules.atmosphere.atmosphereBlock
import de.fabmax.kool.modules.atmosphere.atmosphereData
import de.fabmax.kool.modules.ksl.KslPbrShader
import de.fabmax.kool.modules.ksl.KslShader
import de.fabmax.kool.modules.ksl.blocks.ColorSpaceConversion
import de.fabmax.kool.modules.ksl.blocks.cameraData
import de.fabmax.kool.modules.ksl.blocks.modelMatrix
import de.fabmax.kool.modules.ksl.blocks.mvpMatrix
import de.fabmax.kool.modules.ksl.lang.KslProgram
import de.fabmax.kool.modules.ksl.lang.minus
import de.fabmax.kool.modules.ksl.lang.times
import de.fabmax.kool.modules.ksl.lang.xyz
import de.fabmax.kool.pipeline.*
import de.fabmax.kool.scene.*
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.ColorGradient
import de.fabmax.kool.util.MdColor
import de.fabmax.kool.util.MutableColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class SkyCubePass(opticalDepthLut: Texture2d, size: Int = 256) :
    OffscreenRenderPassCube(Node(), renderPassConfig {
        name = "SkyCubePass"
        setSize(size, size)
        addColorTexture(TexFormat.RGBA_F16)
        clearDepthTexture()
    }) {

    val syncLights = mutableListOf<Light.Directional>()

    var azimuth = 0f
        set(value) {
            field = value
            isEnabled = true
        }

    var elevation = 60f
        set(value) {
            field = value.clamp(-90f, 90f)
            isEnabled = true
        }

    private val lightGradient = ColorGradient(
            -90f to Color.BLACK,
            -2f to Color.BLACK,
            0f to MdColor.ORANGE.mix(Color.WHITE, 0.6f).toLinear().scale(0.7f),
            5f to MdColor.AMBER.mix(Color.WHITE, 0.6f).toLinear(),
            10f to Color.WHITE,
            90f to Color.WHITE,
    )
    private val sunLight = Light.Directional()
            .setup(Vec3f(-1f, -1f, -1f))
            .setColor(Color.WHITE, 3f)

    val groundShader: KslPbrShader
    private val skyShader = SkyShader(opticalDepthLut)

    init {
        lighting = Lighting().apply {
            lights.clear()
            lights += sunLight
        }

        groundShader = KslPbrShader {
            color { constColor(MdColor.BROWN toneLin 500) }
            roughness(0.8f)
            uniformAmbientColor(nightSkyColor)
            colorSpaceConversion = ColorSpaceConversion.AS_IS
        }

        drawNode.apply {
            // sky
            addTextureMesh {
                generate {
                    icoSphere {
                        steps = 2
                    }
                }
                shader = skyShader
            }

            // ground
            addColorMesh {
                generate {
                    translate(0f, -0.011f, 0f)
                    rotate(-90f, Vec3f.X_AXIS)
                    circle {
                        radius = 9f
                        steps = 100
                    }
                }
                shader = groundShader
            }
        }

        onBeforeCollectDrawCommands += {
            updateSunLight()
            skyShader.dirToSun = MutableVec3f(sunLight.direction).scale(-1f)
            skyShader.sunColor = MutableColor(sunLight.color).apply { a /= 3f }
        }

        onAfterDraw += {
            isEnabled = false
        }
    }

    private fun updateSunLight() {
        val phi = -azimuth.toRad()
        val theta = (PI.toFloat() / 2f) - elevation.toRad()

        val dir = Vec3f(
            z = -sin(theta) * cos(phi),
            x = -sin(theta) * sin(phi),
            y = -cos(theta)
        )
        sunLight.setup(dir)

        val syncColor = lightGradient.getColorInterpolated(elevation, MutableColor(), -90f, 90f)
        val strength = sqrt(syncColor.r * syncColor.r + syncColor.g * syncColor.g + syncColor.b * syncColor.b)
        for (light in syncLights) {
            light.setup(sunLight.direction)
            light.setColor(syncColor, strength * sunLight.color.a)
        }
    }

    companion object {
        private val nightSkyColor = Color(0.02f, 0.07f, 0.15f).scaleRgb(1.5f, MutableColor()).toLinear()
    }

    private class SkyShader(opticalDepthLut: Texture2d)
        : KslShader(atmoProg(), FullscreenShaderUtil.fullscreenShaderPipelineCfg)
    {
        var opticalDepthLut by texture2d("tOpticalDepthLut", opticalDepthLut)

        var dirToSun by uniform3f("uDirToSun")
        var sunColor by uniform4f("uSunColor")

        var planetCenter by uniform3f("uPlanetCenter", Vec3f(0f, -60f, 0f))
        var surfaceRadius by uniform1f("uSurfaceRadius", 60f)
        var atmosphereRadius by uniform1f("uAtmosphereRadius", 65f)
        var scatteringCoeffs by uniform3f("uScatteringCoeffs", Vec3f(0.274f, 1.536f, 3.859f))
        var mieG by uniform1f("uMieG", 0.8f)
        var mieColor by uniform4f("uMieColor", Vec4f(1f, 0.35f, 0.35f, 0.5f))
        var rayleighColor by uniform4f("uRayleighColor", Vec4f(0.5f, 0.5f, 1.0f, 1.0f))
        var randomOffset by uniform2f("uAtmoRandOffset", Vec2f(1337f, 1337f))

        companion object {
            private fun atmoProg() = KslProgram("Sky atmosphere").apply {
                val camData = cameraData()
                val modelMat = modelMatrix()
                val mvp = mvpMatrix()
                val worldPos = interStageFloat3()

                vertexStage {
                    main {
                        val localPos = vertexAttribFloat3(Attribute.POSITIONS.name)
                        worldPos.input set (modelMat.matrix * float4Value(localPos, 1f.const)).xyz
                        outPosition set mvp.matrix * float4Value(localPos, 1f.const)
                    }
                }

                fragmentStage {
                    val optDephtLut = texture2d("tOpticalDepthLut")
                    val atmoData = atmosphereData()

                    main {
                        val atmoBlock = atmosphereBlock(atmoData, optDephtLut, randomizeStartOffsets = true, numScatterSamples = 64).apply {
                            inSceneColor(nightSkyColor.const)
                            inSkyColor(nightSkyColor.const)
                            inScenePos(worldPos.output)
                            inCamPos(camData.position)
                            inLookDir(normalize(worldPos.output - camData.position))
                        }
                        colorOutput(atmoBlock.outColor)
                    }
                }
            }
        }
    }
}

class SkyCubeIblSystem(val parentScene: Scene, opticalDepthLut: Texture2d) {
    val skyPass = SkyCubePass(opticalDepthLut)

    val irradianceMapPass = IrradianceMapPass.irradianceMap(parentScene, skyPass.colorTexture!!, 8)
    val reflectionMapPass = ReflectionMapPass.reflectionMap(parentScene, skyPass.colorTexture!!, 128)

    val envMaps = EnvironmentMaps(irradianceMapPass.colorTexture!!, reflectionMapPass.colorTexture!!)

    var isAutoUpdateIblMaps = true

    init {
        irradianceMapPass.isAutoRemove = false
        reflectionMapPass.isAutoRemove = false

        skyPass.onAfterDraw += {
            if (isAutoUpdateIblMaps) {
                updateIblMaps()
            }
        }
    }

    fun updateIblMaps() {
        irradianceMapPass.isEnabled = true
        reflectionMapPass.isEnabled = true
    }

    fun setupOffscreenPasses() {
        parentScene.addOffscreenPass(skyPass)
        parentScene.addOffscreenPass(irradianceMapPass)
        parentScene.addOffscreenPass(reflectionMapPass)
    }

    fun removeOffscreenPasses() {
        parentScene.removeOffscreenPass(skyPass)
        parentScene.removeOffscreenPass(irradianceMapPass)
        parentScene.removeOffscreenPass(reflectionMapPass)
    }

    fun disposeOffscreenPasses(ctx: KoolContext) {
        skyPass.dispose(ctx)
        irradianceMapPass.dispose(ctx)
        reflectionMapPass.dispose(ctx)
    }

    companion object {
        fun simpleSkyCubeIblSystem(parentScene: Scene): SkyCubeIblSystem {
            val opticalDepthLut = OpticalDepthLutPass()
            parentScene.addOffscreenPass(opticalDepthLut)
            val skyPass = SkyCubeIblSystem(parentScene, opticalDepthLut.colorTexture!!)
            skyPass.setupOffscreenPasses()
            return skyPass
        }
    }
}
