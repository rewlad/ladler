package ee.cone.base.framework

import ee.cone.base.connection_api.{Attr, Obj, ObjId}
import ee.cone.base.db.{ItemListOrdering, ObjCollection, ObjSelection, SearchByLabelProp}
import ee.cone.base.vdom.Types.VDomKey
import ee.cone.base.vdom._

trait UserListView {
  def loginView: List[ChildPair[OfDiv]]
}

trait ErrorListView {
  def errorNotification: List[ChildPair[OfDiv]]
}

class ItemList(
  val listed: ObjCollection, val list: List[Obj], val selection: ObjSelection,
  val isEditable: Boolean
)

trait DataTableUtils {
  def creationTimeOrdering: Ordering[Obj]
  def createItemList[Value](
    theListed: ObjCollection,
    filterObj: Obj,
    filters: List[Obj⇒Boolean],
    editable: Boolean
  ): ItemList
  def toggledSelectedRow(item: Obj): List[TagAttr]
  def toggledRow(filterObj: Obj, id: ObjId): List[TagAttr]
  def paperTable(key: VDomKey)(
    controls:List[ChildPair[OfDiv]],tableElements: List[ChildOfTable]
  ): ChildPair[OfDiv]
  def header(attr: Attr[_]):List[ChildPair[OfDiv]]
  def sortingHeader(itemListOrdering: ItemListOrdering, attr: Attr[_]):List[ChildPair[OfDiv]]
  def addRemoveControlViewBase(itemList: ItemList)(add: Obj⇒Unit): List[ChildPair[OfDiv]]
  def addRemoveControlView(itemList: ItemList): List[ChildPair[OfDiv]]
  def selectAllGroup(itemList: ItemList): List[ChildOfTableRow]
  def selectRowGroup(item: Obj): List[ChildOfTableRow]
  def editAllGroup(): List[ChildOfTableRow]
  def editRowGroupBase(on: Boolean)(action: ()⇒Unit): List[ChildOfTableRow]
  def editRowGroup(itemList: ItemList, item: Obj): List[ChildOfTableRow]
}

trait UserAttributes {
  def asUser: Attr[Obj]
  def fullName: Attr[String]
  def username: Attr[String]
  def unEncryptedPassword: Attr[String]
  def unEncryptedPasswordAgain: Attr[String]
  def asActiveUser: Attr[Obj]
  def authenticatedUser: Attr[Obj]
  def world: ObjId
}

trait Users {
  def needToLogIn: Boolean
  def findAll: SearchByLabelProp[Obj]
  def world: Obj
  def loginAction(dialog: Obj): Option[()⇒Unit]
  def changePasswordAction(user: Obj): Option[()⇒Unit]
}
