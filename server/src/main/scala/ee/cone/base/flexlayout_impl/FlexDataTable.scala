
package ee.cone.base.flexlayout_impl

import ee.cone.base.util.Single
import ee.cone.base.vdom.Types.VDomKey
import ee.cone.base.vdom._

import scala.collection.mutable

class FlexDataTableTagsImpl(
  child: ChildPairFactory,
  divTags: Tags, style: TagStyles
) extends TableTags {
  def table(key: VDomKey, attrSeq: TagAttr*)(children: List[ChildOfTable])={
    val attr = attrSeq.toList
    val tableWidth=Single.option[Float](attr.collect{case Width(x)=>x})
    FlexDataTable(tableWidth,children.toIndexedSeq.asInstanceOf[List[FlexDataTableElement]])(divTags).genView()
  }
  def row(key: VDomKey, attrSeq: TagAttr*)(tableElement:List[ChildOfTableRow])={
    val attr = attrSeq.toList
    val maxVisibleLines=Single.option[Int](attr.collect({case MaxVisibleLines(x)=>x})).getOrElse(1)
    val toggled=Single.option[Toggled](attr.collect({case x:Toggled=>x})).getOrElse(Toggled(value = false)(None))
    val isSelected=Single.option[Boolean](attr.collect({case IsSelected(x)=>x})).getOrElse(false)
    val isHeader=Single.option[Boolean](attr.collect({case IsHeader=>true})).getOrElse(false)
    val divider=Single(attr.collect({case Divider(v)=>v}))
    FlexDataTableRow(key,isHeader,maxVisibleLines,toggled.value,isSelected,tableElement.toIndexedSeq.asInstanceOf[List[FlexDataTableElement]])(child, divTags, style, this, divider, toggled.toggleHandle)
  }
  def group(key:VDomKey,attrSeq: TagAttr*)(children:List[ChildPair[OfDiv]]) = {
    val attr = attrSeq.toList
    val basisWidth = Single[Int](attr.collect{case MinWidth(x)=>x})
    val maxWidth = Single.option[Int](attr.collect{case MaxWidth(x)=>x})
    val priority = Single.option[Int](attr.collect{case Priority(x)=>x})
    FlexDataTableColGroup(key, basisWidth, maxWidth, priority.getOrElse(1), children)(divTags, style, this)
  }
  def cell(key:VDomKey, attrSeq: TagAttr*)(children:(CellContentVariant)=> List[ChildPair[OfDiv]]) = {
    val attr = attrSeq.toList
    val basisWidth = Single[Int](attr.collect{case MinWidth(x)=>x})
    val maxWidth = Single.option[Int](attr.collect{case MaxWidth(x)=>x})
    val priority = Single.option[Int](attr.collect{case Priority(x)=>x})
    FlexDataTableCell(key,basisWidth,maxWidth,priority.getOrElse(1),children)(divTags, style, this)
  }
  def defaultRowHeight = 48
  def divSized(key:VDomKey, flexBasis: Int, maxWidthOpt: Option[Int], limitHeight: Boolean)(children: List[ChildPair[OfDiv]]) = {
    val width = maxWidthOpt.map(w⇒List(style.maxWidth(w), style.marginLeftAuto, style.marginRightAuto)).getOrElse(Nil)
    val height = if(limitHeight) style.height(defaultRowHeight) else style.none
    divTags.div(key, FlexBasisTagStyle(flexBasis) :: height :: width :_*)(children)
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

case class FlexBasisTagStyle(value: Int) extends TagStyle {
  def appendStyle(builder: JsonBuilder) =
    builder.append("flex").append(s"1 1 ${value}px")
}

trait FlexDataTableElement{
  var defaultRowHeight=0
  def setAttr(x:Map[String,Int]):Unit={}
  def genView():List[ChildPair[OfDiv]]
  def genAltView():List[ChildPair[OfDiv]]=Nil
  var inHeader=false
  val basisWidth:Int=0
  var parentElementWidth=0.0f
  var elementWidth:Float=0.0f
  var _flexDataTableElements:Seq[FlexDataTableElement]=Nil
  val priority:Int=1
  val maxWidth:Option[Int]=None
  def propagateDefaultParams():Unit={
    _flexDataTableElements.foreach(x=>{
      if(inHeader) x.inHeader=inHeader
      x.defaultRowHeight = defaultRowHeight
      x.parentElementWidth=elementWidth
      x.elementWidth=elementWidth
      x.propagateDefaultParams()
    })
  }
}

case class FlexDataTableHeader(
  flexDataTableElements: Seq[FlexDataTableElement]
)(divTags: Tags) extends FlexDataTableElement{
  _flexDataTableElements=flexDataTableElements
  inHeader=true
  import divTags._
  def genView() =
    List(div("tableHeader")(flexDataTableElements.flatMap(x=>x.genView()).toList))
}

case class FlexDataTableBody(
  flexDataTableElements: Seq[FlexDataTableElement]
)(divTags: Tags) extends FlexDataTableElement{
  _flexDataTableElements=flexDataTableElements
  import divTags._
  def genView() =
    List(div("tableBody")(_flexDataTableElements.flatMap(x=>x.genView()).toList))
}

case class FlexDataTableRow(
  key:VDomKey, isHeader: Boolean, maxVisibleLines: Int, toggled:Boolean,
  selected:Boolean, flexDataTableElements: Seq[FlexDataTableElement]
)(
  child: ChildPairFactory,
  divTags: Tags, style: TagStyles, dataTableTags: FlexDataTableTagsImpl,
  divider: VDomValue,
  toggleHandle:Option[()=>Unit]
) extends FlexDataTableElement with ChildOfTable{
  import divTags._
  _flexDataTableElements=flexDataTableElements
  inHeader=isHeader
  def genView() = {
    val colGroupsBuffer = mutable.ListBuffer[FlexDataTableElement]()
    var i=0
    while(i<_flexDataTableElements.length) { //todo: test performance
      _flexDataTableElements(i) match {
        case _: FlexDataTableColGroup =>
          colGroupsBuffer += _flexDataTableElements(i)
        case _: FlexDataTableCell =>
          if (colGroupsBuffer.isEmpty)
            colGroupsBuffer += FlexDataTableColGroup("wrapped", 1000, None, 1, Nil)(divTags,style,dataTableTags)
          val cellsForGroup = _flexDataTableElements.takeRight(_flexDataTableElements.length - i).
            takeWhile(_.isInstanceOf[FlexDataTableCell])
          colGroupsBuffer.last._flexDataTableElements = cellsForGroup
          if (cellsForGroup.nonEmpty)
            i += cellsForGroup.length - 1
        case _ =>
      }
      i+=1
    }

    val colGroups=colGroupsBuffer.toList

    val sortedColGroups=colGroups.sortWith((a,b)=>a.priority<b.priority)
    val columnsNtoShow=FlexDataTableLayout.getWrapPositions(parentElementWidth,sortedColGroups).take(1).flatMap{_._2}.size
    val visibleColGroups=colGroups.intersect(sortedColGroups.take(columnsNtoShow))
    val hiddenColGroups=colGroups.intersect(sortedColGroups.takeRight(sortedColGroups.length-columnsNtoShow))
    var parentWidthSpaceAvailable=parentElementWidth
    var colGroupsBasisSum=visibleColGroups.map(w=>w.basisWidth).sum
    var offsetNColumns=0
    var widthPerColGroup=(parentWidthSpaceAvailable - colGroupsBasisSum) / columnsNtoShow
    visibleColGroups.foreach(x=>{
      var cColWidth= x.basisWidth+widthPerColGroup
      x.setAttr(Map("maxVisibleLines"->maxVisibleLines,"showAllRecords"->{if(toggled) 1 else 0}))
      if(x.maxWidth.nonEmpty)
        if(cColWidth>x.maxWidth.get) {
          cColWidth=x.maxWidth.get
          colGroupsBasisSum-=x.basisWidth
          parentWidthSpaceAvailable-=cColWidth
          offsetNColumns+=1
          widthPerColGroup=(parentWidthSpaceAvailable - colGroupsBasisSum)/(columnsNtoShow-offsetNColumns)
        }
        x.elementWidth=if(cColWidth>=0) cColWidth else 0
    })

    val hiddenColGroupsCells=
      if(toggled)
      {
        var innerKeys = 1
        hiddenColGroups.flatMap(c => {
        innerKeys = innerKeys + 4
        div((innerKeys - 3).toString, style.displayFlex)(
          c.genAltView()) ::
          div((innerKeys - 1).toString, style.displayFlex, style.flexWrap)(
            c._flexDataTableElements.flatMap(x=>{x.setAttr(Map("showLabel"->1));x.genView()}).toList
            ) ::
          Nil
        })
      }
      else Nil

    if(inHeader) {
      val children = visibleColGroups.flatMap(x => x.genView())
      val headerRow = div(key, style.displayFlex, style.fontSize(12))(children)
      val visibleColGroupsAltView=visibleColGroups.flatMap(x => x.genAltView())
      if(visibleColGroupsAltView.isEmpty) List(headerRow) else List(
        div(key + "_group", style.displayFlex, style.fontSize(13))(visibleColGroupsAltView),
        child(key + "_div", divider, Nil),
        headerRow
      )
    }
    else List(
      child[OfDiv](key + "_div", divider, Nil),
      child[OfDiv](key,DataTableRecordRow(selected)(toggleHandle),
        div(key,style.displayFlex)(visibleColGroups.flatMap(x=>x.genView())) ::
        hiddenColGroupsCells
      )
    )
  }
}

case class FlexDataTableColGroup(
  key:VDomKey, override val basisWidth:Int, override val maxWidth:Option[Int],
  override val priority:Int, children: List[ChildPair[OfDiv]]
)(
  divTags: Tags, style: TagStyles, dataTableTags: FlexDataTableTagsImpl
) extends FlexDataTableElement with ChildOfTableRow{
  import divTags._
  import dataTableTags._
  _flexDataTableElements=Nil
  var maxVisibleLines=1
  var showAllRecords=0
  override def genAltView() =  //todo: possible error due to no divflexwrapper on caption=None
    if(children.isEmpty) Nil
    else List(divSized(key, basisWidth, maxWidth, limitHeight = true)(children))
  def genView()={
    val cells=_flexDataTableElements.filter(_.isInstanceOf[FlexDataTableCell])
    val sortedCells=cells.sortWith((a,b)=>a.priority<b.priority)
    val wrappedLinesToShow=FlexDataTableLayout.getWrapPositions(elementWidth,sortedCells)
    if(inHeader)
      List(divSized(key, basisWidth, maxWidth, limitHeight = false){
          val headersToShow=wrappedLinesToShow.take(maxVisibleLines)
          headersToShow.flatMap{case (i,rowHeaders)=>
            val visibleRowCells=cells.intersect(sortedCells.slice(rowHeaders.head,rowHeaders.last+1))
            List(div(i.toString, style.displayFlex)(
              visibleRowCells.flatMap(x => x.genView()).toList))
          }.toList
      })
    else{
      val recordsToShow=wrappedLinesToShow.take(maxVisibleLines)
      val hiddenRecords=if(showAllRecords>0) wrappedLinesToShow--recordsToShow.keySet else Nil
      val hiddenRowRecords=
        if(showAllRecords>0)
          List(div(key+"_hidden", style.displayFlex, style.flexWrap)({
            hiddenRecords.flatMap{case (i,rowHeaders)=>
              val hiddenRowRecords=cells.intersect(sortedCells.slice(rowHeaders.head,rowHeaders.last+1))
              hiddenRowRecords.flatMap(x =>{x.setAttr(Map("showLabel"->1));x.genView()})
            }}.toList
          ))
        else Nil
      List(divSized(key, basisWidth, maxWidth, limitHeight = false){
        recordsToShow.flatMap{case (i,rowHeaders)=>
          val visibleRowRecords=cells.intersect(sortedCells.slice(rowHeaders.head,rowHeaders.last+1))
          List(div(i.toString, style.displayFlex)(
            visibleRowRecords.flatMap(x => x.genView()).toList))
        }.toList:::hiddenRowRecords
      })
    }
  }
  override def setAttr(x:Map[String,Int])={
    maxVisibleLines=x.getOrElse("maxVisibleLines",1)
    showAllRecords=x.getOrElse("showAllRecords",0)
  }
}

case class FlexDataTableCell(
  key:VDomKey, override val basisWidth:Int, override val maxWidth:Option[Int],
  override val priority:Int, children: (Boolean)=>List[ChildPair[OfDiv]]
)(
  divTags: Tags, style: TagStyles, dataTableTags: FlexDataTableTagsImpl
) extends FlexDataTableElement with ChildOfTableRow{
  import dataTableTags._
  var showLabel:Boolean=false
  def genView() =
    List(divSized(key, basisWidth, maxWidth, !showLabel)(children(showLabel)))
  override def setAttr(x:Map[String,Int]) =
    showLabel = x.getOrElse("showLabel",0)>0
}

case class FlexDataTable(
  tableWidth: Option[Float], flexDataTableElements: Seq[FlexDataTableElement]
)(divTags: Tags) extends FlexDataTableElement{
  _flexDataTableElements=flexDataTableElements
  defaultRowHeight=48
  private def header= {
    val headerRow=_flexDataTableElements.filter(x => x.isInstanceOf[FlexDataTableRow] && x.inHeader)
    val flexDataTableHeaderRow=FlexDataTableHeader(headerRow)(divTags)
    flexDataTableHeaderRow.genView()
  }
  private def body= {
    val bodyRow = _flexDataTableElements.filter(x => x.isInstanceOf[FlexDataTableRow] && !x.inHeader)
    val flexDataTableBodyRow=FlexDataTableBody(bodyRow)(divTags)
    flexDataTableBodyRow.genView()
  }
  def genView()= {
    header:::body
  }
  elementWidth=tableWidth.getOrElse(0.0f)
  propagateDefaultParams()
}

object FlexDataTableLayout{
  def getWrapPositions(cWidth:Float,dtEl:Seq[FlexDataTableElement]):Map[Int,Seq[Int]]={ //todo: test performance
    var line=0
    var lineSum=0
    var i=0
    var itemsForLines:Seq[Int]=Nil
    var result:Map[Int,Seq[Int]]=Map()
    var toBeInserted=dtEl.indices.toList
    while(i<dtEl.length) {
      if (lineSum + dtEl(i).basisWidth <= cWidth||itemsForLines.isEmpty) {
        lineSum += dtEl(i).basisWidth
        itemsForLines=itemsForLines++Seq(i)
        toBeInserted=toBeInserted.tail
      }
      else
      {
        if(itemsForLines.nonEmpty) {
          result = result + (line -> itemsForLines)
          itemsForLines = Nil
          itemsForLines=itemsForLines++Seq(i)
          toBeInserted=toBeInserted.tail
          line += 1
          lineSum = 0
        }
      }
      i+=1
    }
    result=result + (line->{if(itemsForLines.nonEmpty) itemsForLines else toBeInserted})
    result
  }
}