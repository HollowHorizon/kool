package de.fabmax.kool.platform.gl

import de.fabmax.kool.pipeline.*
import de.fabmax.kool.platform.Lwjgl3Context
import de.fabmax.kool.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glBindBufferBase
import org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER

interface MappedUniform {
    fun setUniform(ctx: Lwjgl3Context): Boolean

    companion object {
        fun mappedUniform(uniform: Uniform<*>, location: Int): MappedUniform {
            return when (uniform) {
                is Uniform1f -> MappedUniform1f(uniform, location)
                is Uniform2f -> MappedUniform2f(uniform, location)
                is Uniform3f -> MappedUniform3f(uniform, location)
                is Uniform4f -> MappedUniform4f(uniform, location)
                is Uniform1fv -> MappedUniform1fv(uniform, location)
                is Uniform2fv -> MappedUniform2fv(uniform, location)
                is Uniform3fv -> MappedUniform3fv(uniform, location)
                is Uniform4fv -> MappedUniform4fv(uniform, location)
                is UniformMat3f -> MappedUniformMat3f(uniform, location)
                is UniformMat3fv -> MappedUniformMat3fv(uniform, location)
                is UniformMat4f -> MappedUniformMat4f(uniform, location)
                is UniformMat4fv -> MappedUniformMat4fv(uniform, location)

                is Uniform1i -> MappedUniform1i(uniform, location)
                is Uniform2i -> MappedUniform2i(uniform, location)
                is Uniform3i -> MappedUniform3i(uniform, location)
                is Uniform4i -> MappedUniform4i(uniform, location)
                is Uniform1iv -> MappedUniform1iv(uniform, location)
                is Uniform2iv -> MappedUniform2iv(uniform, location)
                is Uniform3iv -> MappedUniform3iv(uniform, location)
                is Uniform4iv -> MappedUniform4iv(uniform, location)
            }
        }
    }
}

class MappedUbo(val uboDesc: UniformBuffer, val layout: ExternalBufferLayout) : MappedUniform {
    var uboBuffer: BufferResource? = null
    val hostBuffer = MixedBuffer(layout.size)

    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        val gpuBuf = uboBuffer
        return if (gpuBuf != null) {
            layout.putToBuffer(uboDesc.uniforms, hostBuffer)
            gpuBuf.setData(hostBuffer, GL_DYNAMIC_DRAW, ctx)
            glBindBufferBase(GL_UNIFORM_BUFFER, uboDesc.binding, gpuBuf.buffer)
            true
        } else {
            false
        }
    }
}

class MappedUniform1f(val uniform: Uniform1f, val location: Int) : MappedUniform {
    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        glUniform1f(location, uniform.value)
        return true
    }
}

class MappedUniform2f(val uniform: Uniform2f, val location: Int) : MappedUniform {
    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        glUniform2f(location, uniform.value.x, uniform.value.y)
        return true
    }
}

class MappedUniform3f(val uniform: Uniform3f, val location: Int) : MappedUniform {
    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        glUniform3f(location, uniform.value.x, uniform.value.y, uniform.value.z)
        return true
    }
}

class MappedUniform4f(val uniform: Uniform4f, val location: Int) : MappedUniform {
    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        glUniform4f(location, uniform.value.x, uniform.value.y, uniform.value.z, uniform.value.w)
        return true
    }
}

class MappedUniform1fv(val uniform: Uniform1fv, val location: Int) : MappedUniform {
    private val buffer = Float32Buffer(uniform.size) as Float32BufferImpl
    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        for (i in 0 until uniform.size) {
            buffer[i] = uniform.value[i]
        }
        glUniform1fv(location, buffer.buffer)
        return true
    }
}

class MappedUniform2fv(val uniform: Uniform2fv, val location: Int) : MappedUniform {
    private val buffer = Float32Buffer(2 * uniform.size) as Float32BufferImpl
    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        var j = 0
        for (i in 0 until uniform.size) {
            buffer[j++] = uniform.value[i].x
            buffer[j++] = uniform.value[i].y
        }
        glUniform2fv(location, buffer.buffer)
        return true
    }
}

class MappedUniform3fv(val uniform: Uniform3fv, val location: Int) : MappedUniform {
    private val buffer = Float32Buffer(3 * uniform.size) as Float32BufferImpl
    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        var j = 0
        for (i in 0 until uniform.size) {
            buffer[j++] = uniform.value[i].x
            buffer[j++] = uniform.value[i].y
            buffer[j++] = uniform.value[i].z
        }
        glUniform3fv(location, buffer.buffer)
        return true
    }
}

