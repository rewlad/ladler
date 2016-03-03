package ee.cone.base.db

import ee.cone.base.connection_api.{LifeCycle, CanStart}
import ee.cone.base.db.Types._

case class ValidationFailure(hint: String, node: DBNode)

trait NodeFactory {
  def noNode: DBNode
  def toNode(tx: RawTx, objId: ObjId): DBNode
  def seqNode(tx: RawTx): DBNode
}

trait DBNodes[DBEnvKey] {
  def where[Value](attr: Attr[Value], value: Value): List[DBNode]
  def where[Value](label: Attr[Boolean], prop: Attr[Value], value: Value): List[DBNode]
  def create(label: Attr[DBNode]): DBNode
}

trait ListByDBNode {
  def list(node: DBNode): List[Attr[_]]
}

trait PreCommitCheckAllOfConnection {
  def switchTx(tx: RawTx, on: Option[Unit]): Unit
  def checkTx(tx: RawTx): Seq[ValidationFailure]
  def create(later: Seq[DBNode]=>Seq[ValidationFailure]): DBNode=>Unit
}

trait TxManager[DBEnvKey] {
  def tx: RawTx
  def needTx(rw: Boolean): Unit
  def closeTx(): Unit
  def commit(): Unit
}

trait DBEnv extends CanStart {
  def createTx(txLifeCycle: LifeCycle, rw: Boolean): RawTx
}

trait MainEnvKey
trait InstantEnvKey