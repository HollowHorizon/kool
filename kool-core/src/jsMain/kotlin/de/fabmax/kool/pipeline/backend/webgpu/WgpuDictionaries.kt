package de.fabmax.kool.pipeline.backend.webgpu

import de.fabmax.kool.util.Color
import org.w3c.dom.ImageBitmap

class GPUBindGroupLayoutDescriptor(
    @JsName("entries")
    val entries: Array<GPUBindGroupLayoutEntry>,
    @JsName("label")
    val label: String = ""
)

sealed interface GPUBindGroupLayoutEntry

data class GPUBindGroupLayoutEntryBuffer(
    @JsName("binding")
    val binding: Int,
    @JsName("visibility")
    val visibility: Int,
    @JsName("buffer")
    val buffer: GPUBufferBindingLayout,
) : GPUBindGroupLayoutEntry

data class GPUBindGroupLayoutEntrySampler(
    @JsName("binding")
    val binding: Int,
    @JsName("visibility")
    val visibility: Int,
    @JsName("sampler")
    val sampler: GPUSamplerBindingLayout,
) : GPUBindGroupLayoutEntry

data class GPUBindGroupLayoutEntryTexture(
    @JsName("binding")
    val binding: Int,
    @JsName("visibility")
    val visibility: Int,
    @JsName("texture")
    val texture: GPUTextureBindingLayout,
) : GPUBindGroupLayoutEntry

data class GPUBindGroupLayoutEntryStorageTexture(
    @JsName("binding")
    val binding: Int,
    @JsName("visibility")
    val visibility: Int,
    @JsName("storageTexture")
    val storageTexture: GPUStorageTextureBindingLayout,
) : GPUBindGroupLayoutEntry

data class GPUBindGroupLayoutEntryExternaleTexture(
    @JsName("binding")
    val binding: Int,
    @JsName("visibility")
    val visibility: Int,
    @JsName("externalTexture")
    val externalTexture: GPUExternalTextureBindingLayout,
) : GPUBindGroupLayoutEntry

data class GPUBufferBindingLayout(
    @JsName("type")
    val type: GPUBufferBindingType = GPUBufferBindingType.uniform,
    @JsName("hasDynamicOffset")
    val hasDynamicOffset: Boolean = false,
    @JsName("minBindingSize")
    val minBindingSize: Long = 0
)

class GPUSamplerBindingLayout

class GPUTextureBindingLayout(
    @JsName("sampleType")
    val sampleType: GPUTextureSampleType = GPUTextureSampleType.float,
    @JsName("viewDimension")
    val viewDimension: GPUTextureViewDimension = GPUTextureViewDimension.view2d,
    @JsName("multisampled")
    val multisampled: Boolean = false
)

class GPUStorageTextureBindingLayout

class GPUExternalTextureBindingLayout

class GPUBindGroupDescriptor(
    @JsName("layout")
    val layout: GPUBindGroupLayout,
    @JsName("entries")
    val entries: Array<GPUBindGroupEntry>,
    @JsName("label")
    val label: String = ""
)

class GPUBindGroupEntry(
    @JsName("binding")
    val binding: Int,
    @JsName("resource")
    val resource: GPUBindingResource,
)

class GPUBlendState(
    @JsName("color")
    val color: GPUBlendComponent,
    @JsName("alpha")
    val alpha: GPUBlendComponent,
)

class GPUBlendComponent(
    @JsName("operation")
    val operation: GPUBlendOperation = GPUBlendOperation.add,
    @JsName("srcFactor")
    val srcFactor: GPUBlendFactor = GPUBlendFactor.one,
    @JsName("dstFactor")
    val dstFactor: GPUBlendFactor = GPUBlendFactor.zero
)

class GPUBufferBinding(
    @JsName("buffer")
    val buffer: GPUBuffer,
    @JsName("offset")
    val offset: Long = 0,
//    @JsName("size")
//    val size: Long = 0
) : GPUBindingResource

data class GPUBufferDescriptor(
    @JsName("size")
    val size: Long,
    @JsName("usage")
    val usage: Int,
    @JsName("mappedAtCreation")
    val mappedAtCreation: Boolean = false,
    @JsName("label")
    val label: String = ""
)

class GPUCanvasConfiguration(
    @JsName("device")
    val device: GPUDevice,
    @JsName("format")
    val format: GPUTextureFormat,
    @JsName("colorSpace")
    val colorSpace: GPUPredefinedColorSpace = GPUPredefinedColorSpace.srgb,
    @JsName("alphaMode")
    val alphaMode: GPUCanvasAlphaMode = GPUCanvasAlphaMode.opaque
)

data class GPUColorDict(
    @JsName("r")
    val r: Float,
    @JsName("g")
    val g: Float,
    @JsName("b")
    val b: Float,
    @JsName("a")
    val a: Float,
)

