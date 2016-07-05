package ee.cone.base.db

import ee.cone.base.connection_api.ObjId

class LazyObjFactoryImpl(
  objIdFactory: ObjIdFactory,
  attrFactory: AttrFactory,
  findNodes: FindNodes,
  findAttrs: FindAttrs,
  mainTx: CurrentTx[MainEnvKey],
  alien: Alien
) extends LazyObjFactory {
  def create(
    index: SearchByLabelProp[ObjId],
    objIds: List[ObjId],
    wrapForEdit: Boolean
  ) = {
    val key = objIdFactory.compose(objIds) //todo rewrite to getting from different attrs
    val obj = findNodes.single(findNodes.where(mainTx(), index, key, Nil))
    if(!wrapForEdit) obj
    else if(obj(findAttrs.nonEmpty)) alien.wrapForUpdate(obj)
    else alien.demanded { obj ⇒
      obj(attrFactory.toAttr(index.labelId, index.labelType)) = obj
      obj(attrFactory.toAttr(index.propId, index.propType)) = key
    }
  }
}
