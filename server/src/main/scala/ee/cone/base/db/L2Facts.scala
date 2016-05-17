
package ee.cone.base.db

import ee.cone.base.connection_api._
import ee.cone.base.db.Types._
import ee.cone.base.util.{Hex, Never}

class FactIndexImpl(
  rawConverter: RawConverter,
  attrValueConverter: AttrValueConverter,
  rawVisitor: RawVisitor,
  calcLists: CoHandlerLists,
  nodeAttributes: NodeAttrs,
  attrFactory: AttrFactory,
  dbWrapType: WrapType[ObjId],
  noObjId: ObjId,
  zeroObjId: ObjId
) extends FactIndex {
  private var srcObjId = zeroObjId
  def switchReason(node: Obj): Unit = {
    val dbNode = node(nodeAttributes.objId)
    srcObjId = if(dbNode.nonEmpty) dbNode else zeroObjId
  }
  private def getRawIndex(node: ObjId) =
    calcLists.single(TxSelectorKey, ()⇒Never()).rawIndex(node)

  private def key(node: ObjId, attr: ObjId) = rawConverter.toBytes(noObjId,node.hi,node.lo,attr)

  private def get[Value](node: ObjId, attr: ObjId, valueConverter: RawValueConverter[Value]) = {
    val rawValue = if(attr.nonEmpty) getRawIndex(node).get(key(node, attr))
      else Array[Byte]()
    //println(s"get -- $node -- $attr -- {${rawFactConverter.dump(key)}} -- [${Hex(key)}] -- [${Hex(rawIndex.get(key))}]")
    rawConverter.fromBytes(rawValue,0,valueConverter,1)
  }
  private def set[Value](node: ObjId, attr: ObjId, valueConverter: RawValueConverter[Value], value: Value): Unit = {
    val rawValue = valueConverter.toBytes(noObjId, value, srcObjId)
    //println(s"set -- $node -- $attr -- {${rawFactConverter.dump(key)}} -- $value -- [${Hex(key)}] -- [${Hex(rawValue)}]")
    if(attr.nonEmpty) getRawIndex(node).set(key(node, attr), rawValue)
    else Never()
  }
  def execute(obj: Obj)(feed: Attr[Boolean]⇒Boolean): Unit = {
    val node = obj(nodeAttributes.objId)
    val k = key(node,noObjId)
    val rawIndex = getRawIndex(node)
    rawIndex.seek(k)
    rawVisitor.execute(rawIndex, k, b ⇒ feed(rawConverter.fromBytes(b,1,attrValueConverter,0)))
  }
  def handlers[Value](attr: Attr[Value]) = {
    val rawAttr = attr.asInstanceOf[ObjId with RawAttr[Value]]
    val definedAttr = attrFactory.defined(attr)
    val getConverter = () ⇒ calcLists.single(ToRawValueConverter(rawAttr.valueType), ()⇒Never())
    List(
      CoHandler(GetValue(dbWrapType, attr))((obj, innerObj)⇒get(innerObj.data, rawAttr, getConverter())),
      CoHandler(SetValue(dbWrapType, attr)){ (obj, innerObj, value)⇒
        val valueConverter = getConverter()
        if (get(innerObj.data, rawAttr, valueConverter) != value) { // we can't fail on empty values
          for(calc <- calcLists.list(BeforeUpdate(definedAttr))) calc(obj)
          set(innerObj.data, rawAttr, valueConverter, value)
          for(calc <- calcLists.list(AfterUpdate(definedAttr))) calc(obj)
        }
      }
    )
  }
}

