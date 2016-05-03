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
  //val boat:Attr[String]=attr(new PropId(0x6701),stringValueConverter),
  val date: Attr[Option[Instant]] = attr(new PropId(0x6702), instantValueConverter),
  val durationTotal: Attr[Option[Duration]] = attr(new PropId(0x6703), durationValueConverter),
  val asConfirmed: Attr[Obj] = label(0x6704),
  val confirmedBy: Attr[Obj] = attr(new PropId(0x6705), nodeValueConverter),
  //val confirmedBy: Attr[String] = attr(new PropId(0x6705), stringValueConverter),
  val confirmedOn: Attr[Option[Instant]] = attr(new PropId(0x6706), instantValueConverter), //0x6709
  val entryCreated: Attr[Boolean] = attr(new PropId(0x6707), definedValueConverter),
  val entryRemoved: Attr[Boolean] = attr(new PropId(0x6708), definedValueConverter),

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

  val asUser: Attr[Obj] = label(0x6729),
  val userCreated: Attr[Boolean] = attr(new PropId(0x672A), definedValueConverter),
  val userRemoved: Attr[Boolean] = attr(new PropId(0x672B), definedValueConverter),
  val asBoat: Attr[Obj] = label(0x672C),
  val boatCreated: Attr[Boolean] = attr(new PropId(0x672D), definedValueConverter),
  val boatRemoved: Attr[Boolean] = attr(new PropId(0x672E), definedValueConverter),

  val targetStringValue: Attr[String] = attr(new PropId(0x6730), stringValueConverter),
  val targetInstantValue: Attr[Option[Instant]] = attr(new PropId(0x6731), instantValueConverter),

  val entryConfirmed: Attr[Boolean] = attr(new PropId(0x6732),definedValueConverter),
  val entryReopened: Attr[Boolean] = attr(new PropId(0x6733),definedValueConverter)

)(val handlers: List[BaseCoHandler] =
  searchIndex.handlers(asEntry, justIndexed) :::
  searchIndex.handlers(asWork, entryOfWork) :::
  alienCanChange.update(targetInstantValue)(date) :::
    alienCanChange.update(targetInstantValue)(log00Date):::
    alienCanChange.update(targetInstantValue)(log08Date):::
    alienCanChange.update(targetInstantValue)(log24Date):::
  List(
    /*log00Date,*/log00Fuel,log00Comment,log00Engineer,log00Master,
    /*log08Date,*/log08Fuel,log08Comment,log08Engineer,log08Master,
    logRFFuel,logRFComment,logRFEngineer,
    /*log24Date,*/log24Fuel,log24Comment,log24Engineer,log24Master
  ).flatMap(alienCanChange.update(targetStringValue)(_)) :::
    alienCanChange.update(targetInstantValue)(workStart) :::
    alienCanChange.update(targetInstantValue)(workStop) :::
    alienCanChange.update(targetStringValue)(workComment)
) extends CoHandlerProvider

trait ItemList {
  def add(srcId: UUID): Unit
  def list(): List[Obj]
  def select(srcId: UUID, on: Boolean)
  def selectAll(on: Boolean): Unit
  def removeSelected(): Unit
}

class ItemListImpl[Value](
  asType: Attr[Obj],
  atParent: Attr[Value],
  parent: Value,
  atCreated: Attr[Boolean]
)(
  alienAccessAttrs: AlienAccessAttrs,
  handlerLists: CoHandlerLists,
  findNodes: FindNodes, uniqueNodes: UniqueNodes, mainTx: CurrentTx[MainEnvKey]
) extends ItemList with CoHandlerProvider {
  private def eventSource = handlerLists.single(SessionEventSource)
  def add(srcId: UUID) = {
    handlerLists.single(AddCreateEvent(atCreated, asType.defined))(srcId)
    handlerLists.single(AddUpdateEvent(atParent))(srcId, parent)
  }
  def list() = findNodes.where(mainTx(), asType.defined, atParent, parent, Nil)


  // val srcId = UUID.randomUUID

  //  item(atParent) = uniqueNodes.whereSrcId(mainTx(), ev(targetParent).get)


  def removeSelected() = ???

  def selectAll(on: Boolean) = ???

  def select(srcId: UUID, on: Boolean) = ???

  def handlers = ???
}




