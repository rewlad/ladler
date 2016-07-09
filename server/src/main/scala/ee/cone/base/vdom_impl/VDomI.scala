package ee.cone.base.vdom_impl

import ee.cone.base.connection_api.{Attr, AttrValueType, EventKey, Obj}
import ee.cone.base.vdom._
import ee.cone.base.vdom.Types.VDomKey

trait JsonToString {
  def apply(value: VDomValue): String
}

trait WasNoVDomValue extends VDomValue

trait VPair {
  def jsonKey: String
  def sameKey(other: VPair): Boolean
  def value: VDomValue
  def withValue(value: VDomValue): VPair
}

trait MapVDomValue extends VDomValue {
  def pairs: List[VPair]
}

trait Diff {
  def diff(vDom: VDomValue): Option[MapVDomValue]
}
