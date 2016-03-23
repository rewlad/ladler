package ee.cone.base.test_loots

import java.time.{Duration, Instant}
import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.db._
import ee.cone.base.server.SenderOfConnection
import ee.cone.base.vdom.{CurrentVDom, Tags, ViewPath}

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
    tags.root(tags.text("text", "Loading..."))

  private def wrapDBView[R](view: ()=>R): R =
    eventSource.incrementalApplyAndView { () ⇒
      val startTime = System.currentTimeMillis
      val res = view()
      val endTime = System.currentTimeMillis
      currentVDom.until(endTime+(endTime-startTime)*10)
      res
    }

  private def listView(pf: String) = wrapDBView{ ()=> root(
    //class LootsBoatLogList
    withMargin("margin",10,paper("paper",table("table", List(
      row("filter",
        th("filter", isRight = true, 7,
          withMargin("list", 10, btnFilterList("btn",filterListAct())),
          withMargin("clear",10, btnClear("btn",clearSortingAct()))
        )
      ),
      row("headers",
          th("1", isRight = false, "Boat"),
          th("2", isRight = true, "Date"),
          th("3", isRight = true, "Total duration, hrs:min"),
          th("4", isRight = false, "Confirmed"),
          th("5", isRight = false, "Confirmed by"),
          th("6", isRight = true, "Confirmed on"),
          th("7", isRight = false, btnAdd("btn",addAct()))
      )),
      List(???).map{ (entry:Obj)=>
        val srcId = entry(uniqueNodes.srcId).get
        row(srcId.toString,
            td("1", isRight = false, entry(logAt.boat)(at.caption)),
            td("2", isRight = true, toAlienText(entry(logAt.date))),
            td("3", isRight = true, toAlienText(entry(logAt.durationTotal))),
            td("4", isRight = false, {
              val confirmed = entry(logAt.asConfirmed)
              if(confirmed.nonEmpty) "CONFIRMED" else "" //todo: MaterialChip
            }),
            td("5", isRight = false, entry(logAt.confirmedBy)(at.caption)),
            td("6", isRight = true, toAlienText(entry(logAt.confirmedOn))),
            td("7", isRight = false, btnRemove("btn",removeAct()))
        )
      }
    )))
  )}
  private def toAlienText(value: Option[Duration]): String =
    value.map(_.getSeconds.toString).getOrElse("")
  private def toAlienText(value: Option[Instant]): String =
    value.map(_.getEpochSecond.toString).getOrElse("")
  private def filterListAct()() = ???
  private def clearSortingAct()() = ???
  private def addAct()() = ???
  private def removeAct()() = ???

  private def editView(pf: String) = wrapDBView{ ()=>
    println(pf)
    val srcId = UUID.fromString(pf)
    val entry = uniqueNodes.whereSrcId(mainTx(), srcId)(logAt.asEntry)
    root(
      withMargin("1",10,paper("paper",table("table", Nil, List(
        row("1",
          td("boat_c", isRight = false, "Boat"),
          td("boat_i", isRight = false, entry(logAt.boat)(at.caption)/*todo select */),
          td("date_c", isRight = false, "Date"),
          td("date_i", isRight = false, toAlienText(entry(logAt.date)/*todo date */)),
          td("dur_c", isRight = false, "Total duration, hrs:min"),
          td("dur_i", isRight = false, toAlienText(entry(logAt.durationTotal)))
        ),
        row("2",
          td("5", isRight = false, "Confirmed by"),
          td("6", isRight = false, "Confirmed on"),
          td("4", isRight = false, ???),
        )
      )))),
      withMargin("2",10,paper("paper",table("table",
        ???,
        ???
      ))),
      withMargin("3",10,paper("paper",table("table",
        ???,
        ???
      )))
    )
  }

  /*

            ,
            td("4", isRight = false, {
              val confirmed = entry(logAt.asConfirmed)
              if(confirmed.nonEmpty) "CONFIRMED" else "" 
            }),
            td("5", isRight = false, entry(logAt.confirmedBy)(at.caption)),
            td("6", isRight = true, toAlienText(entry(logAt.confirmedOn))),
            td("7", isRight = false, btnRemove("btn",removeAct()))
  *  ::*/


  def handlers = CoHandler(ViewPath(""))(emptyView) ::
    CoHandler(ViewPath("/list"))(listView) ::
    //CoHandler(ViewPath("/edit"))(listView) ::
    Nil
}
