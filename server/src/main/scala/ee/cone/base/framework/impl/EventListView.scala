
package ee.cone.base.framework

import ee.cone.base.connection_api.{CoHandler, CoHandlerLists, CoHandlerProvider, FieldAttributes}
import ee.cone.base.db.{Alien, AlienAttributes, SessionEventSource}
import ee.cone.base.framework.DataTableUtils
import ee.cone.base.material._
import ee.cone.base.util.Never
import ee.cone.base.vdom._

class EventListView(
  handlerLists: CoHandlerLists,
  alienAttrs: AlienAttributes,
  currentVDom: CurrentView,
  tags: Tags,
  htmlTable: TableTags,
  buttonTags: ButtonTags,
  materialTags: MaterialTags,
  eventIconTags: EventIconTags,
  tableUtils: DataTableUtils,
  fieldAttributes: FieldAttributes
) extends CoHandlerProvider {
  import tags._
  import htmlTable._
  import buttonTags.iconButton
  import materialTags._
  import eventIconTags._
  import tableUtils._

  def eventSource = handlerLists.single(SessionEventSource, ()⇒Never())

  private def view(pf: String) = wrap { () =>
    List(
      toolbar("Events"),
      paperTable("table")(Nil,
        row("head", IsHeader)(List(
          cell("1",MinWidth(250))(_⇒List(text("text", "Event"))),
          cell("2",MinWidth(250))(_⇒Nil)
        )) ::
          eventSource.unmergedEvents.map { ev =>
            val srcId = ev(fieldAttributes.aObjIdStr)
            row(srcId)(List(
              cell("1",MinWidth(250))(_⇒List(text("text", ev(alienAttrs.comment)))),
              cell("2",MinWidth(250))(_⇒List(iconButton("btn", "restore",iconRemove)(() => eventSource.addUndo(ev))))
            ))
          }
      )
    )
  }

  private def eventToolbarButtons() =
    if (eventSource.unmergedEvents.isEmpty) Nil
    else List(
      iconButton("events","events",iconEvents)(() ⇒ currentVDom.relocate("/eventList")),
      iconButton("save","save",iconSave)(() ⇒ eventSource.addRequest())
    )

  def handlers = List(
    CoHandler(ViewPath("/eventList"))(view),
    CoHandler(ToolbarButtons)(eventToolbarButtons)
  )
}
