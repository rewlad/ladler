package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api.{CoHandlerProvider, CoHandler, Obj, Attr}
import ee.cone.base.db.Types._
import ee.cone.base.util.Never

//import java.lang.Math.toIntExact

abstract class RawValueConverterImpl[Value] extends RawValueConverter[Value] with CoHandlerProvider {
  def valueType: AttrValueType[Value]
  def handlers = CoHandler(ToRawValueConverter(valueType))(this) :: Nil
}

class NeverValueConverter[Value] extends RawValueConverter[Value] {
  def convertEmpty() = Never()
  def convert(valueA: Long, valueB: Long) = Never()
  def convert(value: String) = Never()
  def allocWrite(before: Int, value: Value, after: Int) = Never()
  def nonEmpty(value: Value) = Never()
}

class NodeValueConverter[DBEnvKey](
  val valueType: AttrValueType[Obj],
  inner: InnerRawValueConverter,
  nodeFactory: NodeFactory, nodeAttributes: NodeAttrs,
  currentTx: CurrentTx[DBEnvKey]
) extends RawValueConverterImpl[Obj] {
  def convertEmpty() = nodeFactory.noNode
  def convert(valueA: Long, valueB: Long) =
    nodeFactory.toNode(currentTx(),valueA,valueB)
  def convert(value: String) = Never()
  def allocWrite(before: Int, node: Obj, after: Int): RawValue = {
    val objId = node(nodeAttributes.dbNode).objId
    inner.allocWrite(before, objId.hiObjId, objId.loObjId, after)
  }
  def nonEmpty(value: Obj) = value(nodeAttributes.nonEmpty)
}

class StringValueConverter(
  val valueType: AttrValueType[String], inner: InnerRawValueConverter
) extends RawValueConverterImpl[String] {
  def convertEmpty() = ""
  def convert(valueA: Long, valueB: Long) = Never()
  def convert(value: String) = value
  def allocWrite(before: Int, value: String, after: Int) =
    inner.allocWrite(before, value, after)
  def nonEmpty(value: String) = value.nonEmpty
}

class UUIDValueConverter(
  val valueType: AttrValueType[Option[UUID]], inner: InnerRawValueConverter
) extends RawValueConverterImpl[Option[UUID]] {
  def convertEmpty() = None
  def convert(valueA: Long, valueB: Long) = Option(new UUID(valueA,valueB))
  def convert(value: String) = Never()
  def allocWrite(before: Int, value: Option[UUID], after: Int) =
    inner.allocWrite(before, value.get.getMostSignificantBits, value.get.getLeastSignificantBits, after)
  def nonEmpty(value: Option[UUID]) = value.nonEmpty
}

class AttrValueConverter(
  val valueType: AttrValueType[Attr[Boolean]],
  inner: InnerRawValueConverter,
  attrFactory: AttrFactory, asDefined: AttrValueType[Boolean]
) extends RawValueConverterImpl[Attr[Boolean]] {
  def convertEmpty() = attrFactory.noAttr
  def convert(valueA: Long, valueB: Long) =
    attrFactory(new HiAttrId(valueA),new LoAttrId(valueB),asDefined)
  def convert(value: String) = Never()
  def allocWrite(before: Int, value: Attr[Boolean], after: Int) = {
    val attr = value.asInstanceOf[RawAttr[Boolean]]
    inner.allocWrite(before, attr.hiAttrId.value, attr.loAttrId.value, after)
  }
  def nonEmpty(value: Attr[Boolean]) = value.isInstanceOf[RawAttr[_]]
}

// for true Boolean converter? if(nonEmpty(value)) inner.allocWrite(before, 1L, 0L, after) else Never()
class DefinedValueConverter(
  val valueType: AttrValueType[Boolean], inner: InnerRawValueConverter
) extends RawValueConverterImpl[Boolean] {
  def convertEmpty() = false
  def convert(valueA: Long, valueB: Long) = true
  def convert(value: String) = true
  def allocWrite(before: Int, value: Boolean, after: Int) = Never()
  def nonEmpty(value: Boolean) = value
}