class MappedUniform4fv(val uniform: Uniform4fv, val location: Int) : MappedUniform {
    private val buffer = Float32Buffer(4 * uniform.size) as Float32BufferImpl
    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        var j = 0
        for (i in 0 until uniform.size) {
            buffer[j++] = uniform.value[i].x
            buffer[j++] = uniform.value[i].y
            buffer[j++] = uniform.value[i].z
            buffer[j++] = uniform.value[i].w
        }
        glUniform4fv(location, buffer.buffer)
        return true
    }
}

class MappedUniformMat3f(val uniform: UniformMat3f, val location: Int) : MappedUniform {
    private val buf = Float32Buffer(9) as Float32BufferImpl
    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        uniform.value.putTo(buf)
        buf.flip()
        glUniformMatrix3fv(location, false, buf.buffer)
        return true
    }
}

class MappedUniformMat3fv(val uniform: UniformMat3fv, val location: Int) : MappedUniform {
    private val buf = Float32Buffer(9 * uniform.size) as Float32BufferImpl
    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        for (i in 0 until uniform.size) {
            uniform.value[i].putTo(buf)
        }
        buf.flip()
        glUniformMatrix3fv(location, false, buf.buffer)
        return true
    }
}

class MappedUniformMat4f(val uniform: UniformMat4f, val location: Int) : MappedUniform {
    private val buf = Float32Buffer(16) as Float32BufferImpl
    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        uniform.value.putTo(buf)
        buf.flip()
        glUniformMatrix4fv(location, false, buf.buffer)
        return true
    }
}

class MappedUniformMat4fv(val uniform: UniformMat4fv, val location: Int) : MappedUniform {
    private val buf = Float32Buffer(16 * uniform.size) as Float32BufferImpl
    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        for (i in 0 until uniform.size) {
            uniform.value[i].putTo(buf)
        }
        buf.flip()
        glUniformMatrix4fv(location, false, buf.buffer)
        return true
    }
}

class MappedUniform1i(val uniform: Uniform1i, val location: Int) : MappedUniform {
    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        glUniform1i(location, uniform.value)
        return true
    }
}

class MappedUniform2i(val uniform: Uniform2i, val location: Int) : MappedUniform {
    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        glUniform2i(location, uniform.value.x, uniform.value.y)
        return true
    }
}

class MappedUniform3i(val uniform: Uniform3i, val location: Int) : MappedUniform {
    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        glUniform3i(location, uniform.value.x, uniform.value.y, uniform.value.z)
        return true
    }
}

class MappedUniform4i(val uniform: Uniform4i, val location: Int) : MappedUniform {
    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        glUniform4i(location, uniform.value.x, uniform.value.y, uniform.value.z, uniform.value.w)
        return true
    }
}

class MappedUniform1iv(val uniform: Uniform1iv, val location: Int) : MappedUniform {
    private val buffer = Int32Buffer(uniform.size) as Int32BufferImpl
    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        for (i in 0 until uniform.size) {
            buffer[i] = uniform.value[i]
        }
        glUniform1iv(location, buffer.buffer)
        return true
    }
}

class MappedUniform2iv(val uniform: Uniform2iv, val location: Int) : MappedUniform {
    private val buffer = Int32Buffer(2 * uniform.size) as Int32BufferImpl
    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        var j = 0
        for (i in 0 until uniform.size) {
            buffer[j++] = uniform.value[i].x
            buffer[j++] = uniform.value[i].y
        }
        glUniform2iv(location, buffer.buffer)
        return true
    }
}

class MappedUniform3iv(val uniform: Uniform3iv, val location: Int) : MappedUniform {
    private val buffer = Int32Buffer(3 * uniform.size) as Int32BufferImpl
    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        var j = 0
        for (i in 0 until uniform.size) {
            buffer[j++] = uniform.value[i].x
            buffer[j++] = uniform.value[i].y
            buffer[j++] = uniform.value[i].z
        }
        glUniform3iv(location, buffer.buffer)
        return true
    }
}

class MappedUniform4iv(val uniform: Uniform4iv, val location: Int) : MappedUniform {
    private val buffer = Int32Buffer(4 * uniform.size) as Int32BufferImpl
    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        var j = 0
        for (i in 0 until uniform.size) {
            buffer[j++] = uniform.value[i].x
            buffer[j++] = uniform.value[i].y
            buffer[j++] = uniform.value[i].z
            buffer[j++] = uniform.value[i].w
        }
        glUniform4iv(location, buffer.buffer)
        return true
    }
}

