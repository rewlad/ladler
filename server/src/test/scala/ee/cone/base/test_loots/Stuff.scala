package ee.cone.base.test_loots

import java.time.{Duration, Instant}
import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.db._
import ee.cone.base.server.SenderOfConnection
import ee.cone.base.vdom._

class FailOfConnection(
  sender: SenderOfConnection
) extends CoHandlerProvider {
  def handlers = CoHandler(FailEventKey){ e =>
    println(e.toString)
    sender.sendToAlien("fail",e.toString) //todo
  } :: Nil
}

class TestAttributes(
  attr: AttrFactory,
  label: LabelFactory,
  strValueConverter: RawValueConverter[String]
)(
  val caption: Attr[String] = attr(new PropId(0x6800), strValueConverter)
)

class BoatLogEntryAttributes(
  attr: AttrFactory,
  label: LabelFactory,
  nodeValueConverter: RawValueConverter[Obj],
  instantValueConverter: RawValueConverter[Option[Instant]], //todo
  durationValueConverter: RawValueConverter[Option[Duration]] //todo
)(
  val asEntry: Attr[Obj] = attr(new PropId(0x6700), nodeValueConverter),
  val boat: Attr[Obj] = attr(new PropId(0x6701), nodeValueConverter),
  val date: Attr[Option[Instant]] = attr(new PropId(0x6702), instantValueConverter),
  val durationTotal: Attr[Option[Duration]] = attr(new PropId(0x6703), durationValueConverter),
  val asConfirmed: Attr[Obj] = attr(new PropId(0x6704), nodeValueConverter),
  val confirmedBy: Attr[Obj] = attr(new PropId(0x6705), nodeValueConverter),
  val confirmedOn: Attr[Option[Instant]] = attr(new PropId(0x6706), instantValueConverter)
)

