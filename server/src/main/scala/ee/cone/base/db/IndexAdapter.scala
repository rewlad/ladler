package ee.cone.base.db

import ee.cone.base.util.Never

case class RuledIndexAdapterImpl[Value](
  ruled: RuledIndex
)(
  val converter: ValueConverter[Value,DBValue]
) extends RuledIndexAdapter[Value] {
  def update(objId: ObjId, value: Value) = ruled(objId) = converter(value)
  def apply(objId: ObjId) = converter(ruled(objId))
}

class ObjIdValueConverter extends ValueConverter[Option[ObjId],DBValue] {
  override def apply(value: Option[ObjId]): DBValue = value match {
    case None => DBRemoved
    case Some(objId) => DBLongValue(objId.value)
  }
  override def apply(value: DBValue): Option[ObjId] = value match {
    case DBRemoved => None
    case DBLongValue(v) => Some(new ObjId(v))
    case _ => Never()
  }
}
