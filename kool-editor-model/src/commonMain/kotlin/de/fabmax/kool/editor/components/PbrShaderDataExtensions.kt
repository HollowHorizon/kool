package de.fabmax.kool.editor.components

import de.fabmax.kool.editor.api.AppAssets
import de.fabmax.kool.editor.api.AssetReference
import de.fabmax.kool.editor.api.SceneShaderData
import de.fabmax.kool.editor.api.loadTexture2d
import de.fabmax.kool.editor.data.*
import de.fabmax.kool.modules.ksl.KslLitShader
import de.fabmax.kool.modules.ksl.KslPbrShader
import de.fabmax.kool.modules.ksl.ModelMatrixComposition
import de.fabmax.kool.modules.ksl.blocks.ColorSpaceConversion
import de.fabmax.kool.pipeline.*
import de.fabmax.kool.util.Color

suspend fun PbrShaderData.createPbrShader(sceneShaderData: SceneShaderData, modelMats: List<ModelMatrixComposition>): KslPbrShader {
    val shader = KslPbrShader {
        pipeline {
            if (genericSettings.isTwoSided) {
                cullMethod = CullMethod.NO_CULLING
            }
        }
        vertices {
            isInstanced = true
            modelMatrixComposition = modelMats
        }
        color {
            when (val color = baseColor) {
                is ConstColorAttribute -> uniformColor()
                is ConstValueAttribute -> uniformColor()
                is MapAttribute -> textureColor()
                is VertexAttribute -> vertexColor(Attribute(color.attribName, GpuType.FLOAT4))
            }
        }
        emission {
            when (val color = emission) {
                is ConstColorAttribute -> uniformColor()
                is ConstValueAttribute -> uniformColor()
                is MapAttribute -> textureColor()
                is VertexAttribute -> vertexColor(Attribute(color.attribName, GpuType.FLOAT4))
            }
        }
        roughness {
            when (val rough = roughness) {
                is ConstColorAttribute -> uniformProperty()
                is ConstValueAttribute -> uniformProperty()
                is MapAttribute -> textureProperty()
                is VertexAttribute -> vertexProperty(Attribute(rough.attribName, GpuType.FLOAT1))
            }
        }
        metallic {
            when (val metal = metallic) {
                is ConstColorAttribute -> uniformProperty()
                is ConstValueAttribute -> uniformProperty()
                is MapAttribute -> textureProperty()
                is VertexAttribute -> vertexProperty(Attribute(metal.attribName, GpuType.FLOAT1))
            }
        }
        this@createPbrShader.aoMap?.let {
            ao { textureProperty(null, it.singleChannelIndex) }
        }
        this@createPbrShader.displacementMap?.let {
            vertices {
                displacement {
                    uniformProperty(parallaxOffset)
                }
            }
            parallaxMapping {
                useParallaxMap(null, parallaxStrength, maxSteps = parallaxSteps, textureChannel = it.singleChannelIndex)
            }
        }
        this@createPbrShader.normalMap?.let {
            normalMapping {
                setNormalMap()
            }
        }

        lighting {
            maxNumberOfLights = sceneShaderData.maxNumberOfLights
            addShadowMaps(sceneShaderData.shadowMaps)
            sceneShaderData.ssaoMap?.let {
                enableSsao(it)
            }
        }
        sceneShaderData.environmentMaps?.let {
            enableImageBasedLighting(it)
        }
        colorSpaceConversion = ColorSpaceConversion.LinearToSrgbHdr(sceneShaderData.toneMapping)
    }
    updatePbrShader(shader, sceneShaderData)
    return shader
}

