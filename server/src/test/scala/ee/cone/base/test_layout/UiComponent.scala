
package ee.cone.base.test_layout

import ee.cone.base.vdom.Types.VDomKey
import ee.cone.base.vdom._

trait OfDiv

/******************************************************************************/

//builder.append("style").startObject()
case class FlexGrid(maxWidth: Option[Int]) extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("FlexGrid")
    builder.append("flexReg").append("def")
    maxWidth.foreach(w => builder.append("maxWidth").append(s"${w}px"))
    builder.end()
  }
}
case class FlexGridShItem(flexBasisWidth: Int, maxWidth: Int) extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("FlexGridShItem")
    builder.append("flexReg").append("def")
    builder.append("flexBasis").append(s"${flexBasisWidth}px")
    builder.append("maxWidth").append(s"${maxWidth}px")
    builder.end()
  }
}
case class FlexGridItem() extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("FlexGridItem")
    builder.append("flexReg").append("def")
    builder.end()
  }
}
trait OfFlexGrid
class FlexTags(
  child: ChildPairFactory
) {
  /*def flexRoot(children: List[ChildPair[OfDiv]]) =
    child("root", FlexRoot(), children).value*/
  def flexGrid(key: VDomKey, children: List[ChildPair[OfFlexGrid]]) =
    child[OfDiv](key,FlexGrid(None),children)
  def flexItem(key: VDomKey, flexBasisWidth: Int, maxWidth: Int, children: List[ChildPair[OfDiv]]) =
    child[OfFlexGrid](key,FlexGridShItem(flexBasisWidth, maxWidth),
      child(key,FlexGridItem(),children) :: Nil
    )
}

/******************************************************************************/

case class WrappingElement() extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("span")
    builder.end()
  }
}
case class TextContentElement(content: String) extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("span")
    builder.append("content").append(content)
    builder.end()
  }
}
class Tags(
  child: ChildPairFactory
) {
  def root(children: List[ChildPair[OfDiv]]) =
    child("root", WrappingElement(), children).value
  def text(key: VDomKey, text: String) =
    child[OfDiv](key, TextContentElement(text), Nil)
}
// no childOfGrid => use grid item only
// no paper:1, child/parent Paper

/******************************************************************************/

case class Paper() extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("Paper")
    builder.end()
  }
}

class MaterialTags(
  child: ChildPairFactory
) {
  def paper(key: VDomKey, children: List[ChildPair[OfDiv]]) =
    child[OfDiv](key, Paper(), children)
}
