
package ee.cone.base.test_layout

import ee.cone.base.vdom.Types.VDomKey
import ee.cone.base.vdom._

/******************************************************************************/

//builder.append("style").startObject()

case class FlexGrid() extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("FlexGrid")
    builder.append("flexReg").append("def")
    builder.end()
  }
}
case class FlexGridShItem(flexBasisWidth: Int, maxWidth: Option[Int]) extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("FlexGridShItem")
    builder.append("flexReg").append("def")
    builder.append("flexBasis").append(s"${flexBasisWidth}px")
    maxWidth.foreach(w=>builder.append("maxWidth").append(s"${w}px"))
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
    child[OfDiv](key,FlexGrid(),children)
  def flexItem(key: VDomKey, flexBasisWidth: Int, maxWidth: Option[Int], children: List[ChildPair[OfDiv]]) =
    child[OfFlexGrid](key,FlexGridShItem(flexBasisWidth, maxWidth),
      child(key,FlexGridItem(),children) :: Nil
    )
}

/******************************************************************************/


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
