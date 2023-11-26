package de.fabmax.kool.platform

import de.fabmax.kool.pipeline.TexFormat
import de.fabmax.kool.pipeline.backend.gl.WebGL2RenderingContext
import org.khronos.webgl.WebGLRenderingContext


val TexFormat.glInternalFormat: Int
    get() = when(this) {
        TexFormat.R -> WebGL2RenderingContext.R8
        TexFormat.RG -> WebGL2RenderingContext.RG8
        TexFormat.RGB -> WebGL2RenderingContext.RGB8
        TexFormat.RGBA -> WebGL2RenderingContext.RGBA8

        TexFormat.R_F16 -> WebGL2RenderingContext.R16F
        TexFormat.RG_F16 -> WebGL2RenderingContext.RG16F
        TexFormat.RGB_F16 -> WebGL2RenderingContext.RGB16F
        TexFormat.RGBA_F16 -> WebGL2RenderingContext.RGBA16F

        TexFormat.R_F32 -> WebGL2RenderingContext.R32F
        TexFormat.RG_F32 -> WebGL2RenderingContext.RG32F
        TexFormat.RGB_F32 -> WebGL2RenderingContext.RGB32F
        TexFormat.RGBA_F32 -> WebGL2RenderingContext.RGBA32F
    }

val TexFormat.glType: Int
    get() = when(this) {
        TexFormat.R -> WebGLRenderingContext.UNSIGNED_BYTE
        TexFormat.RG -> WebGLRenderingContext.UNSIGNED_BYTE
        TexFormat.RGB -> WebGLRenderingContext.UNSIGNED_BYTE
        TexFormat.RGBA -> WebGLRenderingContext.UNSIGNED_BYTE

        TexFormat.R_F16 -> WebGLRenderingContext.FLOAT
        TexFormat.RG_F16 -> WebGLRenderingContext.FLOAT
        TexFormat.RGB_F16 -> WebGLRenderingContext.FLOAT
        TexFormat.RGBA_F16 -> WebGLRenderingContext.FLOAT

        TexFormat.R_F32 -> WebGLRenderingContext.FLOAT
        TexFormat.RG_F32 -> WebGLRenderingContext.FLOAT
        TexFormat.RGB_F32 -> WebGLRenderingContext.FLOAT
        TexFormat.RGBA_F32 -> WebGLRenderingContext.FLOAT
    }

val TexFormat.glFormat: Int
    get() = when(this) {
        TexFormat.R -> WebGL2RenderingContext.RED
        TexFormat.RG -> WebGL2RenderingContext.RG
        TexFormat.RGB -> WebGLRenderingContext.RGB
        TexFormat.RGBA -> WebGLRenderingContext.RGBA

        TexFormat.R_F16 -> WebGL2RenderingContext.RED
        TexFormat.RG_F16 -> WebGL2RenderingContext.RG
        TexFormat.RGB_F16 -> WebGLRenderingContext.RGB
        TexFormat.RGBA_F16 -> WebGLRenderingContext.RGBA

        TexFormat.R_F32 -> WebGL2RenderingContext.RED
        TexFormat.RG_F32 -> WebGL2RenderingContext.RG
        TexFormat.RGB_F32 -> WebGLRenderingContext.RGB
        TexFormat.RGBA_F32 -> WebGLRenderingContext.RGBA
    }
