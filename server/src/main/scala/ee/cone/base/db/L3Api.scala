package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.db.Types._

case class ValidationFailure(hint: String, node: Obj)

trait SearchOption
case class FindAfter(node: Obj) extends SearchOption
case class FindFrom(node: Obj) extends SearchOption
case class FindUpTo(node: Obj) extends SearchOption
case object FindNextValues extends SearchOption
case object FindFirstOnly extends SearchOption
case object FindLastOnly extends SearchOption

trait FindAttrs {
  def justIndexed: Attr[String]
  def nonEmpty: Attr[Boolean]
}

trait FindNodes {
  def noNode: Obj
  def zeroNode: Obj
  def nextNode(obj: Obj): Obj
  def single(l: List[Obj]): Obj
  def where[Value](tx: BoundToTx, index: SearchByLabelProp[Value], value: Value, options: List[SearchOption]): List[Obj]
  def justIndexed: String
  def whereObjId(objId: ObjId): Obj
  def toObjId(uuid: UUID): ObjId
  def toUUIDString(objId: ObjId): String
}

//trait ListByDBNode extends Attr[List[Attr[_]]]

trait PreCommitCheckAllOfConnection {
  def switchTx(tx: BoundToTx, on: Boolean): Unit
  def checkTx(tx: BoundToTx): Seq[ValidationFailure]
  def create(later: Seq[Obj]=>Seq[ValidationFailure]): Obj=>Unit
}

trait CurrentTx[DBEnvKey] {
  def dbId: Long
  def value: Option[BoundToTx]
  def apply(): BoundToTx
}

trait DBEnv[DBEnvKey] {
  def dbId: Long
  def roTx(txLifeCycle: LifeCycle): RawIndex
  def rwTx[R](txLifeCycle: LifeCycle)(f: RawIndex⇒R): R
}

trait MainEnvKey
trait InstantEnvKey

trait DefaultTxManager[DBEnvKey] {
  def roTx[R](f: () ⇒ R): R
  def rwTx[R](f: () ⇒ R): R
  def currentTx: CurrentTx[DBEnvKey]
}
trait SessionMainTxManager {
  def muxTx[R](recreate: Boolean)(f: ()⇒R): R
}

trait LabelFactory {
  def apply(uuid: String): Attr[Obj]
}