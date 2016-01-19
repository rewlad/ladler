package io.github.rewlad.ladler.vdom

case class MapValueImpl(pairs: List[VPair]) extends MapValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    pairs.foreach{ p =>
      builder.append(p.jsonKey)
      p.value.appendJson(builder)
    }
    builder.end()
  }
}
