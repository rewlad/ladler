package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api.{Obj, Attr}
import ee.cone.base.db.Types._
import ee.cone.base.util.Never

import java.lang.Math.toIntExact

class NodeValueConverter(
  inner: InnerRawValueConverter, nodeFactory: NodeFactory,
  instantTx: CurrentTx[InstantEnvKey], mainTx: CurrentTx[MainEnvKey]
)(
  currentTx: List[CurrentTx[_]] = instantTx :: mainTx :: Nil
) extends RawValueConverter[Obj] {
  def convert() = nodeFactory.noNode
  private def get(currentTx: List[CurrentTx[_]], dbId: Long, objId: ObjId): Obj =
    if(currentTx.head.dbId == dbId) nodeFactory.toNode(currentTx.head(),objId)
    else get(currentTx.tail,dbId,objId)
  def convert(valueA: Long, valueB: Long) = {
    println("NodeValueConverter",valueA,valueB)
    get(currentTx.tail,valueA,valueB)
  }
  def convert(value: String) = Never()
  def allocWrite(before: Int, node: Obj, after: Int): RawValue = {
    def set(currentTx: List[CurrentTx[_]]): RawValue = {
      val tx = currentTx.head.value
      if(tx.nonEmpty && node.tx == tx.get)
        inner.allocWrite(before, currentTx.head.dbId, node(nodeFactory.objId), after)
      else set(currentTx.tail)
    }
    set(currentTx)
  }
  def nonEmpty(value: Obj) = value.nonEmpty
}

class StringValueConverter(inner: InnerRawValueConverter) extends RawValueConverter[String] {
  def convert() = ""
  def convert(valueA: Long, valueB: Long) = Never()
  def convert(value: String) = value
  def allocWrite(before: Int, value: String, after: Int) =
    inner.allocWrite(before, value, after)
  def nonEmpty(value: String) = value.nonEmpty
}

class UUIDValueConverter(inner: InnerRawValueConverter) extends RawValueConverter[Option[UUID]] {
  def convert() = None
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
  def convert() = attrFactory.noAttr
  def convert(valueA: Long, valueB: Long) = attrFactory(valueA,valueB,definedValueConverter)
  def convert(value: String) = Never()
  def allocWrite(before: Int, value: Attr[Boolean], after: Int) = {
    val attr = value.asInstanceOf[RawAttr[Boolean]]
    inner.allocWrite(before, attr.labelId, attr.propId, after)
  }
  def nonEmpty(value: Attr[Boolean]) = true
}

// for true Boolean converter? if(nonEmpty(value)) inner.allocWrite(before, 1L, 0L, after) else Never()
class DefinedValueConverter(inner: InnerRawValueConverter) extends RawValueConverter[Boolean] {
  def convert() = false
  def convert(valueA: Long, valueB: Long) = true
  def convert(value: String) = true
  def allocWrite(before: Int, value: Boolean, after: Int) = Never()
  def nonEmpty(value: Boolean) = value
}

