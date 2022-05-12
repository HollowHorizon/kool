package de.fabmax.kool.modules.ksl.blocks

import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.modules.ksl.lang.*
import kotlin.math.PI

fun KslScopeBuilder.pbrMaterialBlock(reflectionMap: KslExpression<KslTypeColorSamplerCube>,
                                     brdfLut: KslExpression<KslTypeColorSampler2d>,
                                     block: PbrMaterialBlock.() -> Unit): PbrMaterialBlock {
    val pbrMaterialBlock = PbrMaterialBlock(parentStage.program.nextName("pbrMaterialBlock"), reflectionMap, brdfLut, this)
    ops += pbrMaterialBlock.apply(block)
    return pbrMaterialBlock
}

class PbrMaterialBlock(
    name: String,
    reflectionMap: KslExpression<KslTypeColorSamplerCube>,
    brdfLut: KslExpression<KslTypeColorSampler2d>,
    parentScope: KslScopeBuilder
) : LitMaterialBlock(name, parentScope) {

    val inRoughness = inFloat1("inRoughness")
    val inMetallic = inFloat1("inRoughness")

    val inAmbientOrientation = inMat3("inAmbientOrientation")
    val inIrradiance = inFloat3("inIrradiance")

    val inReflectionStrength = inFloat3("inReflectionStrength", KslValueFloat3(1f, 1f, 1f))

    val inOcclusion = inFloat1("inOcclusion", KslValueFloat1(1f))

    init {
        body.apply {
            val viewDir = float3Var(normalize(inCamPos - inFragmentPos))
            val roughness = floatVar(clamp(inRoughness, 0.05f.const, 1f.const))
            val f0 = mix(Vec3f(0.04f).const, inBaseColor, inMetallic)
            val lo = float3Var(Vec3f.ZERO.const)

            fori (0.const, inLightCount) { i ->
                val lightDir = float3Var(normalize(getLightDirectionFromFragPos(inFragmentPos, inEncodedLightPositions[i])))
                val h = float3Var(normalize(viewDir + lightDir))
                val normalDotLight = floatVar(dot(inNormal, lightDir))
                val radiance = float3Var(inShadowFactors[i] * inLightStrength *
                        getLightRadiance(inFragmentPos, inEncodedLightPositions[i], inEncodedLightDirections[i], inEncodedLightColors[i]))

                // cook-torrance BRDF
                val ndf = floatVar(distributionGgx(inNormal, h, roughness))
                val g = floatVar(geometrySmith(inNormal, viewDir, lightDir, roughness))
                val f = float3Var(fresnelSchlick(max(dot(h, viewDir), 0f.const), f0))

                val kD = float3Var(1f.const - f)
                kD set kD * 1f.const - inMetallic

                val num = ndf * g * f
                val denom = 4f.const * max(dot(inNormal, viewDir), 0f.const) * max(normalDotLight, 0f.const)
                val specular = float3Var(num / max(denom, 0.001f.const))

                // add to outgoing radiance
                lo set lo + (kD * inBaseColor / PI.const + specular) * radiance * max(normalDotLight, 0f.const)
            }

            val normalDotView = floatVar(max(dot(inNormal, viewDir), 0f.const))

            // simple version without ibl
//            val kS = float3Var(fresnelSchlickRoughness(normalDotView, f0, roughness))
//            val kD = float3Var(1f.const - kS)
//            val diffuse = float3Var(Color.DARK_GRAY.toLinear().const.rgb * inBaseColor)
//            val ambient = float3Var(kD * diffuse)
//            outColor set ambient + lo

            val f = float3Var(fresnelSchlickRoughness(normalDotView, f0, roughness))
            val kD = float3Var((1f.const - f) * (1f.const - inMetallic))
            val diffuse = float3Var(inIrradiance * inBaseColor)

            val r = inAmbientOrientation * reflect(-viewDir, inNormal)
            val prefilteredColor = float3Var(sampleTexture(reflectionMap, r, roughness * 6f.const).rgb * inReflectionStrength)

            val brdf = float2Var(sampleTexture(brdfLut, float2Value(normalDotView, roughness)).float2("rg"))
            val specular = float3Var(prefilteredColor * (f * brdf.r + brdf.g))
            val ambient = float3Var(kD * diffuse * inOcclusion)
            val reflection = float3Var(specular * inOcclusion)
            outColor set ambient + lo + reflection
        }
    }
}