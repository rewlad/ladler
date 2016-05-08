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
case class DataTable(flexGrid:Boolean)(val onResize:Option[String=>Unit])
  extends VDomValue with OnResizeReceiver
{
  def appendJson(builder:JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("DataTable")
      builder.append("onResize").append("send")
      if(flexGrid)
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
case class DataTableCells(key:VDomKey)(val onClick:Option[()=>Unit]) extends VDomValue with OnClickReceiver{

  def appendJson(builder: JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("DataTableCells")
      builder.append("onClick").append("sendThen")
    builder.end()
  }
}
case class DataTableBody() extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("DataTableBody")
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

  import materialTags._
  import tags._

  class DtTable(cWidth:Float,controlAllCheckboxShow:Boolean,checkboxShow:Boolean,adjustForCheckbox:Boolean){
    var defaultRowHeight=48

    def controlDiv(key:VDomKey,id:VDomKey,dtState:DataTablesState):ChildPair[OfDiv]={
     /* if(controlAllCheckboxShow)
      divWrapper(key,None,None,None,None,None,None,
        List(
          divWrapper("1",Some("inline-block"),Some("50px"),Some("50px"),Some(s"${defaultRowHeight}px"),None,None,List(
            divPositionWrapper("1",Some("inline-block"),Some("relative"),Some("50%"),Some("translateY(-50%)"),
              List(checkBox("1",dtState.dtTableCheckAll.getOrElse(id,false),dtState.handleCheckAll(id,_)))))),
          divWrapper("2",None,None,None,None,Some("right"),None,dtControls))
        )*/
      //else if(adjustForCheckbox)
        divWrapper(key,None,None,None,None,None,None,
          List(
            divWrapper("1",Some("inline-block"),Some("1px"),Some("1px"),Some(s"${defaultRowHeight}px"),None,None,List()),
            divWrapper("2",None,None,None,None,Some("right"),None,dtControls))
        )
      //else  divEmpty(key)
      /*
        divWrapper(key,None,None,None,None,None,None,
          List(
            divWrapper("1",Some("inline-block"),Some("50px"),Some("50px"),Some("40px"),None,None,List(
              divPositionWrapper("1",Some("inline-block"),Some("relative"),Some("50%"),Some("translateY(-50%)"),
                List()))),
            divWrapper("2",None,None,None,None,Some("right"),None,dtControls))
        )*/
    }

    def dtCheckBox(key:VDomKey,id:VDomKey,checked:Boolean=false,check:(VDomKey,Boolean)=>Unit)={

        List(divFlexWrapper(key,Some(50),displayFlex = false,flexWrap = false,Some(50),Some(50),None,None,borderRight = false,List(
          divPositionWrapper("1",Some("inline-block"),Some("relative"),Some("2px"),None,
            List(checkBox("1",checked,check(id,_)))))))
    }
    trait DtElement{
      def basisWidth:Int
      def maxWidth:Option[Int]
      def header:Boolean
      def priority:Int
      def id:VDomKey
      var visible:Boolean
      def maxHeaderLines:Int
      def wrapAdjust:Int
      def getElementView(showRightBorder:Boolean=false):ChildPair[OfDiv]
    }
    case class DtColumn(key:VDomKey,basisWidth:Int,maxWidth:Option[Int],textAlign:String,priority:Int,wrapAdjust:Int,
                        maxHeaderLines:Int,columnText:Option[String])
      extends DtElement{
      def header=false
      //def maxWidth=None
      val id="column"
      var visible=true
      def getElementView(showRightBorder:Boolean)=
        divFlexWrapper(key, Some(basisWidth),displayFlex = false,flexWrap = false,None,maxWidth,
          None,Some(textAlign),borderRight = showRightBorder,
          columnText match{
            case Some(c)=>List(text(key, c))
            case _=> List()
          })
    }
    class DtHeader(key:VDomKey,_basisWidth:Int,_maxWidth:Option[Int],_priority:Int,children:List[ChildPair[OfDiv]])
      extends DtElement{
      def header=true
      def basisWidth=_basisWidth
      def maxWidth=_maxWidth
      def priority=_priority
      def wrapAdjust=0
      val id="header"
      def maxHeaderLines=1
      var visible=true
      def getElementView(showRightBorder:Boolean)={
        divFlexWrapper(key,Some(basisWidth),displayFlex = false,flexWrap = false,None,maxWidth,
          Some(defaultRowHeight),
          None,borderRight = false,children)
      }
    }
    class DtRecord(key:VDomKey,visibleChildren:List[ChildPair[OfDiv]],onShowChildren:List[ChildPair[OfDiv]])
      extends DtElement{
      var basisWidth=0
      var id=""
      var maxWidth:Option[Int]=None
      var priority=0
      var visible=true
      def wrapAdjust=0
      def maxHeaderLines=1
      def header=false
      def getElementView(showRightBorder:Boolean)={

        divFlexWrapper(key,Some(basisWidth),displayFlex = false,flexWrap = false,None,maxWidth,
          if(visible) Some(defaultRowHeight) else None,
          None,
          borderRight = false,if(visible)visibleChildren else onShowChildren)
      }
    }

    private var dtColumns:List[DtColumn]=Nil
    private var dtHeaders:List[Map[String,List[DtElement]]]=Nil
    private var dtRecords:List[Map[String,List[DtElement]]]=Nil
    private var dtFields:List[ChildPair[OfDiv]]=Nil
    private var dtControls:List[ChildPair[OfDiv]]=Nil
    def setControls(children:List[ChildPair[OfDiv]])=
      dtControls=children
    def dtField(children:List[ChildPair[OfDiv]])=
      dtFields=children
    def dtColumn(key:VDomKey,basisWidth:Int,maxWidth:Option[Int],textAlign:String,priority:Int,wrapAdjust:Int,maxHeaderLines:Int,
                 columnText:Option[String]) ={
      DtColumn(key,basisWidth,maxWidth,textAlign,priority,wrapAdjust,maxHeaderLines,columnText)
    }
    def dtColumn(key:VDomKey,basisWidth:Int,textAlign:String,wrapAdjust:Int,columnText:Option[String]) ={
      DtColumn(key,basisWidth,None,textAlign,0,wrapAdjust,1,columnText)
    }
    def dtHeader(key:VDomKey,basisWidth:Int,maxWidth:Option[Int],children:List[ChildPair[OfDiv]])={
      new DtHeader(key,basisWidth,maxWidth,0,children)
    }
    def dtHeader(key:VDomKey,basisWidth:Int,maxWidth:Option[Int],priority:Int,children:List[ChildPair[OfDiv]])={
      new DtHeader(key,basisWidth,maxWidth,priority,children)
    }
    def dtRecord(key:VDomKey,children:List[ChildPair[OfDiv]],onShowChildren:List[ChildPair[OfDiv]]=Nil)={
      new DtRecord(key,children,if(onShowChildren.isEmpty) children else onShowChildren)
    }
    def addColumns(columns:List[DtColumn]):Unit=
      dtColumns=columns
    def addHeadersForColumn(headers:Map[String,List[DtHeader]]):Unit= {
      dtHeaders = List(headers)
    }
    def addRecordsForColumn(id:VDomKey,records:Map[String,List[DtRecord]]):Unit= {
      dtHeaders.foreach(x=>{
        val dtHeadersKeys=x.keySet
        dtHeadersKeys.foreach(w=>{

          val colRecordsList=records.getOrElse(w,Nil)
          val colHeadersList=x.getOrElse(w,Nil)

          for(i<- colHeadersList.indices){
            colRecordsList(i).basisWidth=colHeadersList(i).basisWidth
            colRecordsList(i).maxWidth=colHeadersList(i).maxWidth
            colRecordsList(i).priority=colHeadersList(i).priority
            colRecordsList(i).id=id
          }
        })


      })
      dtRecords = dtRecords:::List(records)
    }

    private def getWrapPositions(cWidth:Float,dtEl:List[DtElement]):Map[Int,List[Int]]={
      var line=0
      var lineSum=0
      var i=0
      var itemsForLines:List[Int]=Nil
      var result:Map[Int,List[Int]]=Map()
      var toBeInserted=dtEl.indices.toList
      while(i<dtEl.length) {
        if (lineSum + dtEl(i).basisWidth <= cWidth) {
          lineSum += dtEl(i).basisWidth
          itemsForLines=itemsForLines:::i::Nil
          toBeInserted=toBeInserted diff List(i)
        }
        else
        {
          if(itemsForLines.nonEmpty) {
            result = result + (line -> itemsForLines)
            itemsForLines = Nil
            itemsForLines=itemsForLines:::i::Nil
            toBeInserted=toBeInserted diff List(i)
            line += 1
            lineSum = 0
          }
        }
        i+=1
      }
      result=result + (line->{if(itemsForLines.nonEmpty) itemsForLines else toBeInserted})
      result
    }
    private def calcVisibleColGroup(visibleColumns:List[DtColumn],columnsNtoShow:Int)={
      visibleColumns.zipWithIndex.map{case(x,i)=>
        val lastCol=i==columnsNtoShow-1
        x.getElementView(false)
      }
    }
    private def calcVisibleHeaders(firstKey:Long,visibleColumns:List[DtColumn],columnsNtoShow:Int)={
      var cKey=firstKey
      dtHeaders.flatMap(dtR=>{
        cKey=cKey+2

        val headersOfColumnsToShow={
          val headersWithoutCheckBox={

            visibleColumns.zipWithIndex.map{case (x,i)=> {
              val headers=dtR.getOrElse(x.key,Nil)

              val basisSum=dtColumns.take(columnsNtoShow).map(w=>w.basisWidth).sum
              val cColWidth=if(cWidth==0.0f) cWidth else x.basisWidth+(cWidth-basisSum)/columnsNtoShow

              val sortedHeaders=headers.sortWith((a,b)=>a.priority<b.priority)

              val wrappedLinesToShow=getWrapPositions(cColWidth,sortedHeaders)

              val lastCol=i==columnsNtoShow-1

              divFlexWrapper(x.key, Some(x.basisWidth),displayFlex = false,flexWrap = false,None,x.maxWidth,None,None,borderRight = false,
                {
                  val headersToShow=wrappedLinesToShow.take(x.maxHeaderLines)

                  headersToShow.flatMap{case (i,rowHeaders)=>{
                    val visibleRowHeaders=headers.intersect(sortedHeaders.slice(rowHeaders.head,rowHeaders.last+1))
                    List(divFlexWrapper(i.toString,None,displayFlex = true,flexWrap = false,None,None,None,None,borderRight = false,
                      visibleRowHeaders.map(r => r.getElementView())))
                  }}.toList
                }
              )
            }}}

          headersWithoutCheckBox
        }

        divider((cKey-2).toString)::dataTableHeaderRow((cKey-1).toString,headersOfColumnsToShow)::Nil

      })
    }
    private def calcRecords(firstKey:Long,visibleColumns:List[DtColumn],
                                   sortedColumns:List[DtColumn],dtState:DataTablesState,
                                   columnsNtoShow:Int)={
      var cKey=firstKey
      dtRecords.flatMap(dtR=>{
        cKey=cKey+2

        val recordsOfColumnsToShow={

          val recordsWithoutCheckBox=
          {
            visibleColumns.zipWithIndex.map{case (x,i)=> {
              val records=dtR.getOrElse(x.key,Nil)
              val toggled=dtState.isRowToggled(records.head.id)
              val basisSum=dtColumns.take(columnsNtoShow).map(w=>w.basisWidth).sum

              val cColWidth=if(cWidth==0.0f) cWidth else x.basisWidth+(cWidth-basisSum)/columnsNtoShow

             // println(i,cColWidth)
              val sortedRecords=records.sortWith((a,b)=>a.priority<b.priority)

              val wrappedLinesToShow=getWrapPositions(cColWidth,sortedRecords)
              val lastCol=i==columnsNtoShow-1
              divFlexWrapper(x.key, Some(x.basisWidth),displayFlex = false,flexWrap = false,None,x.maxWidth,None,None,borderRight = false,
                {
                  val recordsToShow=wrappedLinesToShow.take(x.maxHeaderLines)
                  val hiddenRecords=if(toggled) wrappedLinesToShow--recordsToShow.keySet else Nil
                  recordsToShow.flatMap{case (i,rowHeaders)=>{
                    val visibleRowRecords=records.intersect(sortedRecords.slice(rowHeaders.head,rowHeaders.last+1))
                    List(divFlexWrapper(i.toString,None,displayFlex = true,flexWrap = false,None,None,None,None,borderRight = false,
                      visibleRowRecords.map(r => r.getElementView())))
                  }}.toList:::
                  List(divFlexWrapper(i.toString+"_hidden",None,displayFlex = true,flexWrap = true,None,None,None,None,false,{
                    hiddenRecords.flatMap{case (i,rowHeaders)=>{
                      val hiddenRowRecords=records.intersect(sortedRecords.slice(rowHeaders.head,rowHeaders.last+1))
                      hiddenRowRecords.map(r =>{r.visible=false; r.getElementView()})
                    }}.toList
                  }))
                }
              )
            }}}

          recordsWithoutCheckBox:::hiddenColumnRecords("2",sortedColumns,dtR,dtState,columnsNtoShow)
        }
        divider((cKey-2).toString)::dataTableRecordRow((cKey-1).toString,false,Some(()=>dtState.handleToggle(dtR.head._2.head.id)),recordsOfColumnsToShow)::Nil

      })
    }
    private def hiddenColumnRecords(key:VDomKey,sortedColumns:List[DtColumn],
                                    currentRecords:Map[String,List[DtElement]],dtState:DataTablesState,
                                    columnsNtoShow:Int)= {
      val hiddenColumns=dtColumns.intersect(sortedColumns.takeRight(sortedColumns.length-columnsNtoShow))
      var innerKeys=1
      divWrapper(key+"hidden", None, None, None, None, None, None,
        hiddenColumns.flatMap(c => {
          val recordsForC = currentRecords.getOrElse(c.key, Nil)
          val toggled=dtState.isRowToggled(recordsForC.head.id)
          if(!toggled) Nil
          else
          {
            innerKeys = innerKeys + 4
            //divider((innerKeys-4).toString)::
            divFlexWrapper((innerKeys - 3).toString, None, displayFlex = true, flexWrap = false, None, None, None, None, borderRight = false,
              c.getElementView(false) :: Nil) ::
              //divider((innerKeys-2).toString)::
              divFlexWrapper((innerKeys - 1).toString, None, displayFlex = true, flexWrap = true, None, None, None, None, borderRight = false,
                recordsForC.map(r =>{r.visible=false; r.getElementView(false)})) ::
              Nil
          }
        })
      ) :: Nil
    }
    def getTableView(firstRecordKey:Long,id:VDomKey,dtState:DataTablesState):List[ChildPair[OfDiv]] ={

      val sortedColumns=dtColumns.sortWith((a,b)=>a.priority<b.priority)
      val wrappedLinesToShow=getWrapPositions(cWidth,sortedColumns)
      val columnsNtoShow=wrappedLinesToShow.take(1).flatMap{case(i,x)=>x}.size
      val visibleColumns=dtColumns.intersect(sortedColumns.take(columnsNtoShow))

      val columnsDiv=dataTableColumnRow(firstRecordKey.toString,calcVisibleColGroup(visibleColumns,columnsNtoShow))

      val headersDiv=calcVisibleHeaders(firstRecordKey+1,visibleColumns,columnsNtoShow)

      val recordsDiv=calcRecords(firstRecordKey+1+headersDiv.length,visibleColumns,sortedColumns,dtState,columnsNtoShow)

      columnsDiv::headersDiv:::recordsDiv
    }

  }
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
  def divTableCell(key:VDomKey,width:String,align:String,verticalAlign:String,children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,DivTableCell(width,align,verticalAlign),children)
  def flexGrid(key: VDomKey, children: List[ChildPair[OfDiv]]) =
    child[OfDiv](key,FlexGrid(),children)
  def flexGridItem(key: VDomKey, flexBasisWidth: Int, maxWidth: Option[Int], children: List[ChildPair[OfDiv]]) =
    child[OfDiv](key,FlexGridShItem(flexBasisWidth, maxWidth),
      child(key,FlexGridItem(),children) :: Nil
    )
  def flexGridItemTable(key:VDomKey,id:String,flexBasisWidth:Int,maxWidth:Option[Int],
                        dtTable: DtTable,
                        dtTableState:DataTablesState,
                        children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,FlexGridShItem(flexBasisWidth,maxWidth),
      dataTable(key,id,dtTable,dtTableState,true)::Nil
    )
  def dataTableColumnRow(key:VDomKey,children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,DataTableColGroupRow(),children)
  def dataTableCells(key:VDomKey,id:VDomKey,dtTablesState:DataTablesState,children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,DataTableCells(id)(Some(()=>dtTablesState.handleToggle(id))),children)
  def dataTableRecordRow(key:VDomKey,selected:Boolean,handle:Option[()=>Unit], children:List[ChildPair[OfDiv]])={
    //val selected=dtTablesState.dtTableToggleRecordRow.getOrElse(id,false)
    child[OfDiv](key,DataTableRecordRow(selected)(handle),children)}
  def dataTableHeaderRow(key:VDomKey,children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,DataTableHeaderRow(),children)
  def dataTableBody(key:VDomKey,children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,DataTableBody(),children)
  def dataTable(key:VDomKey,
                id:String,
                dtTable: DtTable,
                dtTablesState: DataTablesState,
                flexGrid:Boolean=false)={

    child[OfDiv](key, DataTable(flexGrid)(Some(newVal=>dtTablesState.handleResize(id,newVal.toFloat))),
      dtTable.controlDiv("1",id,dtTablesState)::/*divider("2")::*/
      dtTable.getTableView(3,id,dtTablesState)
    )
  }
  def flexGridItemWidthSync(key:VDomKey,flexBasisWidth:Int,maxWidth:Option[Int],OnResize:Option[(String)=>Unit],children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,FlexGridShItem(flexBasisWidth,maxWidth),
      child[OfDiv](key,FlexGridItemWidthSync()(OnResize),children)::Nil
    )::Nil
}

