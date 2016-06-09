
package ee.cone.base.db

import ee.cone.base.connection_api._
import ee.cone.base.db.Types._
import ee.cone.base.util.{Hex, Never}

class DBWrapType extends WrapType[ObjId]

class FactIndexImpl(
  rawConverter: RawConverter,
  dBObjIdValueConverter: RawValueConverter[ObjId],
  rawVisitor: RawVisitor,
  calcLists: CoHandlerLists,
  nodeAttributes: NodeAttrs,
  attrFactory: AttrFactory,
  objIdFactory: ObjIdFactory,
  zeroObjId: ObjId,
  asDefined: AttrValueType[Boolean],
  dbWrapType: WrapType[ObjId]
) extends FactIndex {
  private var srcObjId = zeroObjId
  def switchReason(node: Obj): Unit = {
    val dbNode = node(nodeAttributes.objId)
    srcObjId = if(dbNode.nonEmpty) dbNode else zeroObjId
  }
  private def getRawIndex(node: ObjId) =
    calcLists.single(TxSelectorKey, ()⇒Never()).rawIndex(node)

  private def key(node: ObjId, attr: ObjId) = rawConverter.toBytes(objIdFactory.noObjId,node.hi,node.lo,attr)

  private def get[Value](node: ObjId, attr: ObjId, valueConverter: RawValueConverter[Value]) = {
    val rawValue =
      if(node.nonEmpty && attr.nonEmpty) getRawIndex(node).get(key(node, attr))
      else Array[Byte]()
    //println(s"get -- $node -- $attr -- {${rawFactConverter.dump(key)}} -- [${Hex(key)}] -- [${Hex(rawIndex.get(key))}]")
    rawConverter.fromBytes(rawValue,0,valueConverter,1)
  }
  private def set[Value](node: ObjId, attr: ObjId, valueConverter: RawValueConverter[Value], value: Value): Unit = {
    val rawValue = valueConverter.toBytes(objIdFactory.noObjId, value, srcObjId)
    //println(s"set -- $node -- $attr -- {${rawFactConverter.dump(key)}} -- $value -- [${Hex(key)}] -- [${Hex(rawValue)}]")
    if(attr.nonEmpty) getRawIndex(node).set(key(node, attr), rawValue)
    else Never()
  }
  def execute(obj: Obj)(feed: ObjId⇒Boolean): Unit = {
    val node = obj(nodeAttributes.objId)
    val k = key(node,objIdFactory.noObjId)
    val rawIndex = getRawIndex(node)
    rawIndex.seek(k)
    rawVisitor.execute(rawIndex, k, b ⇒ feed(rawConverter.fromBytes(b,1,dBObjIdValueConverter,0)))
  }
  def handlers[Value](attr: Attr[Value]) = {
    val attrId = attrFactory.attrId(attr)
    val valueType = attrFactory.valueType(attr)
    val definedAttr = attrFactory.define(attrId, asDefined)
    CoHandler(SetValue(dbWrapType,attr)){ (obj, innerObj, value) ⇒
      val objId = innerObj.data
      val valueConverter = attrFactory.converter(valueType)
      if(get(objId, attrId, valueConverter) != value) {
        // we can't fail on empty values
        for(calc <- calcLists.list(BeforeUpdate(attrId))) calc(obj)
        set(objId, attrId, valueConverter, value)
        for(calc <- calcLists.list(AfterUpdate(attrId))) calc(obj)
      }
    } ::
    attrFactory.handlers(attr)((obj,objId)⇒
      get(objId, attrId, attrFactory.converter(valueType))
    ) :::
    attrFactory.handlers(definedAttr)((obj,objId)⇒
      get(objId, attrId, attrFactory.converter(asDefined))
    )
  }
  def defined(attrId: ObjId) = attrFactory.toAttr(attrId, asDefined)
}

