package ee.cone.base.test_loots

import ee.cone.base.util.Single
import ee.cone.base.vdom.Types.VDomKey
import ee.cone.base.vdom.{OfDiv, ChildPair}

import scala.collection.mutable


/**
  * Created by wregs on 5/3/2016.
  */

trait ChildOfTable
trait ChildOfTableRow

trait TableTags {
  type CellContentVariant = Boolean
  def table(key: VDomKey, attr: List[TagAttr])(children:List[ChildOfTable]):List[ChildPair[OfDiv]]
  def row(key: VDomKey, attr: List[TagAttr])(children:List[ChildOfTableRow]):ChildOfTable
  def group(key:VDomKey, attr: TagAttr*):ChildOfTableRow
  def cell(key:VDomKey, attr: TagAttr*)(children:CellContentVariant=>List[ChildPair[OfDiv]]):ChildOfTableRow
}

case class MaxWidth(value:Int) extends TagAttr
case class MinWidth(value:Int) extends TagAttr

case class Width(value:Float) extends TagAttr
case class Priority(value:Int) extends TagAttr
case class Caption(value:String) extends TagAttr //no
case class MaxVisibleLines(value:Int) extends TagAttr
case class Toggled(value:Boolean)(val toggleHandle:Option[()=>Unit]) extends TagAttr
case class IsSelected(value:Boolean) extends TagAttr
case object IsHeader extends TagAttr

class FlexDataTableTagsImpl(flexTags: FlexTags) extends TableTags{
  //type TableElement=FlexDataTableElement
  def table(key: VDomKey,attr: List[TagAttr])(children: List[ChildOfTable])={
    val tableWidth=Single.option[Float](attr.collect{case Width(x)=>x})
    FlexDataTable(tableWidth,flexTags,children.toIndexedSeq.asInstanceOf[List[FlexDataTableElement]]).genView()
  }
  def row(key: VDomKey, attr:List[TagAttr])(tableElement:List[ChildOfTableRow])={
    val maxVisibleLines=Single.option[Int](attr.collect({case MaxVisibleLines(x)=>x})).getOrElse(1)
    val toggled=Single.option[Toggled](attr.collect({case x:Toggled=>x})).getOrElse(Toggled(value = false)(None))
    val isSelected=Single.option[Boolean](attr.collect({case IsSelected(x)=>x})).getOrElse(false)
    val isHeader=Single.option[Boolean](attr.collect({case IsHeader=>true})).getOrElse(false)
    FlexDataTableRow(key,isHeader,maxVisibleLines,toggled.value,isSelected,flexTags,tableElement.toIndexedSeq.asInstanceOf[List[FlexDataTableElement]])(toggled.toggleHandle)
  }
  def group(key:VDomKey,attr: TagAttr*)=group(key, attr.toList)
  private def group(key:VDomKey,attr: List[TagAttr])={
    val basisWidth = Single[Int](attr.collect{case MinWidth(x)=>x})
    val maxWidth = Single.option[Int](attr.collect{case MaxWidth(x)=>x})
    val textAlign = Single.option[String](attr.collect{case x:TextAlign=>x}.map(x=> x match {
      case TextAlignRight=> "right"
      case TextAlignCenter=> "center"
      case _ => "left"
    }))
    val verticalAlign = Single.option[String](attr.collect{case x: VerticalAlign=>x}.map(x =>x match{
      case VerticalAlignBottom=>"bottom"
      case VerticalAlignMiddle=>"middle"
      case _=>"top"
    }))
    val priority = Single.option[Int](attr.collect{case Priority(x)=>x})
    val caption = Single.option[String](attr.collect{case Caption(x)=>x})
    FlexDataTableColGroup(key, basisWidth,maxWidth, textAlign,verticalAlign, priority.getOrElse(1), caption,flexTags)
  }
  def cell(key:VDomKey, attr: TagAttr*)(children:(CellContentVariant)=> List[ChildPair[OfDiv]])= cell(key,attr.toList)(children)
  private def cell(key:VDomKey, attr: List[TagAttr])(children:(CellContentVariant)=> List[ChildPair[OfDiv]])={
    val basisWidth = Single[Int](attr.collect{case MinWidth(x)=>x})
    val maxWidth = Single.option[Int](attr.collect{case MaxWidth(x)=>x})
    val textAlign = Single.option[String](attr.collect{case x:TextAlign=>x}.map(x=> x match {
      case TextAlignRight=> "right"
      case TextAlignCenter=> "center"
      case _ => "left"
    }))
    val verticalAlign = Single.option[String](attr.collect{case x: VerticalAlign=>x}.map(x =>x match{
      case VerticalAlignBottom=>"bottom"
      case VerticalAlignMiddle=>"middle"
      case _=>"top"
    }))
    val priority = Single.option[Int](attr.collect{case Priority(x)=>x})
    FlexDataTableCell(key,basisWidth,maxWidth,textAlign,verticalAlign,priority.getOrElse(1),flexTags,children)
  }
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
)(divTags: DivTags) extends FlexDataTableElement{
  _flexDataTableElements=flexDataTableElements
  inHeader=true
  import divTags._
  def genView() =
    List(div("tableHeader")(flexDataTableElements.flatMap(x=>x.genView()).toList))
}

