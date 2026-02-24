package de.fabmax.kool.demo.uidemo

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.MdColor

class FlowLayoutWindow(uiDemo: UiDemo) : DemoWindow("Flow Layout", uiDemo) {

    init {
        windowDockable.setFloatingBounds(width = Dp(550f), height = Dp(600f))
    }

    override fun UiScope.windowContent() = ScrollArea(Grow.Std, Grow.Std) {
        modifier
            .width(Grow.Std)
            .padding(horizontal = sizes.gap, vertical = sizes.largeGap)
            .layout(ColumnLayout)
        Text("Horizontal Flow:") {
            modifier
                .font(sizes.largeText)
                .margin(bottom = sizes.smallGap)
        }

        FlowRow(Grow.Std, FitContent) {
            modifier.gap(horizontal = sizes.smallGap, vertical = sizes.smallGap)
            modifier
                .margin(bottom = sizes.gap)
                .padding(sizes.smallGap)
                .backgroundColor(colors.secondaryVariantAlpha(0.1f))

            val tags = listOf(
                "Kotlin", "UI", "Flow", "Layout", "Responsive",
                "Wrap", "Container", "Flex", "Grid", "Auto",
                "Dynamic", "Adaptive", "Modern", "Kool"
            )

            for (tag in tags) {
                Box(FitContent, FitContent) {
                    modifier
                        .background(RoundRectBackground(colors.primary, sizes.smallGap))
                        .padding(horizontal = sizes.gap, vertical = sizes.smallGap)
                        .border(RoundRectBorder(colors.primaryVariant, sizes.smallGap, 1.dp))

                    Text(tag) {
                        modifier
                            .textColor(colors.onPrimary)
                            .font(sizes.smallText)
                    }
                }
            }
        }

        Text("Cards:") {
            modifier
                .font(sizes.largeText)
                .margin(bottom = sizes.smallGap)
        }

        FlowRow(Grow.Std, FitContent) {
            modifier.gap(horizontal = sizes.gap, vertical = sizes.gap)
            modifier.margin(bottom = sizes.gap)

            val cardColors = listOf(
                MdColor.RED to "Red",
                MdColor.PINK to "Pink",
                MdColor.PURPLE to "Purple",
                MdColor.INDIGO to "Indigo",
                MdColor.BLUE to "Blue",
                MdColor.CYAN to "Cyan",
                MdColor.TEAL to "Teal",
                MdColor.GREEN to "Green"
            )

            for ((color, name) in cardColors) {
                Box(120.dp, 80.dp) {
                    modifier
                        .backgroundColor(color)
                        .border(RoundRectBorder(color, sizes.gap, 1.dp))

                    Text(name) {
                        modifier
                            .textColor(Color.WHITE)
                            .align(AlignmentX.Center, AlignmentY.Center)
                    }
                }
            }
        }

        Text("Vertical Flow:") {
            modifier
                .font(sizes.largeText)
                .margin(bottom = sizes.smallGap)
        }

        Row(Grow.Std, 150.dp) {
            modifier.margin(bottom = sizes.gap)

            FlowColumn(FitContent, Grow.Std) {
                modifier.gap(horizontal = sizes.smallGap, vertical = sizes.smallGap)
                modifier
                    .padding(sizes.smallGap)
                    .backgroundColor(colors.secondaryVariantAlpha(0.1f))

                val items = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L")

                for (item in items) {
                    Box(40.dp, 30.dp) {
                        modifier.backgroundColor(colors.secondary)
                        Text(item) {
                            modifier
                                .textColor(colors.onSecondary)
                                .align(AlignmentX.Center, AlignmentY.Center)
                        }
                    }
                }
            }
        }

        Text("Alignment Options:") {
            modifier
                .font(sizes.largeText)
                .margin(bottom = sizes.smallGap)
        }

        var selectedAlignX by remember(AlignmentX.Start)
        var selectedAlignY by remember(AlignmentY.Top)

        Row {
            modifier.margin(bottom = sizes.smallGap)

            Text("Item Align: ") { modifier.alignY(AlignmentY.Center) }

            ComboBox {
                modifier
                    .width(100.dp)
                    .margin(end = sizes.gap)
                    .items(listOf("Start", "Center", "End"))
                    .selectedIndex(
                        when (selectedAlignX) {
                            AlignmentX.Start -> 0
                            AlignmentX.Center -> 1
                            AlignmentX.End -> 2
                        }
                    )
                    .onItemSelected {
                        selectedAlignX = when (it) {
                            0 -> AlignmentX.Start
                            1 -> AlignmentX.Center
                            else -> AlignmentX.End
                        }
                    }
            }

            Text("Row Align: ") { modifier.alignY(AlignmentY.Center).margin(start = sizes.gap) }

            ComboBox {
                modifier
                    .width(100.dp)
                    .items(listOf("Top", "Center", "Bottom"))
                    .selectedIndex(
                        when (selectedAlignY) {
                            AlignmentY.Top -> 0
                            AlignmentY.Center -> 1
                            AlignmentY.Bottom -> 2
                        }
                    )
                    .onItemSelected {
                        selectedAlignY = when (it) {
                            0 -> AlignmentY.Top
                            1 -> AlignmentY.Center
                            else -> AlignmentY.Bottom
                        }
                    }
            }
        }

        FlowRow(Grow.Std, FitContent) {
            modifier.gap(horizontal = sizes.smallGap, vertical = sizes.smallGap)
            modifier.flowAlign(x = selectedAlignX, y = selectedAlignY)
            modifier
                .padding(sizes.smallGap)
                .backgroundColor(colors.secondaryVariantAlpha(0.1f))

            for (i in 1..12) {
                Box(FitContent, FitContent) {
                    modifier
                        .backgroundColor(colors.primaryVariant)
                        .padding(sizes.smallGap)

                    Text("#$i") {
                        modifier.textColor(colors.onPrimary)
                    }
                }
            }
        }
    }
}
