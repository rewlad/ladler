package ee.cone.base.vdom_impl

import ee.cone.base.vdom.JsonBuilder

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
