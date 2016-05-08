package ee.cone.base.test_loots

import java.time.format.DateTimeFormatter
import java.time._
import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.db._
import ee.cone.base.server.SenderOfConnection
import ee.cone.base.util.Never
import ee.cone.base.vdom.Types.VDomKey
import ee.cone.base.vdom._

/*
object TimeZoneOffsetProvider{
  val defaultTimeZoneID="Europe/Tallinn" //EST
  def getZoneId=ZoneId.of(defaultTimeZoneID)
  def getZoneOffset={
    ZonedDateTime.now(ZoneId.of(defaultTimeZoneID)).getOffset
  }
}
*/
//  println(work(logAt.workStart))
/*
val workDuration=
 if(work(logAt.workStart).isEmpty||work(logAt.workStop).isEmpty)
   None
 else
    Some(Duration.between(
      work(logAt.workStart).getOrElse(Instant.now()).atZone(TimeZoneOffsetProvider.getZoneId),
      work(logAt.workStop).getOrElse(Instant.now()).atZone(TimeZoneOffsetProvider.getZoneId)
    ))
work.update(logAt.workDuration,workDuration)
val totalDuration=
      if(workList(entry).nonEmpty)
        Some(workList(entry).map{w:Obj=>w(logAt.workDuration).getOrElse(Duration.ZERO)}.reduce((a,b)=>a plus b))
      else None
*/

class FailOfConnection(
  sender: SenderOfConnection
) extends CoHandlerProvider {
  def handlers = CoHandler(FailEventKey){ e =>
    println(s"error: ${e.toString}")
    sender.sendToAlien("fail",e.toString) //todo
  } :: Nil
}

class DurationValueConverter(inner: InnerRawValueConverter) extends RawValueConverter[Option[Duration]] {
  def convertEmpty() = None
  def convert(valueA: Long, valueB: Long) = Option(Duration.ofSeconds(valueA,valueB))
  def convert(value: String) = Never()
  def allocWrite(before: Int, value: Option[Duration], after: Int) =
    inner.allocWrite(before, value.get.getSeconds, value.get.getNano, after)
  def nonEmpty(value: Option[Duration]) = value.nonEmpty
}

class InstantValueConverter(inner: InnerRawValueConverter) extends RawValueConverter[Option[Instant]] {
  def convertEmpty() = None
  def convert(valueA: Long, valueB: Long) = Option(Instant.ofEpochSecond(valueA,valueB))
  def convert(value: String) = Never()
  def allocWrite(before: Int, value: Option[Instant], after: Int) =
    inner.allocWrite(before, value.get.getEpochSecond, value.get.getNano, after)
  def nonEmpty(value: Option[Instant]) = value.nonEmpty
}

class TestAttributes(
  attr: AttrFactory,
  label: LabelFactory,
  strValueConverter: RawValueConverter[String]
)(
  val caption: Attr[String] = attr(new PropId(0x6800), strValueConverter)
)

