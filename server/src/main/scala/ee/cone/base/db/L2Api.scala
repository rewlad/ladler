package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.db.Types._

trait BoundToTx

case object TxSelectorKey extends EventKey[TxSelector]
trait TxSelector {
  def txOf(obj: Obj): BoundToTx
  def rawIndex(objId: ObjId): RawIndex
  def rawIndex(tx: BoundToTx): RawIndex
}

trait NodeAttrs {
  def objId: Attr[ObjId]
}

trait ObjIdFactory {
  def noObjId: ObjId
  def toObjId(hiObjId: Long, loObjId: Long): ObjId
  def toObjId(uuid: UUID): ObjId
  def toObjId(uuid: String): ObjId
}

trait AttrFactory {
  def apply[V](uuid: String, valueType: AttrValueType[V]): Attr[V]
  def define[V](attrId: ObjId, valueType: AttrValueType[V]): Attr[V]
  def derive[V](attrA: ObjId, attrB: Attr[V]): Attr[V]
  def defined(attrId: ObjId): Attr[Boolean]
  def attrId[V](attr: Attr[V]): ObjId
  def converter[V](attr: Attr[V]): RawValueConverter[V]
}

class AttrValueType[Value]

trait FactIndex {
  def switchReason(node: Obj): Unit
  def execute(obj: Obj)(feed: ObjId⇒Boolean): Unit
  def handlers[Value](attr: Attr[Value]): List[BaseCoHandler]
}

trait SearchIndex {
  def handlers[Value](labelAttr: Attr[_], propAttr: Attr[Value]): List[BaseCoHandler]
}
case class SearchByLabelProp[Value](label: ObjId, prop: ObjId)
  extends EventKey[SearchRequest[Value]=>Unit]
class SearchRequest[Value](
  val tx: BoundToTx,
  val value: Value, val onlyThisValue: Boolean,
  val objId: ObjId, val feed: ObjId⇒Boolean
)

case class BeforeUpdate(attrId: ObjId) extends EventKey[Obj=>Unit]
case class AfterUpdate(attrId: ObjId) extends EventKey[Obj=>Unit]
trait OnUpdate {
  //invoke will be called before and after update if all attrs are defined
  def handlers(definedAttrs: List[ObjId], invoke: (Boolean,Obj) ⇒ Unit): List[BaseCoHandler]
}
case class ToDefined(attrId: ObjId) extends EventKey[Attr[Boolean]]
case class ToRawValueConverter[Value](valueType: AttrValueType[Value])
  extends EventKey[RawValueConverter[Value]]
