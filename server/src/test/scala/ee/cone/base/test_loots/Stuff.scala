package ee.cone.base.test_loots

import java.time.{Duration, Instant}
import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.db._
import ee.cone.base.server.SenderOfConnection
import ee.cone.base.util.Never
import ee.cone.base.vdom._

class FailOfConnection(
  sender: SenderOfConnection
) extends CoHandlerProvider {
  def handlers = CoHandler(FailEventKey){ e =>
    println(e.toString)
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
  durationValueConverter: RawValueConverter[Option[Duration]]
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

  val log00Date: Attr[String] = attr(new PropId(0x6710), stringValueConverter),
  val log00Fuel: Attr[String] = attr(new PropId(0x6711), stringValueConverter),
  val log00Comment: Attr[String] = attr(new PropId(0x6712), stringValueConverter),
  val log00Engineer: Attr[String] = attr(new PropId(0x6713), stringValueConverter),
  val log00Master: Attr[String] = attr(new PropId(0x6714), stringValueConverter),
  val log08Date: Attr[String] = attr(new PropId(0x6715), stringValueConverter),
  val log08Fuel: Attr[String] = attr(new PropId(0x6716), stringValueConverter),
  val log08Comment: Attr[String] = attr(new PropId(0x6717), stringValueConverter),
  val log08Engineer: Attr[String] = attr(new PropId(0x6718), stringValueConverter),
  val log08Master: Attr[String] = attr(new PropId(0x6719), stringValueConverter),
  val logRFFuel: Attr[String] = attr(new PropId(0x670B), stringValueConverter),
  val logRFComment: Attr[String] = attr(new PropId(0x670C), stringValueConverter),
  val logRFEngineer: Attr[String] = attr(new PropId(0x670D), stringValueConverter), // 0x670A,0x670E,0x670F
  val log24Date: Attr[String] = attr(new PropId(0x671A), stringValueConverter),
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
  val targetEntryOfWork: Attr[Option[UUID]] = attr(new PropId(0x6728), uuidValueConverter)
)(val handlers: List[BaseCoHandler] =
  searchIndex.handlers(asEntry, justIndexed) :::
  searchIndex.handlers(asWork, entryOfWork)
) extends CoHandlerProvider

class TestComponent(
  at: TestAttributes,
  logAt: BoatLogEntryAttributes,
  alienAccessAttrs: AlienAccessAttrs,
  handlerLists: CoHandlerLists,
  findNodes: FindNodes, uniqueNodes: UniqueNodes, mainTx: CurrentTx[MainEnvKey],
  alienAttr: AlienAttrFactory,
  tags: Tags,
  materialTags: MaterialTags,
  currentVDom: CurrentVDom
) extends CoHandlerProvider {
  import tags._
  import materialTags._
  private def eventSource = handlerLists.single(SessionEventSource)

  private def toAlienText[Value](obj: Obj, attr: Attr[Value], valueToText: Value⇒String ): List[ChildPair[OfDiv]] =
    if(obj.nonEmpty) List(text("1",valueToText(obj(attr)))) else Nil
  private def strField(obj: Obj, attr: Attr[String], editable: Boolean): List[ChildPair[OfDiv]] =
    if(!obj.nonEmpty) Nil
    else if(!editable) List(text("1",obj(attr)))
    else {
      val srcId = obj(uniqueNodes.srcId).get
      List(textInput("1",""/*todo label*/, obj(attr), alienAttr(attr)(srcId)))
    }
  private def durationField(obj: Obj, attr: Attr[Option[Duration]]): List[ChildPair[OfDiv]] =
    toAlienText[Option[Duration]](obj,attr,v⇒v.map(_.getSeconds.toString).getOrElse(""))

  private def instantField(obj: Obj, attr: Attr[Option[Instant]], editable: Boolean): List[ChildPair[OfDiv]] =
    if(!obj.nonEmpty) Nil
    else if(!editable) obj(attr).map(v⇒text("1",v.getEpochSecond.toString)).toList
    else {
      val srcId = obj(uniqueNodes.srcId).get
      List(dateInput("1","",obj(attr),alienAttr(attr)(srcId)))
    }

  private def objField(obj: Obj, attr: Attr[Obj], editable: Boolean): List[ChildPair[OfDiv]] =
    toAlienText[Obj](obj,attr,v⇒if(v.nonEmpty) v(at.caption) else "")



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

/*
  private def createTaskAction()() = {
    eventSource.addEvent{ ev =>
      ev(alienAccessAttrs.targetSrcId) = Option(UUID.randomUUID)
      (at.taskCreated, "task was created")
    }
    currentVDom.invalidate()
  }*/


  private def filterListAct()() = ???
  private def clearSortingAct()() = ???
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
  private def entryReopenAct(entrySrcId: UUID)() = ???
  private def entryConfirmAct(entrySrcId: UUID)() = ???
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
    work(logAt.entryOfWork) = uniqueNodes.noNode
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

  private def listView(pf: String) = wrapDBView{ ()=> root(List(
    //class LootsBoatLogList
    withMargin("margin",10,paper("paper",table("table",
      List(
        row("filter",
          cell("filter", isHead = true, isRight = true, colSpan = 7)(List(
            withMargin("list", 10, btnFilterList("btn",filterListAct())),
            withMargin("clear",10, btnClear("btn",clearSortingAct()))
          ))
        ),
        row("headers",
            cell("1", isHead = true, isRight = false)(List(text("1","Boat"))),
            cell("2", isHead = true, isRight = true)(List(text("1","Date"))),
            cell("3", isHead = true, isRight = true)(List(text("1","Total duration, hrs:min"))),
            cell("4", isHead = true, isRight = false)(List(text("1","Confirmed"))),
            cell("5", isHead = true, isRight = false)(List(text("1","Confirmed by"))),
            cell("6", isHead = true, isRight = true)(List(text("1","Confirmed on"))),
            cell("7", isHead = true)(List(btnAdd("btn",entryAddAct())))
        )
      ),
      //row("empty-test-row",cell("empty-test-cell")(List(text("empty-test-text","test")))) ::
      entryList().map{ (entry:Obj)=>
        val srcId = entry(uniqueNodes.srcId).get
        val go = Some(()⇒ currentVDom.relocate(s"/edit/$srcId"))
        row(srcId.toString,
          cell("1", isRight = false)(objField(entry, logAt.boat, editable = false), go),
          cell("2", isRight = true)(instantField(entry, logAt.date, editable = false), go),
          cell("3", isRight = true)(durationField(entry, logAt.durationTotal), go),
          cell("4", isRight = false)({
              val confirmed = entry(logAt.asConfirmed)
              if(confirmed.nonEmpty) List(text("1","CONFIRMED")) else Nil //todo: MaterialChip
          }, go),
          cell("5", isRight = false)(objField(entry, logAt.confirmedBy, editable = false), go),
          cell("6", isRight = true)(instantField(entry, logAt.confirmedOn, editable = false), go),
          cell("7", isRight = false)(List(btnRemove("btn",entryRemoveAct(srcId))))
        )
      }
    )))
  ))}

  private def editView(pf: String) = wrapDBView{ ()=>
    println(pf)
    val srcId = UUID.fromString(pf.tail)
    val entry = uniqueNodes.whereSrcId(mainTx(), srcId)(logAt.asEntry)
    val editable = true /*todo rw rule*/
    root(List(
      withMargin(s"$srcId-1",10,paper("paper",table("table", Nil, List(
        row("1",
          cell("boat_c")(List(text("1","Boat"))),
          cell("boat_i")(objField(entry, logAt.boat, editable)/*todo select */),
          cell("date_c")(List(text("1","Date"))),
          cell("date_i")(instantField(entry, logAt.date, editable/*todo date */)),
          cell("dur_c")(List(text("1","Total duration, hrs:min"))),
          cell("dur_i")(durationField(entry, logAt.durationTotal))
        ),
        row("2",
          cell("conf_by_c")(List(text("1","Confirmed by"))),
          cell("conf_by_i")(objField(entry, logAt.confirmedBy, editable)),
          cell("conf_on_c")(List(text("1","Confirmed on"))),
          cell("conf_on_i")(instantField(entry, logAt.confirmedOn, editable)),
          cell("conf_do"){
            if(!entry.nonEmpty) Nil
            else if(entry(logAt.asConfirmed).nonEmpty)
              List(btnRaised("reopen","Reopen")(entryReopenAct(srcId)))
            else
              List(btnRaised("confirm","Confirm")(entryConfirmAct(srcId)))
          }
        )
      )))),
      withMargin(s"$srcId-2",10,paper("paper",table("table",
        List(row("1",
          cell("1", isHead = true)(List(text("1","Time"))), /* todo widths */
          cell("2", isHead = true)(List(text("1","ME Hours.Min"))),
          cell("3", isHead = true)(List(text("1","Fuel rest/quantity"))),
          cell("4", isHead = true)(List(text("1","Comment"))),
          cell("5", isHead = true)(List(text("1","Engineer"))),
          cell("6", isHead = true)(List(text("1","Master")))
        )),
        List(
          row("2",
            cell("1")(List(text("1","00:00"))),
            cell("2")(strField(entry, logAt.log00Date, editable)),
            cell("3")(strField(entry, logAt.log00Fuel, editable)),
            cell("4")(strField(entry, logAt.log00Comment, editable)),
            cell("5")(strField(entry, logAt.log00Engineer, editable)),
            cell("6")(strField(entry, logAt.log00Master, editable))
          ),
          row("3",
            cell("1")(List(text("1","08:00"))),
            cell("2")(strField(entry, logAt.log08Date, editable)),
            cell("3")(strField(entry, logAt.log08Fuel, editable)),
            cell("4")(strField(entry, logAt.log08Comment, editable)),
            cell("5")(strField(entry, logAt.log08Engineer, editable)),
            cell("6")(strField(entry, logAt.log08Master, editable))
          ),
          row("4",
            cell("1",colSpan=2)(List(text("1","Received / passed fuel"))),
            cell("3")(strField(entry, logAt.logRFFuel, editable)),
            cell("4")(strField(entry, logAt.logRFComment, editable)),
            cell("5")(strField(entry, logAt.logRFEngineer, editable))
          ),
          row("5",
            cell("1")(List(text("1","24:00"))),
            cell("2")(strField(entry, logAt.log24Date, editable)),
            cell("3")(strField(entry, logAt.log24Fuel, editable)),
            cell("4")(strField(entry, logAt.log24Comment, editable)),
            cell("5")(strField(entry, logAt.log24Engineer, editable)),
            cell("6")(strField(entry, logAt.log24Master, editable))
          )
        )
      ))),
      withMargin("3",10,paper("paper",table("table",
        List(
          row("headers",
            cell("1", isHead = true, isRight = true)(List(text("1","Start"))),
            cell("2", isHead = true, isRight = true)(List(text("1","Stop"))),
            cell("3", isHead = true, isRight = true)(List(text("1","Duration, hrs:min"))),
            cell("4", isHead = true)(List(text("1","Comment"))),
            cell("5", isHead = true)(List(btnAdd("btn",workAddAct(srcId))))
          )
        ),
        workList(entry).map { (work: Obj) =>
          val workSrcId = entry(uniqueNodes.srcId).get
          row(workSrcId.toString,
            cell("1", isRight = true)(instantField(work, logAt.workStart, editable)),
            cell("2", isRight = true)(instantField(work, logAt.workStop, editable)),
            cell("3", isRight = true)(durationField(work, logAt.workDuration)),
            cell("4")(strField(work, logAt.workComment, editable)),
            cell("5")(if(editable) List(btnAdd("btn",workRemoveAct(workSrcId))) else Nil)
          )
        }
      )))
    ))
  }

  def handlers = CoHandler(ViewPath(""))(emptyView) ::
    CoHandler(ViewPath("/list"))(listView) ::
    CoHandler(ViewPath("/edit"))(editView) :: //todo redir
    CoHandler(ApplyEvent(logAt.entryCreated))(entryCreated) ::
    CoHandler(ApplyEvent(logAt.entryRemoved))(entryRemoved) ::
    CoHandler(ApplyEvent(logAt.workCreated))(workCreated) ::
    CoHandler(ApplyEvent(logAt.workRemoved))(workRemoved) ::
    Nil
}
