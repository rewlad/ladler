
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

trait Ref[T] {
  def value: T
  def value_=(value: T): Unit
}

class DataTablesState(currentVDom: CurrentVDom){
  private val widthOfTables = collection.mutable.Map[VDomKey,Float]()
  def widthOfTable(id: VDomKey) = new Ref[Float] {
    def value = widthOfTables.getOrElse(id,0.0f)
    def value_=(value: Float) = {
      widthOfTables(id) = value
      currentVDom.until(System.currentTimeMillis+200)
    }
  }
}

class FlexTagsImpl(child: ChildPairFactory, dtTablesState: DataTablesState) extends FlexTags {
  def flexGrid(key: VDomKey)(children: List[ChildPair[OfFlexGrid]]) =
    child[OfDiv](key,FlexGrid(),children)

  def flexItem(key: VDomKey, flexBasisWidth: Int, maxWidth: Option[Int], sync: Boolean=false)(
    children: List[TagAttr]⇒List[ChildPair[OfDiv]]
  ) = {
    val item = if(!sync) child(key, FlexGridItem(), children(Nil)) else {
      val tableWidth = dtTablesState.widthOfTable(key)
      child(key,
        FlexGridItemWidthSync()(Some(w⇒tableWidth.value=w.toFloat)),
        children(List(Width(tableWidth.value)))
      )
    }
    child[OfFlexGrid](key, FlexGridShItem(flexBasisWidth, maxWidth), List(item))
  }
}
