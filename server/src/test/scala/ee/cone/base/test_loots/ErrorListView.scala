
package ee.cone.base.test_loots // to app

import ee.cone.base.connection_api.{CoHandler, CoHandlerProvider, FieldAttributes, Obj}
import ee.cone.base.db._
import ee.cone.base.framework.{ErrorListView, DataTableUtils, Users}
import ee.cone.base.material.{MaterialTags, MenuItems, OptionTags, TableUtilTags}
import ee.cone.base.vdom._

class ErrorListViewImpl(
  attrFactory: AttrFactory,
  filterObjFactory: FilterObjFactory,
  listedFactory: IndexedObjCollectionFactory,
  itemListOrderingFactory: ItemListOrderingFactory,

  currentVDom: CurrentView,
  divTags: Tags,
  style: TagStyles,

  materialTags: MaterialTags,
  optionTags: OptionTags,
  tableUtilTags: TableUtilTags,

  htmlTable: TableTags,
  tableUtils: DataTableUtils,
  fieldAttributes: FieldAttributes,
  fields: Fields,

  errorAttributes: ErrorAttributes,
  errors: Errors,
  users: Users
) extends ErrorListView with CoHandlerProvider {
  import divTags._
  import htmlTable._
  import materialTags._
  import tableUtils._
  import fields.field
  import tableUtilTags._

  def errorNotification = {
    val err = errors.lastError
    notification(err(errorAttributes.errorMsg), "", err(errorAttributes.show), ()=>err(errorAttributes.show)=false)
  }

  private def view(pf: String) = wrap{ () => //todo: replace with actual errors
    val filterObj = filterObjFactory.create(List(attrFactory.attrId(errorAttributes.asError)))
    val listed = listedFactory.create(errors.findAll, users.world)
    val itemList = createItemList[Obj](listed, filterObj, Nil, editable=true) //todo roles
    val itemListOrdering = itemListOrderingFactory.itemList(filterObj)
    //println(itemList.list.length)
    List(
      toolbar("Errors"),
      div("maxWidth", style.maxWidth(800), style.marginLeftAuto, style.marginRightAuto)(List(
        paperTable("table")(
          controlPanel(Nil, addRemoveControlView(itemList)),
          List(
            row("head",IsHeader)(
              selectAllGroup(itemList) :::
                List(
                  group("2_group",MinWidth(50))(Nil),
                  cell("1",MinWidth(250))(_⇒sortingHeader(itemListOrdering,errorAttributes.realm)), //todo: sortingHeader for errors
                  cell("2",MinWidth(250))(_⇒sortingHeader(itemListOrdering,errorAttributes.show)) //todo: sortingHeader for errors
                ):::
                editAllGroup()
            )
          ) :::
            itemList.list.map{error ⇒
              val srcId = error(fieldAttributes.aObjIdStr)
              row(srcId,toggledSelectedRow(error):_*)(
                selectRowGroup(error) :::
                  List(
                    group("2_group",MinWidth(50))(Nil),
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
      optionTags.option("errors","Errors")(()⇒currentVDom.relocate("/errorList"))
    )),
    CoHandler(ViewPath("/errorList"))(view)
  )
}