abstract class MappedUniformTex(val texUnit: Int, val target: Int) : MappedUniform {
    protected fun checkLoadingState(ctx: Lwjgl3Context, texture: Texture, arrayIdx: Int): Boolean {
        if (texture.loadingState == Texture.LoadingState.NOT_LOADED) {
            when (texture.loader) {
                is AsyncTextureLoader -> {
                    texture.loadingState = Texture.LoadingState.LOADING
                    CoroutineScope(Dispatchers.RenderLoop).launch {
                        val texData = texture.loader.loadTextureDataAsync().await()
                        texture.loadedTexture = getLoadedTex(texData, texture, ctx)
                        texture.loadingState = Texture.LoadingState.LOADED
                    }
                }
                is SyncTextureLoader -> {
                    val data = texture.loader.loadTextureDataSync()
                    texture.loadedTexture = getLoadedTex(data, texture, ctx)
                    texture.loadingState = Texture.LoadingState.LOADED
                }
                is BufferedTextureLoader -> {
                    texture.loadedTexture = getLoadedTex(texture.loader.data, texture, ctx)
                    texture.loadingState = Texture.LoadingState.LOADED
                }
                else -> {
                    // loader is null
                    texture.loadingState = Texture.LoadingState.LOADING_FAILED
                }
            }
        }
        if (texture.loadingState == Texture.LoadingState.LOADED) {
            val tex = texture.loadedTexture as LoadedTextureGl
            glActiveTexture(texUnit + arrayIdx)
            glBindTexture(target, tex.texture)
            return true
        }

        return false
    }

    companion object {
        private val loadedTextures = mutableMapOf<TextureData, LoadedTextureGl>()

        protected fun getLoadedTex(texData: TextureData, texture: Texture, ctx: Lwjgl3Context): LoadedTextureGl {
            loadedTextures.values.removeAll { it.isDestroyed }
            return loadedTextures.getOrPut(texData) {
                val loaded = when (texture) {
                    is Texture1d -> TextureLoader.loadTexture1d(ctx, texture.props, texData)
                    is Texture2d -> TextureLoader.loadTexture2d(ctx, texture.props, texData)
                    is Texture3d -> TextureLoader.loadTexture3d(ctx, texture.props, texData)
                    is TextureCube -> TextureLoader.loadTextureCube(ctx, texture.props, texData as TextureDataCube)
                    else -> throw IllegalArgumentException("Unsupported texture type")
                }
                loaded
            }
        }
    }
}

class MappedUniformTex1d(private val sampler1d: TextureSampler1d, texUnit: Int, val locations: List<Int>) :
        MappedUniformTex(texUnit, GL_TEXTURE_2D) {
    // 1d texture internally uses a 2d texture to be compatible with glsl version 300 es

    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        var texUnit = texUnit
        var isValid = true
        for (i in 0 until sampler1d.arraySize) {
            val tex = sampler1d.textures[i]
            if (tex != null && checkLoadingState(ctx, tex, i)) {
                glUniform1i(locations[i], this.texUnit - GL_TEXTURE0 + i)
            } else {
                isValid = false
            }
            texUnit++
        }
        return isValid
    }
}

class MappedUniformTex2d(private val sampler2d: TextureSampler2d, texUnit: Int, val locations: List<Int>) :
        MappedUniformTex(texUnit, GL_TEXTURE_2D) {
    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        var texUnit = texUnit
        var isValid = true
        for (i in 0 until sampler2d.arraySize) {
            val tex = sampler2d.textures[i]
            if (tex != null && checkLoadingState(ctx, tex, i)) {
                glUniform1i(locations[i], this.texUnit - GL_TEXTURE0 + i)
            } else {
                isValid = false
            }
            texUnit++
        }
        return isValid
    }
}

class MappedUniformTex3d(private val sampler3d: TextureSampler3d, texUnit: Int, val locations: List<Int>) :
        MappedUniformTex(texUnit, GL_TEXTURE_3D) {
    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        var texUnit = texUnit
        var isValid = true
        for (i in 0 until sampler3d.arraySize) {
            val tex = sampler3d.textures[i]
            if (tex != null && checkLoadingState(ctx, tex, i)) {
                glUniform1i(locations[i], this.texUnit - GL_TEXTURE0 + i)
            } else {
                isValid = false
            }
            texUnit++
        }
        return isValid
    }
}

class MappedUniformTexCube(private val samplerCube: TextureSamplerCube, texUnit: Int, val locations: List<Int>) :
        MappedUniformTex(texUnit, GL_TEXTURE_CUBE_MAP) {
    override fun setUniform(ctx: Lwjgl3Context): Boolean {
        var texUnit = texUnit
        var isValid = true
        for (i in 0 until samplerCube.arraySize) {
            val tex = samplerCube.textures[i]
            if (tex != null && checkLoadingState(ctx, tex, i)) {
                glUniform1i(locations[i], this.texUnit - GL_TEXTURE0 + i)
            } else {
                isValid = false
            }
            texUnit++
        }
        return isValid
    }
}