case class FlexDataTableBody(
  flexDataTableElements: Seq[FlexDataTableElement]
)(divTags: DivTags) extends FlexDataTableElement{
  _flexDataTableElements=flexDataTableElements
  import divTags._
  def genView() =
    List(div("tableBody")(_flexDataTableElements.flatMap(x=>x.genView()).toList))
}

case class FlexDataTableRow(
  key:VDomKey, isHeader: Boolean, maxVisibleLines: Int, toggled:Boolean,
  selected:Boolean, flexDataTableElements: Seq[FlexDataTableElement]
)(
  divTags: DivTags, style: TagStyles, flexTags: FlexTags,
  toggleHandle:Option[()=>Unit]
) extends FlexDataTableElement with ChildOfTable{
  import divTags._
  _flexDataTableElements=flexDataTableElements
  inHeader=isHeader
  private def calcRow={

    val colGroupsBuffer = mutable.ListBuffer[FlexDataTableElement]()
    var i=0
    while(i<_flexDataTableElements.length) { //todo: test performance
      _flexDataTableElements(i) match {
        case _: FlexDataTableColGroup =>
          colGroupsBuffer += _flexDataTableElements(i)
        case _: FlexDataTableCell =>
          if (colGroupsBuffer.isEmpty)
            colGroupsBuffer += FlexDataTableColGroup("wrapped", 1000, None, None,None, 1, None, flexTags, Nil)
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
      val visibleColGroupsAltView=visibleColGroups.flatMap(x => x.genAltView())
      if(visibleColGroupsAltView.nonEmpty)
        flexTags.dataTableColumnRow(key + "_group", visibleColGroupsAltView) ::
        flexTags.materialTags.divider(key + "_div") ::
        flexTags.dataTableHeaderRow(key, visibleColGroups.flatMap(x => x.genView())) :: Nil
      else
        flexTags.dataTableHeaderRow(key, visibleColGroups.flatMap(x => x.genView())) :: Nil
    }
    else
      flexTags.materialTags.divider(key+"_div")::
        flexTags.dataTableRecordRow(key,selected,toggleHandle,
          div(key,style.displayFlex)(visibleColGroups.flatMap(x=>x.genView()))
            ::hiddenColGroupsCells
        )::Nil
  }
  def genView()={
    calcRow

  }
}

case class FlexDataTableColGroup(
  key:VDomKey, override val basisWidth:Int, override val maxWidth:Option[Int],
  textAlign:Option[String], verticalAlign:Option[String],
  override val priority:Int, caption:Option[String],
  flexDataTableElements: Seq[FlexDataTableElement]=Nil
)(divTags: DivTags, flexTags: FlexTags, style: TagStyles) extends FlexDataTableElement with ChildOfTableRow{
  import divTags._
  _flexDataTableElements=flexDataTableElements
  var maxVisibleLines=1
  var showAllRecords=0
  private def wrap(textAlign:Option[String],verticalAlign:Option[String],children:List[ChildPair[OfDiv]])=
    if (textAlign.nonEmpty || verticalAlign.nonEmpty) {
      flexTags.divAlignWrapper("1", textAlign.getOrElse("left"), verticalAlign.getOrElse("top"),
        children
      )
    } :: Nil
    else
      children
  override def genAltView()= { //todo: possible error due to no divflexwrapper on caption=None
    caption match {
      case Some(x)=>
        div (key, flexTags.flexBasis(basisWidth), flexTags.maxWidth(maxWidth), style.height(defaultRowHeight))(
          caption match {
            case Some (c) => wrap (textAlign, verticalAlign, flexTags.tags.text (key, c) :: Nil)
            case _ => Nil
          }
        ) :: Nil
      case _=> Nil
    }
  }
  def genView()={
    val cells=_flexDataTableElements.filter(_.isInstanceOf[FlexDataTableCell])
    val sortedCells=cells.sortWith((a,b)=>a.priority<b.priority)
    val wrappedLinesToShow=FlexDataTableLayout.getWrapPositions(elementWidth,sortedCells)

    if(inHeader)
      div(key, flexTags.flexBasis(basisWidth), flexTags.maxWidth(maxWidth))(
        {
          val headersToShow=wrappedLinesToShow.take(maxVisibleLines)
          headersToShow.flatMap{case (i,rowHeaders)=>
            val visibleRowCells=cells.intersect(sortedCells.slice(rowHeaders.head,rowHeaders.last+1))
            List(div(i.toString, style.displayFlex)(
              visibleRowCells.flatMap(x => x.genView()).toList))
          }.toList
        }
      )::Nil
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
      div(key, flexTags.flexBasis(basisWidth), flexTags.maxWidth(maxWidth))(
        {
          recordsToShow.flatMap{case (i,rowHeaders)=>
            val visibleRowRecords=cells.intersect(sortedCells.slice(rowHeaders.head,rowHeaders.last+1))
            List(div(i.toString, style.displayFlex)(
              visibleRowRecords.flatMap(x => x.genView()).toList))
          }.toList:::hiddenRowRecords
        }
      )::Nil
    }
  }
  override def setAttr(x:Map[String,Int])={
    maxVisibleLines=x.getOrElse("maxVisibleLines",1)
    showAllRecords=x.getOrElse("showAllRecords",0)
  }
}

