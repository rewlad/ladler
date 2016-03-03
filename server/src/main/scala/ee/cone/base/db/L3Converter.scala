package ee.cone.base.db

import java.util.UUID

import ee.cone.base.db.Types._
import ee.cone.base.util.Never

import java.lang.Math.toIntExact

class NodeValueConverter(
  inner: InnerRawValueConverter, nodeFactory: NodeFactory,
  instantTxManager: TxManager[InstantEnvKey],
  mainTxManager: TxManager[MainEnvKey]
)(
  managers: Array[TxManager[_]] = Array(instantTxManager,mainTxManager)
) extends RawValueConverter[DBNode] {
  def convert() = nodeFactory.noNode
  def convert(valueA: Long, valueB: Long) =
    nodeFactory.toNode(managers(toIntExact(valueA)).tx,valueB)
  def convert(value: String) = Never()
  def allocWrite(before: Int, node: DBNode, after: Int): RawValue = {
    var pos = 0
    while(true) if(node.tx == managers(pos).tx)
      return inner.allocWrite(before, pos, node.objId, after)
    Never()
  }
  def nonEmpty(value: DBNode) = value.nonEmpty
}

class StringValueConverter(inner: InnerRawValueConverter) extends RawValueConverter[String] {
  def convert() = ""
  def convert(valueA: Long, valueB: Long) = Never()
  def convert(value: String) = value
  def allocWrite(before: Int, value: String, after: Int) =
    if(nonEmpty(value)) inner.allocWrite(before, value, after) else Never()
  def nonEmpty(value: String) = value.nonEmpty
}

class UUIDValueConverter(inner: InnerRawValueConverter) extends RawValueConverter[UUID] {
  def convert() = Never()
  def convert(valueA: Long, valueB: Long) = new UUID(valueA,valueB)
  def convert(value: String) = Never()
  def allocWrite(before: Int, value: UUID, after: Int) =
    if(nonEmpty(value)) inner.allocWrite(before, value.getMostSignificantBits, value.getLeastSignificantBits, after) else Never()
  def nonEmpty(value: UUID) = true
}

// for true Boolean converter? if(nonEmpty(value)) inner.allocWrite(before, 1L, 0L, after) else Never()
class DefinedValueConverter(inner: InnerRawValueConverter) extends RawValueConverter[Boolean] {
  def convert() = false
  def convert(valueA: Long, valueB: Long) = true
  def convert(value: String) = true
  def allocWrite(before: Int, value: Boolean, after: Int) = Never()
  def nonEmpty(value: Boolean) = value
}

