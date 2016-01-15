package io.github.rewlad.sseserver

trait ToJson {
  def appendJson(builder: JsonBuilder): Unit
}

trait Value extends ToJson

trait Pair {
  def jsonKey: String
  def sameKey(other: Pair): Boolean
  def value: Value
  def withValue(value: Value): Pair
}
///

case class MapValue(value: List[Pair]) extends Value {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    value.foreach{ p =>
      builder.append(p.jsonKey)
      p.value.appendJson(builder)
    }
    builder.end()
  }
}
