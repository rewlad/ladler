package ee.cone.base.test_loots

import ee.cone.base.connection_api.{CoHandler, CoHandlerProvider, Obj}
import ee.cone.base.db._
import ee.cone.base.vdom.{CurrentView, ViewPath}

class ErrorListView(
  attrFactory: AttrFactory,
  alien: Alien,
  filterObjFactory: FilterObjFactory,
  itemListFactory: ItemListFactory,
  itemListOrderingFactory: ItemListOrderingFactory,
  errorAttributes: ErrorAttributes,
  errors: Errors,
  users: Users,
  currentVDom: CurrentView,
  divTags: DivTags,
  style: TagStyles,
  htmlTable: TableTags,
  materialTags: MaterialTags,
  materialIconTags: MaterialIconTags,
  appUtils: AppUtils,
  tableUtils: MaterialDataTableUtils,
  fields: Fields
) extends CoHandlerProvider {
  import appUtils._
  import divTags._
  import htmlTable._
  import materialTags._
  import materialIconTags._
  import tableUtils._
  import fields.field

  def errorNotification = {
    val err = errors.lastError
    notification(err(errorAttributes.errorMsg), "", err(errorAttributes.show), ()=>err(errorAttributes.show)=false)
  }

  private def view(pf: String) = wrapDBView{ () => //todo: replace with actual errors
    val filterObj = filterObjFactory.create(List(attrFactory.attrId(errorAttributes.asError)))
    val itemList = itemListFactory.create[Obj](errors.findAll, users.world, filterObj, Nil, editable=true) //todo roles
  val itemListOrdering = itemListOrderingFactory.itemList(filterObj)
    //println(itemList.list.length)
    List(
      toolbar("Errors"),
      div("maxWidth",style.maxWidth(800))(List(
        paperTable("table")(
          controlPanel(Nil, addRemoveControlView(itemList)),
          List(
            row("head",List(IsHeader))(
              selectAllGroup(itemList) :::
                List(
                  group("2_group",MinWidth(50)),
                  cell("1",MinWidth(250))(_⇒sortingHeader(itemListOrdering,errorAttributes.realm)), //todo: sortingHeader for errors
                  cell("2",MinWidth(250))(_⇒sortingHeader(itemListOrdering,errorAttributes.show)) //todo: sortingHeader for errors
                ):::
                editAllGroup()
            )
          ) :::
            itemList.list.map{error ⇒
              val srcId = error(alien.objIdStr)
              row(srcId,toggledSelectedRow(error))(
                selectRowGroup(error) :::
                  List(
                    group("2_group",MinWidth(50)),
                    cell("1",MinWidth(250))(showLabel⇒field(error, errorAttributes.errorMsg, showLabel = false)),
                    cell("2",MinWidth(250))(showLabel⇒field(error, errorAttributes.show, showLabel = false))
                  )::: editRowGroup(itemList, error)
              )
            }
        )
      ))
    )
  }
  def handlers = List(
    CoHandler(MenuItems)(()⇒List(
      option("errors","Errors")(()⇒currentVDom.relocate("/errorList"))
    )),
    CoHandler(ViewPath("/errorList"))(view)
  )
}

