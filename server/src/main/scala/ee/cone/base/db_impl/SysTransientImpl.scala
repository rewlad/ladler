package ee.cone.base.db_impl

import ee.cone.base.connection_api._
import ee.cone.base.db.{TransientChanged, Transient}

class TransientImpl(handlerLists: CoHandlerLists, attrFactory: AttrFactoryI, dbWrapType: WrapType[ObjId]) extends Transient {
  def update[R](attr: Attr[R]) = {
    val data = collection.mutable.Map[ObjId,R]()
    CoHandler(SetValue(dbWrapType,attr)) { (obj, innerObj, value) ⇒
      data(innerObj.data) = value
      handlerLists.list(TransientChanged).foreach(_())
    } ::
    attrFactory.handlers(attr)( (obj,objId)⇒
      data.getOrElse(objId, attrFactory.converter(attrFactory.valueType(attr)).convertEmpty())
    )
  }
}