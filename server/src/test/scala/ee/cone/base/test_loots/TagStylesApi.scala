package ee.cone.base.test_loots

import ee.cone.base.vdom.JsonBuilder

trait TagAttr
trait TagStyle extends TagAttr {
  def appendStyle(builder: JsonBuilder): Unit
}
trait Color {
  def value: String
}

trait TagStyles {
  def none: TagStyle
  def displayInline: TagStyle
  def displayInlineBlock: TagStyle
  def displayBlock: TagStyle
  def displayCell: TagStyle
  def displayTable: TagStyle
  def displayFlex: TagStyle
  def margin: Int⇒TagStyle
  def marginSide: Int⇒TagStyle
  def padding: Int⇒TagStyle
  def paddingSide: Int⇒TagStyle
  def maxWidth: Int⇒TagStyle
  def minWidth: Int⇒TagStyle
  def width: Int⇒TagStyle
  def widthAll: TagStyle
  def widthAllBut: Int⇒TagStyle
  def maxWidthPercent: Int⇒TagStyle
  def minWidthPercent: Int⇒TagStyle
  def minHeight: Int⇒TagStyle
  def height: Int⇒TagStyle
  def heightAll: TagStyle
  def alignLeft: TagStyle
  def alignCenter: TagStyle
  def alignRight: TagStyle
  def alignBottom: TagStyle
  def alignMiddle: TagStyle
  def alignTop: TagStyle
  def isTextAlign(style: TagStyle): Boolean
  def isVerticalAlign(style: TagStyle): Boolean
  def floatRight: TagStyle
  def relative: TagStyle
  def color: Color⇒TagStyle
  def noWrap: TagStyle
  def flexWrap: TagStyle
}
