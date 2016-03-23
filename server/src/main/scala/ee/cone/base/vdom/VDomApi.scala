package ee.cone.base.vdom

import java.util.UUID

import ee.cone.base.connection_api.{Attr, DictMessage, EventKey}
import ee.cone.base.vdom.Types.VDomKey

trait JsonToString {
  def apply(value: VDomValue): String
}

trait JsonBuilder {
  def startArray(): JsonBuilder
  def startObject(): JsonBuilder
  def end(): JsonBuilder
  def append(value: String): JsonBuilder
  def append(value: Boolean): JsonBuilder
}

trait ToJson {
  def appendJson(builder: JsonBuilder): Unit
}

trait VDomValue extends ToJson

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

case class ViewPath(path: String) extends EventKey[String=>VDomValue]

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
  def apply[C](key: VDomKey, theElement: VDomValue, elements: List[ChildPair[_]]): ChildPair[C]
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

trait AlienAttrFactory {
  def apply[Value](attr: Attr[Value]): UUID => Value => Unit
}

////

trait Tags {
  def root(children: ChildPair[OfDiv]*): VDomValue
  def text(key: VDomKey, text: String): ChildPair[OfDiv]
}