suspend fun PbrShaderData.updatePbrShader(shader: KslPbrShader, sceneShaderData: SceneShaderData): Boolean {
    if (!matchesPbrShaderConfig(shader)) {
        return false
    }

    val colorConv = shader.cfg.colorSpaceConversion
    if (colorConv is ColorSpaceConversion.LinearToSrgbHdr && colorConv.toneMapping != sceneShaderData.toneMapping) {
        return false
    }

    val ibl = sceneShaderData.environmentMaps
    val isIbl = ibl != null
    val isSsao = sceneShaderData.ssaoMap != null
    val isMaterialAo = aoMap != null

    when {
        (shader.ambientCfg is KslLitShader.AmbientLight.ImageBased) != isIbl -> return false
        shader.isSsao != isSsao -> return false
        shader.cfg.lightingConfig.maxNumberOfLights != sceneShaderData.maxNumberOfLights -> return false
        shader.shadowMaps != sceneShaderData.shadowMaps -> return false
        (shader.materialAoCfg.primaryTexture != null) != isMaterialAo -> return false
    }

    val colorMap = (baseColor as? MapAttribute)?.let { AppAssets.loadTexture2d(it.mapPath) }
    val roughnessMap = (roughness as? MapAttribute)?.let { AppAssets.loadTexture2d(it.mapPath) }
    val metallicMap = (metallic as? MapAttribute)?.let { AppAssets.loadTexture2d(it.mapPath) }
    val emissionMap = (emission as? MapAttribute)?.let { AppAssets.loadTexture2d(it.mapPath) }
    val normalMap = normalMap?.let { AppAssets.loadTexture2d(it.mapPath) }
    val aoMap = aoMap?.let { AppAssets.loadTexture2d(it.mapPath) }
    val displacementMap = displacementMap?.let { AppAssets.loadTexture2d(AssetReference.Texture(it.mapPath, TexFormat.R)) }

    when (val color = baseColor) {
        is ConstColorAttribute -> shader.color = color.color.toColorLinear()
        is ConstValueAttribute -> shader.color = Color(color.value, color.value, color.value)
        is MapAttribute -> shader.colorMap = colorMap
        is VertexAttribute -> { }
    }
    when (val color = emission) {
        is ConstColorAttribute -> shader.emission = color.color.toColorLinear()
        is ConstValueAttribute -> shader.emission = Color(color.value, color.value, color.value)
        is MapAttribute -> shader.emissionMap = emissionMap
        is VertexAttribute -> { }
    }
    when (val rough = roughness) {
        is ConstColorAttribute -> shader.roughness = rough.color.r
        is ConstValueAttribute -> shader.roughness = rough.value
        is MapAttribute -> shader.roughnessMap = roughnessMap
        is VertexAttribute -> { }
    }
    when (val metal = metallic) {
        is ConstColorAttribute -> shader.metallic = metal.color.r
        is ConstValueAttribute -> shader.metallic = metal.value
        is MapAttribute -> shader.metallicMap = metallicMap
        is VertexAttribute -> { }
    }
    shader.normalMap = normalMap
    shader.materialAoMap = aoMap
    shader.parallaxMap = displacementMap
    shader.parallaxMapSteps = parallaxSteps
    shader.parallaxStrength = parallaxStrength
    shader.vertexDisplacementStrength = parallaxOffset

    if (ibl != null) {
        shader.ambientFactor = Color.WHITE
        shader.ambientMap = ibl.irradianceMap
        shader.reflectionMap = ibl.reflectionMap
    } else {
        shader.ambientFactor = sceneShaderData.ambientColorLinear
    }
    return true
}

fun PbrShaderData.matchesPbrShaderConfig(shader: DrawShader?): Boolean {
    if (shader !is KslPbrShader) {
        return false
    }
    return baseColor.matchesCfg(shader.colorCfg)
            && roughness.matchesCfg(shader.roughnessCfg)
            && metallic.matchesCfg(shader.metallicCfg)
            && emission.matchesCfg(shader.emissionCfg)
            && aoMap?.matchesCfg(shader.materialAoCfg) != false
            && shader.isParallaxMapped == (displacementMap != null)
            && shader.isNormalMapped == (normalMap != null)
            && genericSettings.matchesPipelineConfig(shader.pipelineConfig)
}