class BoatLogEntryAttributes(
  sysAttrs: SysAttrs,
  attr: AttrFactory,
  label: LabelFactory,
  searchIndex: SearchIndex,
  definedValueConverter: RawValueConverter[Boolean],
  nodeValueConverter: RawValueConverter[Obj],
  stringValueConverter: RawValueConverter[String],
  uuidValueConverter: RawValueConverter[Option[UUID]],
  instantValueConverter: RawValueConverter[Option[Instant]],
  durationValueConverter: RawValueConverter[Option[Duration]],

  alienCanChange: AlienCanChange
)(
  val justIndexed: Attr[String] = sysAttrs.justIndexed,

  val asEntry: Attr[Obj] = label(0x6700),
  val boat: Attr[Obj] = attr(new PropId(0x6701), nodeValueConverter),
  val date: Attr[Option[Instant]] = attr(new PropId(0x6702), instantValueConverter),
  val durationTotal: Attr[Option[Duration]] = attr(new PropId(0x6703), durationValueConverter),
  val asConfirmed: Attr[Obj] = label(0x6704),
  val confirmedBy: Attr[Obj] = attr(new PropId(0x6705), nodeValueConverter),
  val confirmedOn: Attr[Option[Instant]] = attr(new PropId(0x6706), instantValueConverter), //0x6709
  val entryCreated: Attr[Boolean] = attr(new PropId(0x6707), definedValueConverter),
  val entryRemoved: Attr[Boolean] = attr(new PropId(0x6708), definedValueConverter),

  val chuckNorris:Attr[Obj] = attr(new PropId(0x666),nodeValueConverter),
  val chuckCreated: Attr[Boolean] = attr(new PropId(0x6669), definedValueConverter),

  val log00Date: Attr[Option[Instant]] = attr(new PropId(0x6710), instantValueConverter),
  val log00Fuel: Attr[String] = attr(new PropId(0x6711), stringValueConverter),
  val log00Comment: Attr[String] = attr(new PropId(0x6712), stringValueConverter),
  val log00Engineer: Attr[String] = attr(new PropId(0x6713), stringValueConverter),
  val log00Master: Attr[String] = attr(new PropId(0x6714), stringValueConverter),
  val log08Date: Attr[Option[Instant]] = attr(new PropId(0x6715), instantValueConverter),
  val log08Fuel: Attr[String] = attr(new PropId(0x6716), stringValueConverter),
  val log08Comment: Attr[String] = attr(new PropId(0x6717), stringValueConverter),
  val log08Engineer: Attr[String] = attr(new PropId(0x6718), stringValueConverter),
  val log08Master: Attr[String] = attr(new PropId(0x6719), stringValueConverter),
  val logRFFuel: Attr[String] = attr(new PropId(0x670B), stringValueConverter),
  val logRFComment: Attr[String] = attr(new PropId(0x670C), stringValueConverter),
  val logRFEngineer: Attr[String] = attr(new PropId(0x670D), stringValueConverter), // 0x670A,0x670E,0x670F
  val log24Date: Attr[Option[Instant]] = attr(new PropId(0x671A), instantValueConverter),
  val log24Fuel: Attr[String] = attr(new PropId(0x671B), stringValueConverter),
  val log24Comment: Attr[String] = attr(new PropId(0x671C), stringValueConverter),
  val log24Engineer: Attr[String] = attr(new PropId(0x671D), stringValueConverter),
  val log24Master: Attr[String] = attr(new PropId(0x671E), stringValueConverter), // 0x671F

  val asWork: Attr[Obj] = label(0x6720),
  val workStart: Attr[Option[Instant]] = attr(new PropId(0x6721), instantValueConverter),
  val workStop: Attr[Option[Instant]] = attr(new PropId(0x6722), instantValueConverter),
  val workDuration: Attr[Option[Duration]] = attr(new PropId(0x6723), durationValueConverter),
  val workComment: Attr[String] = attr(new PropId(0x6724), stringValueConverter),
  val entryOfWork: Attr[Obj] = attr(new PropId(0x6725), nodeValueConverter),
  val workCreated: Attr[Boolean] = attr(new PropId(0x6726), definedValueConverter),
  val workRemoved: Attr[Boolean] = attr(new PropId(0x6727), definedValueConverter),

  val targetEntryOfWork: Attr[Option[UUID]] = attr(new PropId(0x6728), uuidValueConverter),

  val targetStringValue: Attr[String] = attr(new PropId(0x6730), stringValueConverter),
  val targetInstantValue: Attr[Option[Instant]] = attr(new PropId(0x6731), instantValueConverter),
  val entryConfirmed: Attr[Boolean] = attr(new PropId(0x6732),definedValueConverter),
  val entryReopened: Attr[Boolean] = attr(new PropId(0x6733),definedValueConverter)

)(val handlers: List[BaseCoHandler] =
  searchIndex.handlers(asEntry, justIndexed) :::
  searchIndex.handlers(asWork, entryOfWork) :::
  alienCanChange.handlers(targetInstantValue)(date) :::
    alienCanChange.handlers(targetInstantValue)(log00Date):::
    alienCanChange.handlers(targetInstantValue)(log08Date):::
    alienCanChange.handlers(targetInstantValue)(log24Date):::
  List(
    /*log00Date,*/log00Fuel,log00Comment,log00Engineer,log00Master,
    /*log08Date,*/log08Fuel,log08Comment,log08Engineer,log08Master,
    logRFFuel,logRFComment,logRFEngineer,
    /*log24Date,*/log24Fuel,log24Comment,log24Engineer,log24Master
  ).flatMap(alienCanChange.handlers(targetStringValue)(_)) :::
    alienCanChange.handlers(targetInstantValue)(workStart) :::
    alienCanChange.handlers(targetInstantValue)(workStop) :::
    alienCanChange.handlers(targetStringValue)(workComment)
) extends CoHandlerProvider

