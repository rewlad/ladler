
package ee.cone.base.db

import ee.cone.base.connection_api._
import ee.cone.base.db.Types._
import ee.cone.base.util.{Hex, Never}

class FactIndexImpl(
  rawFactConverter: RawFactConverter,
  rawKeyExtractor: RawKeyExtractor,
  rawVisitor: RawVisitor,
  calcLists: CoHandlerLists,
  nodeAttributes: NodeAttrs,
  attrFactory: AttrFactory,
  dbWrapType: WrapType[ObjId],
  zeroObjId: ObjId
) extends FactIndex {
  private var srcObjId = zeroObjId
  def switchReason(node: Obj): Unit = {
    val dbNode = node(nodeAttributes.objId)
    srcObjId = if(dbNode.nonEmpty) dbNode else zeroObjId
  }
  private def getRawIndex(node: ObjId) =
    calcLists.single(TxSelectorKey, ()⇒Never()).rawIndex(node)
  private def get[Value](node: ObjId, attr: RawAttr[Value], valueConverter: RawValueConverter[Value]) = {
    val rawValue =
      if(node.nonEmpty) getRawIndex(node).get(rawFactConverter.key(node, attr))
      else Array[Byte]()
    //println(s"get -- $node -- $attr -- {${rawFactConverter.dump(key)}} -- [${Hex(key)}] -- [${Hex(rawIndex.get(key))}]")
    rawFactConverter.valueFromBytes(valueConverter, rawValue)
  }
  private def set[Value](node: ObjId, attr: RawAttr[Value], valueConverter: RawValueConverter[Value], value: Value): Unit = {
    val rawIndex = getRawIndex(node)
    val key = rawFactConverter.key(node, attr)
    val rawValue = rawFactConverter.value(attr, valueConverter, value, srcObjId)
    //println(s"set -- $node -- $attr -- {${rawFactConverter.dump(key)}} -- $value -- [${Hex(key)}] -- [${Hex(rawValue)}]")
    rawIndex.set(key, rawValue)
  }
  def execute(node: Obj, feed: Feed): Unit = {
    val dbNode = node(nodeAttributes.objId)
    val key = rawFactConverter.keyWithoutAttrId(dbNode)
    val rawIndex = getRawIndex(dbNode)
    rawIndex.seek(key)
    rawVisitor.execute(rawIndex, rawKeyExtractor, key, key.length, feed)
  }
  def handlers[Value](attr: Attr[Value]) = {
    val rawAttr = attr.asInstanceOf[RawAttr[Value]]
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

