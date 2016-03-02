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
  def get(node: DBNode) = rawAttr.factIndex.get(node, rawAttr)
  def set(node: DBNode, value: Value) = rawAttr.factIndex.set(node, rawAttr, value)
  lazy val ref = new RefAttr(this)
  def defined: Attr[Boolean] = getNonEmpty(this)
  def rawAttr = this
}

case class RefAttr[Value](inner: Attr[Value]) extends Attr[Ref[Value]] {
  lazy val defined = TrueAttr(inner.defined.rawAttr)
  def set(node: DBNode, value: Ref[Value]) = Never()
  def get(node: DBNode) = RefImpl[Value](node, inner)
  def rawAttr = Never()
  lazy val ref = new RefAttr(this)
}

case class TrueAttr(rawAttr: RawAttr[Boolean]) extends Attr[Boolean] {
  def defined = this
  def set(node: DBNode, value: Boolean) = if(!value) Never()
  def get(node: DBNode) = true
  lazy val ref = new RefAttr(this)
}

case class RefImpl[Value](node: DBNode, attr: Attr[Value]) extends Ref[Value] {
  def apply() = attr.get(node)
  def update(value: Value) = attr.set(node, value)
}
