package ee.cone.base.db

import ee.cone.base.connection_api._

trait ItemListOrdering {
  def compose(defaultOrdering: Ordering[Obj]): Ordering[Obj]
  def action(attr: Attr[_]): (Option[()⇒Unit],Option[Boolean])
}

trait ItemListOrderingFactory {
  def itemList(filterObj: Obj): ItemListOrdering
}

trait ValidationAttributes {
  def validation: Attr[ObjValidation]
}

trait ValidationContext { def wrap(obj: Obj): Obj }

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

trait ObjCollection {
  def toList: List[Obj]
  def add(list: List[Obj]): Unit
  def remove(list: List[Obj]): Unit
}

trait ObjSelection {
  def collection: ObjCollection
  def wrap(obj: Obj): Obj
}

trait ObjSelectionAttributes {
  def isSelected: Attr[Boolean]
  def isExpanded: Attr[Boolean]
  def expandedItem: Attr[ObjId]
}

trait ObjSelectionFactory {
  def create(filterObj: Obj): ObjSelection
}

trait IndexedObjCollectionFactory {
  def create[Value](index: SearchByLabelProp[Value], parentValue: Value): ObjCollection
}
