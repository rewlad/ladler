package ee.cone.base.test_loots

import ee.cone.base.vdom.JsonBuilder

case object NoTagStyle extends TagStyle {
  def appendStyle(builder: JsonBuilder) = ()
}

abstract class DisplayTagStyle(display: String) extends TagStyle {
  def appendStyle(builder: JsonBuilder) =
    builder.append("display").append(display)
}
case object InlineTagStyle extends DisplayTagStyle("inline")
case object InlineBlockTagStyle extends DisplayTagStyle("inline-block")
case object BlockTagStyle extends DisplayTagStyle("block")
case object FlexTagStyle extends DisplayTagStyle("flex")
case object CellTagStyle extends DisplayTagStyle("table-cell")
case object TableTagStyle extends DisplayTagStyle("table")


case object RightFloatTagStyle extends TagStyle {
  def appendStyle(builder: JsonBuilder) =
    builder.append("float").append("right")
}

case class MarginTagStyle(value: Int) extends TagStyle {
  def appendStyle(builder: JsonBuilder) =
    builder.append("margin").append(s"${value}px")
}

case class MarginSideTagStyle(value:Int) extends TagStyle {
  def appendStyle(builder: JsonBuilder) = {
    builder.append("marginLeft").append(s"${value}px")
    builder.append("marginRight").append(s"${value}px")
  }
}

case class MaxWidthTagStyle(value:Int) extends TagStyle {
  def appendStyle(builder: JsonBuilder) = {
    builder.append("maxWidth").append(s"${value}px")
    builder.append("margin").append("0px auto") //?
  }
}

case class WidthTagStyle(value:Int) extends TagStyle {
  def appendStyle(builder: JsonBuilder) =
    builder.append("width").append(s"${value}px")
}

case class MinWidthTagStyle(value:Int) extends TagStyle {
  def appendStyle(builder: JsonBuilder) =
    builder.append("minWidth").append(s"${value}px")
}

case class MinHeightTagStyle(value:Int) extends TagStyle {
  def appendStyle(builder: JsonBuilder) =
    builder.append("minHeight").append(s"${value}px")
}

case class PaddingTagStyle(value: Int) extends TagStyle {
  def appendStyle(builder: JsonBuilder) =
    builder.append("padding").append(s"${value}px")
}

case class PaddingSideTagStyle(value:Int) extends TagStyle { //?
  def appendStyle(builder: JsonBuilder) = {
    builder.append("paddingLeft").append(s"${value}px")
    builder.append("paddingRight").append(s"${value}px")
    builder.append("height").append("100%")
  }
}

case class HeightTagStyle(height:Int) extends TagStyle {
  def appendStyle(builder: JsonBuilder) =
    builder.append("height").append(s"${height}px")
}

case class MinWidthPercentTagStyle(value: Int) extends TagStyle {
  def appendStyle(builder: JsonBuilder) =
    builder.append("minWidth").append(s"$value%")
}
case class MaxWidthPercentTagStyle(value: Int) extends TagStyle {
  def appendStyle(builder: JsonBuilder) =
    builder.append("maxWidth").append(s"$value%")
}

case object WidthAllTagStyle extends TagStyle {
  def appendStyle(builder: JsonBuilder) =
    builder.append("width").append(s"100%")
}
case class WidthAllButTagStyle(value: Int) extends TagStyle {
  def appendStyle(builder: JsonBuilder) =
    builder.append("width").append(s"calc(100% - ${value}px)")
}
case object HeightAllTagStyle extends TagStyle {
  def appendStyle(builder: JsonBuilder) =
    builder.append("height").append(s"100%")
}




abstract class TextAlignTagStyle(textAlign: String) extends TagStyle {
  def appendStyle(builder: JsonBuilder) =
    builder.append("textAlign").append(textAlign)
}
case object LeftAlignTagStyle extends TextAlignTagStyle("left")
case object CenterAlignTagStyle extends TextAlignTagStyle("center")
case object RightAlignTagStyle extends TextAlignTagStyle("right")
abstract class VerticalAlignTagStyle(verticalAlign: String) extends TagStyle {
  def appendStyle(builder: JsonBuilder) =
    builder.append("verticalAlign").append(verticalAlign)
}
case object BottomAlignTagStyle extends VerticalAlignTagStyle("bottom")
case object MiddleAlignTagStyle extends VerticalAlignTagStyle("middle")
case object TopAlignTagStyle extends VerticalAlignTagStyle("top")

case object RelativeTagStyle extends TagStyle {
  def appendStyle(builder: JsonBuilder) =
    builder.append("position").append("relative")
}

case class ColorTagStyle(color: Color) extends TagStyle {
  def appendStyle(builder: JsonBuilder) =
    builder.append("color").append(color.value)
}

case object NoWrapTagStyle extends TagStyle {
  def appendStyle(builder: JsonBuilder) =
    builder.append("whiteSpace").append("nowrap")
}

case object FlexWrapTagStyle extends TagStyle {
  def appendStyle(builder: JsonBuilder) =
    builder.append("flexWrap").append("wrap")
}

class TagStylesImpl extends TagStyles {
  def none = NoTagStyle
  def margin = MarginTagStyle
  def displayInline = InlineTagStyle
  def displayInlineBlock = InlineBlockTagStyle
  def displayBlock = BlockTagStyle
  def floatRight = RightFloatTagStyle
  def alignLeft = LeftAlignTagStyle
  def alignRight = RightAlignTagStyle
  def paddingSide = PaddingTagStyle
  def displayCell = CellTagStyle
  def widthAll = WidthAllTagStyle
  def widthAllBut = WidthAllButTagStyle
  def maxWidthPercent = MaxWidthPercentTagStyle
  def minWidthPercent = MinWidthPercentTagStyle
  def marginSide = MarginSideTagStyle
  def alignCenter = CenterAlignTagStyle
  def minWidth = MinWidthTagStyle
  def alignMiddle = MiddleAlignTagStyle
  def displayTable = TableTagStyle
  def displayFlex = FlexTagStyle
  def padding = PaddingTagStyle
  def maxWidth = MaxWidthTagStyle
  def width = WidthTagStyle
  def height = HeightTagStyle
  def heightAll = HeightAllTagStyle
  def minHeight = MinHeightTagStyle
  def alignBottom = BottomAlignTagStyle
  def alignTop = TopAlignTagStyle
  def isTextAlign(style: TagStyle) = style.isInstanceOf[TextAlignTagStyle]
  def isVerticalAlign(style: TagStyle) = style.isInstanceOf[VerticalAlignTagStyle]
  def relative = RelativeTagStyle
  def color = ColorTagStyle
  def noWrap = NoWrapTagStyle
  def flexWrap = FlexWrapTagStyle
}