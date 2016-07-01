package ee.cone.base.test_loots

import ee.cone.base.vdom._
import ee.cone.base.vdom.Types.VDomKey
/**
  * Created by wregs on 4/1/2016.
  */

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

  def appendJson(builder:JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("FlexGridItemWidthSync")
    builder.append("onResize").append("send")
    builder.append("flexReg").append("def")
    builder.end()
  }
}

case class DataTableColGroupRow() extends VDomValue{
  def elementType="div"
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("div")
    builder.append("style"); {
      builder.startObject()
      builder.append("display").append("flex")
      //  builder.append("fontFamily").append("Roboto,sans serif")
      builder.append("fontSize").append("13px")
      builder.append("fontWeight").append("500")
      builder.append("color").append("rgba(0,0,0,0.54)")
      builder.end()
    }
    builder.end()
  }

}
case class DataTableRecordRow(selected:Boolean)(val onClick:Option[()=>Unit]) extends VDomValue with OnClickReceiver{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("DataTableRow")
      builder.append("selected").append(selected)
      onClick.foreach(_⇒ builder.append("onClick").append("sendThen"))
    builder.end()
  }
}
case class DataTableHeaderRow() extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("div")
    builder.append("style"); {
      builder.startObject()
      builder.append("display").append("flex")
      //builder.append("fontFamily").append("Roboto,sans serif")
      builder.append("fontSize").append("12px")
      builder.append("fontWeight").append("500")
      builder.append("color").append("rgba(0,0,0,0.54)")
      builder.end()
    }
    builder.end()
  }
}

trait OfFlexGrid extends OfDiv
trait OfFlexDataTable extends OfDiv

case class FlexBasisTagStyle(value: Int) extends TagStyle {
  def appendStyle(builder: JsonBuilder) =
    builder.append("flex").append(s"1 1 ${value}px")
}

class FlexTags(child: ChildPairFactory,val tags:Tags,val materialTags: MaterialTags, divTags: DivTags, style: TagStyles) {
  import divTags._

  def flexBasis = FlexBasisTagStyle
  def maxWidth(value: Option[Int]) = value.map(style.maxWidth).getOrElse(style.none)

  def divAlignWrapper(key:VDomKey,align:String,verticalAlign:String,children:List[ChildPair[OfDiv]])=
    div(key,style.displayTable,style.widthAll,style.heightAll)(List(
      div("1",style.displayCell,style.widthAll,style.heightAll,align,verticalAlign)(children)::Nil
    ))

  def flexGrid(key: VDomKey, children: List[ChildPair[OfDiv]]) =
    child[OfDiv](key,FlexGrid(),children)
  def flexGridItem(key: VDomKey, flexBasisWidth: Int, maxWidth: Option[Int], children: List[ChildPair[OfDiv]]) =
    child[OfDiv](key,FlexGridShItem(flexBasisWidth, maxWidth),
      child(key,FlexGridItem(),children) :: Nil
    )

  def dataTableColumnRow(key:VDomKey,children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,DataTableColGroupRow(),children)
  def dataTableRecordRow(key:VDomKey,selected:Boolean,handle:Option[()=>Unit], children:List[ChildPair[OfDiv]])={
    child[OfDiv](key,DataTableRecordRow(selected)(handle),children)}
  def dataTableHeaderRow(key:VDomKey,children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,DataTableHeaderRow(),children)
  def flexGridItemWidthSync(key:VDomKey,/*flexBasisWidth:Int,maxWidth:Option[Int],*/OnResize:String=>Unit,children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,FlexGridShItem(1000/*temp*/,None),
      child[OfDiv](key,FlexGridItemWidthSync()(Some(OnResize)),children)::Nil
    )::Nil
}

