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
      builder.append("style").startObject()
        builder.append("display").append("flex")
      //  builder.append("fontFamily").append("Roboto,sans serif")
        builder.append("fontSize").append("13")
        builder.append("fontWeight").append("500")
        builder.append("color").append("rgba(0,0,0,0.54)")
      builder.end()
    builder.end()
  }

}
case class DataTableRecordRow(selected:Boolean)(val onClick:Option[()=>Unit]) extends VDomValue with OnClickReceiver{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("DataTableRow")
      builder.append("selected").append(selected)
      builder.append("onClick").append("sendThen")
    builder.end()
  }
}
case class DataTableHeaderRow() extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("div")
      builder.append("style").startObject()
        builder.append("display").append("flex")
        //builder.append("fontFamily").append("Roboto,sans serif")
        builder.append("fontSize").append("12")
        builder.append("fontWeight").append("500")
        builder.append("color").append("rgba(0,0,0,0.54)")
      builder.end()
    builder.end()
  }
}
case class DivFlexWrapper(flexBasis:Option[Int],
                          displayFlex:Boolean,
                          flexWrap:Boolean,
                          minWidth:Option[Int],
                          maxWidth:Option[Int],
                          height:Option[Int],
                          textAlign:Option[String],
                          borderRight:Boolean
                         ) extends VDomValue{

  def appendJson(builder: JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("div")
      builder.append("style").startObject()
        flexBasis.foreach(x=>builder.append("flex").append(s"1 1 ${x}px"))
        if(displayFlex) builder.append("display").append("flex")
        if(flexWrap) builder.append("flexWrap").append("wrap")
        minWidth.foreach(x=>builder.append("minWidth").append(s"${x}px"))
        maxWidth.foreach(x=>builder.append("maxWidth").append(s"${x}px"))
        height.foreach(x=>builder.append("height").append(s"${x}px"))
        textAlign.foreach(x=>builder.append("textAlign").append(x))
        if(borderRight) builder.append("borderRight").append("1px solid red")
      builder.end()
    builder.end()
  }
}
case class DivWrapper(display:Option[String],
                      minWidth:Option[String],
                      maxWidth:Option[String],
                      height:Option[String],
                      float:Option[String],
                      position:Option[String]) extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("div")
      builder.append("style").startObject()
        display.foreach(x=>builder.append("display").append(x))
        minWidth.foreach(x=>builder.append("minWidth").append(x))
        maxWidth.foreach(x=>builder.append("maxWidth").append(x))
        height.foreach(x=>builder.append("height").append(x))
        float.foreach(x=>builder.append("float").append(x))
        position.foreach(x=>builder.append("position").append(x))
      builder.end()
    builder.end()
  }
}

case class DivPositionWrapper(display:Option[String],
                              position:Option[String],
                              top:Option[String],
                              transform:Option[String]) extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("div")
      builder.append("style").startObject()
        display.foreach(x=>builder.append("display").append(x))
        position.foreach(x=>builder.append("position").append(x))
        top.foreach(x=>builder.append("top").append(x))
        transform.foreach(x=>builder.append("transform").append(x))
      builder.end()
    builder.end()
  }
}

case class DivTableCell(width:String,align:String,verticalAlign:String) extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("div")
      builder.append("style").startObject()
        builder.append("display").append("table-cell")
        builder.append("width").append(width)
        builder.append("height").append("100%")
        builder.append("textAlign").append(align)
        builder.append("verticalAlign").append(verticalAlign)
      builder.end()
    builder.end()
  }
}

case class DivAlignWrapper() extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("div")
      builder.append("style").startObject()
        builder.append("display").append("table")
        builder.append("height").append("100%")
        builder.append("width").append("100%")
      builder.end()
    builder.end()
  }
}

trait OfFlexGrid extends OfDiv
trait OfFlexDataTable extends OfDiv

class FlexTags(child: ChildPairFactory,val tags:Tags,val materialTags: MaterialTags) {

  def divAlignWrapper(key:VDomKey,align:String,verticalAlign:String,children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,DivAlignWrapper(),
      child[OfDiv](key,DivTableCell("100%",align,verticalAlign),children)::Nil
    )
  def divFlexWrapper(key:VDomKey,
                     flexBasis:Option[Int],
                     displayFlex:Boolean,
                     flexWrap:Boolean,
                     minWidth:Option[Int],
                     maxWidth:Option[Int],
                     height:Option[Int],
                     textAlign:Option[String],
                     borderRight:Boolean,
                     children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,DivFlexWrapper(flexBasis,displayFlex,flexWrap,minWidth,maxWidth,height,textAlign,borderRight),children)
  def divWrapper(key:VDomKey,
                 display:Option[String],
                 minWidth:Option[String],
                 maxWidth:Option[String],
                 height:Option[String],
                 float:Option[String],
                 position:Option[String],
                 children: List[ChildPair[OfDiv]])=
    child[OfDiv](key,DivWrapper(display,minWidth,maxWidth,height,float,position),children)
  def divPositionWrapper(key:VDomKey,
                         display:Option[String],
                         position:Option[String],
                         top:Option[String],
                         transform:Option[String],
                         children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,DivPositionWrapper(display,position,top,transform),children)
  def flexGrid(key: VDomKey, children: List[ChildPair[OfDiv]]) =
    child[OfDiv](key,FlexGrid(),children)
  def flexGridItem(key: VDomKey, flexBasisWidth: Int, maxWidth: Option[Int], children: List[ChildPair[OfDiv]]) =
    child[OfDiv](key,FlexGridShItem(flexBasisWidth, maxWidth),
      child(key,FlexGridItem(),children) :: Nil
    )

  def dataTableColumnRow(key:VDomKey,children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,DataTableColGroupRow(),children)
  def dataTableRecordRow(key:VDomKey,selected:Boolean,handle:Option[()=>Unit], children:List[ChildPair[OfDiv]])={
    //val selected=dtTablesState.dtTableToggleRecordRow.getOrElse(id,false)
    child[OfDiv](key,DataTableRecordRow(selected)(handle),children)}
  def dataTableHeaderRow(key:VDomKey,children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,DataTableHeaderRow(),children)
  def flexGridItemWidthSync(key:VDomKey,flexBasisWidth:Int,maxWidth:Option[Int],OnResize:Option[(String)=>Unit],children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,FlexGridShItem(flexBasisWidth,maxWidth),
      child[OfDiv](key,FlexGridItemWidthSync()(OnResize),children)::Nil
    )::Nil
}

