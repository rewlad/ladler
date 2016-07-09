package ee.cone.base.db_impl

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.db._
import ee.cone.base.db.Types._



case object TxSelectorKey extends EventKey[TxSelector]
trait TxSelector {
  def txOf(obj: Obj): BoundToTx
  def rawIndex(objId: ObjId): RawIndex
  def rawIndex(tx: BoundToTx): RawIndex
}

trait ObjIdFactoryI extends ObjIdFactory {
  def toObjId(hiObjId: Long, loObjId: Long): ObjId
  def toObjId(uuid: UUID): ObjId
  def compose(objIds: List[ObjId]): ObjId
  def toUUIDString(objId: ObjId): String
}

trait AttrFactoryI extends AttrFactory {
  def define[V](attrId: ObjId, valueType: AttrValueType[V]): Attr[V]
  def toAttr[V](attrId: ObjId, valueType: AttrValueType[V]): Attr[V]
  def converter[V](valueType: AttrValueType[V]): RawValueConverter[V]
}

trait FactIndexI extends FactIndex {
  def switchReason(node: Obj): Unit
  def execute(obj: Obj)(feed: ObjId⇒Boolean): Unit
  def defined(attrId: ObjId): Attr[Boolean]
}

class SearchRequestI[Value](
  val tx: BoundToTx,
  val value: Value, val onlyThisValue: Boolean,
  val objId: ObjId, val feed: ObjId⇒Boolean
) extends SearchRequest[Value]

case class BeforeUpdate(attrId: ObjId) extends EventKey[Obj=>Unit]
case class AfterUpdate(attrId: ObjId) extends EventKey[Obj=>Unit]

case class ToRawValueConverter[Value](valueType: AttrValueType[Value])
  extends EventKey[RawValueConverter[Value]]