case class FlexDataTableCell(
  key:VDomKey, override val basisWidth:Int, override val maxWidth:Option[Int],
  textAlign:Option[String], verticalAlign:Option[String],
  override val priority:Int,
  children: (Boolean)=>List[ChildPair[OfDiv]]
)(
  divTags: DivTags, flexTags: FlexTags, style: TagStyles
) extends FlexDataTableElement with ChildOfTableRow{
  import divTags._
  var showLabel:Boolean=false
  private def wrap(textAlign:Option[String],verticalAlign:Option[String],children:List[ChildPair[OfDiv]])=
    if (textAlign.nonEmpty || verticalAlign.nonEmpty) {
      flexTags.divAlignWrapper("1", textAlign.getOrElse("left"), verticalAlign.getOrElse("top"),
        children
      )
    } :: Nil
    else
      children

  def genView()= {
    div(key,
      flexTags.flexBasis(basisWidth), flexTags.maxWidth(maxWidth),
      if(showLabel) style.none else style.height(defaultRowHeight)
    )(
      wrap(textAlign,verticalAlign,children(showLabel))
    ) :: Nil
  }
  override def setAttr(x:Map[String,Int])={
    showLabel=x.getOrElse("showLabel",0)>0
  }
}

case class FlexDataTable(
  tableWidth: Option[Float], flexTags: FlexTags,flexDataTableElements: Seq[FlexDataTableElement]
)(divTags: DivTags) extends FlexDataTableElement{
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