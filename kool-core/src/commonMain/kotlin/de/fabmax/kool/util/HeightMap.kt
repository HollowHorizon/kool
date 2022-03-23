package de.fabmax.kool.util

import de.fabmax.kool.math.clamp
import de.fabmax.kool.pipeline.TexFormat
import de.fabmax.kool.pipeline.TextureData2d
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class HeightMap(heightData: Float32Buffer, heightScale: Float, heightOffset: Float, val width: Int, val height: Int) {

    val heightMap = FloatArray(width * height)
    val minHeight: Float
    val maxHeight: Float

    init {
        check(heightData.capacity == width * height) {
            "Supplied height data has not the correct size: ${heightData.capacity} elements != $width x $height"
        }

        var min = Float.POSITIVE_INFINITY
        var max = Float.NEGATIVE_INFINITY
        for (i in heightMap.indices) {
            heightMap[i] = heightData[i] * heightScale + heightOffset
            min = min(min, heightMap[i])
            max = max(max, heightMap[i])
        }
        minHeight = min
        maxHeight = max
    }

    fun getHeightLinear(x: Float, y: Float): Float {
        val x0 = floor(x).toInt()
        val x1 = x0 + 1
        val xf = x - x0
        val y0 = floor(y).toInt()
        val y1 = y0 + 1
        val yf = y - y0

        val h00 = getHeight(x0, y0)
        val h01 = getHeight(x0, y1)
        val h10 = getHeight(x1, y0)
        val h11 = getHeight(x1, y1)

        val ha = h00 * (1f - yf) + h01 * yf
        val hb = h10 * (1f - yf) + h11 * yf
        return ha * (1f - xf) + hb * xf
    }

    fun getHeight(x: Int, y: Int): Float {
        val cx = x.clamp(0, width - 1)
        val cy = y.clamp(0, height - 1)
        return heightMap[cy * width + cx]
    }

    companion object {
        /**
         * Constructs a [HeightMap] from the provided [TextureData2d] object. Format must be either [TexFormat.R_F16]
         * or [TexFormat.R]
         */
        fun fromTextureData2d(textureData2d: TextureData2d, heightScale: Float, heightOffset: Float = 0f): HeightMap {
            check(textureData2d.format == TexFormat.R_F16 || textureData2d.format == TexFormat.R) {
                "Texture format must be either TexFormat.R_F16 or TexFormat.R"
            }

            val buf: Float32Buffer = if (textureData2d.data is Float32Buffer) {
                textureData2d.data as Float32Buffer
            } else {
                val uint8Buf = textureData2d.data as Uint8Buffer
                val floatBuf = createFloat32Buffer(textureData2d.width * textureData2d.height)
                for (i in 0 until textureData2d.width * textureData2d.height) {
                    floatBuf[i] = (uint8Buf[i].toInt() and 0xff) / 255f
                }
                floatBuf
            }
            return HeightMap(buf, heightScale, heightOffset, textureData2d.width, textureData2d.height)
        }

        /**
         * Constructs a [HeightMap] from the provided binary buffer. Buffer content is expected to be a plain array of
         * 16-bit integers with dimension [width] x [height]. If [width] and / or [height] are not specified, a square
         * map is assumed.
         */
        fun fromRawData(rawData: Uint8Buffer, heightScale: Float, width: Int = 0, height: Int = 0, heightOffset: Float = 0f): HeightMap {
            val elems = rawData.capacity / 2

            // if width and / or height are not supplied, a square map is assumed
            val w = if (width > 0) width else sqrt(elems.toDouble()).toInt()
            val h = if (height > 0) height else w
            if (w * h != elems) {
                throw IllegalArgumentException("Raw data size ($elems) does not match supplied size: $width x $height")
            }

            val floatBuf = createFloat32Buffer(elems)
            for (i in 0 until elems) {
                val ia = rawData[i*2].toInt() and 0xff
                val ib = rawData[i*2+1].toInt() and 0xff
                floatBuf[i] = (ia or (ib shl 8)) / 65535f
            }
            return HeightMap(floatBuf, heightScale, heightOffset, w, h)
        }
    }
}