class TestComponent(
  at: TestAttributes,
  logAt: BoatLogEntryAttributes,
  handlerLists: CoHandlerLists,
  uniqueNodes: UniqueNodes, mainTx: CurrentTx[MainEnvKey],
  tags: Tags,
  materialTags: MaterialTags,
  currentVDom: CurrentVDom
) extends CoHandlerProvider {
  import tags._
  import materialTags._
  private def eventSource = handlerLists.single(SessionEventSource)

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
    withMargin("margin",10,paper("paper",table("table", List(
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
          cell("7", isHead = true)(List(btnAdd("btn",addAct())))
      )),
      List(???).map{ (entry:Obj)=>
        val srcId = entry(uniqueNodes.srcId).get
        row(srcId.toString,
          cell("1", isRight = false, content=field(entry, logAt.boat))(),
          cell("2", isRight = true, content=field(entry, logAt.date))(),
          cell("3", isRight = true, content=field(entry, logAt.durationTotal))(),
          cell("4", isRight = false, content={
              val confirmed = entry(logAt.asConfirmed)
              if(confirmed.nonEmpty) "CONFIRMED" else "" //todo: MaterialChip
            })(),
          cell("5", isRight = false, content=field(entry, logAt.confirmedBy))(),
          cell("6", isRight = true, content=field(entry, logAt.confirmedOn))(),
          cell("7", isRight = false)(List(btnRemove("btn",removeAct())))
        )
      }
    )))
  ))}

  private def toAlienText[Value](obj: Obj, attr: Attr[Value], valueToText: Value⇒String ): String =
    if(obj.nonEmpty) valueToText(obj(attr)) else ""

  private def field(obj: Obj, attr: Attr[Obj]): ChildPair[OfDiv] =
    toAlienText[Obj](obj,attr,v⇒if(v.nonEmpty) v(at.caption) else "")
  private def field(obj: Obj, attr: Attr[Option[Duration]]): String =
    toAlienText[Option[Duration]](obj,attr,v⇒v.map(_.getSeconds.toString).getOrElse(""))
  private def field(obj: Obj, attr: Attr[Option[Instant]]): String =
    toAlienText[Option[Instant]](obj,attr,v⇒v.map(_.getEpochSecond.toString).getOrElse(""))

  private def filterListAct()() = ???
  private def clearSortingAct()() = ???
  private def addAct()() = ???
  private def removeAct()() = ???

  private def reopenAct(entrySrcId: UUID)() = ???
  private def confirmAct(entrySrcId: UUID)() = ???

  private def editView(pf: String) = wrapDBView{ ()=>
    println(pf)
    val srcId = UUID.fromString(pf)
    val entry = uniqueNodes.whereSrcId(mainTx(), srcId)(logAt.asEntry)
    root(List(
      withMargin("1",10,paper("paper",table("table", Nil, List(
        row("1",
          cell("boat_c", content = "Boat")(),
          cell("boat_i", content = field(entry, logAt.boat)/*todo select */)(),
          cell("date_c", content = "Date")(),
          cell("date_i", content = field(entry, logAt.date/*todo date */))(),
          cell("dur_c", content = "Total duration, hrs:min")(),
          cell("dur_i", content = field(entry, logAt.durationTotal))()
        ),
        row("2",
          cell("conf_by_c", content = "Confirmed by")(),
          cell("conf_by_i", content = field(entry, logAt.confirmedBy))(),
          cell("conf_on_c", content = "Confirmed on")(),
          cell("conf_on_i", content = field(entry, logAt.confirmedOn))(),
          cell("conf_do"){
            if(!entry.nonEmpty) Nil
            else if(entry(logAt.asConfirmed).nonEmpty)
              List(btnRaised("reopen","Reopen")(reopenAct(srcId)))
            else
              List(btnRaised("confirm","Confirm")(confirmAct(srcId)))
          }
        )
      )))),
      withMargin("2",10,paper("paper",table("table",
        List(row("1",
          cell("1", isHead = true, content = "Time")(), /* todo widths */
          cell("2", isHead = true, content = "ME Hours.Min")(),
          cell("3", isHead = true, content = "Fuel rest/quantity")(),
          cell("4", isHead = true, content = "Comment")(),
          cell("5", isHead = true, content = "Engineer")(),
          cell("6", isHead = true, content = "Master")()
        )),
        List(row(),row(),row(),row())
      ))),
      withMargin("3",10,paper("paper",table("table",
        ???,
        ???
      )))
    ))
  }

  /*

        rows(^M
          row(^M
            cell(sLabel("00:00")),^M
            cell(zk.drawGREFieldAnyView(boatLog.lootsBoatLogZeroDate, this, ro=boatCanNotEdit)),^M
            cell(zk.drawGREFieldAnyView(boatLog.lootsBoatLogZeroFuel, this, ro=boatCanNotEdit)),^M
            cell(zk.drawGREFieldAnyView(boatLog.lootsBoatLogZeroComment, this, ro=boatCanNotEdit)),^M
            cell(zk.drawGREFieldAnyView(boatLog.lootsBoatLogZeroEngineer, this, ro=boatCanNotEdit)),^M
            cell(zk.drawGREFieldAnyView(boatLog.lootsBoatLogZeroMaster, this, ro=boatCanNotEdit))^M
          ),^M
          row(^M
            cell(sLabel("08:00")),^M
            cell(zk.drawGREFieldAnyView(boatLog.lootsBoatLog8Date, this, ro=boatCanNotEdit)),^M
            cell(zk.drawGREFieldAnyView(boatLog.lootsBoatLog8Fuel, this, ro=boatCanNotEdit)),^M
            cell(zk.drawGREFieldAnyView(boatLog.lootsBoatLog8Comment, this, ro=boatCanNotEdit)),^M
            cell(zk.drawGREFieldAnyView(boatLog.lootsBoatLog8Engineer, this, ro=boatCanNotEdit)),^M
            cell(zk.drawGREFieldAnyView(boatLog.lootsBoatLog8Master, this, ro=boatCanNotEdit))^M
          ),^M
          row(^M
            cell(sLabel("Received / passed fuel"), CellColSpanKey(2)),^M
            cell(zk.drawGREFieldAnyView(boatLog.lootsBoatLogPassedFuel, this, ro=boatCanNotEdit)),^M
            cell(zk.drawGREFieldAnyView(boatLog.lootsBoatLogPassedComment, this, ro=boatCanNotEdit)),^M
            cell(zk.drawGREFieldAnyView(boatLog.lootsBoatLogPassedEngineer, this, ro=boatCanNotEdit))^M
          ),^M
          row(^M
            cell(sLabel("24:00")),^M
            cell(zk.drawGREFieldAnyView(boatLog.lootsBoatLog24Date, this, ro=boatCanNotEdit)),^M
            cell(zk.drawGREFieldAnyView(boatLog.lootsBoatLog24Fuel, this, ro=boatCanNotEdit)),^M
            cell(zk.drawGREFieldAnyView(boatLog.lootsBoatLog24Comment, this, ro=boatCanNotEdit)),^M
            cell(zk.drawGREFieldAnyView(boatLog.lootsBoatLog24Engineer, this, ro=boatCanNotEdit)),^M
            cell(zk.drawGREFieldAnyView(boatLog.lootsBoatLog24Master, this, ro=boatCanNotEdit))^M
          )^M
  *  ::*/


  def handlers = CoHandler(ViewPath(""))(emptyView) ::
    CoHandler(ViewPath("/list"))(listView) ::
    //CoHandler(ViewPath("/edit"))(listView) ::
    Nil
}
