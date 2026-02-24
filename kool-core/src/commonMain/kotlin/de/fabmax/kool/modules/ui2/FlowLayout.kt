package de.fabmax.kool.modules.ui2

import de.fabmax.kool.KoolContext
import de.fabmax.kool.modules.ui2.Layout.Companion.LAYOUT_EPS
import de.fabmax.kool.util.Color
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.max
import kotlin.math.round

enum class FlowDirection {
    Row, Column
}

object FlowLayout : Layout {
    override fun measureContentSize(uiNode: UiNode, ctx: KoolContext) {
        val modifier = uiNode.modifier as? FlowModifier ?: return
        if (modifier.flowDirection == FlowDirection.Row) {
            measureRow(uiNode, modifier)
        } else {
            measureColumn(uiNode, modifier)
        }
    }

    override fun layoutChildren(uiNode: UiNode, ctx: KoolContext) {
        val modifier = uiNode.modifier as? FlowModifier ?: return
        if (modifier.flowDirection == FlowDirection.Row) {
            layoutRow(uiNode, modifier)
        } else {
            layoutColumn(uiNode, modifier)
        }
    }

    private fun measureRow(node: UiNode, mod: FlowModifier) = node.run {
        val hGap = mod.horizontalGap.px
        val vGap = mod.verticalGap.px
        val isFixedW = modifier.width is Dp
        val isFixedH = modifier.height is Dp

        val maxWidth = if (isFixedW) (modifier.width as Dp).px - paddingStartPx - paddingEndPx
        else getAvailableWidth(node)

        var width = 0f
        var height = 0f
        var rowW = 0f
        var rowH = 0f
        var first = true

        for (i in children.indices) {
            val child = children[i]
            val itemW = child.marginStartPx + child.contentWidthPx + child.marginEndPx
            val itemH = child.marginTopPx + child.contentHeightPx + child.marginBottomPx
            val gap = if (first) 0f else hGap

            if (!first && rowW + gap + itemW > maxWidth + LAYOUT_EPS) {
                width = max(width, rowW)
                height += rowH + vGap
                rowW = 0f
                rowH = 0f
                first = true
            }

            val effGap = if (first) 0f else hGap
            rowW += effGap + itemW
            rowH = max(rowH, itemH)
            first = false
        }

        if (!first) {
            width = max(width, rowW)
            height += rowH
        }

        val finalW = if (isFixedW) (modifier.width as Dp).px else width + paddingStartPx + paddingEndPx
        val finalH = if (isFixedH) (modifier.height as Dp).px else height + paddingTopPx + paddingBottomPx

        setContentSize(finalW, finalH)
    }

    private fun layoutRow(node: UiNode, mod: FlowModifier) = node.run {
        val hGap = mod.horizontalGap.px
        val vGap = mod.verticalGap.px
        val maxWidth = widthPx - paddingStartPx - paddingEndPx
        val maxHeight = heightPx - paddingTopPx - paddingBottomPx

        class Line(val start: Int, val end: Int, val width: Float, val height: Float)
        val lines = mutableListOf<Line>()
        var start = 0
        var rowW = 0f
        var rowH = 0f
        var totalContentH = 0f

        for (i in children.indices) {
            val child = children[i]
            val itemW = child.marginStartPx + child.contentWidthPx + child.marginEndPx
            val itemH = child.marginTopPx + child.contentHeightPx + child.marginBottomPx
            val gap = if (i == start) 0f else hGap

            if (i > start && rowW + gap + itemW > maxWidth + LAYOUT_EPS) {
                lines += Line(start, i, rowW, rowH)
                totalContentH += rowH + vGap
                start = i
                rowW = 0f
                rowH = 0f
            }
            val effGap = if (i == start) 0f else hGap
            rowW += effGap + itemW
            rowH = max(rowH, itemH)
        }
        if (start < children.size) {
            lines += Line(start, children.size, rowW, rowH)
            totalContentH += rowH
        }

        // Global Vertical Alignment (Align Content)
        var y = topPx + paddingTopPx
        if (maxHeight > totalContentH) {
            y += when (mod.flowAlignY) {
                AlignmentY.Center -> (maxHeight - totalContentH) * 0.5f
                AlignmentY.Bottom -> maxHeight - totalContentH
                else -> 0f
            }
        }

        for (line in lines) {
            var x = leftPx + paddingStartPx

            // Horizontal Alignment (Justify Content)
            if (maxWidth > line.width) {
                x += when (mod.flowAlignX) {
                    AlignmentX.Center -> (maxWidth - line.width) * 0.5f
                    AlignmentX.End -> maxWidth - line.width
                    else -> 0f
                }
            }

            for (i in line.start until line.end) {
                val child = children[i]
                if (i > line.start) x += hGap

                val childX = x + child.marginStartPx
                val childW = child.contentWidthPx

                // Local Vertical Alignment (Align Items)
                val slotH = line.height - child.marginTopPx - child.marginBottomPx
                val childH = child.contentHeightPx

                val localYOffset = when (mod.flowAlignY) {
                    AlignmentY.Center -> (slotH - childH) * 0.5f
                    AlignmentY.Bottom -> slotH - childH
                    else -> 0f
                }
                val childY = y + child.marginTopPx + localYOffset

                child.setBounds(round(childX), round(childY), round(childX + childW), round(childY + childH))
                x += childW + child.marginEndPx
            }
            y += line.height + vGap
        }
    }

