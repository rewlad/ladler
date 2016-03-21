package ee.cone.base.vdom

case class MapVDomValueImpl(pairs: List[VPair]) extends MapVDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    pairs.foreach{ p =>
      builder.append(p.jsonKey)
      p.value.appendJson(builder)
    }
    builder.end()
  }
}
