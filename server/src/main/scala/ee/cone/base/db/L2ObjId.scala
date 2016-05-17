package ee.cone.base.db

import ee.cone.base.util.Never

case object NoObjId extends ObjId {
  def hi: Long = Never()
  def lo: Long = Never()
  def nonEmpty = false
}
case class ObjIdImpl(hi: Long, lo: Long) extends ObjId {
  def nonEmpty = true
}
class ObjIdFactoryImpl extends ObjIdFactory {
  def noObjId = NoObjId
  def toObjId(hiObjId: Long, loObjId: Long) = new ObjIdImpl(hiObjId, loObjId)
}
