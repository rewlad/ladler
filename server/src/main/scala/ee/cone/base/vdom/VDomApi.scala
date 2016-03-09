package ee.cone.base.vdom

import ee.cone.base.connection_api.{DictMessage, EventKey}
import ee.cone.base.vdom.Types.VDomKey

trait JsonToString {
  def apply(value: Value): String
}

trait JsonBuilder {
  def startArray(): JsonBuilder
  def startObject(): JsonBuilder
  def end(): JsonBuilder
  def append(value: String): JsonBuilder
}

trait ToJson {
  def appendJson(builder: JsonBuilder): Unit
}

trait Value extends ToJson

trait WasNoValue extends Value

trait VPair {
  def jsonKey: String
  def sameKey(other: VPair): Boolean
  def value: Value
  def withValue(value: Value): VPair
}

trait MapValue extends Value {
  def pairs: List[VPair]
}

trait Diff {
  def diff(vDom: Value): Option[MapValue]
}

case class ViewPath(path: String) extends EventKey[String,Value]

trait CurrentView {
  def invalidate(): Unit
  def until(value: Long): Unit
}

trait MessageReceiver {
  def receive: PartialFunction[DictMessage,Unit]
}

////

object Types {
  type VDomKey = String
}

trait ChildPairFactory {
  def apply[C](key: VDomKey, theElement: Value, elements: List[ChildPair[_]]): ChildPair[C]
}

trait ChildPair[C] extends VPair {
  def key: VDomKey
}

trait InputAttributes {
  def appendJson(builder: JsonBuilder, value: String, deferSend: Boolean): Unit
}

trait OnChange {
  def unapply(message: DictMessage): Option[String]
}

trait OnClick {
  def unapply(message: DictMessage): Option[Unit]
}