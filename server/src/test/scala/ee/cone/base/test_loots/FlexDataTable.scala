package ee.cone.base.test_loots

import ee.cone.base.util.Single
import ee.cone.base.vdom.Types.VDomKey
import ee.cone.base.vdom.{OfDiv, ChildPair}

import scala.collection.mutable


/**
  * Created by wregs on 5/3/2016.
  */
trait AttrOfTableElement
trait ChildOfTable
trait ChildOfTableRow

trait TableControlPanel{
  type TableElement
  def controlPanel(key:VDomKey,children:ChildPair[OfDiv]*):TableElement with ChildOfTable
  def controlPanel(key:VDomKey,children:List[ChildPair[OfDiv]]):TableElement with ChildOfTable
}
trait SimpleHtmlTable{
  type TableElement
  def table(key: VDomKey, attr:AttrOfTableElement*)(tableElement:(TableElement with ChildOfTable)*):List[ChildPair[OfDiv]]
  def table(key: VDomKey, attr:List[AttrOfTableElement])(tableElement:List[TableElement with ChildOfTable]):List[ChildPair[OfDiv]]
  def row(key: VDomKey, attr: AttrOfTableElement*)(tableElement:(TableElement with ChildOfTableRow)*):TableElement with ChildOfTable
  def row(key: VDomKey, attr: List[AttrOfTableElement])(tableElement:List[TableElement with ChildOfTableRow]):TableElement with ChildOfTable
  def group(key:VDomKey, attr: AttrOfTableElement*):TableElement with ChildOfTableRow
  def group(key:VDomKey, attr: List[AttrOfTableElement]):TableElement with ChildOfTableRow
  def cell(key:VDomKey, attr:AttrOfTableElement*)(children: ChildPair[OfDiv]*):TableElement with ChildOfTableRow
  def cell(key:VDomKey, attr:List[AttrOfTableElement])(children: List[ChildPair[OfDiv]]):TableElement with ChildOfTableRow

}
trait HtmlTableWithControl extends SimpleHtmlTable with TableControlPanel

case class Width(value:Float) extends AttrOfTableElement
case class MaxWidth(value:Int) extends AttrOfTableElement
case class Priority(value:Int) extends AttrOfTableElement
case class TextAlign(value:String) extends AttrOfTableElement
case class VerticalAlign(value:String) extends AttrOfTableElement
case class MinWidth(value:Int) extends AttrOfTableElement
case class Caption(value:String) extends AttrOfTableElement
case class MaxVisibleLines(value:Int) extends AttrOfTableElement
case class Toggled(value:Boolean)(val toggleHandle:Option[()=>Unit]) extends AttrOfTableElement{}
case class Selected(value:Boolean) extends AttrOfTableElement
case class IsHeader(value:Boolean) extends AttrOfTableElement

