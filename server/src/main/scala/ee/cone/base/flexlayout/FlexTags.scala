
package ee.cone.base.flexlayout

import ee.cone.base.vdom.Types.VDomKey
import ee.cone.base.vdom._

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
case class FlexGridItemWidthSync()(val onResize:Option[(String)=>Unit])
  extends VDomValue with OnResizeReceiver
{
  def appendJson(builder:JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("FlexGridItemWidthSync")
    builder.append("onResize").append("send")
    builder.append("flexReg").append("def")
    builder.end()
  }
}

class FlexTagsImpl(child: ChildPairFactory) extends FlexTags {
  def flexGrid(key: VDomKey)(children: List[ChildPair[OfDiv]]) =
    child[OfDiv](key,FlexGrid(),children)
  def flexItem(key: VDomKey, flexBasisWidth: Int, maxWidth: Option[Int], onResize: Option[String=>Unit]=None)(children: List[ChildPair[OfDiv]]) =
    child[OfDiv](key,
      FlexGridShItem(flexBasisWidth, maxWidth),
      List(
        child(key, onResize.map(f⇒FlexGridItemWidthSync()(Some(f))).getOrElse(FlexGridItem()), children)
      )
    )
}
