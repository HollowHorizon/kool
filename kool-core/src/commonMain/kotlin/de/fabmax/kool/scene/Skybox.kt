package de.fabmax.kool.scene

import de.fabmax.kool.math.Mat3f
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.modules.ksl.KslShader
import de.fabmax.kool.modules.ksl.blocks.ColorSpaceConversion
import de.fabmax.kool.modules.ksl.blocks.convertColorSpace
import de.fabmax.kool.modules.ksl.blocks.mvpMatrix
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.pipeline.CullMethod
import de.fabmax.kool.pipeline.PipelineConfig
import de.fabmax.kool.pipeline.TextureCube
import de.fabmax.kool.pipeline.ibl.EnvironmentMaps
import de.fabmax.kool.scene.geometry.IndexedVertexList
import de.fabmax.kool.util.UniqueId

fun Scene.skybox(ibl: EnvironmentMaps, lod: Float = 1f) {
    this += Skybox.cube(ibl.reflectionMap, lod)
}

object Skybox {

    fun cube(
        environmentMap: TextureCube,
        texLod: Float = 0f,
        hdriInput: Boolean = true,
        hdrOutput: Boolean = false,
        isInfiniteDepth: Boolean = false
    ): Cube {
        val colorSpaceConversion = when {
            hdriInput == hdrOutput -> ColorSpaceConversion.AS_IS
            hdriInput -> ColorSpaceConversion.LINEAR_TO_sRGB_HDR
            else -> ColorSpaceConversion.sRGB_TO_LINEAR
        }
        return Cube(environmentMap, texLod, colorSpaceConversion, isInfiniteDepth)
    }

    class Cube(
        skyTex: TextureCube? = null,
        texLod: Float = 0f,
        colorSpaceConversion: ColorSpaceConversion = ColorSpaceConversion.LINEAR_TO_sRGB_HDR,
        isInfiniteDepth: Boolean = false
    ) : Mesh(IndexedVertexList(Attribute.POSITIONS), UniqueId.nextId("Skybox.Cube")) {

        val skyboxShader: KslSkyCubeShader

        init {
            generate {
                cube { }
            }
            isFrustumChecked = false
            isCastingShadow = false
            rayTest = MeshRayTest.nopTest()
            skyboxShader = KslSkyCubeShader(colorSpaceConversion, isInfiniteDepth).apply {
                setSingleSky(skyTex)
                lod = texLod
            }
            shader = skyboxShader
        }
    }

    class KslSkyCubeShader(colorSpaceConversion: ColorSpaceConversion, isInfiniteDepth: Boolean)
        : KslShader(Model(colorSpaceConversion, isInfiniteDepth), PipelineConfig().apply {
            cullMethod = CullMethod.CULL_FRONT_FACES
            isWriteDepth = false
        }) {

        val skies: Array<TextureCube?> by textureCubeArray("tSkies", 2)
        var skyWeights: Vec2f by uniform2f("uSkyWeights", Vec2f.X_AXIS)

        var skyOrientation: Mat3f by uniformMat3f("uSkyOrientation")
        var lod: Float by uniform1f("uLod")

        fun setSingleSky(skyTex: TextureCube?) = setBlendSkies(skyTex, 1f, skyTex, 0f)

        fun setBlendSkies(skyA: TextureCube?, weightA: Float, skyB: TextureCube?, weightB: Float) {
            skies[0] = skyA
            skies[1] = skyB
            skyWeights = Vec2f(weightA, weightB)
        }

        class Model(colorSpaceConversion: ColorSpaceConversion, isInfiniteDepth: Boolean) : KslProgram("skycube-shader") {
            init {
                val orientedPos = interStageFloat3()
                vertexStage {
                    main {
                        val mvpMat = mvpMatrix().matrix
                        val skyOrientation = uniformMat3("uSkyOrientation")
                        val localPos = vertexAttribFloat3(Attribute.POSITIONS.name)
                        orientedPos.input set skyOrientation * localPos

                        if (isInfiniteDepth) {
                            // infinite depth comes with reversed depth, so we need clip z to be 0. i.e. the xyww
                            // trick does not work here. On the upside we have practically infinite range so simple
                            // scale the position to some *large* value.
                            outPosition set (mvpMat * float4Value(localPos * 1e16f.const, 1f))
                        } else {
                            // by using xyww as output position clip depth is guaranteed to be w/w = 1 -> maximum depth
                            outPosition set (mvpMat * float4Value(localPos, 0f)).float4("xyww")
                        }
                    }
                }
                fragmentStage {
                    main {
                        val skies = textureArrayCube("tSkies", 2)
                        val skyWeights = uniformFloat2("uSkyWeights")
                        val texLod = uniformFloat1("uLod")
                        val color = float3Var(sampleTexture(skies[0], orientedPos.output, texLod).rgb * skyWeights.x)
                        `if` (skyWeights.y gt 0f.const) {
                            color += sampleTexture(skies[1], orientedPos.output, texLod).rgb * skyWeights.y
                        }
                        colorOutput(convertColorSpace(color, colorSpaceConversion), 1f.const)
                    }
                }
            }
        }
    }
}