fun GPUColorDict(color: Color): GPUColorDict = GPUColorDict(color.r, color.g, color.b, color.a)

interface GPUColorTargetState

fun GPUColorTargetState(
    format: GPUTextureFormat,
    blend: GPUBlendState? = null
) : GPUColorTargetState {
    val o = js("({})")
    o["format"] = format
    blend?.let { o["blend"] = it }
    return o
}

class GPUFragmentState(
    @JsName("module")
    val module: GPUShaderModule,
    @JsName("entryPoint")
    val entryPoint: String,
    @JsName("targets")
    val targets: Array<GPUColorTargetState>,
)

class GPUImageCopyExternalImage(
    @JsName("source")
    val source: ImageBitmap
)

class GPUImageCopyTexture(
    @JsName("texture")
    val texture: GPUTexture
    /*
mipLevel: GPUIntegerCoordinate = 0
origin: GPUOrigin3D = {}
aspect: GPUTextureAspect = 'all'
     */
)

class GPUImageCopyTextureTagged(
    @JsName("texture")
    val texture: GPUTexture
    /*
mipLevel: GPUIntegerCoordinate = 0
origin: GPUOrigin3D = {}
aspect: GPUTextureAspect = 'all'
colorSpace: PredefinedColorSpace = 'srgb'
premultipliedAlpha: boolean = 'false'
     */
)

class GPUImageDataLayout(
    @JsName("bytesPerRow")
    val bytesPerRow: Int,
    @JsName("rowsPerImage")
    val rowsPerImage: Int,
    @JsName("offset")
    val offset: Long = 0
)

data class GPUMultisampleState(
    @JsName("count")
    val count: Int = 1,
//    @JsName("mask")
//    val mask: UInt = 0xffffffffu,
    @JsName("alphaToCoverageEnabled")
    val alphaToCoverageEnabled: Boolean = false
)

class GPUPipelineLayoutDescriptor(
    @JsName("bindGroupLayouts")
    val bindGroupLayouts: Array<GPUBindGroupLayout>,
    @JsName("label")
    val label: String = ""
)

data class GPUPrimitiveState(
    @JsName("topology")
    val topology: GPUPrimitiveTopology = GPUPrimitiveTopology.triangleList,
    @JsName("frontFace")
    val frontFace: GPUFrontFace = GPUFrontFace.ccw,
    @JsName("cullMode")
    val cullMode: GPUCullMode = GPUCullMode.none,
    @JsName("unclippedDepth")
    val unclippedDepth: Boolean = false
)

fun GPUPrimitiveStateStrip(
    stripIndexFormat: GPUIndexFormat,
    frontFace: GPUFrontFace = GPUFrontFace.ccw,
    cullMode: GPUCullMode = GPUCullMode.none,
    unclippedDepth: Boolean = false
): GPUPrimitiveState {
    val primState = GPUPrimitiveState(GPUPrimitiveTopology.triangleStrip, frontFace, cullMode, unclippedDepth)
    primState.asDynamic()["stripIndexFormat"] = stripIndexFormat
    return primState
}

interface GPURenderPassColorAttachment

fun GPURenderPassColorAttachment(
    view: GPUTextureView,
    clearValue: GPUColorDict? = null,
    resolveTarget: GPUTextureView? = null,
    loadOp: GPULoadOp = if (clearValue != null) GPULoadOp.clear else GPULoadOp.load,
    storeOp: GPUStoreOp = GPUStoreOp.store,
    label: String = ""
) : GPURenderPassColorAttachment {
    val o = js("({})")
    o["label"] = label
    clearValue?.let { o["clearValue"] = it }
    o["loadOp"] = loadOp
    o["view"] = view
    o["storeOp"] = storeOp
    resolveTarget?.let { o["resolveTarget"] = it }
    return o
}

data class GPURenderPassDepthStencilAttachment(
    @JsName("view")
    val view: GPUTextureView,
    @JsName("depthLoadOp")
    val depthLoadOp: GPULoadOp,
    @JsName("depthStoreOp")
    val depthStoreOp: GPUStoreOp,
    @JsName("depthClearValue")
    val depthClearValue: Float = 1f,
)

interface GPURenderPassDescriptor
fun GPURenderPassDescriptor(
    colorAttachments: Array<GPURenderPassColorAttachment>,
    depthStencilAttachment: GPURenderPassDepthStencilAttachment? = null,
    label: String = ""
): GPURenderPassDescriptor {
    val o = js("({})")
    o["colorAttachments"] = colorAttachments
    depthStencilAttachment?.let { o["depthStencilAttachment"] = it }
    o["label"] = label
    return o
}

interface GPURenderPipelineDescriptor

