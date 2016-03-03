package ee.cone.base.db

import ee.cone.base.connection_api.{LifeCycle, CanStart, CoHandlerProvider,
BaseCoHandler}

import ee.cone.base.db.Types._

case class ValidationFailure(hint: String, node: DBNode)

trait ListByValueStart[DBEnvKey] {
  def of[Value](attr: Attr[Value]): ListByValue[Value]
  def of[Value](label: Attr[Boolean], prop: Attr[Value]): ListByValue[Value]
}

trait ListByValue[Value] {
  def list(value: Value): List[DBNode]
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