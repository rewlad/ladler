package io.github.rewlad.ladler.vdom

import io.github.rewlad.ladler.connection_api.ReceivedMessage

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
  def diff(prevValue: Value, currValue: Value): Option[MapValue]
}

trait MessageHandler {
  def handleMessage(message: ReceivedMessage): Unit
}