package ee.cone.base.test_loots

import ee.cone.base.connection_api.{CoHandler, CoHandlerLists, CoHandlerProvider}
import ee.cone.base.db.{Alien, AlienAccessAttrs}
import ee.cone.base.vdom.{CurrentView, Tags, ViewPath}

class EventListView(
  handlerLists: CoHandlerLists,
  alienAttrs: AlienAccessAttrs,
  alien: Alien,
  currentVDom: CurrentView,
  tags: Tags,
  htmlTable: TableTags,
  materialTags: MaterialTags,
  materialIconTags: MaterialIconTags,
  appUtils: AppUtils,
  tableUtils: MaterialDataTableUtils
) extends CoHandlerProvider {
  import htmlTable._
  import materialTags._
  import materialIconTags._
  import appUtils._
  import tableUtils._

  private def view(pf: String) = wrapDBView { () =>
    List(
      toolbar("Events"),
      paperTable("table")(Nil,
        row("head", List(IsHeader))(List(
          cell("1",MinWidth(250))(_⇒List(text("text", "Event"))),
          cell("2",MinWidth(250))(_⇒Nil)
        )) ::
          eventSource.unmergedEvents.map(alien.wrapForEdit).map { ev =>
            val srcId = ev(alien.objIdStr)
            row(srcId,Nil)(List(
              cell("1",MinWidth(250))(_⇒List(text("text", ev(alienAttrs.comment)))),
              cell("2",MinWidth(250))(_⇒List(btnRemove("btn", () => eventSource.addUndo(ev))))
            ))
          }
      )
    )
  }

  private def eventToolbarButtons() =
    if (eventSource.unmergedEvents.isEmpty) Nil
    else List(
      btnRestore("events", () ⇒ currentVDom.relocate("/eventList")),
      btnSave("save", () ⇒ eventSource.addRequest())
    )

  def handlers = List(
    CoHandler(ViewPath("/eventList"))(view),
    CoHandler(ToolbarButtons)(eventToolbarButtons)
  )
}
