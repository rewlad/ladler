package ee.cone.base.db

import ee.cone.base.util.Never

class AttrFactoryImpl(booleanConverter: RawValueConverter[Boolean], db: FactIndex) extends AttrFactory {
  def apply[Value](labelId: Long, propId: Long, converter: RawValueConverter[Value]) = {
    val booleanAttr = AttrImpl[Boolean](labelId, propId)(db, booleanConverter, identity)
    AttrImpl(labelId, propId)(db, converter, _=>booleanAttr)
  }
}

case class AttrImpl[Value](labelId: Long, propId: Long)(
  val factIndex: FactIndex, val converter: RawValueConverter[Value],
  getNonEmpty: Attr[Value]=>Attr[Boolean]
) extends Attr[Value] with RawAttr[Value] {
  def get(node: DBNode) = factIndex.get(node, this)
  def set(node: DBNode, value: Value) = factIndex.set(node, this, value)
  def defined: Attr[Boolean] = getNonEmpty(this)
  def rawAttr = this
}

case class RefImpl[Value](node: DBNode, attr: Attr[Value]) extends Ref[Value] {
  def apply() = attr.get(node)
  def update(value: Value) = attr.set(node, value)
}
