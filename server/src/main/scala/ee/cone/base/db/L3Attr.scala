package ee.cone.base.db

import ee.cone.base.connection_api.Obj

class LabelFactoryImpl(attrFactory: AttrFactory, asObj: AttrValueType[Obj]) extends LabelFactory {
  def apply(id: String) = attrFactory(id, asObj)
}
