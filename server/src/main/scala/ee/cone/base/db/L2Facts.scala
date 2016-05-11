
package ee.cone.base.db

import ee.cone.base.connection_api._
import ee.cone.base.db.Types._
import ee.cone.base.util.{Hex, Never}

class FactIndexImpl(
  rawFactConverter: RawFactConverter,
  rawKeyExtractor: RawKeyExtractor,
  rawVisitor: RawVisitor,
  calcLists: CoHandlerLists,
  nodeFactory: NodeFactory,
  attrFactory: AttrFactory,
  dbWrapType: WrapType[DBNode]
) extends FactIndex {
  private var srcObjId = new ObjId(0L)
  def switchReason(node: Obj): Unit = {
    val dbNode = node(nodeFactory.dbNode)
    srcObjId = if(dbNode.nonEmpty) dbNode.objId else new ObjId(0L)
  }

  private def get[Value](node: DBNode, attr: RawAttr[Value], valueConverter: RawValueConverter[Value]) = {
    val rawValue =
      if(node.nonEmpty) node.rawIndex.get(rawFactConverter.key(node.objId, attr))
      else Array[Byte]()
    //println(s"get -- $node -- $attr -- {${rawFactConverter.dump(key)}} -- [${Hex(key)}] -- [${Hex(rawIndex.get(key))}]")
    rawFactConverter.valueFromBytes(valueConverter, rawValue)
  }
  private def set[Value](node: DBNode, attr: RawAttr[Value], valueConverter: RawValueConverter[Value], value: Value): Unit = {
    val rawIndex = node.rawIndex
    val key = rawFactConverter.key(node.objId, attr)
    val rawValue = rawFactConverter.value(attr, valueConverter, value, srcObjId)
    //println(s"set -- $node -- $attr -- {${rawFactConverter.dump(key)}} -- $value -- [${Hex(key)}] -- [${Hex(rawValue)}]")
    rawIndex.set(key, rawValue)
  }
  def execute(node: Obj, feed: Feed): Unit = {
    val dbNode = node(nodeFactory.dbNode)
    val key = rawFactConverter.keyWithoutAttrId(dbNode.objId)
    val rawIndex = dbNode.rawIndex
    rawIndex.seek(key)
    rawVisitor.execute(rawIndex, rawKeyExtractor, key, key.length, feed)
  }
  def handlers[Value](attr: Attr[Value]) = {
    val rawAttr = attr.asInstanceOf[RawAttr[Value]]
    val definedAttr = attrFactory.defined(attr)
    val getConverter = () ⇒ calcLists.single(ToRawValueConverter(rawAttr.valueType))
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

