package ee.cone.base.db

import ee.cone.base.connection_api.ConnectionComponent

trait DBNode {
  def objId: Long
  def apply[Value](attr: Prop[Value]): Value
  def update[Value](attr: Prop[Value], value: Value): Unit
}

trait ComponentProvider {
  def components: List[ConnectionComponent]
}

trait Prop[Value] extends ComponentProvider {
  def get(node: DBNode): Value
  def set(node: DBNode, value: Value): Unit
  def attrId: AttrId[Value]
  def nonEmpty: Prop[Boolean]
}

case class ValidationFailure(calc: PreCommitCheck, node: DBNode)

trait PreCommitCheckAttrCalc extends AttrCalc {
  def checkAll(): Seq[ValidationFailure]
}

trait PreCommitCheck extends {
  def affectedBy: List[Prop[_]]
  def check(nodes: Seq[DBNode]): Seq[ValidationFailure]
}

trait ListFeed[From,To] extends Feed {
  def result: List[To]
}

trait ListByValue[Value] extends ComponentProvider {
  def list(value: Value): List[DBNode]
  def list(value: Value, fromNode: DBNode): List[DBNode]
}

trait NodeAttrCalc {
  def affectedBy: List[Prop[_]]
  def beforeUpdate(node: DBNode): Unit
  def afterUpdate(node: DBNode): Unit
}