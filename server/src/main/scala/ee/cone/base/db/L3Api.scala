package ee.cone.base.db

trait DBNode {
  def objId: Long
  def apply[Value](attr: Prop[Value]): Value
  def update[Value](attr: Prop[Value], value: Value): Unit
}

trait DBValueConverter[A] {
  def apply(value: A): DBValue
  def apply(value: DBValue): A
}

trait AttrInfo {
  def attrCalcList: List[AttrCalc]
}

trait Prop[Value] extends AttrInfo {
  def get(node: DBNode): Value
  def set(node: DBNode, value: Value): Unit
  def converter: DBValueConverter[Value]
  def attrId: AttrId
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

trait ListFeed[From,To] extends Feed[From] {
  def result: List[To]
}

trait ListByValue[Value] extends AttrInfo {
  def list(value: Value): List[DBNode]
  def list(value: Value, fromNode: DBNode): List[DBNode]
}

trait NodeAttrCalc {
  def affectedBy: List[Prop[_]]
  def beforeUpdate(node: DBNode): Unit
  def afterUpdate(node: DBNode): Unit
}