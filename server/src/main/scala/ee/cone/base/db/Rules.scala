package ee.cone.base.db

import ee.cone.base.connection_api.{Obj, BaseCoHandler, Attr}

trait Mandatory {
  def apply(condAttr: Attr[_], thenAttr: Attr[_], mutual: Boolean): List[BaseCoHandler]
}

trait Unique {
  def apply[Value](label: Attr[Obj], uniqueAttr: Attr[Value]): List[BaseCoHandler]
}

trait InheritAttrRule {
  def apply[Value](fromAttr: Attr[Value], toAttr: Attr[Value], byIndex: SearchByLabelProp[Obj]): List[BaseCoHandler]
}