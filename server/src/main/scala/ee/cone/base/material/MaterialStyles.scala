package ee.cone.base.material

import ee.cone.base.vdom.{JsonBuilder, TagStyle}

case object MarginSideTagStyle extends TagStyle {
  def appendStyle(builder: JsonBuilder) = {
    builder.append("marginLeft").append(s"10px")
    builder.append("marginRight").append(s"10px")
  }
}

case class PaddingSideTagStyle(value:Int) extends TagStyle { //?
  def appendStyle(builder: JsonBuilder) = {
    builder.append("paddingLeft").append(s"${value}px")
    builder.append("paddingRight").append(s"${value}px")
    builder.append("height").append("100%")
  }
}

class MaterialStylesImpl extends MaterialStyles {
  def paddingSide = PaddingSideTagStyle
  def marginSide = MarginSideTagStyle
}
