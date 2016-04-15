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
case class DataTable(id:String,flexGrid:Boolean)(val onResize:Option[(VDomKey,Float)=>Unit])
  extends VDomValue with OnResizeReceiver{

  def appendJson(builder:JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("DataTable")
      builder.append("onResize").append("feedback()")
      if(flexGrid)
        builder.append("flexReg").append("def")
    builder.end()
  }
}

case class DataTableColumnRow() extends VDomValue{
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
case class DataTableRecordRow(selected:Boolean) extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("DataTableRow")
      builder.append("selected").append(selected)
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

case class DivFlexWrapper(flexBasis:Option[String],
                          displayFlex:Boolean,
                          flexWrap:Boolean,
                          minWidth:Option[String],
                          maxWidth:Option[String],
                          minHeight:Option[String],
                          textAlign:Option[String],
                          borderRight:Boolean
                         ) extends VDomValue{

  def appendJson(builder: JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("div")
      builder.append("style").startObject()
        flexBasis.foreach(x=>builder.append("flex").append(s"1 1 ${x}"))
        if(displayFlex) builder.append("display").append("flex")
        if(flexWrap) builder.append("flexWrap").append("wrap")
        minWidth.foreach(x=>builder.append("minWidth").append(x))
        maxWidth.foreach(x=>builder.append("maxWidth").append(x))
        minHeight.foreach(x=>builder.append("minHeight").append(x))
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

class FlexTags(child: ChildPairFactory,tags:Tags,materialTags: MaterialTags) {

  import materialTags._
  import tags._

  class DtTable(cWidth:Float,controlAllCheckboxShow:Boolean,checkboxShow:Boolean,adjustForCheckbox:Boolean){
    var defaultRowHeight=40

    def controlDiv(key:VDomKey,id:VDomKey,dtState:DataTablesState)={
      if(controlAllCheckboxShow)
      divWrapper(key,None,None,None,None,None,None,
        List(
          divWrapper("1",Some("inline-block"),Some("50px"),Some("50px"),Some("40px"),None,None,List(
            divPositionWrapper("1",Some("inline-block"),Some("relative"),Some("50%"),Some("translateY(-50%)"),
              List(checkBox("1",id,dtState.dtTableCheckAll.getOrElse(id,false),dtState.handleCheckAll))))),
          divWrapper("2",None,None,None,None,Some("right"),None,List(text("text","Control"))))
      )
      else if(adjustForCheckbox)
        divWrapper(key,None,None,None,None,None,None,
          List(
            divWrapper("1",Some("inline-block"),Some("50px"),Some("50px"),Some("40px"),None,None,List(
              divPositionWrapper("1",Some("inline-block"),Some("relative"),Some("50%"),Some("translateY(-50%)"),
                List()))),
            divWrapper("2",None,None,None,None,Some("right"),None,List(text("text","Control"))))
        )
      else
        divWrapper(key,None,None,None,None,None,None,
          List(
            divWrapper("1",Some("inline-block"),Some("50px"),Some("50px"),Some("40px"),None,None,List(
              divPositionWrapper("1",Some("inline-block"),Some("relative"),Some("50%"),Some("translateY(-50%)"),
                List()))),
            divWrapper("2",None,None,None,None,Some("right"),None,List(text("text","Control"))))
        )
    }

    def dtCheckBox(key:VDomKey,id:VDomKey,show:Boolean,adjustSpace:Boolean,checked:Boolean=false,check:(VDomKey,Boolean)=>Unit)={
      if(show)
        List(divFlexWrapper(key,Some("50px"),false,false,Some("50px"),Some("50px"),None,None,false,List(
          divPositionWrapper("1",Some("inline-block"),Some("relative"),Some("2px"),None,
            List(checkBox("1",id,checked,check)))))
        )

      else if(adjustSpace)
        List(divFlexWrapper(key,Some("50px"),false,false,Some("50px"),Some("50px"),None,Some("center"),false,List()))
      else Nil

    }
    trait DtElement{
      def basisWidth:Int
      def maxWidth:Option[String]
      def header:Boolean
      def priority:Int
      var visible:Boolean
      def maxHeaderLines:Int
      def wrapAdjust:Int
      def getElementView(showRightBorder:Boolean=false):ChildPair[OfDiv]
    }
    case class DtColumn(key:VDomKey,basisWidth:Int,textAlign:String,priority:Int,wrapAdjust:Int,
                        maxHeaderLines:Int,columnText:Option[String])
      extends DtElement{
      def header=false
      def maxWidth=None
      var visible=true
      def getElementView(showRightBorder:Boolean)=
        divFlexWrapper(key, Some(s"${basisWidth}px"),displayFlex = false,flexWrap = false,None,None,
          None,Some(textAlign),borderRight = showRightBorder,
          columnText match{
            case Some(c)=>List(text(key, c))
            case _=> List()
          })
    }
    class DtHeader(key:VDomKey,_basisWidth:Int,_maxWidth:Option[String],_priority:Int,children:List[ChildPair[OfDiv]])
      extends DtElement{
      def header=true
      def basisWidth=_basisWidth
      def maxWidth=_maxWidth
      def priority=_priority
      def wrapAdjust=0
      def maxHeaderLines=1
      var visible=true
      def getElementView(showRightBorder:Boolean)={
        divFlexWrapper(key,Some(s"${basisWidth}px"),displayFlex = false,flexWrap = false,None,maxWidth,
          Some(s"${defaultRowHeight}"),
          None,borderRight = false,children)
      }
    }
    class DtRecord(key:VDomKey,visibleChildren:List[ChildPair[OfDiv]],onShowChildren:List[ChildPair[OfDiv]])
      extends DtElement{
      var basisWidth=0
      var maxWidth:Option[String]=None
      var priority=0
      var visible=true
      def wrapAdjust=0
      def maxHeaderLines=1
      def header=false
      def getElementView(showRightBorder:Boolean)={

        divFlexWrapper(key,Some(s"${basisWidth}px"),displayFlex = false,flexWrap = false,None,maxWidth,
          Some(s"${defaultRowHeight}"),
          None,
          borderRight = false,if(visible)visibleChildren else onShowChildren)
      }
    }

    private var dtColumns:List[DtColumn]=Nil
    private var dtHeaders:List[Map[String,List[DtElement]]]=Nil
    private var dtRecords:List[Map[String,List[DtElement]]]=Nil
    private var dtFields:List[ChildPair[OfDiv]]=Nil
    def dtField(children:List[ChildPair[OfDiv]])=
      dtFields=children
    def dtColumn(key:VDomKey,basisWidth:Int,textAlign:String,priority:Int,wrapAdjust:Int,maxHeaderLines:Int,
                 columnText:Option[String]) ={
      DtColumn(key,basisWidth,textAlign,priority,wrapAdjust,maxHeaderLines,columnText)
    }
    def dtColumn(key:VDomKey,basisWidth:Int,textAlign:String,wrapAdjust:Int,columnText:Option[String]) ={
      DtColumn(key,basisWidth,textAlign,0,wrapAdjust,1,columnText)
    }
    def dtHeader(key:VDomKey,basisWidth:Int,maxWidth:Option[String],children:List[ChildPair[OfDiv]])={
      new DtHeader(key,basisWidth,maxWidth,0,children)
    }
    def dtHeader(key:VDomKey,basisWidth:Int,maxWidth:Option[String],priority:Int,children:List[ChildPair[OfDiv]])={
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
    def addRecordsForColumn(records:Map[String,List[DtRecord]]):Unit= {
      dtHeaders.foreach(x=>{
        val dtHeadersKeys=x.keySet
        dtHeadersKeys.foreach(w=>{

          val colRecordsList=records.getOrElse(w,Nil)
          val colHeadersList=x.getOrElse(w,Nil)

          for(i<- 0 until colHeadersList.length){
            colRecordsList(i).basisWidth=colHeadersList(i).basisWidth
            colRecordsList(i).maxWidth=colHeadersList(i).maxWidth
            colRecordsList(i).priority=colHeadersList(i).priority

          }
        })


      })
      dtRecords = dtRecords:::List(records)
    }
    def getRecordsN(sum:Int,i:Int,cWidth:Float,dtEl:List[DtElement],maxLines:Int=1): Int ={
      if(i>=dtEl.length) return i
      if(sum+dtEl(i).basisWidth>cWidth)
        if(maxLines<=1) return i
        else
          getRecordsN(dtEl(i).basisWidth, i + 1, cWidth, dtEl, maxLines - 1)
      else
        getRecordsN(sum+dtEl(i).basisWidth,i+1,cWidth,dtEl,maxLines)
    }
    def getTableView(firstRecordKey:Long,id:VDomKey,dtState:DataTablesState):List[ChildPair[OfDiv]] ={
      val checkBoxWidth=if(controlAllCheckboxShow||checkboxShow||adjustForCheckbox)
        50+dtColumns.length else 0+dtColumns.length

      var recordsN=getRecordsN(0,0,cWidth-checkBoxWidth,dtColumns); recordsN=if(recordsN<=0)1 else recordsN
      var cKey=firstRecordKey+1

      val colGroupsToShow={
        val sortedColumns=dtColumns.sortWith((a,b)=>a.priority<b.priority)
        val selectedColumns=dtColumns.intersect(sortedColumns.take(recordsN))
        dtCheckBox("1",id,show = false,adjustSpace = adjustForCheckbox,checked = false, (VDomKey, Boolean)=>{}):::
          selectedColumns.zipWithIndex.map{case(x,i)=>
          if(i==recordsN-1)
            x.getElementView(false)
          else
            x.getElementView(true)
        }
      }

      val columnsDiv=dataTableColumnRow(firstRecordKey.toString,colGroupsToShow)

      val headersDiv=
        dtHeaders.flatMap(dtR=>{
          cKey=cKey+2

          val headersOfColumnsToShow={
            val headersWithoutCheckBox={
              val sortedColumns=dtColumns.sortWith((a,b)=>a.priority<b.priority)
              val selectedColumns=dtColumns.intersect(sortedColumns.take(recordsN))
              selectedColumns.zipWithIndex.map{case (x,i)=> {
                val records=dtR.getOrElse(x.key,Nil)

                val basisSum=dtColumns.take(recordsN).map(w=>w.basisWidth).sum
                val cColWidth=if(cWidth==0.0f) cWidth else x.basisWidth+(cWidth-checkBoxWidth-basisSum)/recordsN


                val sortedRecords=records.sortWith((a,b)=>a.priority<b.priority)
                val recordsN2=getRecordsN(0,0,cColWidth-x.wrapAdjust,sortedRecords,x.maxHeaderLines)
                val selectedRecords=records.intersect(sortedRecords.take(recordsN2))
                //println(id,recordsN2,cColWidth,cWidth,x.basisWidth,basisSum)
                if(i==recordsN-1)
                  divFlexWrapper(x.key, Some(s"${x.basisWidth}px"),true,true,None,None,None,None,false,
                    selectedRecords.map(r=>r.getElementView()))
                else
                  divFlexWrapper(x.key, Some(s"${x.basisWidth}px"),true,true,None,None,None,None,true,
                    selectedRecords.map(r=>r.getElementView()))
              }}}

            dtCheckBox("1",id,show = false,adjustSpace = adjustForCheckbox,checked = false, (VDomKey, Boolean)=>{}) :::
              headersWithoutCheckBox
          }

          divider((cKey-2).toString)::dataTableHeaderRow((cKey-1).toString,headersOfColumnsToShow)::Nil

        })
      val recordsDiv=
        dtRecords.flatMap(dtR=>{
          cKey=cKey+2
          val dtCRowRecordId=id+"_"+(cKey-1).toString
          val toggled=dtState.dtTableToggleRecordRow.getOrElse(dtCRowRecordId,false)//if(prop.get("dt_rowToggled_"+(cKey-1))==1.0f) true else false
          val dtCRowCheckBoxId=id+"_"+(cKey-1).toString
          val dtCRowChecked=dtState.dtTableCheck.getOrElse(dtCRowCheckBoxId,false)
          dtState.dtTableCheck(dtCRowCheckBoxId)=dtCRowChecked
          val recordsOfColumnsToShow={
             val sortedColumns=dtColumns.sortWith((a,b)=>a.priority<b.priority)
            val recordsWithoutCheckBox=
            {

              val selectedColumns=dtColumns.intersect(sortedColumns.take(recordsN))



              selectedColumns.zipWithIndex.map{case (x,i)=> {
                val records=dtR.getOrElse(x.key,Nil)

                val basisSum=dtColumns.take(recordsN).map(w=>w.basisWidth).sum

                val cColWidth=if(cWidth==0.0f) cWidth else x.basisWidth+(cWidth-checkBoxWidth-basisSum)/recordsN

                val sortedRecords=records.sortWith((a,b)=>a.priority<b.priority)
                val recordsN2=getRecordsN(0,0,cColWidth-x.wrapAdjust,sortedRecords,x.maxHeaderLines)
                val selectedRecords=records.intersect(sortedRecords.take(recordsN2))

                def unselectedRecords(key:VDomKey)= {

                  if (toggled)
                    divWrapper(key,None,None,None,None,None,None ,
                      /*divider("1") ::*/ divFlexWrapper("2",None,true,true,None,None,None,None,false,
                        records.diff(sortedRecords.take(recordsN2)).map(r =>{r.visible=false; r.getElementView()})) :: Nil) :: Nil

                  else Nil
                }

                if(i==recordsN-1)
                  divFlexWrapper(x.key, Some(s"${x.basisWidth}px"),false,false,None,None,None,None,false,
                    divFlexWrapper("1",None,true,true,None,None,None,None,false,
                      selectedRecords.map(r=>r.getElementView()))::unselectedRecords("2"):::Nil//unselectedColumnRecords(3)

                  )
                else
                  divFlexWrapper(x.key, Some(s"${x.basisWidth}px"),false,false,None,None,None,None,true,
                    divFlexWrapper("1",None,true,true,None,None,None,None,false,
                      selectedRecords.map(r=>r.getElementView()))::unselectedRecords("2"):::Nil//unselectedColumnRecords(3)
                  )
              }}}

            val unselectedColumns=
              if(toggled){
                dtColumns.diff(sortedColumns.take(recordsN))
              }
              else
                Nil
            var innerKeys=1
            def unselectedColumnRecords(key:VDomKey)=

              divWrapper(key,None,None,None,None,None,None,
                unselectedColumns.flatMap(c => {
                  val recordsForC=dtR.getOrElse(c.key,Nil)
                  innerKeys=innerKeys+4
                  //divider((innerKeys-4).toString)::
                    divFlexWrapper((innerKeys-3).toString,None,true,false,None,None,None,None,false,
                      c.getElementView(false)::Nil)::
                    //divider((innerKeys-2).toString)::
                    divFlexWrapper((innerKeys-1).toString,None,true,true,None,None,None,None,false,
                      recordsForC.map(r=>r.getElementView(false)))::
                    Nil
                })
              )::Nil
            if(!(adjustForCheckbox||checkboxShow))
              dataTableCells((cKey-1).toString,dtCRowRecordId,dtState,
                divFlexWrapper("1",None,true,false,None,None,None,None,false,
                  recordsWithoutCheckBox)::unselectedColumnRecords("2"))::Nil
            else
              divTableCell("1","50px","left","middle",
                dtCheckBox("1",dtCRowCheckBoxId,checkboxShow,adjustForCheckbox,
                  dtCRowChecked,
                  dtState.handleCheck))::
                dataTableCells((cKey-1).toString,dtCRowRecordId,dtState,
                  divFlexWrapper("1",None,true,false,None,None,None,None,false,
                    recordsWithoutCheckBox)::unselectedColumnRecords("2"))::Nil
          }

          divider((cKey-2).toString)::dataTableRecordRow((cKey-1).toString,
            /*if(prop.get("dt_controlAll")==1.0f) true else*/ dtCRowChecked,recordsOfColumnsToShow)::Nil

        })
      columnsDiv::headersDiv:::recordsDiv
    }

  }
  def divAlignWrapper(key:VDomKey,align:String,verticalAlign:String,children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,DivAlignWrapper(),
      child[OfDiv](key,DivTableCell("100%",align,verticalAlign),children)::Nil
    )
  def divFlexWrapper(key:VDomKey,
                     flexBasis:Option[String],
                     displayFlex:Boolean,
                     flexWrap:Boolean,
                     minWidth:Option[String],
                     maxWidth:Option[String],
                     minHeight:Option[String],
                     textAlign:Option[String],
                     borderRight:Boolean,
                     children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,DivFlexWrapper(flexBasis,displayFlex,flexWrap,minWidth,maxWidth,minHeight,textAlign,borderRight),children)
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
    child[OfDiv](key,DataTableColumnRow(),children)
  def dataTableCells(key:VDomKey,id:VDomKey,dtTablesState:DataTablesState,children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,DataTableCells(id)(Some(()=>dtTablesState.handleToggle(id))),children)
  def dataTableRecordRow(key:VDomKey, selected:Boolean, children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,DataTableRecordRow(selected),children)
  def dataTableHeaderRow(key:VDomKey,children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,DataTableHeaderRow(),children)
  def dataTable(key:VDomKey,
                id:String,
                dtTable: DtTable,
                dtTablesState: DataTablesState,
                flexGrid:Boolean=false)={
    //println(cWidth)
    child[OfDiv](key, DataTable(id,flexGrid)(Some((id,newVal)=>dtTablesState.handleResize(id,newVal))),
      dtTable.controlDiv("1",id,dtTablesState)::divider("2")::
      dtTable.getTableView(3,id,dtTablesState)
    )
  }
}

