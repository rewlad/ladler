package ee.cone.base.db

import ee.cone.base.connection_api.BaseCoHandler

import ee.cone.base.db.Types._

trait CoHandlerProvider {
  def handlers: List[BaseCoHandler]
}

case class ValidationFailure(calc: PreCommitCheck, node: DBNode)

trait PreCommitCheck extends {
  def affectedBy: List[Attr[_]]
  def check(nodes: Seq[DBNode]): Seq[ValidationFailure]
}

trait ListFeed[From,To] extends Feed {
  def result: List[To]
}

trait ListByValue[Value] extends CoHandlerProvider {
  def list(value: Value): List[DBNode]
  def list(value: Value, fromObjId: ObjId, limit: Long): List[DBNode]
}

trait PreCommitCheckAllOfConnection {
  def switchTx(tx: RawTx, on: Option[Unit]): Unit
  def checkTx(tx: RawTx): Seq[ValidationFailure]
}

trait TxManager {
  def tx: RawTx
  def needTx(rw: Boolean): Unit
  def closeTx(): Unit
  def commit(): Unit
}


