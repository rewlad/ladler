package ee.cone.base.db

import ee.cone.base.connection_api.ConnectionComponent

import ee.cone.base.db.Types._

trait ComponentProvider {
  def components: List[ConnectionComponent]
}

case class ValidationFailure(calc: PreCommitCheck, node: DBNode)

trait PreCommitCheck extends {
  def affectedBy: List[Attr[_]]
  def check(nodes: Seq[DBNode]): Seq[ValidationFailure]
}

trait ListFeed[From,To] extends Feed {
  def result: List[To]
}

trait ListByValue[Value] extends ComponentProvider {
  def list(value: Value): List[DBNode]
  def list(value: Value, fromObjId: ObjId, limit: Long): List[DBNode]
}

trait PreCommitCheckAllOfConnection {
  def switchTx(on: Option[Unit]): Unit
  def checkTx(): Seq[ValidationFailure]
}