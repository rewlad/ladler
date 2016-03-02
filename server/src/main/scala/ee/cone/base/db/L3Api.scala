package ee.cone.base.db

import ee.cone.base.connection_api.{CoHandlerProvider, BaseCoHandler}

import ee.cone.base.db.Types._

case class ValidationFailure(calc: PreCommitCheck, node: DBNode)

trait PreCommitCheck extends {
  def affectedBy: List[Attr[_]]
  def check(nodes: Seq[DBNode]): Seq[ValidationFailure]
}

trait ListFeed[From,To] extends Feed {
  def result: List[To]
}

trait ListByValueStart {
  def of[Value](attr: Attr[Value]): ListByValue[Value]
  def of[Value](label: Attr[Boolean], prop: Attr[Value]): ListByValue[Value]
}

trait ListByValue[Value] {
  def list(value: Value): List[DBNode]
  def list(value: Value, fromObjId: Option[ObjId], limit: Long): List[DBNode]
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


