package ee.cone.base.db_impl

import ee.cone.base.connection_api.{AttrValueType, Obj}
import ee.cone.base.db.LabelFactory

class LabelFactoryImpl(attrFactory: AttrFactoryI, asObj: AttrValueType[Obj]) extends LabelFactory {
  def apply(id: String) = attrFactory(id, asObj)
}
