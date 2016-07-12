
package ee.cone.base.db

import java.time.ZoneId
import ee.cone.base.connection_api.{LifeCycle, Attr, ObjId, Obj}

trait FindAttrs {
  def nonEmpty: Attr[Boolean]
}

trait SearchOption
case object FindFirstOnly extends SearchOption

trait FindNodes {
  def noNode: Obj
  def where[Value](
      tx: BoundToTx, index: SearchByLabelProp[Value], value: Value,
      options: List[SearchOption]
  ): List[Obj]
  def single(l: List[Obj]): Obj
  def whereObjId(objId: ObjId): Obj
}

trait DBEnv[DBEnvKey] {
  def dbId: Long
  def roTx(txLifeCycle: LifeCycle): RawIndex
  def rwTx[R](txLifeCycle: LifeCycle)(f: RawIndex⇒R): R
}

trait InstantEnvKey
trait MainEnvKey

trait CurrentTx[DBEnvKey] {
  def apply(): BoundToTx
}

trait LabelFactory {
  def apply(uuid: String): Attr[Obj]
}

trait ObjOrderingFactory {
  def ordering[Value](attr: Attr[Value], reverse: Boolean): Option[Ordering[Obj]]
}

trait ZoneIds { def zoneId: ZoneId  }