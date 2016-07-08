package ee.cone.base.db_impl

import ee.cone.base.connection_api.{AttrValueType, Obj}

class LabelFactoryImpl(attrFactory: AttrFactory, asObj: AttrValueType[Obj]) extends LabelFactory {
  def apply(id: String) = attrFactory(id, asObj)
}