fun GPURenderPipelineDescriptor(
    layout: GPUPipelineLayout,
    vertex: GPUVertexState,
    fragment: GPUFragmentState? = null,
    depthStencil: GPUDepthStencilState? = null,
    primitive: GPUPrimitiveState? = null,
    multisample: GPUMultisampleState? = null,
    label: String = "",
): GPURenderPipelineDescriptor {
    val o = js("({})")
    o["layout"] = layout
    o["vertex"] = vertex
    fragment?.let { o["fragment"] = it }
    depthStencil?.let { o["depthStencil"] = it }
    primitive?.let { o["primitive"] = it }
    multisample?.let { o["multisample"] = it }
    o["label"] = label
    return o
}

data class GPUDepthStencilState(
    @JsName("format")
    val format: GPUTextureFormat,
    @JsName("depthWriteEnabled")
    val depthWriteEnabled: Boolean,
    @JsName("depthCompare")
    val depthCompare: GPUCompareFunction,
    @JsName("depthBias")
    val depthBias: Int = 0,
    @JsName("depthBiasSlopeScale")
    val depthBiasSlopeScale: Float = 0f,
    @JsName("depthBiasClamp")
    val depthBiasClamp: Float = 0f
)

data class GPUShaderModuleDescriptor(
    @JsName("code")
    val code: String,
    @JsName("label")
    val label: String = ""
)

class GPUSamplerDescriptor(
    @JsName("label")
    val label: String = "",
    @JsName("addressModeU")
    val addressModeU: GPUAddressMode = GPUAddressMode.clampToEdge,
    @JsName("addressModeV")
    val addressModeV: GPUAddressMode = GPUAddressMode.clampToEdge,
    @JsName("addressModeW")
    val addressModeW: GPUAddressMode = GPUAddressMode.clampToEdge,
    @JsName("magFilter")
    val magFilter: GPUFilterMode = GPUFilterMode.nearest,
    @JsName("minFilter")
    val minFilter: GPUFilterMode = GPUFilterMode.nearest,
    @JsName("mipmapFilter")
    val mipmapFilter: GPUMipmapFilterMode = GPUMipmapFilterMode.nearest,
    @JsName("lodMinClamp")
    val lodMinClamp: Float = 0f,
    @JsName("lodMaxClamp")
    val lodMaxClamp: Float = 32f,
    //@JsName("compare")
    //val compare: GPUCompareFunction,
    @JsName("maxAnisotropy")
    val maxAnisotropy: Int = 1,
)

class GPUTextureDescriptor(
    @JsName("size")
    val size: IntArray,
    @JsName("format")
    val format: GPUTextureFormat,
    @JsName("usage")
    val usage: Int,
    @JsName("label")
    val label: String = "",
    @JsName("mipLevelCount")
    val mipLevelCount: Int = 1,
    @JsName("sampleCount")
    val sampleCount: Int = 1,
    @JsName("dimension")
    val dimension: GPUTextureDimension = GPUTextureDimension.texture2d,
    @JsName("viewFormats")
    val viewFormats: Array<GPUTextureFormat> = emptyArray(),
)

interface GPUTextureViewDescriptor
fun GPUTextureViewDescriptor(
    label: String = "",
    format: GPUTextureFormat? = null,
    dimension: GPUTextureViewDimension? = null,
    //val aspect: GPUTextureAspect = 'all'
    baseMipLevel: Int = 0,
    mipLevelCount: Int? = null,
    baseArrayLayer: Int = 0,
    arrayLayerCount: Int? = null
): GPUTextureViewDescriptor {
    val o = js("({})")
    o["label"] = label
    format?.let { o["format"] = it }
    dimension?.let { o["dimension"] = it }
    o["baseMipLevel"] = baseMipLevel
    mipLevelCount?.let { o["mipLevelCount"] = it }
    o["baseArrayLayer"] = baseArrayLayer
    arrayLayerCount?.let { o["arrayLayerCount"] = it }
    return o
}

class GPUVertexAttribute(
    @JsName("format")
    val format: GPUVertexFormat,
    @JsName("offset")
    val offset: Long,
    @JsName("shaderLocation")
    val shaderLocation: Int
)

class GPUVertexBufferLayout(
    @JsName("arrayStride")
    val arrayStride: Long,
    @JsName("attributes")
    val attributes: Array<GPUVertexAttribute>,
    @JsName("stepMode")
    val stepMode: GPUVertexStepMode = GPUVertexStepMode.vertex
)

class GPUVertexState(
    @JsName("module")
    val module: GPUShaderModule,
    @JsName("entryPoint")
    val entryPoint: String,
    @JsName("buffers")
    val buffers: Array<GPUVertexBufferLayout> = emptyArray(),
)

data class GPURequestAdapterOptions(
    @JsName("powerPreference")
    val powerPreference: GPUPowerPreference,
    @JsName("forceFallbackAdapter")
    val forceFallbackAdapter: Boolean = false
)