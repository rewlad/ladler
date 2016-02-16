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

trait Prop[Value] {
  def get(node: DBNode): Value
  def set(node: DBNode, value: Value): Unit
  def converter: DBValueConverter[Value]
}

trait ListFeed[From,To] extends Feed[From] {
  def result: List[To]
}