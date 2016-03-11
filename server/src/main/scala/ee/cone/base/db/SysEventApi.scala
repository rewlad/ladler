package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api.{Obj, Attr, EventKey, BaseCoHandler}
import ee.cone.base.db.Types._

trait SessionState {
  def sessionKey: UUID
}

////

case object AddEvent extends EventKey[Obj=>Attr[Boolean],Unit]
case class ApplyEvent(attr: Attr[Boolean]) extends EventKey[Obj,Unit]

trait Ref[Value] {
  def apply(): Value
  def update(value: Value): Unit
}

trait EventSourceOperations {
  def isUndone(event: Obj): Boolean
  def ref(node: Obj, attr: Attr[Obj]): Ref[Obj]
  def createEventSource[Value](label: Attr[Obj], prop: Attr[Value], value: Value, seqRef: Ref[Obj], options: List[SearchOption]): EventSource
  def applyEvents(instantSession: Obj, options: List[SearchOption]): Unit
  def addEventStatus(event: Obj, ok: Boolean): Unit
  def addInstant(instantSession: Obj, label: Attr[Obj]): Obj
  def requested: String
}

trait EventSource {
  def poll(): Obj
}

trait SessionEventSourceOperations {
  def incrementalApplyAndView[R](view: ()=>R): R
  def addRequest(): Unit
  def addUndo(eventSrcId: UUID): Unit
}

trait MergerEventSourceAttrs {
  def lastMergedRequest: Attr[Obj]
  def instantSession: Attr[Obj]
  def asRequest: Attr[Obj]
  def requested: Attr[String]
}

trait SessionEventSourceAttrs {
  def asInstantSession: Attr[Obj]
  def instantSession: Attr[Obj]
  def sessionKey: Attr[Option[UUID]]
  def asEvent: Attr[Obj]
  def asRequest: Attr[Obj]
  def asEventStatus: Attr[Obj]
  def requested: Attr[String]
  def applyAttr: Attr[Attr[Boolean]]
}
