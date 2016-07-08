package ee.cone.base.db_impl

import ee.cone.base.connection_api.Obj



class IndexedObjCollectionFactoryImpl(
    attrFactory: AttrFactory, findNodes: FindNodes, mainTx: CurrentTx[MainEnvKey]
) extends IndexedObjCollectionFactory {
  def create[Value](index: SearchByLabelProp[Value], parentValue: Value) = {
    val parentAttr = attrFactory.toAttr(index.propId, index.propType)
    val asType = attrFactory.toAttr(index.labelId, index.labelType)

    new ObjCollection {
      def toList = findNodes.where(mainTx(), index, parentValue, Nil)
      def add(list: List[Obj]) = for(obj ← list){
        obj(asType) = obj
        obj(parentAttr) = parentValue
      }
      def remove(list: List[Obj]) = {
        val empty = attrFactory.converter(attrFactory.valueType(parentAttr)).convertEmpty()
        for(obj ← list) obj(parentAttr) = empty
      }
    }
  }
}
