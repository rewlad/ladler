
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
  def switchReason(node: Obj): Unit =
    srcObjId = if(node(nodeFactory.nonEmpty)) node(nodeFactory.objId) else new ObjId(0L)
  private def get[Value](node: DBNode, attr: RawAttr[Value]) = {
    val rawValue =
      if(node.nonEmpty) node.rawIndex.get(rawFactConverter.key(node.objId, attr))
      else Array[Byte]()
    //println(s"get -- $node -- $attr -- {${rawFactConverter.dump(key)}} -- [${Hex(key)}] -- [${Hex(rawIndex.get(key))}]")
    rawFactConverter.valueFromBytes(attr.converter, rawValue)
  }
  private def set[Value](node: DBNode, attr: RawAttr[Value], value: Value): Unit = {
    val rawIndex = node.rawIndex
    val key = rawFactConverter.key(node.objId, attr)
    val rawValue = rawFactConverter.value(attr, value, srcObjId)
    //println(s"set -- $node -- $attr -- {${rawFactConverter.dump(key)}} -- $value -- [${Hex(key)}] -- [${Hex(rawValue)}]")
    rawIndex.set(key, rawValue)
  }
  def execute(node: Obj, feed: Feed): Unit = {
    val key = rawFactConverter.keyWithoutAttrId(node(nodeFactory.objId))
    val rawIndex = node(nodeFactory.rawIndex)
    rawIndex.seek(key)
    rawVisitor.execute(rawIndex, rawKeyExtractor, key, key.length, feed)
  }
  def handlers[Value](attr: Attr[Value]) = {
    val rawAttr = attr.asInstanceOf[RawAttr[Value]]
    val definedAttr = attrFactory.defined(attr)
    List(
      CoHandler(GetValue(dbWrapType, attr))((obj, innerObj)⇒get(innerObj.data, rawAttr)),
      CoHandler(SetValue(dbWrapType, attr)){ (obj, innerObj, value)⇒
        if (get(innerObj.data, rawAttr) != value) { // we can't fail on empty values
          for(calc <- calcLists.list(BeforeUpdate(definedAttr))) calc(obj)
          set(innerObj.data, rawAttr, value)
          for(calc <- calcLists.list(AfterUpdate(definedAttr))) calc(obj)
        }
      }
    )
  }
}

