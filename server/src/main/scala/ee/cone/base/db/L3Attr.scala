package ee.cone.base.db

import ee.cone.base.connection_api.Obj

class LabelFactoryImpl(attrFactory: AttrFactory, converter: RawValueConverter[Obj]) extends LabelFactory {
  def apply(id: String) = attrFactory(id, converter)
}
