package ee.cone.base.db_impl

import ee.cone.base.connection_api._

class EditingImpl(
  nodeAttrs: NodeAttrs,
  objIdFactory: ObjIdFactory,
  alienAttrs: AlienAttributes,
  alien: Alien,
  dbWrapType: WrapType[ObjId]
)(
  var editingId: ObjId = objIdFactory.noObjId
) extends Editing with CoHandlerProvider {
  def wrap(obj: Obj) =
    if(obj(nodeAttrs.objId) == editingId) alien.wrapForUpdate(obj) else obj
  def reset() = editingId = objIdFactory.noObjId
  def handlers = CoHandler(SetValue(dbWrapType,alienAttrs.isEditing)){ (obj,innerObj,value)⇒
    if(value) editingId = innerObj.data else if(obj(alienAttrs.isEditing)) reset()
  } :: Nil
}