class FlexDataTableImpl(flexTags: FlexTags) extends HtmlTableWithControl{
  type TableElement=FlexDataTableElement
  def table(key: VDomKey,attr: AttrOfTableElement*)(tableElement: (TableElement with ChildOfTable)*)={
    table(key,attr.toList)(tableElement.toList)
  }
  def table(key: VDomKey,attr: List[AttrOfTableElement])(tableElement: List[TableElement with ChildOfTable])={
    val tableWidth=Single.option[Float](attr.collect{case Width(x)=>x})
    FlexDataTable(tableWidth,flexTags,tableElement.toIndexedSeq).genView()
  }
  def row(key: VDomKey, attr:AttrOfTableElement*)(tableElement:(TableElement with ChildOfTableRow)*) =
    row(key,attr.toList)(tableElement.toList)
  def row(key: VDomKey, attr:List[AttrOfTableElement])(tableElement:List[TableElement with ChildOfTableRow])={
    val maxVisibleLines=Single.option[Int](attr.collect({case MaxVisibleLines(x)=>x})).getOrElse(1)
    val toggled=Single.option[Toggled](attr.collect({case x:Toggled=>x})).getOrElse(Toggled(value = false)(None))
    val selected=Single.option[Boolean](attr.collect({case Selected(x)=>x})).getOrElse(false)
    val isHeader=Single.option[Boolean](attr.collect({case IsHeader(x)=>x})).getOrElse(false)
    FlexDataTableRow(key,isHeader,maxVisibleLines,toggled.value,selected,flexTags,tableElement.toIndexedSeq)(toggled.toggleHandle)
  }
  def group(key:VDomKey,attr: AttrOfTableElement*)=group(key, attr.toList)
  def group(key:VDomKey,attr: List[AttrOfTableElement])={
    val basisWidth = Single[Int](attr.collect{case MinWidth(x)=>x})
    val maxWidth = Single.option[Int](attr.collect{case MaxWidth(x)=>x})
    val textAlign = Single.option[String](attr.collect{case TextAlign(x)=>x})
    val priority = Single.option[Int](attr.collect{case Priority(x)=>x})
    val caption = Single.option[String](attr.collect{case Caption(x)=>x})
    FlexDataTableColGroup(key, basisWidth,maxWidth, textAlign, priority.getOrElse(1), caption,flexTags)
  }
  def cell(key:VDomKey, attr: AttrOfTableElement*)(children: ChildPair[OfDiv]*)= cell(key,attr.toList)(children.toList)
  def cell(key:VDomKey, attr: List[AttrOfTableElement])(children: List[ChildPair[OfDiv]])={
    val basisWidth = Single[Int](attr.collect{case MinWidth(x)=>x})
    val maxWidth = Single.option[Int](attr.collect{case MaxWidth(x)=>x})
    val textAlign = Single.option[String](attr.collect{case TextAlign(x)=>x})
    val verticalAlign = Single.option[String](attr.collect{case VerticalAlign(x)=>x})
    val priority = Single.option[Int](attr.collect{case Priority(x)=>x})
    FlexDataTableCell(key,basisWidth,maxWidth,textAlign,verticalAlign,priority.getOrElse(1),flexTags,children)
  }
  def controlPanel(key:VDomKey,children:ChildPair[OfDiv]*)=controlPanel(key,children.toList)
  def controlPanel(key:VDomKey,children:List[ChildPair[OfDiv]])=
    FlexDataTableControlPanel(flexTags,children)
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

case class FlexDataTableHeader(flexTags: FlexTags,flexDataTableElements: Seq[FlexDataTableElement])
  extends FlexDataTableElement{
  _flexDataTableElements=flexDataTableElements
  inHeader=true
  def elType="header"
  def genView()={
    flexTags.divWrapper(elType,None,None,None,None,None,None,
      flexDataTableElements.flatMap(x=>x.genView()).toList
    )::Nil
  }

}

case class FlexDataTableBody(flexTags: FlexTags,flexDataTableElements: Seq[FlexDataTableElement])
  extends FlexDataTableElement{
  _flexDataTableElements=flexDataTableElements
  def elType="body"
  def genView()={
    flexTags.divWrapper(elType,None,None,None,None,None,None,
      _flexDataTableElements.flatMap(x=>x.genView()).toList
    )::Nil
  }
}

case class FlexDataTableRow(key:VDomKey,isHeader: Boolean,maxVisibleLines: Int,toggled:Boolean,selected:Boolean,flexTags: FlexTags,
                            flexDataTableElements: Seq[FlexDataTableElement])(handle:Option[()=>Unit])
  extends FlexDataTableElement with ChildOfTable{
  _flexDataTableElements=flexDataTableElements
  inHeader=isHeader
  def elType="row"

  private def calcRow={

    val colGroupsBuffer = mutable.ListBuffer[FlexDataTableElement]()
    var i=0
    while(i<_flexDataTableElements.length) { //todo: test performance
      _flexDataTableElements(i) match {
        case _: FlexDataTableColGroup =>
          colGroupsBuffer += _flexDataTableElements(i)
        case _: FlexDataTableCell =>
          if (colGroupsBuffer.isEmpty)
            colGroupsBuffer += FlexDataTableColGroup("wrapped", 1000, None, None, 1, None, flexTags, Nil)
          val cellsForGroup = _flexDataTableElements.takeRight(_flexDataTableElements.length - i).takeWhile(_.isInstanceOf[FlexDataTableCell])
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
        flexTags.divFlexWrapper((innerKeys - 3).toString, None, displayFlex = true, flexWrap = false, None, None, None, None, borderRight = false,
          c.genAltView()) ::
          flexTags.divFlexWrapper((innerKeys - 1).toString, None, displayFlex = true, flexWrap = true, None, None, None, None, borderRight = false,
            c._flexDataTableElements.flatMap(x=>x.genView()).toList
            ) ::
          Nil
        })
      }
      else Nil

    if(inHeader)
      flexTags.dataTableColumnRow(key+"_group", visibleColGroups.flatMap(x => x.genAltView()))::
        flexTags.materialTags.divider(key+"_div")::
        flexTags.dataTableHeaderRow(key,visibleColGroups.flatMap(x=>x.genView()))::Nil
    else
      flexTags.materialTags.divider(key+"_div")::
        flexTags.dataTableRecordRow(key,selected,handle,
          flexTags.divWrapper(key,Some("flex"),None,None,None,None,None,
            visibleColGroups.flatMap(x=>x.genView())
          )::hiddenColGroupsCells
        )::Nil


  }
  def genView()={
    calcRow

  }
}

