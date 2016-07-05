package ee.cone.base.db

import java.time.Instant

import ee.cone.base.connection_api._

trait ItemListOrdering {
  def compose(defaultOrdering: Ordering[Obj]): Ordering[Obj]
  def action(attr: Attr[_]): (Option[()⇒Unit],Option[Boolean])
}

trait ItemListOrderingFactory {
  def itemList(filterObj: Obj): ItemListOrdering
}

trait ValidationContext { def wrap(obj: Obj): Obj }
trait ValidationAttributes {
  def validation: Attr[ObjValidation]
}
trait ValidationFactory {
  def context(stateList: List[ValidationState]): ValidationContext
  def need[Value](obj: Obj, attr: Attr[Value], check: Value⇒Option[String]): List[ValidationState]
}

trait LazyObjFactory {
  def create(index: SearchByLabelProp[ObjId], objIds: List[ObjId], wrapForEdit: Boolean): Obj
}

trait FilterObjFactory {
  def create(ids: List[ObjId]): Obj
}

trait Editing {
  def wrap(obj: Obj): Obj
  def reset(): Unit
}

trait ItemListAttributes {
  def isSelected: Attr[Boolean]
  def isExpanded: Attr[Boolean]
  def selectedItems: Attr[Set[ObjId]]
  def expandedItem: Attr[ObjId]
  def createdAt: Attr[Option[Instant]]
}
trait ItemList {
  def filter: Obj
  def add(): Obj
  def list: List[Obj]
  def selectAllListed(): Unit
  def removeSelected(): Unit
  def isEditable: Boolean
}
trait ItemListFactory {
  def create[Value](
    index: SearchByLabelProp[Value],
    parentValue: Value,
    filterObj: Obj,
    filters: List[Obj⇒Boolean],
    editable: Boolean
  ): ItemList
}

