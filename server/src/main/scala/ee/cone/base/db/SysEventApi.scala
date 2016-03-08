package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api.{EventKey, BaseCoHandler}
import ee.cone.base.db.Types._

trait SessionState {
  def sessionKey: UUID
}

////

case class ApplyEvent(attr: Attr[Boolean]) extends EventKey[DBNode,Unit]

trait EventSourceOperations {
  def isUndone(event: DBNode): Boolean
  def createEventSource[Value](label: Attr[DBNode], prop: Attr[Value], value: Value, seqRef: Ref[DBNode], max: ObjId): EventSource
  def applyEvents(instantSession: DBNode, max: ObjId): Unit
  def addEventStatus(event: DBNode, ok: Boolean): Unit
  def requested: String
}

trait EventSource {
  def poll(): DBNode
}

trait SessionEventSourceOperations {
  def incrementalApplyAndView[R](view: ()=>R): R
  def addEvent(fill: DBNode=>Unit): Unit
  def addRequest(): Unit
  def addUndo(eventObjId: ObjId): Unit
}

trait MergerEventSourceAttrs {
  def lastMergedRequest: Attr[DBNode]
  def instantSession: Attr[DBNode]
  def asRequest: Attr[DBNode]
  def requested: Attr[String]
}

trait SessionEventSourceAttrs {
  def asInstantSession: Attr[DBNode]
  def instantSession: Attr[DBNode]
  def sessionKey: Attr[UUID]
  def asEvent: Attr[DBNode]
  def asRequest: Attr[DBNode]
  def asEventStatus: Attr[DBNode]
  def requested: Attr[String]
}
