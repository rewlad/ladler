package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api.{Obj, Attr}
import ee.cone.base.db.Types._
import ee.cone.base.util.Never

//import java.lang.Math.toIntExact

class NeverValueConverter[Value] extends RawValueConverter[Value] {
  def convertEmpty() = Never()
  def convert(valueA: Long, valueB: Long) = Never()
  def convert(value: String) = Never()
  def allocWrite(before: Int, value: Value, after: Int) = Never()
  def nonEmpty(value: Value) = Never()
}

class MainNodeValueConverter(
  inner: InnerRawValueConverter, nodeFactory: NodeFactory,
  mainTx: CurrentTx[MainEnvKey], instantTx: CurrentTx[InstantEnvKey]
) extends NodeValueConverter[MainEnvKey](inner,nodeFactory,mainTx)(
  new NodeValueConverter[InstantEnvKey](inner,nodeFactory,instantTx)(
    new NeverValueConverter[Obj]
  )
)

class NodeValueConverter[DBEnvKey](
  inner: InnerRawValueConverter, nodeFactory: NodeFactory,
  currentTx: CurrentTx[DBEnvKey]
)(
  next: RawValueConverter[Obj]
) extends RawValueConverter[Obj] {
  def convertEmpty() = nodeFactory.noNode
  def convert(dbId: Long, objId: Long) = {
    //println("NodeValueConverter",dbId,objId)
    if(currentTx.dbId == dbId) nodeFactory.toNode(currentTx(),new ObjId(objId))
    else next.convert(dbId,objId)
  }
  def convert(value: String) = Never()
  def allocWrite(before: Int, node: Obj, after: Int): RawValue = {
    val tx = currentTx.value
    if(tx.nonEmpty && node(nodeFactory.boundToTx) == tx.get)
      inner.allocWrite(before, currentTx.dbId, node(nodeFactory.objId).value, after)
    else next.allocWrite(before, node, after)
  }
  def nonEmpty(value: Obj) = value.nonEmpty
}

class StringValueConverter(inner: InnerRawValueConverter) extends RawValueConverter[String] {
  def convertEmpty() = ""
  def convert(valueA: Long, valueB: Long) = Never()
  def convert(value: String) = value
  def allocWrite(before: Int, value: String, after: Int) =
    inner.allocWrite(before, value, after)
  def nonEmpty(value: String) = value.nonEmpty
}

class UUIDValueConverter(inner: InnerRawValueConverter) extends RawValueConverter[Option[UUID]] {
  def convertEmpty() = None
  def convert(valueA: Long, valueB: Long) = Option(new UUID(valueA,valueB))
  def convert(value: String) = Never()
  def allocWrite(before: Int, value: Option[UUID], after: Int) =
    inner.allocWrite(before, value.get.getMostSignificantBits, value.get.getLeastSignificantBits, after)
  def nonEmpty(value: Option[UUID]) = value.nonEmpty
}

class AttrValueConverter(
  inner: InnerRawValueConverter,
  attrFactory: AttrFactory, definedValueConverter: RawValueConverter[Boolean]
) extends RawValueConverter[Attr[Boolean]] {
  def convertEmpty() = attrFactory.noAttr
  def convert(valueA: Long, valueB: Long) =
    attrFactory(new HiAttrId(valueA),new LoAttrId(valueB),definedValueConverter)
  def convert(value: String) = Never()
  def allocWrite(before: Int, value: Attr[Boolean], after: Int) = {
    val attr = value.asInstanceOf[RawAttr[Boolean]]
    inner.allocWrite(before, attr.hiAttrId.value, attr.loAttrId.value, after)
  }
  def nonEmpty(value: Attr[Boolean]) = value.isInstanceOf[RawAttr[_]]
}

// for true Boolean converter? if(nonEmpty(value)) inner.allocWrite(before, 1L, 0L, after) else Never()
class DefinedValueConverter(inner: InnerRawValueConverter) extends RawValueConverter[Boolean] {
  def convertEmpty() = false
  def convert(valueA: Long, valueB: Long) = true
  def convert(value: String) = true
  def allocWrite(before: Int, value: Boolean, after: Int) = Never()
  def nonEmpty(value: Boolean) = value
}

