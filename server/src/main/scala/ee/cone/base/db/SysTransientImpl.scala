package ee.cone.base.db

import ee.cone.base.connection_api.{Attr, CoHandler, CoHandlerLists, WrapType}

class TransientImpl(handlerLists: CoHandlerLists, attrFactory: AttrFactory, wrapType: WrapType[ObjId]) extends Transient {
  def update[R](attr: Attr[R]) = {
    val data = collection.mutable.Map[ObjId,R]()
    List(
      CoHandler(GetValue(wrapType,attr))((obj,innerObj)⇒
        data.getOrElse(innerObj.data, attrFactory.converter(attrFactory.valueType(attr)).convertEmpty())
      ),
      CoHandler(SetValue(wrapType,attr)) { (obj, innerObj, value) ⇒
        data(innerObj.data) = value
        handlerLists.list(TransientChanged).foreach(_())
      }
    )
  }
}