    private fun measureColumn(node: UiNode, mod: FlowModifier) = node.run {
        val hGap = mod.horizontalGap.px
        val vGap = mod.verticalGap.px
        val isFixedW = modifier.width is Dp
        val isFixedH = modifier.height is Dp

        val maxHeight = if (isFixedH) (modifier.height as Dp).px - paddingTopPx - paddingBottomPx
        else getAvailableHeight(node)

        var width = 0f
        var height = 0f
        var colW = 0f
        var colH = 0f
        var first = true

        for (i in children.indices) {
            val child = children[i]
            val itemW = child.marginStartPx + child.contentWidthPx + child.marginEndPx
            val itemH = child.marginTopPx + child.contentHeightPx + child.marginBottomPx
            val gap = if (first) 0f else vGap

            if (!first && colH + gap + itemH > maxHeight + LAYOUT_EPS) {
                height = max(height, colH)
                width += colW + hGap
                colH = 0f
                colW = 0f
                first = true
            }

            val effGap = if (first) 0f else vGap
            colH += effGap + itemH
            colW = max(colW, itemW)
            first = false
        }

        if (!first) {
            height = max(height, colH)
            width += colW
        }

        val finalW = if (isFixedW) (modifier.width as Dp).px else width + paddingStartPx + paddingEndPx
        val finalH = if (isFixedH) (modifier.height as Dp).px else height + paddingTopPx + paddingBottomPx

        setContentSize(finalW, finalH)
    }

    private fun layoutColumn(node: UiNode, mod: FlowModifier) = node.run {
        val hGap = mod.horizontalGap.px
        val vGap = mod.verticalGap.px
        val maxWidth = widthPx - paddingStartPx - paddingEndPx
        val maxHeight = heightPx - paddingTopPx - paddingBottomPx

        class Col(val start: Int, val end: Int, val width: Float, val height: Float)
        val cols = mutableListOf<Col>()
        var start = 0
        var colW = 0f
        var colH = 0f
        var totalContentW = 0f

        for (i in children.indices) {
            val child = children[i]
            val itemW = child.marginStartPx + child.contentWidthPx + child.marginEndPx
            val itemH = child.marginTopPx + child.contentHeightPx + child.marginBottomPx
            val gap = if (i == start) 0f else vGap

            if (i > start && colH + gap + itemH > maxHeight + LAYOUT_EPS) {
                cols += Col(start, i, colW, colH)
                totalContentW += colW + hGap
                start = i
                colW = 0f
                colH = 0f
            }
            val effGap = if (i == start) 0f else vGap
            colH += effGap + itemH
            colW = max(colW, itemW)
        }
        if (start < children.size) {
            cols += Col(start, children.size, colW, colH)
            totalContentW += colW
        }

        // Global Horizontal Alignment (Align Content)
        var x = leftPx + paddingStartPx
        if (maxWidth > totalContentW) {
            x += when (mod.flowAlignX) {
                AlignmentX.Center -> (maxWidth - totalContentW) * 0.5f
                AlignmentX.End -> maxWidth - totalContentW
                else -> 0f
            }
        }

        for (col in cols) {
            var y = topPx + paddingTopPx

            // Vertical Alignment (Justify Content)
            if (maxHeight > col.height) {
                y += when (mod.flowAlignY) {
                    AlignmentY.Center -> (maxHeight - col.height) * 0.5f
                    AlignmentY.Bottom -> maxHeight - col.height
                    else -> 0f
                }
            }

            for (i in col.start until col.end) {
                val child = children[i]
                if (i > col.start) y += vGap

                val childY = y + child.marginTopPx
                val childH = child.contentHeightPx

                // Local Horizontal Alignment (Align Items)
                val slotW = col.width - child.marginStartPx - child.marginEndPx
                val childW = child.contentWidthPx

                val localXOffset = when (mod.flowAlignX) {
                    AlignmentX.Center -> (slotW - childW) * 0.5f
                    AlignmentX.End -> slotW - childW
                    else -> 0f
                }
                val childX = x + child.marginStartPx + localXOffset

                child.setBounds(round(childX), round(childY), round(childX + childW), round(childY + childH))
                y += childH + child.marginBottomPx
            }
            x += col.width + hGap
        }
    }

