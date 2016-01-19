package io.github.rewlad.ladler.vdom

trait ToJson {
  def appendJson(builder: JsonBuilder): Unit
}

trait Value extends ToJson

trait VPair {
  def jsonKey: String
  def sameKey(other: VPair): Boolean
  def value: Value
  def withValue(value: Value): VPair
}
///

case class MapValue(value: List[VPair]) extends Value {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    value.foreach{ p =>
      builder.append(p.jsonKey)
      p.value.appendJson(builder)
    }
    builder.end()
  }
}