class DataTablesState(currentVDom: CurrentVDom){
  val widthOfTables=scala.collection.mutable.Map[VDomKey,Float]()
  val areAllRowsCheckedOfTables=scala.collection.mutable.Map[VDomKey,Boolean]()
  val isRowCheckedOfTables=scala.collection.mutable.Map[VDomKey,Boolean]()
  val isRowToggledOfTables=scala.collection.mutable.Map[VDomKey,Boolean]()
  def width(tableId:VDomKey)=widthOfTables.getOrElse(tableId,0.0f)
  def areAllRowsChecked(tableId:VDomKey)=areAllRowsCheckedOfTables.getOrElse(tableId,false)
  def isRowChecked(tableId:VDomKey,rowId:VDomKey)=isRowCheckedOfTables.getOrElseUpdate(tableId+rowId,false)
  def isRowToggled(tableId:VDomKey,rowId:VDomKey)=isRowToggledOfTables.getOrElseUpdate(tableId+rowId,false)
  def getCheckedRowsSrcId(id:VDomKey):Seq[UUID]={
    isRowCheckedOfTables.filter{case (k,v)=>k.indexOf(id)==0 && v}.
      map{case (k,_)=> k.takeRight(k.length-id.length).toString}.
      map(x=>UUID.fromString(x)).toList
  }
  def handleResize(tableId:VDomKey,cWidth:Float)={
    widthOfTables(tableId)=cWidth
    println(tableId,cWidth)
    currentVDom.invalidate()
  }
  def handleCheckAll(tableId:VDomKey,checked:Boolean): Unit ={
    areAllRowsCheckedOfTables(tableId)=checked
    isRowCheckedOfTables.foreach{case(k,_)=>if(k.indexOf(tableId)==0) isRowCheckedOfTables(k)=checked}
    currentVDom.invalidate()
  }
  def handleCheck(tableId:VDomKey,rowId:VDomKey,checked:Boolean)={
    val id=tableId+rowId
    isRowCheckedOfTables(id)=checked
    currentVDom.invalidate()
  }
  def handleToggle(tableId:VDomKey,rowId:VDomKey)={
    val id=tableId+rowId
    //println("toggle",id)
    val newVal=true
    isRowToggledOfTables(id)=newVal
    isRowToggledOfTables.foreach{case (k,v)=>if(k!=id&&newVal)isRowToggledOfTables(k)=false}
    currentVDom.invalidate()
  }

}
class TestComponent(
  at: TestAttributes,
  logAt: BoatLogEntryAttributes,
  alienAccessAttrs: AlienAccessAttrs,
  handlerLists: CoHandlerLists,
  findNodes: FindNodes, uniqueNodes: UniqueNodes, mainTx: CurrentTx[MainEnvKey],
  alienAttr: AlienAttrFactory,
  onUpdate: OnUpdate,
  tags: Tags,
  materialTags: MaterialTags,
  flexTags:FlexTags,
  currentVDom: CurrentVDom,
  dtTablesState: DataTablesState
) extends CoHandlerProvider {
  import tags._
  import materialTags._
  import flexTags._
  val tbl: HtmlTableWithControl = new FlexDataTableImpl(flexTags)
  private def eventSource = handlerLists.single(SessionEventSource)

  private def toAlienText[Value](obj: Obj, attr: Attr[Value], valueToText: Value⇒String,label:String,showLabel:Boolean): List[ChildPair[OfDiv]] =
    if(!obj.nonEmpty) Nil
    else if(!showLabel)
      List(text("1",valueToText(obj(attr))))
    else
      List(labeledText("1",valueToText(obj(attr)),label))


  private def strField(obj: Obj, attr: Attr[String], editable: Boolean,label:String,showLabel:Boolean): List[ChildPair[OfDiv]] =
    if(!obj.nonEmpty) Nil
    else if(!editable)
      if(!showLabel)
        List(text("1",obj(attr)))
      else
        List(labeledText("1",obj(attr),label))
    else {
      val srcId = obj(uniqueNodes.srcId).get
      List(textInput("1",obj(attr), alienAttr(attr)(srcId),label,showLabel))
    }
  private def durationField(obj: Obj, attr: Attr[Option[Duration]],label:String,showLabel:Boolean): List[ChildPair[OfDiv]] = {
    toAlienText[Option[Duration]](obj, attr, v ⇒ v.map(x=>
      x.abs.toHours+"h:"+x.abs.minusHours(x.abs.toHours).toMinutes.toString+"m").getOrElse(""),label,showLabel
    )
  }

  private def instantField(obj: Obj, attr: Attr[Option[Instant]], editable: Boolean,label:String,showLabel:Boolean): List[ChildPair[OfDiv]] = {

    if(!obj.nonEmpty) Nil
    else if(!editable) {
      val dateVal=obj(attr) match {
        case Some(v) ⇒ {
          val date = LocalDate.from(v.atZone(ZoneId.of("UTC")))
          val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
          date.format(formatter)

        }
        case _=>""
      }
      if (!showLabel)
        text("1",dateVal)::Nil
      else
        labeledText("1", dateVal, label)::Nil
    }
    else {
      val srcId = obj(uniqueNodes.srcId).get
      List(dateInput("1",obj(attr),alienAttr(attr)(srcId),label,showLabel))
    }
  }


  private def objField(obj: Obj, attr: Attr[Obj], editable: Boolean,label:String,showLabel:Boolean): List[ChildPair[OfDiv]] ={
    toAlienText[Obj](obj,attr,v⇒if(v.nonEmpty) v(at.caption) else "",label,showLabel)
  }
  private def entryList(): List[Obj] = findNodes.where(
    mainTx(), logAt.asEntry.defined,
    logAt.justIndexed, findNodes.justIndexed,
    Nil
  )
  private def workList(entry: Obj): List[Obj] = if(entry.nonEmpty) findNodes.where(
    mainTx(), logAt.asWork.defined,
    logAt.entryOfWork, entry,
    Nil
  ) else Nil

  //private def filterListAct()() = ???
  //private def clearSortingAct()() = ???
  private def entryAddAct()() = {
    eventSource.addEvent{ ev =>
      ev(alienAccessAttrs.targetSrcId) = Option(UUID.randomUUID)
      (logAt.entryCreated, "entry was created")
    }
    currentVDom.invalidate()
  }
  private def entryCreated(ev: Obj): Unit = {
    val srcId = ev(alienAccessAttrs.targetSrcId).get
    val entry = uniqueNodes.create(mainTx(), logAt.asEntry, srcId)
    entry(logAt.justIndexed) = findNodes.justIndexed
  }
  private def entryRemoveAct(entrySrcId: UUID)() = {
    eventSource.addEvent { ev =>
      ev(alienAccessAttrs.targetSrcId) = Some(entrySrcId)
      (logAt.entryRemoved, "entry was removed")
    }

  }
  private def entryRemoved(ev: Obj): Unit = {
    val entry = uniqueNodes.whereSrcId(mainTx(), ev(alienAccessAttrs.targetSrcId).get)
    entry(logAt.justIndexed) = ""
  }
  private def entryReopenAct(entrySrcId: UUID)() = {
    eventSource.addEvent{ev =>
      ev(alienAccessAttrs.targetSrcId)=Some(entrySrcId)
      (logAt.entryReopened,"entry was reopened")
    }
    currentVDom.invalidate()
  }
  private def entryReopened(ev:Obj)={
    val entry=uniqueNodes.whereSrcId(mainTx(),ev(alienAccessAttrs.targetSrcId).get)
    entry(logAt.asConfirmed)=uniqueNodes.noNode
  }
  private def entryConfirmAct(entrySrcId: UUID)() = {
    eventSource.addEvent{ev =>
      ev(alienAccessAttrs.targetSrcId)=Some(entrySrcId)
      (logAt.entryConfirmed,"entry was confirmed")
    }
    currentVDom.invalidate()
  }
  private def entryConfirmed(ev:Obj)={
    val entry=uniqueNodes.whereSrcId(mainTx(),ev(alienAccessAttrs.targetSrcId).get)
    entry(logAt.asConfirmed)=entry
  }
  private def workAddAct(entrySrcId: UUID)() = {
    eventSource.addEvent{ ev =>
      ev(alienAccessAttrs.targetSrcId) = Option(UUID.randomUUID)
      ev(logAt.targetEntryOfWork) = Some(entrySrcId)
      (logAt.workCreated, "work was created")
    }
    currentVDom.invalidate()
  }
  private def workCreated(ev: Obj): Unit = {
    val srcId = ev(alienAccessAttrs.targetSrcId).get
    val work = uniqueNodes.create(mainTx(), logAt.asWork, srcId)
    val entry = uniqueNodes.whereSrcId(mainTx(), ev(logAt.targetEntryOfWork).get)
    work(logAt.entryOfWork) = entry
    //println(s"workCreated: ${work(logAt.entryOfWork)}")
  }
  private def workRemoveAct(workSrcId: UUID)() = {
    eventSource.addEvent { ev =>
      ev(alienAccessAttrs.targetSrcId) = Some(workSrcId)
      (logAt.workRemoved, "work was removed")
    }

  }
  private def workRemoved(ev: Obj): Unit = {
    val work = uniqueNodes.whereSrcId(mainTx(), ev(alienAccessAttrs.targetSrcId).get)
    //println(s"workRemovedBefore: ${work(logAt.entryOfWork)}")
    work(logAt.entryOfWork) = uniqueNodes.noNode
    //println(s"workRemovedAfter: ${work(logAt.entryOfWork)}")
  }

  private def emptyView(pf: String) =
    tags.root(List(tags.text("text", "Loading...")))

  private def wrapDBView[R](view: ()=>R): R =
    eventSource.incrementalApplyAndView { () ⇒
      val startTime = System.currentTimeMillis
      val res = view()
      val endTime = System.currentTimeMillis
      currentVDom.until(endTime+(endTime-startTime)*10)
      res
    }

  private def paperWithMargin(key: VDomKey, child: ChildPair[OfDiv]) =
    withMargin(key, 10, paper("paper", withPadding(key, 10, child)))

  private def entryListView(pf: String) = wrapDBView{ ()=>{
  root(
    List(
    //class LootsBoatLogList
      toolbar(),
      withMaxWidth("1",1200,
        List(
          paperWithMargin("margin2",flexGrid("flexGridList2",
            flexGridItemWidthSync("widthSync",1000,None,Some(newVal=>dtTablesState.handleResize("dtTableList2",newVal.toFloat)),
              tbl.table("1",Width(dtTablesState.width("dtTableList2"))::Nil)(List(
                tbl.controlPanel("",btnDelete("1", ()=>removeSelectedRows("dtTableList2",entryRemoveAct)),btnAdd("2", entryAddAct())),
                tbl.row("row",MaxVisibleLines(2),IsHeader(true))(
                  tbl.group("1_grp",MinWidth(50),MaxWidth(50),Priority(0),TextAlign("center"),Caption("x1")),
                  tbl.cell("1",MinWidth(50),VerticalAlign("middle"))((_)=>
                    checkBox("1", dtTablesState.areAllRowsChecked("dtTableList2"), dtTablesState.handleCheckAll("dtTableList2", _))::Nil
                  ),
                  tbl.group("2_grp",MinWidth(150),Priority(3),TextAlign("center"),Caption("x2")),
                  tbl.cell("2",MinWidth(100),VerticalAlign("middle"))((_)=>
                    withSideMargin("1",10,List(text("1","Boat")))::Nil
                  ),
                  tbl.cell("3",MinWidth(150),VerticalAlign("middle"))((_)=>
                    withSideMargin("1",10,List(text("1","Date")))::Nil
                  ),
                  tbl.cell("4",MinWidth(180),VerticalAlign("middle"))((_)=>
                    withSideMargin("1",10,List(text("1","Total duration, hrs:min")))::Nil
                  ),
                  tbl.cell("5",MinWidth(100),VerticalAlign("middle"))((_)=>
                    withSideMargin("1",10,List(text("1","Confirmed")))::Nil
                  ),
                  tbl.cell("6",MinWidth(150),VerticalAlign("middle"))((_)=>
                    withSideMargin("1",10,List(text("1","Confirmed by")))::Nil
                  ),
                  tbl.cell("7",MinWidth(150),VerticalAlign("middle"))((_)=>
                    withSideMargin("1",10,List(text("1","Confirmed on")))::Nil
                  ),
                  tbl.cell("8",MinWidth(100),Priority(0),TextAlign("center"),VerticalAlign("middle"))((_)=>
                    withSideMargin("1",10,List(text("1","xx")))::Nil
                  )
                )
              ):::
                entryList().map{ (entry:Obj)=>
                  val entrySrcId = entry(uniqueNodes.srcId).get
                  val go = Some(()⇒ currentVDom.relocate(s"/entryEdit/$entrySrcId"))
                  tbl.row(entrySrcId.toString,
                    Toggled(dtTablesState.isRowToggled("dtTableList2",entrySrcId.toString))(
                      Some(() => dtTablesState.handleToggle("dtTableList2",entrySrcId.toString))),
                    Selected(dtTablesState.isRowChecked("dtTableList2",entrySrcId.toString)),
                    MaxVisibleLines(2))(
                    tbl.group("1_grp", MinWidth(50),MaxWidth(50), Priority(1),TextAlign("center"), Caption("x1")),
                    tbl.cell("1", MinWidth(50),VerticalAlign("middle"))((_)=>
                      checkBox("1", dtTablesState.isRowChecked("dtTableList2",entrySrcId.toString),
                        dtTablesState.handleCheck("dtTableList2",entrySrcId.toString, _))::Nil
                    ),
                    tbl.group("2_grp", MinWidth(150),Priority(3), TextAlign("center"),Caption("x2")),
                    tbl.cell("2",MinWidth(100))((showLabel)=>
                      withSideMargin("1", 10, objField(entry, logAt.boat, editable = false,"Boat",showLabel>0)) :: Nil
                    ),
                    tbl.cell("3",MinWidth(150),VerticalAlign("middle"))((showLabel)=>
                      instantField(entry, logAt.date, editable = false,"Data",showLabel>0)
                    ),
                    tbl.cell("4",MinWidth(180),VerticalAlign("middle"))((showLabel)=>
                      durationField(entry, logAt.durationTotal,"Total duration, hrs:min",showLabel>0)
                    ),
                    tbl.cell("5",MinWidth(100),VerticalAlign("middle"))((_)=>
                      withSideMargin("1",10,{
                        if(entry(logAt.asConfirmed).nonEmpty)
                          List(materialChip("1","CONFIRMED"))
                        else Nil
                      })::Nil
                    ),
                    tbl.cell("6",MinWidth(150))((showLabel)=>
                      withSideMargin("1",10,objField(entry, logAt.confirmedBy, editable = false,"Confirmed by",showLabel>0))::Nil
                    ),
                    tbl.cell("7",MinWidth(150))((showLabel)=>
                      withSideMargin("1",10,instantField(entry, logAt.confirmedOn, editable = false,"Confirmed on",showLabel>0))::Nil
                    ),
                    tbl.cell("8",MinWidth(100),Priority(0),TextAlign("center"),VerticalAlign("middle"))((_)=>
                      btnCreate("btn2",go.get)::Nil
                    )
                  )
                }
              )
            )
          ))
        )
      )
    )
  )
}}
  private def removeSelectedRows(tableId:VDomKey,handle:(UUID)=>()=>Unit)={

    dtTablesState.getCheckedRowsSrcId(tableId).foreach(srcId=>{
      handle(srcId)()
      dtTablesState.handleCheck(tableId,srcId.toString,checked = false)
    })

    if(dtTablesState.areAllRowsChecked(tableId))
      dtTablesState.handleCheckAll(tableId,checked = false)
    currentVDom.invalidate()
  }
  private def entryEditView(pf: String) = wrapDBView { () =>
    //println(pf)
    val srcId = UUID.fromString(pf.tail)
    val obj = uniqueNodes.whereSrcId(mainTx(), srcId)
    if(!obj.nonEmpty) root(List(text("text","???")))
    else editViewInner(srcId, obj(logAt.asEntry))
  }


  private def editViewInner(srcId: UUID, entry: Obj) = {
    val editable = true /*todo rw rule*/

    root(List(
      toolbar(),
      withMaxWidth("1",1200,List(
      paperWithMargin(s"$srcId-1",
        flexGrid("flexGridEdit1",List(
          flexGridItem("1",500,None,List(
            flexGrid("FlexGridEdit11",List(
              flexGridItem("boat",150,None,objField(entry,logAt.boat,editable = false,"Boat",showLabel = true)),
              flexGridItem("date",150,None,instantField(entry, logAt.date, editable,"Date",showLabel = true/*todo date */)),
              flexGridItem("dur",170,None,List(divAlignWrapper("1","left","middle",
                durationField(entry,logAt.durationTotal,"Total duration, hrs:min",showLabel = true))))
            ))
          )),
          flexGridItem("2",500,None,List(
            flexGrid("flexGridEdit12",List(
              flexGridItem("conf_by",150,None,objField(entry,logAt.confirmedBy,editable = false,"Confirmed by",showLabel = true)),
              flexGridItem("conf_on",150,None,instantField(entry, logAt.confirmedOn, editable = false,"Confirmed on",showLabel = true/*todo date */)),
              flexGridItem("conf_do",150,None,List(
                divHeightWrapper("1",72,
                  divAlignWrapper("1","right","bottom",

                    if(!entry.nonEmpty) Nil
                    else if(entry(logAt.asConfirmed).nonEmpty)
                      List(btnRaised("reopen","Reopen")(entryReopenAct(srcId)))
                    else
                      List(btnRaised("confirm","Confirm")(entryConfirmAct(srcId)))

                  ))
              ))
            ))
          )))
        )
      ))),

      withMaxWidth("2",1200,List(
        paperWithMargin(s"$srcId-2",flexGrid("flexGridEdit2",
          flexGridItemWidthSync("widthSync",1000,None,Some(newVal=>dtTablesState.handleResize("dtTableEdit1",newVal.toFloat)),
            tbl.table("dtTableEdit1",Width(dtTablesState.width("dtTableEdit1")))(
              tbl.row("row",IsHeader(true))(
                tbl.cell("1",MinWidth(100),Priority(3),VerticalAlign("middle"))((_)=>withSideMargin("1",10,List(text("1","Time")))::Nil),
                tbl.cell("2",MinWidth(150),Priority(1),VerticalAlign("middle"))((_)=>withSideMargin("1",10,List(text("1","ME Hours.Min")))::Nil),
                tbl.cell("3",MinWidth(100),Priority(1),VerticalAlign("middle"))((_)=>withSideMargin("1",10,List(text("1","Fuel rest/quantity")))::Nil),
                tbl.cell("4",MinWidth(250),Priority(3),VerticalAlign("middle"))((_)=>withSideMargin("1",10,List(text("1","Comment")))::Nil),
                tbl.cell("5",MinWidth(150),Priority(2),VerticalAlign("middle"))((_)=>withSideMargin("1",10,List(text("1","Engineer")))::Nil),
                tbl.cell("6",MinWidth(150),Priority(2),VerticalAlign("middle"))((_)=>withSideMargin("1",10,List(text("1","Master")))::Nil)
              ),
              tbl.row("row1",Toggled(dtTablesState.isRowToggled("dtTableEdit1","row1"))(
                Some(()=>dtTablesState.handleToggle("dtTableEdit1","row1"))))(
                tbl.cell("1",MinWidth(100),Priority(3),VerticalAlign("middle"))((showLabel)=>
                  withSideMargin("1",10,List(if(showLabel>0) labeledText("1","00:00","Time") else text("1","00:00")))::Nil
                ),
                tbl.cell("2",MinWidth(150),Priority(1),VerticalAlign("middle"))((showLabel)=>
                  withSideMargin("1",10,timeInput("1",entry(logAt.log00Date),
                  "Date",showLabel>0,alienAttr(logAt.log00Date)(entry(uniqueNodes.srcId).get)))::Nil
                ),
                tbl.cell("3",MinWidth(100),Priority(1),VerticalAlign("middle"))((showLabel)=>
                  withSideMargin("1",10,strField(entry, logAt.log00Fuel, editable,"Fuel rest/quantity",showLabel>0))::Nil
                ),
                tbl.cell("4",MinWidth(250),Priority(3),VerticalAlign("middle"))((showLabel)=>
                  withSideMargin("1",10,strField(entry, logAt.log00Comment, editable,"Comment",showLabel>0))::Nil
                ),
                tbl.cell("5",MinWidth(150),Priority(2),VerticalAlign("middle"))((showLabel)=>
                  withSideMargin("1",10,strField(entry, logAt.log00Engineer, editable,"Engineer",showLabel>0))::Nil
                ),
                tbl.cell("6",MinWidth(150),Priority(2),VerticalAlign("middle"))((showLabel)=>
                  withSideMargin("1",10,strField(entry, logAt.log00Master, editable,"Master",showLabel>0))::Nil
                )

              ),
              tbl.row("row2",Toggled(dtTablesState.isRowToggled("dtTableEdit1","row2"))(
                Some(()=>dtTablesState.handleToggle("dtTableEdit1","row2"))))(
                tbl.cell("1",MinWidth(100),Priority(3),VerticalAlign("middle"))((showLabel)=>
                  withSideMargin("1",10,List(if(showLabel>0) labeledText("1","08:00","Time") else text("1","08:00")))::Nil
                ),
                tbl.cell("2",MinWidth(150),Priority(1),VerticalAlign("middle"))((showLabel)=>
                  withSideMargin("1",10,timeInput("1",entry(logAt.log08Date),"Date",showLabel>0,
                  alienAttr(logAt.log08Date)(entry(uniqueNodes.srcId).get)))::Nil
                ),
                tbl.cell("3",MinWidth(100),Priority(1),VerticalAlign("middle"))((showLabel)=>
                  withSideMargin("1",10,strField(entry, logAt.log08Fuel, editable,"Fuel rest/quantity",showLabel>0))::Nil
                ),
                tbl.cell("4",MinWidth(250),Priority(3),VerticalAlign("middle"))((showLabel)=>
                  withSideMargin("1",10,strField(entry, logAt.log08Comment, editable,"Comment",showLabel>0))::Nil
                ),
                tbl.cell("5",MinWidth(150),Priority(2),VerticalAlign("middle"))((showLabel)=>
                  withSideMargin("1",10,strField(entry, logAt.log08Engineer, editable,"Engineer",showLabel>0))::Nil
                ),
                tbl.cell("6",MinWidth(150),Priority(2),VerticalAlign("middle"))((showLabel)=>
                  withSideMargin("1",10,strField(entry, logAt.log08Master, editable,"Master",showLabel>0))::Nil
                )

              ),
              tbl.row("row3",Toggled(dtTablesState.isRowToggled("dtTableEdit1","row3"))(
                Some(()=>dtTablesState.handleToggle("dtTableEdit1","row3"))))(
                tbl.cell("1",MinWidth(100),Priority(3),VerticalAlign("middle"))((_)=>withSideMargin("1",10,List(text("1","Passed")))::Nil),
                tbl.cell("2",MinWidth(150),Priority(1),VerticalAlign("middle"))((_)=>withSideMargin("1",10,List(text("1","Received Fuel")))::Nil),
                tbl.cell("3",MinWidth(100),Priority(1),VerticalAlign("middle"))((showLabel)=>
                  withSideMargin("1",10,strField(entry, logAt.logRFFuel, editable,"Fuel rest/quantity",showLabel>0))::Nil
                ),
                tbl.cell("4",MinWidth(250),Priority(3),VerticalAlign("middle"))((showLabel)=>
                  withSideMargin("1",10,strField(entry, logAt.logRFComment, editable,"Comment",showLabel>0))::Nil),
                tbl.cell("5",MinWidth(150),Priority(2),VerticalAlign("middle"))((showLabel)=>
                  withSideMargin("1",10,strField(entry, logAt.logRFEngineer, editable,"Engineer",showLabel>0))::Nil),
                tbl.cell("6",MinWidth(150),Priority(2),VerticalAlign("middle"))((_)=>withSideMargin("1",10,Nil)::Nil)

              ),
              tbl.row("row4",Toggled(dtTablesState.isRowToggled("dtTableEdit1","row4"))(
                Some(()=>dtTablesState.handleToggle("dtTableEdit1","row4"))))(
                tbl.cell("1",MinWidth(100),Priority(3),VerticalAlign("middle"))((showLabel)=>
                  withSideMargin("1",10,List(if(showLabel>0) labeledText("1","24:00","Time") else text("1","24:00")))::Nil
                ),
                tbl.cell("2",MinWidth(150),Priority(1),VerticalAlign("middle"))((showLabel)=>
                  withSideMargin("1",10,timeInput("1",entry(logAt.log24Date),"Date",showLabel>0,
                  alienAttr(logAt.log24Date)(entry(uniqueNodes.srcId).get)))::Nil),
                tbl.cell("3",MinWidth(100),Priority(1),VerticalAlign("middle"))((showLabel)=>
                  withSideMargin("1",10,strField(entry, logAt.log24Fuel, editable,"Fuel rest/quantity",showLabel>0))::Nil
                ),
                tbl.cell("4",MinWidth(250),Priority(3),VerticalAlign("middle"))((showLabel)=>
                  withSideMargin("1",10,strField(entry, logAt.log24Comment, editable,"Comment",showLabel>0))::Nil
                ),
                tbl.cell("5",MinWidth(150),Priority(2),VerticalAlign("middle"))((showLabel)=>
                  withSideMargin("1",10,strField(entry, logAt.log24Engineer, editable,"Engineer",showLabel>0))::Nil
                ),
                tbl.cell("6",MinWidth(150),Priority(2),VerticalAlign("middle"))((showLabel)=>
                  withSideMargin("1",10,strField(entry, logAt.log24Master, editable,"Master",showLabel>0))::Nil
                )
              )
            )
          )
        ))
      )),
      withMaxWidth("3",1200,List(
        paperWithMargin(s"$srcId-3",flexGrid("flexGridEdit3",
          flexGridItemWidthSync("widthSync",1000,None,Some(newVal=>dtTablesState.handleResize("dtTableEdit2",newVal.toFloat)),
            tbl.table("dtTableEdit2",Width(dtTablesState.width("dtTableEdit2"))::Nil)(List(
              tbl.controlPanel("",btnDelete("1", ()=>removeSelectedRows("dtTableEdit2",workRemoveAct)),btnAdd("2", workAddAct(srcId))),
              tbl.row("row",IsHeader(true))(
                tbl.group("1_group",MinWidth(50),MaxWidth(50),Priority(0)),
                tbl.cell("1",MinWidth(50),VerticalAlign("middle"))((_)=>
                  checkBox("1",dtTablesState.areAllRowsChecked("dtTableEdit2"),dtTablesState.handleCheckAll("dtTableEdit2",_))::Nil
                ),
                tbl.group("2_group",MinWidth(150)),
                tbl.cell("2",MinWidth(100),VerticalAlign("middle"))((_)=>withSideMargin("1",10,List(text("1","Start")))::Nil),
                tbl.cell("3",MinWidth(100),VerticalAlign("middle"))((_)=>withSideMargin("1",10,List(text("1","Stop")))::Nil),
                tbl.cell("4",MinWidth(150),VerticalAlign("middle"))((_)=>withSideMargin("1",10,List(text("1","Duration, hrs:min")))::Nil),
                tbl.cell("5",MinWidth(250),Priority(3),VerticalAlign("middle"))((_)=>withSideMargin("1",10,List(text("1","Comment")))::Nil)
              )):::
              workList(entry).map { (work: Obj) =>
                val workSrcId = work(uniqueNodes.srcId).get
                tbl.row(workSrcId.toString,Toggled(dtTablesState.isRowToggled("dtTableEdit2",workSrcId.toString))(
                  Some(()=>dtTablesState.handleToggle("dtTableEdit2",workSrcId.toString))),
                  Selected(dtTablesState.isRowChecked("dtTableEdit2",workSrcId.toString)))(
                  tbl.group("1_group",MinWidth(50),MaxWidth(50),Priority(0)),
                  tbl.cell("1",MinWidth(50),VerticalAlign("middle"))((_)=>
                    checkBox("1", dtTablesState.isRowChecked("dtTableEdit2",workSrcId.toString),
                      dtTablesState.handleCheck("dtTableEdit2",workSrcId.toString, _))::Nil
                  ),
                  tbl.group("2_group",MinWidth(150)),
                  tbl.cell("2",MinWidth(100),VerticalAlign("middle"))((showLabel)=>
                    withSideMargin("1",10,timeInput("1",work(logAt.workStart),"Start",showLabel>0,
                    alienAttr(logAt.workStart)(work(uniqueNodes.srcId).get)))::Nil
                  ),
                  tbl.cell("3",MinWidth(100),VerticalAlign("middle"))((showLabel)=>
                    withSideMargin("1",10,timeInput("1",work(logAt.workStop),"Stop",showLabel>0,
                    alienAttr(logAt.workStop)(work(uniqueNodes.srcId).get)))::Nil
                  ),
                  tbl.cell("4",MinWidth(150),VerticalAlign("middle"))((showLabel)=>
                    withSideMargin("1",10,durationField(work, logAt.workDuration,"Duration, hrs:min",showLabel>0))::Nil
                  ),
                  tbl.cell("5",MinWidth(250),Priority(3),VerticalAlign("middle"))((showLabel)=>
                    withSideMargin("1",10,strField(entry, logAt.workComment, editable,"Comment",showLabel>0))::Nil
                  )
                )
              }
            )
          )
        ))
      ))
    ))
  }


  private def eventListView(pf: String) = wrapDBView { () =>
    root(List(
      toolbar(),
      paperWithMargin("margin",table("table",
        List(
          row("head",
            cell("1", isHead=true, isUnderline = true)(List(text("text", "Event"))),
            cell("2", isHead=true, isUnderline = true)(Nil)
          )
        ),
        eventSource.unmergedEvents.map { ev =>
          val srcId = ev(uniqueNodes.srcId).get
          row(srcId.toString,
            cell("1")(List(text("text", ev(eventSource.comment)))),
            cell("2")(List(btnRemove("btn", () => eventSource.addUndo(srcId))))
          )
        }
      ))
    ))
  }

  private def saveAction()() = {
    eventSource.addRequest()
    currentVDom.invalidate()
  }

  private def toolbar() = {
    paperWithMargin("toolbar", table("table", Nil, List(row("1",
      //cell("1")(List(btnRaised("boats","Boats")(()⇒currentVDom.relocate("/boatList")))),
      cell("2")(List(btnRaised("entries","Entries")(()=>currentVDom.relocate("/entryList")))),
      cell("3")(
        if(eventSource.unmergedEvents.isEmpty) Nil else List(
          btnRaised("events","Events")(()⇒currentVDom.relocate("/eventList")),
          btnRaised("save","Save")(saveAction())
        )
      )
    ))))
  }

  private def calcWorkDuration(on: Boolean, work: Obj): Unit = {
    work(logAt.workDuration) = if(!on) None else
      Option(Duration.between(work(logAt.workStart).get, work(logAt.workStop).get))
  }
  private def calcEntryDuration(on: Boolean, work: Obj): Unit = {
    val entry = work(logAt.entryOfWork)
    val was = entry(logAt.durationTotal).getOrElse(Duration.ofSeconds(0L))
    val delta = work(logAt.workDuration).get
    entry(logAt.durationTotal) =
      Option(if(on) was.plus(delta) else was.minus(delta))
  }
  private def setEntryConfirmDate(on: Boolean, entry: Obj): Unit={
    if(on){
      entry(logAt.confirmedOn)=Option(Instant.now())
    }
  }
  def handlers = CoHandler(ViewPath(""))(emptyView) ::
    CoHandler(ViewPath("/eventList"))(eventListView) ::
    CoHandler(ViewPath("/entryList"))(entryListView) ::
    CoHandler(ViewPath("/entryEdit"))(entryEditView) ::
    CoHandler(ApplyEvent(logAt.entryCreated))(entryCreated) ::
    CoHandler(ApplyEvent(logAt.entryRemoved))(entryRemoved) ::
    CoHandler(ApplyEvent(logAt.entryConfirmed))(entryConfirmed)::
    CoHandler(ApplyEvent(logAt.entryReopened))(entryReopened)::
    CoHandler(ApplyEvent(logAt.workCreated))(workCreated) ::
    CoHandler(ApplyEvent(logAt.workRemoved))(workRemoved) ::
    onUpdate.handlers(List(logAt.asWork,logAt.workStart,logAt.workStop), calcWorkDuration) :::
    onUpdate.handlers(List(logAt.asWork,logAt.workDuration,logAt.entryOfWork), calcEntryDuration) :::
    onUpdate.handlers(List(logAt.asEntry,logAt.asConfirmed), setEntryConfirmDate) :::
    Nil
}