    private fun getAvailableWidth(node: UiNode): Float {
        val parentW = node.parent?.innerWidthPx ?: 0f
        if (parentW <= LAYOUT_EPS) return Float.MAX_VALUE
        return (parentW - node.marginStartPx - node.marginEndPx - node.paddingStartPx - node.paddingEndPx).coerceAtLeast(0f)
    }

    private fun getAvailableHeight(node: UiNode): Float {
        val parentH = node.parent?.innerHeightPx ?: 0f
        if (parentH <= LAYOUT_EPS) return Float.MAX_VALUE
        return (parentH - node.marginTopPx - node.marginBottomPx - node.paddingTopPx - node.paddingBottomPx).coerceAtLeast(0f)
    }
}

interface FlowScope : UiScope {
    override val modifier: FlowModifier get() = uiNode.modifier as FlowModifier

    fun divider(
        color: Color = colors.secondaryVariant,
        thickness: Dp = sizes.borderWidth,
        margin: Dp = sizes.smallGap
    ) {
        Box(width = thickness, height = thickness) {
            modifier.backgroundColor(color).margin(all = margin)
        }
    }
}

open class FlowModifier(surface: UiSurface) : UiModifier(surface) {
    var flowDirection: FlowDirection by property(FlowDirection.Row)
    var horizontalGap: Dp by property(Dp.ZERO)
    var verticalGap: Dp by property(Dp.ZERO)
    var flowAlignX: AlignmentX by property(AlignmentX.Start)
    var flowAlignY: AlignmentY by property(AlignmentY.Top)
}

fun <T : FlowModifier> T.flowDirection(direction: FlowDirection): T { flowDirection = direction; return this }
fun <T : FlowModifier> T.gap(horizontal: Dp = horizontalGap, vertical: Dp = verticalGap): T { horizontalGap = horizontal; verticalGap = vertical; return this }
fun <T : FlowModifier> T.flowAlignX(alignment: AlignmentX): T { flowAlignX = alignment; return this }
fun <T : FlowModifier> T.flowAlignY(alignment: AlignmentY): T { flowAlignY = alignment; return this }
fun <T : FlowModifier> T.flowAlign(x: AlignmentX, y: AlignmentY): T { flowAlignX = x; flowAlignY = y; return this }

inline fun UiScope.Flow(
    width: Dimension = FitContent,
    height: Dimension = FitContent,
    scopeName: String? = null,
    block: FlowScope.() -> Unit
): FlowScope {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val flow = uiNode.createChild(scopeName, FlowNode::class, FlowNode.factory)
    flow.modifier.size(width, height).layout(FlowLayout)
    flow.block()
    return flow
}

inline fun UiScope.FlowRow(
    width: Dimension = FitContent,
    height: Dimension = FitContent,
    scopeName: String? = null,
    block: FlowScope.() -> Unit
): FlowScope {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val flow = uiNode.createChild(scopeName, FlowNode::class, FlowNode.factory)
    flow.modifier.size(width, height).layout(FlowLayout).flowDirection(FlowDirection.Row)
    flow.block()
    return flow
}

inline fun UiScope.FlowColumn(
    width: Dimension = FitContent,
    height: Dimension = FitContent,
    scopeName: String? = null,
    block: FlowScope.() -> Unit
): FlowScope {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val flow = uiNode.createChild(scopeName, FlowNode::class, FlowNode.factory)
    flow.modifier.size(width, height).layout(FlowLayout).flowDirection(FlowDirection.Column)
    flow.block()
    return flow
}

open class FlowNode(parent: UiNode?, surface: UiSurface) : UiNode(parent, surface), FlowScope {
    override val modifier = FlowModifier(surface)
    companion object { val factory: (UiNode, UiSurface) -> FlowNode = { parent, surface -> FlowNode(parent, surface) } }
}