case class FlexDataTableColGroup(key:VDomKey,override val basisWidth:Int,override val maxWidth:Option[Int],
                                 textAlign:Option[String],override val priority:Int,caption:Option[String],
                                 flexTags: FlexTags, flexDataTableElements: Seq[FlexDataTableElement]=Nil)
                                  extends FlexDataTableElement with ChildOfTableRow{
  _flexDataTableElements=flexDataTableElements
  var maxVisibleLines=1
  var showAllRecords=0
  def elType="group"
  override def genAltView()= {
    flexTags.divFlexWrapper(key, Some(basisWidth), displayFlex = false, flexWrap = false, None, maxWidth,
      Some(defaultRowHeight), textAlign, borderRight = false,
      caption match {
        case Some(c) => flexTags.tags.text(key, c) :: Nil
        case _ => Nil
      }
    )::Nil
  }
  def genView()={
    val cells=_flexDataTableElements.filter(_.isInstanceOf[FlexDataTableCell])
    val sortedCells=cells.sortWith((a,b)=>a.priority<b.priority)
    val wrappedLinesToShow=FlexDataTableLayout.getWrapPositions(elementWidth,sortedCells)

    if(inHeader)
      flexTags.divFlexWrapper(key, Some(basisWidth),displayFlex = false,flexWrap = false,None,maxWidth,None,None,borderRight = false,
        {
          val headersToShow=wrappedLinesToShow.take(maxVisibleLines)
          headersToShow.flatMap{case (i,rowHeaders)=>
            val visibleRowCells=cells.intersect(sortedCells.slice(rowHeaders.head,rowHeaders.last+1))
            List(flexTags.divFlexWrapper(i.toString,None,displayFlex = true,flexWrap = false,None,None,None,None,borderRight = false,
              visibleRowCells.flatMap(x => x.genView()).toList))
          }.toList
        }
      )::Nil
    else{
      val recordsToShow=wrappedLinesToShow.take(maxVisibleLines)
      val hiddenRecords=if(showAllRecords>0) wrappedLinesToShow--recordsToShow.keySet else Nil
      val hiddenRowRecords=
        if(showAllRecords>0)
          List(flexTags.divFlexWrapper(key+"_hidden",None,displayFlex = true,flexWrap = true,None,None,None,None,borderRight = false,{
            hiddenRecords.flatMap{case (i,rowHeaders)=>
              val hiddenRowRecords=cells.intersect(sortedCells.slice(rowHeaders.head,rowHeaders.last+1))
              hiddenRowRecords.flatMap(x =>x.genView())
            }}.toList
          ))
        else Nil
      flexTags.divFlexWrapper(key, Some(basisWidth),displayFlex = false,flexWrap = false,None,maxWidth,None,None,borderRight = false,
        {
          recordsToShow.flatMap{case (i,rowHeaders)=>
            val visibleRowRecords=cells.intersect(sortedCells.slice(rowHeaders.head,rowHeaders.last+1))
            List(flexTags.divFlexWrapper(i.toString,None,displayFlex = true,flexWrap = false,None,None,None,None,borderRight = false,
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

case class FlexDataTableCell(key:VDomKey, override val basisWidth:Int, override val maxWidth:Option[Int],
                             textAlign:Option[String], verticalAlign:Option[String],
                             override val priority:Int, flexTags: FlexTags,
                             children: List[ChildPair[OfDiv]]) extends FlexDataTableElement with ChildOfTableRow{
  def elType="cell"
  private def wrap(textAlign:Option[String],verticalAlign:Option[String],children:List[ChildPair[OfDiv]])=
    if (textAlign.nonEmpty || verticalAlign.nonEmpty) {
      flexTags.divAlignWrapper("1", textAlign.getOrElse("left"), verticalAlign.getOrElse("top"),
        children
      )
    } :: Nil
    else
      children

  def genView()= {
    flexTags.divFlexWrapper(key, Some(basisWidth), displayFlex = false, flexWrap = false, None, maxWidth,
      Some(defaultRowHeight), None, borderRight = false,wrap(textAlign,verticalAlign,children.toList)
    ) :: Nil
  }

}
case class FlexDataTableControlPanel(flexTags: FlexTags,children:List[ChildPair[OfDiv]]) extends FlexDataTableElement with ChildOfTable{

  _flexDataTableElements=Nil
  def elType="control"
  def genView()={

    flexTags.divWrapper(elType,None,None,None,None,None,None,
      List(
        flexTags.divWrapper("1",Some("inline-block"),Some("1px"),Some("1px"),Some(s"${defaultRowHeight}px"),None,None,List()),
        flexTags.divWrapper("2",None,None,None,None,Some("right"),None,children.toList))
    )::Nil
  }
}

case class FlexDataTable(tableWidth:Option[Float], flexTags: FlexTags,flexDataTableElements: Seq[FlexDataTableElement]) extends FlexDataTableElement{
  _flexDataTableElements=flexDataTableElements
  def elType="table"
  defaultRowHeight=48
  private def controlPanel={
    val cPanel=_flexDataTableElements.filter(_.isInstanceOf[FlexDataTableControlPanel])
    if(cPanel.nonEmpty){ cPanel.head.genView()}
    else Nil
  }
  private def header= {
    val headerRow=_flexDataTableElements.filter(x => x.isInstanceOf[FlexDataTableRow] && x.inHeader)
    val flexDataTableHeaderRow=FlexDataTableHeader(flexTags,headerRow)
    flexDataTableHeaderRow.genView()
  }
  private def body= {
    val bodyRow = _flexDataTableElements.filter(x => x.isInstanceOf[FlexDataTableRow] && !x.inHeader)
    val flexDataTableBodyRow=FlexDataTableBody(flexTags,bodyRow)
    flexDataTableBodyRow.genView()
  }
  def genView()= {
    controlPanel:::header:::body
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