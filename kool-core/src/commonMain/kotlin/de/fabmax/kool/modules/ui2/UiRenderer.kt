package de.fabmax.kool.modules.ui2

import de.fabmax.kool.math.Vec4f
import de.fabmax.kool.util.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

interface UiRenderer<in T: UiNode> {
    fun renderUi(node: T)
}

fun UiRenderer(renderUi: (UiNode) -> Unit) = object : UiRenderer<UiNode> {
    override fun renderUi(node: UiNode) {
        renderUi(node)
    }
}

class RectBackground(val backgroundColor: Color) : UiRenderer<UiNode> {
    override fun renderUi(node: UiNode) {
        node.apply {
            val lt = max(leftPx, clipLeftPx)
            val rt = min(rightPx, clipRightPx)
            val tp = max(topPx, clipTopPx)
            val bt = min(bottomPx, clipBottomPx)

            node.getUiPrimitives(UiSurface.LAYER_BACKGROUND)
                .rect(lt, tp, rt - lt, bt - tp, clipBoundsPx, backgroundColor)
        }
    }
}

class RoundRectBackground(val backgroundColor: Color, val cornerRadius: Dp) : UiRenderer<UiNode> {
    override fun renderUi(node: UiNode) {
        node.apply {
            val c = cornerRadius.px
            val lt = max(leftPx, clipLeftPx - c)
            val rt = min(rightPx, clipRightPx + c)
            val tp = max(topPx, clipTopPx - c)
            val bt = min(bottomPx, clipBottomPx + c)

            node.getUiPrimitives(UiSurface.LAYER_BACKGROUND)
                .roundRect(lt, tp, rt - lt, bt - tp, c, clipBoundsPx, backgroundColor)
        }
    }
}

class CircularBackground(val backgroundColor: Color) : UiRenderer<UiNode> {
    override fun renderUi(node: UiNode) {
        node.apply {
            node.getUiPrimitives(UiSurface.LAYER_BACKGROUND)
                .localCircle(widthPx * 0.5f, heightPx * 0.5f, min(widthPx, heightPx) * 0.5f, backgroundColor)
        }
    }
}

class RectGradientBackground(
    val colorA: Color, val colorB: Color,
    val gradientCx: Dp, val gradientCy: Dp,
    val gradientRx: Dp, val gradientRy: Dp
) : UiRenderer<UiNode> {
    override fun renderUi(node: UiNode) {
        node.apply {
            node.getUiPrimitives(UiSurface.LAYER_BACKGROUND)
                .localRectGradient(0f, 0f, widthPx, heightPx, colorA, colorB, gradientCx.px, gradientCy.px, gradientRx.px, gradientRy.px)
        }
    }
}

class RoundRectGradientBackground(
    val cornerRadius: Dp, val colorA: Color, val colorB: Color,
    val gradientCx: Dp, val gradientCy: Dp,
    val gradientRx: Dp, val gradientRy: Dp
) : UiRenderer<UiNode> {
    override fun renderUi(node: UiNode) {
        node.apply {
            node.getUiPrimitives(UiSurface.LAYER_BACKGROUND)
                .localRoundRectGradient(0f, 0f, widthPx, heightPx, cornerRadius.px, colorA, colorB, gradientCx.px, gradientCy.px, gradientRx.px, gradientRy.px)
        }
    }
}

open class RectBorder(val borderColor: Color, val borderWidth: Dp, val inset: Dp = Dp.ZERO) : UiRenderer<UiNode> {
    override fun renderUi(node: UiNode) {
        node.apply {
            val inPx = inset.px
            node.getUiPrimitives().localRectBorder(
                inPx, inPx, widthPx - inPx * 2f, heightPx - inPx * 2f, borderWidth.px, borderColor
            )
        }
    }
}

object DebugBorder : RectBorder(Color.RED, Dp(1f))

class RoundRectBorder(val borderColor: Color, val cornerRadius: Dp, val borderWidth: Dp, val inset: Dp = Dp.ZERO) : UiRenderer<UiNode> {
    override fun renderUi(node: UiNode) {
        node.apply {
            val inPx = round(inset.px)
            val bw = round(borderWidth.px)
            node.getUiPrimitives().localRoundRectBorder(
                inPx, inPx, widthPx - inPx * 2f, heightPx - inPx * 2f, cornerRadius.px, bw, borderColor
            )
        }
    }
}

class CircularBorder(val borderColor: Color, val borderWidth: Dp, val inset: Dp = Dp.ZERO) : UiRenderer<UiNode> {
    override fun renderUi(node: UiNode) {
        node.apply {
            val bw = round(borderWidth.px)
            val x = widthPx * 0.5f
            val y = heightPx * 0.5f
            val r = min(x, y) - round(inset.px)
            node.getUiPrimitives().localCircleBorder(x, y, r, bw, borderColor)
        }
    }
}

private fun Vec4f.inflate(amount: Float): Vec4f {
    return Vec4f(x - amount, y - amount, z + amount, w + amount)
}

open class Shadow(val shadowColor: Color, val blurRadius: Dp, val spread: Dp = Dp.ZERO) : UiRenderer<UiNode> {
    override fun renderUi(node: UiNode) {
        node.apply {
            val blur = blurRadius.px
            val sprd = spread.px

            val x = leftPx - sprd
            val y = topPx - sprd
            val w = widthPx + sprd * 2
            val h = heightPx + sprd * 2

            // TODO: Join this with clip of some parents? (Like ScrollPane)
            val shadowClip = clipBoundsPx.inflate(blur)

            node.getUiPrimitives(UiSurface.LAYER_BACKGROUND)
                .rectShadow(x, y, w, h, blur, shadowClip, shadowColor, inset = false)
        }
    }
}

class RoundRectShadow(
    shadowColor: Color,
    val cornerRadius: Dp,
    blurRadius: Dp,
    spread: Dp = Dp.ZERO
) : Shadow(shadowColor, blurRadius, spread) {

    override fun renderUi(node: UiNode) {
        node.apply {
            val blur = blurRadius.px
            val sprd = spread.px
            val cr = cornerRadius.px

            val x = leftPx - sprd
            val y = topPx - sprd
            val w = widthPx + sprd * 2
            val h = heightPx + sprd * 2

            val effCorner = cr + sprd
            val shadowClip = clipBoundsPx.inflate(blur)

            node.getUiPrimitives(UiSurface.LAYER_BACKGROUND)
                .roundRectShadow(x, y, w, h, effCorner, blur, shadowClip, shadowColor, inset = false)
        }
    }
}

class InnerShadow(
    val shadowColor: Color,
    val blurRadius: Dp,
    val cornerRadius: Dp = Dp.ZERO
) : UiRenderer<UiNode> {

    override fun renderUi(node: UiNode) {
        node.apply {
            val blur = blurRadius.px
            val cr = cornerRadius.px

            if (cr > 0f) {
                node.getUiPrimitives()
                    .roundRectShadow(leftPx, topPx, widthPx, heightPx, cr, blur, clipBoundsPx, shadowColor, inset = true)
            } else {
                node.getUiPrimitives()
                    .rectShadow(leftPx, topPx, widthPx, heightPx, blur, clipBoundsPx, shadowColor, inset = true)
            }
        }
    }
}