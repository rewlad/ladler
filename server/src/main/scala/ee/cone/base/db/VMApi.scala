package ee.cone.base.db

import ee.cone.base.connection_api.{Attr, Obj}

trait ItemListOrdering {
  def compose(defaultOrdering: Ordering[Obj]): Ordering[Obj]
  def action(attr: Attr[_]): (Option[()⇒Unit],Option[Boolean])
}

trait ItemListOrderingFactory {
  def itemList(filterObj: Obj): ItemListOrdering
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