class DataTablesState(currentVDom: CurrentVDom){
  val dtTableWidths=scala.collection.mutable.Map[VDomKey,Float]()
  val dtTableCheckAll=scala.collection.mutable.Map[VDomKey,Boolean]()
  val dtTableCheck=scala.collection.mutable.Map[VDomKey,Boolean]()
  val dtTableToggleRecordRow=scala.collection.mutable.Map[VDomKey,Boolean]()

  def handleResize(id:VDomKey,cWidth:Float)={
    dtTableWidths(id)=cWidth

    currentVDom.invalidate()
  }
  def handleCheckAll(id:VDomKey,checked:Boolean): Unit ={
    dtTableCheckAll(id)=checked
    val selKeys=dtTableCheck.filter{case(k,v)=>k.indexOf(id)==0}.keySet
    selKeys.foreach(k=>dtTableCheck(k)=checked)
    currentVDom.invalidate()
  }
  def handleCheck(id:VDomKey,checked:Boolean)={
    dtTableCheck(id)=checked
    currentVDom.invalidate()
  }
  def handleToggle(id:VDomKey)={
    //println("toggle",id)
    val newVal=true
    dtTableToggleRecordRow(id)=newVal
    dtTableToggleRecordRow.foreach{case (k,v)=>if(k!=id&&newVal)dtTableToggleRecordRow(k)=false}
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

  private def eventSource = handlerLists.single(SessionEventSource)

  private def forUpdate(obj: Obj)(create: ()⇒UUID = ()⇒Never()): Obj = {
    val existingSrcId = if(obj.nonEmpty) Some(obj(uniqueNodes.srcId).get) else None
    lazy val justCreated = create()
    def srcId = existingSrcId.getOrElse(justCreated)
    new Obj {
      def apply[Value](attr: Attr[Value]) = obj(attr)
      def update[Value](attr: Attr[Value], value: Value) = {
        handlerLists.single(AddUpdateEvent(attr))(srcId,value)
        currentVDom.invalidate()
      }
      def nonEmpty = Never()
      def tx = Never()
    }
  }
  private def lazyLinkingObj[Value](atCreated: Attr[Boolean], asType: Attr[Obj], atKey: Attr[Value], key: Value): Obj = {
    val obj = findNodes.where(mainTx(), asType.defined, atKey, key, Nil) match {
      case Nil ⇒ uniqueNodes.noNode
      case o :: Nil ⇒ o
    }
    forUpdate(obj) { ()⇒
      val srcId = UUID.randomUUID
      handlerLists.single(AddCreateEvent(atCreated, asType.defined))(srcId)
      handlerLists.single(AddUpdateEvent(atKey))(srcId, key)
      srcId
    }
  }
  def filterCreated: Attr[Boolean]
  def asFilter: Attr[Obj]
  def filterFullKey: Attr[String]
  private def filterObj(key: String): Obj = {
    lazyLinkingObj(filterCreated, asFilter, filterFullKey, s"${eventSource.sessionKey}$key")
  }


  private def toAlienText[Value](obj: Obj, attr: Attr[Value], valueToText: Value⇒String,label:Option[String] ): List[ChildPair[OfDiv]] =
    if(!obj.nonEmpty) Nil
    else if(label.isEmpty)
      List(text("1",valueToText(obj(attr))))
    else
      List(labeledText("1",valueToText(obj(attr)),label.getOrElse("")))

  private def strField(obj: Obj, attr: Attr[String], editable: Boolean,label:Option[String] = None): List[ChildPair[OfDiv]] =
    if(!editable) List(text("1",obj(attr)))
    else List(textInput("1",label.getOrElse(""), obj(attr), obj(attr)=_))


  private def durationField(obj: Obj, attr: Attr[Option[Duration]],label:Option[String]=None): List[ChildPair[OfDiv]] = {
    toAlienText[Option[Duration]](obj, attr, v ⇒ v.map(x=>
      x.abs.toHours+"h:"+x.abs.minusHours(x.abs.toHours).toMinutes.toString+"m"
    ).getOrElse(""), label)
  }

  private def dateField(obj: Obj, attr: Attr[Option[Instant]], editable: Boolean,label:Option[String] = None): List[ChildPair[OfDiv]] = {
    //println(attr,obj.nonEmpty,editable)
    if(!editable)
      obj(attr).map(v⇒ {
        val date = LocalDate.from(v.atZone(ZoneId.of("UTC")))
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")

        if (label.isEmpty)
          text("1",date.format(formatter))
        else
          labeledText("1", date.format(formatter), label.getOrElse(""))
      }).toList
    else List(dateInput("1",label.getOrElse(""),obj(attr),obj(attr)=_))
  }
  private def timeField(obj: Obj, attr: Attr[Option[Instant]], editable: Boolean,label:Option[String] = None): List[ChildPair[OfDiv]] = {
    //println(attr,obj.nonEmpty,editable)
    if(!editable) ???
    else List(timeInput("1",label.getOrElse(""),obj(attr),obj(attr)=_))
  }

  private def objField(obj: Obj, attr: Attr[Obj], editable: Boolean): List[ChildPair[OfDiv]] =
    toAlienText[Obj](obj,attr,v⇒if(v.nonEmpty) v(at.caption) else "",None)
  private def objField(obj: Obj, attr: Attr[Obj],tmp:String, editable: Boolean,label:Option[String]): List[ChildPair[OfDiv]] = {
    //toAlienText[Obj](obj,attr,v⇒if(v.nonEmpty) v(at.caption) else "")
    if (!obj.nonEmpty) return Nil
    List(textInput("1", label.getOrElse(""), tmp, (String) => {}, !editable))
  }


  private def userList(): List[Obj] = findNodes.where(
    mainTx(), logAt.asUser.defined,
    logAt.justIndexed, findNodes.justIndexed,
    Nil
  )
  private def boatList(): List[Obj] = findNodes.where(
    mainTx(), logAt.asBoat.defined,
    logAt.justIndexed, findNodes.justIndexed,
    Nil
  )
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
  private def userAddAct()() = {
    eventSource.addEvent{ ev =>
      ev(alienAccessAttrs.targetSrcId) = Option(UUID.randomUUID)
      (logAt.userCreated, "user was created")
    }
    currentVDom.invalidate()
  }
  private def userCreated(ev: Obj): Unit = {
    val srcId = ev(alienAccessAttrs.targetSrcId).get
    val user = uniqueNodes.create(mainTx(), logAt.asUser, srcId)
    user(logAt.justIndexed) = findNodes.justIndexed
  }
  private def userRemoveAct(userSrcId: UUID)() = {
    eventSource.addEvent { ev =>
      ev(alienAccessAttrs.targetSrcId) = Some(userSrcId)
      (logAt.userRemoved, "user was removed")
    }
    currentVDom.invalidate()
  }
  private def userRemoved(ev: Obj): Unit = {
    val user = uniqueNodes.whereSrcId(mainTx(), ev(alienAccessAttrs.targetSrcId).get)
    user(logAt.justIndexed) = ""
  }

  private def boatAddAct()() = {
    eventSource.addEvent{ ev =>
      ev(alienAccessAttrs.targetSrcId) = Option(UUID.randomUUID)
      (logAt.boatCreated, "boat was created")
    }
    currentVDom.invalidate()
  }
  private def boatCreated(ev: Obj): Unit = {
    val srcId = ev(alienAccessAttrs.targetSrcId).get
    val boat = uniqueNodes.create(mainTx(), logAt.asBoat, srcId)
    boat(logAt.justIndexed) = findNodes.justIndexed
  }
  private def boatRemoveAct(boatSrcId: UUID)() = {
    eventSource.addEvent { ev =>
      ev(alienAccessAttrs.targetSrcId) = Some(boatSrcId)
      (logAt.boatRemoved, "boat was removed")
    }
    currentVDom.invalidate()
  }
  private def boatRemoved(ev: Obj): Unit = {
    val boat = uniqueNodes.whereSrcId(mainTx(), ev(alienAccessAttrs.targetSrcId).get)
    boat(logAt.justIndexed) = ""
  }


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
    currentVDom.invalidate()
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
    currentVDom.invalidate()
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
    val dtTable0=new DtTable(dtTablesState.dtTableWidths.getOrElse("dtTableList",0.0f),true,true,true)
    dtTable0.setControls(List(btnDelete("1", ()=>{}),btnAdd("2", entryAddAct())))
    dtTable0.addColumns(List(
      dtTable0.dtColumn("2",1000,"center",0,0,1,None)
    ))

    dtTable0.addHeadersForColumn(
      Map(
        "2"->List(
          dtTable0.dtHeader("2",100,None,1,List(withSideMargin("1",10,divAlignWrapper("1","left","middle",
            List(text("1","Boat")))))),
          dtTable0.dtHeader("3",150,None,1,List(withSideMargin("1",10,divAlignWrapper("1","left","middle",
            List(text("1","Date")))))),
          dtTable0.dtHeader("4",100,None,1,List(withSideMargin("1",10,divAlignWrapper("1","left","middle",
            List(text("1","Total duration, hrs:min")))))),
          dtTable0.dtHeader("5",100,None,3,List(withSideMargin("1",10,divAlignWrapper("1","left","middle",
            List(text("1","Confirmed")))))),
          dtTable0.dtHeader("6",150,None,2,List(withSideMargin("1",10,divAlignWrapper("1","left","middle",
            List(text("1","Confirmed by")))))),
          dtTable0.dtHeader("7",150,None,2,List(withSideMargin("1",10,divAlignWrapper("1","left","middle",
            List(text("1","Confirmed on")))))),
          dtTable0.dtHeader("8",100,None,List(withSideMargin("1",10,divAlignWrapper("1","center","middle",
            Nil))))
        )
      )
    )
  entryList().foreach{ (entry:Obj)=>{
    val entrySrcId = entry(uniqueNodes.srcId).get
    val go = Some(()⇒ currentVDom.relocate(s"/entryEdit/$entrySrcId"))

    dtTable0.addRecordsForColumn(
      Map(
        "2"->List(
          dtTable0.dtRecord("2",List(withSideMargin("1",10,objField(entry, logAt.boat, editable = false)))//,
            //List(withSideMargin("1",10,dateField(work, logAt.workStart, editable,Some("Start"))))
          ),
          dtTable0.dtRecord("3",List(withSideMargin("1",10,divAlignWrapper("1","left","middle",
            dateField(entry, logAt.date, editable = false)))),

            List(withSideMargin("1",10,divAlignWrapper("1","left","middle",
              dateField(entry, logAt.date, editable=false,Some("Date")))))
          ),
          dtTable0.dtRecord("4",List(withSideMargin("1",10,divAlignWrapper("1","left","middle",
            durationField(entry, logAt.durationTotal))))//,
            //List(withSideMargin("1",10,durationField(work, logAt.workDuration,false,Some("Duration hrs:min"))))
          ),
          dtTable0.dtRecord("5",List(withSideMargin("1",10,divAlignWrapper("1","left","middle",{
            val confirmed = entry(logAt.asConfirmed)
            if(confirmed.nonEmpty) List(materialChip("1","CONFIRMED")) else Nil //todo: MaterialChip
            })))//,
            //List(withSideMargin("1",10,strField(entry, logAt.workComment, editable,Some("Comment"))))
          ),

          dtTable0.dtRecord("6",List(withSideMargin("1",10,objField(entry, logAt.confirmedBy, editable = false)))//,
           // List(withSideMargin("1",10,strField(entry, logAt.log00Engineer, editable,Some("Engineer"))))
          ),
          dtTable0.dtRecord("7",List(withSideMargin("1",10,dateField(entry, logAt.confirmedOn, editable = false)))//,
            //List(withSideMargin("1",10,strField(entry, logAt.log00Master, editable,Some("Master"))))
          ),
          dtTable0.dtRecord("8",List(divAlignWrapper("1","center","middle",
            List(
              btnRemove("btn1",entryRemoveAct(entrySrcId)),
              btnCreate("btn2",go.get)
            )))
          )
        )
      )
    )
  }}
  root(List(
    //class LootsBoatLogList
    toolbar(),
    withMaxWidth("1",1200,List(
      paperWithMargin("margin",flexGrid("flexGridList",
        flexGridItemTable("dtTableList","dtTableList",1000,None,dtTable0,dtTablesState,Nil)::Nil)

        )
      ))
    )
  )
}}
  private def deleteSelected()={


  }
  private def entryEditView(pf: String) = wrapDBView { () =>
    //println(pf)
    val srcId = UUID.fromString(pf.tail)
    val obj = uniqueNodes.whereSrcId(mainTx(), srcId)
    if(!obj.nonEmpty) root(List(text("text","???")))
    else editViewInner(srcId, forUpdate(obj(logAt.asEntry))())
  }


  private def editViewInner(srcId: UUID, entry: Obj) = {
    val editable = true /*todo rw rule*/
    val dtTable1=new DtTable(dtTablesState.dtTableWidths.getOrElse("dtTableEdit1",0.0f),false,false,false)
    dtTable1.addColumns(List(
      dtTable1.dtColumn("2",1000,"center",0,0,1,None)
    ))

    dtTable1.addHeadersForColumn(
      Map(
        "2"->List(
          dtTable1.dtHeader("2",100,None,3,List(withSideMargin("1",10,divAlignWrapper("1","left","middle",List(text("1","Time")))))),
          dtTable1.dtHeader("3",150,None,1,List(withSideMargin("1",10,divAlignWrapper("1","left","middle",List(text("1","ME Hours.Min")))))),
          dtTable1.dtHeader("4",100,None,1,List(withSideMargin("1",10,divAlignWrapper("1","left","middle",List(text("1","Fuel rest/quantity")))))),
          dtTable1.dtHeader("5",250,None,3,List(withSideMargin("1",10,divAlignWrapper("1","left","middle",List(text("1","Comment")))))),
          dtTable1.dtHeader("6",150,None,2,List(withSideMargin("1",10,divAlignWrapper("1","left","middle",List(text("1","Engineer")))))),
          dtTable1.dtHeader("7",150,None,2,List(withSideMargin("1",10,divAlignWrapper("1","left","middle",List(text("1","Master"))))))//,
        //  dtTable1.dtHeader("8",50,None,List())
        )
      )
    )

    dtTable1.addRecordsForColumn(
      Map(
        "2"->List(
          dtTable1.dtRecord("2",List(withSideMargin("1",10,divAlignWrapper("1","left","middle",List(text("1","00:00"))))),
            List(withSideMargin("1",10,divAlignWrapper("1","left","middle",List(labeledText("1","00:00","Time")))))
          ),
          dtTable1.dtRecord("3",List(withSideMargin("1",10,timeField(entry, logAt.log00Date, editable, None)))//,
            //List(withSideMargin("1",10,strField(entry, logAt.log00Date, editable,Some("ME Hours.Min"))))
          ),
          dtTable1.dtRecord("4",List(withSideMargin("1",10,strField(entry, logAt.log00Fuel, editable))),
            List(withSideMargin("1",10,strField(entry, logAt.log00Fuel, editable,Some("Fuel rest/quantity"))))
          ),
          dtTable1.dtRecord("5",List(withSideMargin("1",10,strField(entry, logAt.log00Comment, editable))),
            List(withSideMargin("1",10,strField(entry, logAt.log00Comment, editable,Some("Comment"))))),
          dtTable1.dtRecord("6",List(withSideMargin("1",10,strField(entry, logAt.log00Engineer, editable))),
            List(withSideMargin("1",10,strField(entry, logAt.log00Engineer, editable,Some("Engineer"))))
          ),
          dtTable1.dtRecord("7",List(withSideMargin("1",10,strField(entry, logAt.log00Master, editable))),
            List(withSideMargin("1",10,strField(entry, logAt.log00Master, editable,Some("Master"))))
          )//,
         // dtTable1.dtRecord("8",List(divAlignWrapper("1","center","middle",List(text("1","+")))))

        )
      )
    )
    dtTable1.addRecordsForColumn(
      Map(
        "2"->List(
          dtTable1.dtRecord("2",List(withSideMargin("1",10,divAlignWrapper("1","left","middle",List(text("1","08:00"))))),
            List(withSideMargin("1",10,divAlignWrapper("1","left","middle",List(labeledText("1","08:00","Time")))))
          ),
          dtTable1.dtRecord("3",List(withSideMargin("1",10,timeField(entry, logAt.log08Date, editable, None)))//,
           // List(withSideMargin("1",10,strField(entry, logAt.log08Date, editable,Some("ME Hours.Min"))))
          ),
          dtTable1.dtRecord("4",List(withSideMargin("1",10,strField(entry, logAt.log08Fuel, editable))),
            List(withSideMargin("1",10,strField(entry, logAt.log08Fuel, editable,Some("Fuel rest/quantity"))))
          ),
          dtTable1.dtRecord("5",List(withSideMargin("1",10,strField(entry, logAt.log08Comment, editable))),
            List(withSideMargin("1",10,strField(entry, logAt.log08Comment, editable,Some("Comment"))))
          ),
          dtTable1.dtRecord("6",List(withSideMargin("1",10,strField(entry, logAt.log08Engineer, editable))),
            List(withSideMargin("1",10,strField(entry, logAt.log08Engineer, editable,Some("Engineer"))))
          ),
          dtTable1.dtRecord("7",List(withSideMargin("1",10,strField(entry, logAt.log08Master, editable))),
            List(withSideMargin("1",10,strField(entry, logAt.log08Master, editable,Some("Master"))))
          )//,
          //dtTable1.dtRecord("8",List(divAlignWrapper("1","center","middle",List(text("1","+")))))
        )
      )
    )
    dtTable1.addRecordsForColumn(
      Map(
        "2"->List(
          dtTable1.dtRecord("2",List(withSideMargin("1",10,divAlignWrapper("1","left","middle",List(text("1","Passed"))))),
            List(withSideMargin("1",10,divAlignWrapper("1","left","middle",List(text("1","Passed")))))
          ),
          dtTable1.dtRecord("3",List(withSideMargin("1",10,divAlignWrapper("1","left","middle",List(text("1","Received Fuel"))))),
            List(withSideMargin("1",10,divAlignWrapper("1","left","middle",List(text("1","Received Fuel")))))
          ),
          dtTable1.dtRecord("4",List(withSideMargin("1",10,strField(entry, logAt.logRFFuel, editable))),
            List(withSideMargin("1",10,strField(entry, logAt.logRFFuel, editable,Some("Fuel rest/quantity"))))
          ),
          dtTable1.dtRecord("5",List(withSideMargin("1",10,strField(entry, logAt.logRFComment, editable))),
            List(withSideMargin("1",10,strField(entry, logAt.logRFComment, editable,Some("Comment"))))
          ),
          dtTable1.dtRecord("6",List(withSideMargin("1",10,strField(entry, logAt.logRFEngineer, editable))),
            List(withSideMargin("1",10,strField(entry, logAt.logRFEngineer, editable,Some("Engineer"))))
          ),
          dtTable1.dtRecord("7",List())//,
         // dtTable1.dtRecord("8",List(divAlignWrapper("1","center","middle",List(text("1","+")))))
        )
      )
    )
    dtTable1.addRecordsForColumn(
      Map(
        "2"->List(
          dtTable1.dtRecord("2",List(withSideMargin("1",10,divAlignWrapper("1","left","middle",List(text("1","24:00"))))),
            List(withSideMargin("1",10,divAlignWrapper("1","left","middle",List(labeledText("1","24:00","Time")))))
          ),
          dtTable1.dtRecord("3",List(withSideMargin("1",10,timeField(entry, logAt.log24Date, editable, None)/*strField(entry, logAt.log24Date, editable)*/))//,
            //List(withSideMargin("1",10,strField(entry, logAt.log24Date, editable,Some("ME Hours.Min"))))
          ),
          dtTable1.dtRecord("4",List(withSideMargin("1",10,strField(entry, logAt.log24Fuel, editable))),
            List(withSideMargin("1",10,strField(entry, logAt.log24Fuel, editable,Some("Fuel rest/quantity"))))
          ),
          dtTable1.dtRecord("5",List(withSideMargin("1",10,strField(entry, logAt.log24Comment, editable))),
            List(withSideMargin("1",10,strField(entry, logAt.log24Comment, editable,Some("Comment"))))
          ),
          dtTable1.dtRecord("6",List(withSideMargin("1",10,strField(entry, logAt.log24Engineer, editable))),
            List(withSideMargin("1",10,strField(entry, logAt.log24Engineer, editable,Some("Engineer"))))
          ),
          dtTable1.dtRecord("7",List(withSideMargin("1",10,strField(entry, logAt.log24Master, editable))),
            List(withSideMargin("1",10,strField(entry, logAt.log24Master, editable,Some("Master"))))
          )//,
          //dtTable1.dtRecord("8",List(divAlignWrapper("1","center","middle",List(text("1","+")))))
        )
      )
    )
    val dtTable2=new DtTable(dtTablesState.dtTableWidths.getOrElse("dtTableEdit2",0.0f),true,true,true)
    dtTable2.setControls(List(btnDelete("1", ()=>{}),btnAdd("2", workAddAct(srcId))))
    dtTable2.addColumns(List(
      dtTable2.dtColumn("2",1000,"center",0,20,1,None)
    ))

    dtTable2.addHeadersForColumn(
      Map(
        "2"->List(
          dtTable2.dtHeader("2",100,None,1,List(withSideMargin("1",10,divAlignWrapper("1","left","middle",
            List(text("1","Start")))))),
          dtTable2.dtHeader("3",100,None,1,List(withSideMargin("1",10,divAlignWrapper("1","left","middle"
            ,List(text("1","Stop")))))),
          dtTable2.dtHeader("4",150,None,1,List(withSideMargin("1",10,divAlignWrapper("1","left","middle",
            List(text("1","Duration, hrs:min")))))),
          dtTable2.dtHeader("5",250,None,3,List(withSideMargin("1",10,divAlignWrapper("1","left","middle",
            List(text("1","Comment")))))),


          dtTable2.dtHeader("8",50,None,List(withSideMargin("1",10,divAlignWrapper("1","center","middle",
            Nil))))
        )
      )
    )
    workList(entry).foreach { (obj: Obj) =>
      val work = forUpdate(obj)()
      val workSrcId = work(uniqueNodes.srcId).get


    dtTable2.addRecordsForColumn(
      Map(
        "2"->List(
          dtTable2.dtRecord("2",List(withSideMargin("1",10,timeField(work, logAt.workStart, editable, None)))//,
            //List(withSideMargin("1",10,dateField(work, logAt.workStart, editable,Some("Start"))))
          ),
          dtTable2.dtRecord("3",List(withSideMargin("1",10,timeField(work, logAt.workStop, editable, None)))//,
            //List(withSideMargin("1",10,dateField(work, logAt.workStop, editable,Some("Stop"))))
          ),
          dtTable2.dtRecord("4",List(withSideMargin("1",10,divAlignWrapper("1","left","middle",durationField(work, logAt.workDuration)))),
            List(withSideMargin("1",10,divAlignWrapper("1","left","middle",
              durationField(work, logAt.workDuration,Some("Duration hrs:min")))))
          ),
          dtTable2.dtRecord("5",List(withSideMargin("1",10,strField(entry, logAt.workComment, editable))),
            List(withSideMargin("1",10,strField(entry, logAt.workComment, editable,Some("Comment"))))),

          dtTable2.dtRecord("8",List(withSideMargin("1",10,List(divAlignWrapper("1","center","middle",if(editable)
            List(btnRemove("btn",workRemoveAct(workSrcId))) else Nil)))))

        )
      )
    )

    }

    root(List(
      toolbar(),
      withMaxWidth("1",1200,List(
      paperWithMargin(s"$srcId-1",
        flexGrid("flexGridEdit1",List(
          flexGridItem("1",500,None,List(
            flexGrid("FlexGridEdit11",List(
              flexGridItem("boat",150,None,objField(entry,logAt.boat,"Boat-A01",false,Some("Boat"))),
              flexGridItem("date",150,None,dateField(entry, logAt.date, editable,Some("Date")/*todo date */)),
              flexGridItem("dur",170,None,List(divAlignWrapper("1","left","middle",
                durationField(entry,logAt.durationTotal,Some("Total duration, hrs:min")))))
            ))
          )),
          flexGridItem("2",500,None,List(
            flexGrid("flexGridEdit12",List(
              flexGridItem("conf_by",150,None,objField(entry,logAt.confirmedBy,"",editable = false,Some("Confirmed by"))),
              flexGridItem("conf_on",150,None,dateField(entry, logAt.confirmedOn, editable = false,Some("Confirmed on")/*todo date */)),
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
        flexGridItemTable("dtTableEdit1","dtTableEdit1",1000,None,dtTable1,dtTablesState,Nil)::Nil)


      ))),
      withMaxWidth("3",1200,List(
      paperWithMargin(s"$srcId-3",flexGrid("flexGridEdit3",
        flexGridItemTable("dtTableEdit2","dtTableEdit2",1000,None,dtTable2,dtTablesState,Nil)::Nil)

      )))
    ))
  }

  private def userListView(pf: String) = wrapDBView { () =>



    root(List(
      toolbar(),
      btnAdd("2", userAddAct()),
      paperWithMargin("margin",table("table",
        List(
          row("head",
            cell("0", isHead=true, isUnderline = true)(List(checkBox("1", ))),
            cell("1", isHead=true, isUnderline = true)(List(text("text", "Full Name")))
          )
        ),
        userList().map{ obj ⇒
          val user = forUpdate(obj)()
          val srcId = user(uniqueNodes.srcId).get
          row(srcId.toString,
            cell("0")(List(checkBox("1", ))),
            cell("1")(strField(user,at.caption,editable = true))
          )
        }
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
      cell("0")(List(btnRaised("users","Users")(()⇒currentVDom.relocate("/userList")))),
      cell("1")(List(btnRaised("boats","Boats")(()⇒currentVDom.relocate("/boatList")))),
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

  def handlers = CoHandler(ViewPath(""))(emptyView) ::
    CoHandler(ViewPath("/eventList"))(eventListView) ::
    CoHandler(ViewPath("/userList"))(userListView) ::
    CoHandler(ViewPath("/boatList"))(boatListView) ::
    CoHandler(ViewPath("/entryList"))(entryListView) ::
    CoHandler(ViewPath("/entryEdit"))(entryEditView) ::
    CoHandler(ApplyEvent(logAt.userCreated))(userCreated) ::
    CoHandler(ApplyEvent(logAt.userRemoved))(userRemoved) ::
    CoHandler(ApplyEvent(logAt.boatCreated))(boatCreated) ::
    CoHandler(ApplyEvent(logAt.boatRemoved))(boatRemoved) ::
    CoHandler(ApplyEvent(logAt.entryCreated))(entryCreated) ::
    CoHandler(ApplyEvent(logAt.entryRemoved))(entryRemoved) ::
    CoHandler(ApplyEvent(logAt.entryConfirmed))(entryConfirmed)::
    CoHandler(ApplyEvent(logAt.entryReopened))(entryReopened)::
    CoHandler(ApplyEvent(logAt.workCreated))(workCreated) ::
    CoHandler(ApplyEvent(logAt.workRemoved))(workRemoved) ::
    onUpdate.handlers(List(logAt.asWork,logAt.workStart,logAt.workStop), calcWorkDuration) :::
    onUpdate.handlers(List(logAt.asWork,logAt.workDuration,logAt.entryOfWork), calcEntryDuration) :::
    Nil
}
