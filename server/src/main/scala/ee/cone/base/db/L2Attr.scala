package ee.cone.base.db

import ee.cone.base.connection_api.{Obj, Attr}
import ee.cone.base.util.Never

class AttrFactoryImpl(
  booleanConverter: RawValueConverter[Boolean], db: FactIndex
)(val noAttr: NoAttr=NoAttr) extends AttrFactory {
  def apply[Value](labelId: Long, propId: Long, converter: RawValueConverter[Value]) = {
    val booleanAttr = AttrImpl[Boolean](labelId, propId)(db, booleanConverter, identity)
    AttrImpl(labelId, propId)(db, converter, _=>booleanAttr)
  }
}

trait NoAttr extends Attr[Boolean]
case object NoAttr extends NoAttr {
  def defined = this
  def set(node: Obj, value: Boolean) = Never()
  def get(node: Obj) = Never()
}

case class AttrImpl[Value](labelId: Long, propId: Long)(
  val factIndex: FactIndex, val converter: RawValueConverter[Value],
  getNonEmpty: Attr[Value]=>Attr[Boolean]
) extends Attr[Value] with RawAttr[Value] {
  def get(node: Obj) = factIndex.get(node, this)
  def set(node: Obj, value: Value) = factIndex.set(node, this, value)
  def defined: Attr[Boolean] = getNonEmpty(this)
  def rawAttr = this
}
