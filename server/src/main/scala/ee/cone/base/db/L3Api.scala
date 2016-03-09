package ee.cone.base.db

import ee.cone.base.connection_api.{EventKey, LifeCycle, CanStart}
import ee.cone.base.db.Types._

case class ValidationFailure(hint: String, node: DBNode)

trait NodeFactory {
  def noNode: DBNode
  def toNode(tx: BoundToTx, objId: ObjId): DBNode
  def seqNode(tx: BoundToTx): DBNode
}

trait DBNodes {
  def where[Value](tx: BoundToTx, label: Attr[Boolean], prop: Attr[Value], value: Value): List[DBNode]
  def where[Value](tx: BoundToTx, label: Attr[Boolean], prop: Attr[Value], value: Value, from: Option[Long], limit: Long): List[DBNode]
  def create(tx: BoundToTx, label: Attr[DBNode]): DBNode
}

trait ListByDBNode extends Attr[List[Attr[_]]]

trait PreCommitCheckAllOfConnection {
  def switchTx(tx: BoundToTx, on: Boolean): Unit
  def checkTx(tx: BoundToTx): Seq[ValidationFailure]
  def create(later: Seq[DBNode]=>Seq[ValidationFailure]): DBNode=>Unit
}

trait CurrentTx[DBEnvKey] {
  def apply(): BoundToTx
}

trait DBEnv extends CanStart {
  def roTx(txLifeCycle: LifeCycle): RawIndex
  def rwTx[R](f: RawIndex⇒R): R
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