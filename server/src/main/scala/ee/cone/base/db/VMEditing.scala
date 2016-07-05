package ee.cone.base.db

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
    if(obj(nodeAttrs.objId) == editingId) alien.wrapForEdit(obj) else obj
  def reset() = editingId = objIdFactory.noObjId
  def handlers = CoHandler(SetValue(dbWrapType,alienAttrs.isEditing)){ (obj,innerObj,value)⇒
    if(value) editingId = innerObj.data else if(obj(alienAttrs.isEditing)) reset()
  } :: Nil
}
