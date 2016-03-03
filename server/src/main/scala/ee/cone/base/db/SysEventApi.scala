package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api.{EventKey, BaseCoHandler}
import ee.cone.base.db.Types._

trait SessionState {
  def sessionKey: UUID
}

case class ApplyEvent(attr: Attr[Boolean]) extends EventKey[DBNode,Unit]

////

trait EventSourceOperations {
  def addEventStatus(event: DBNode, ok: Boolean): Unit
  def applyEvents(instantSession: DBNode, isNotLast: DBNode=>Boolean): Unit
  def createEventSource[Value](label: Attr[DBNode], prop: Attr[Value], value: Value, seqRef: Ref[DBNode]): EventSource
  def addEvent(instantSession: DBNode, fill: DBNode=>Unit): Unit
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

trait MergerEventSourceOperations {
  def incrementalApplyAndCommit(): Unit
}

trait MergerEventSourceAttrs {
  def lastMergedRequest: Attr[DBNode]
  def instantSession: Attr[DBNode]
  def asRequest: Attr[DBNode]
  def requested: Attr[String]
}

trait SessionEventSourceAttrs {
  def asInstantSession: Attr[DBNode]
  def sessionKey: Attr[UUID]
  def asRequest: Attr[DBNode]
  def requested: Attr[String]
}
