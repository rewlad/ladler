package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api.{EventKey, BaseCoHandler}
import ee.cone.base.db.Types._

trait SessionState {
  def sessionKey: UUID
}
trait Mandatory {
  def apply(condAttr: Attr[_], thenAttr: Attr[_]): List[BaseCoHandler]
  def mutual(attrA: Attr[_], attrB: Attr[_]): List[BaseCoHandler]
}
case class ApplyEvent(attr: Attr[Boolean]) extends EventKey[DBNode,Unit]

////

trait EventSourceOperations {
  def addEventStatus(event: DBNode, ok: Boolean): Unit
  def applyEvents(sessionId: Long, isNotLast: DBNode=>Boolean): Unit
  def createEventSource[Value](listByValue: ListByValue[Value], value: Value, seqRef: Ref[Option[Long]]): EventSource
  def addInstant(label: Attr[Option[DBNode]])(fill: DBNode=>Unit): Unit
}

trait EventSource {
  def poll(): Option[DBNode]
}

trait SessionEventSourceOperations {
  def incrementalApplyAndView[R](view: ()=>R): R
  def addEvent(label: Attr[Option[DBNode]])(fill: DBNode=>Unit): Unit
  def addRequest(): Unit
  def addUndo(eventObjId: ObjId): Unit
}

trait MergerEventSourceOperations {
  def incrementalApplyAndCommit(): Unit
}

trait MergerEventSourceAttrs {
  def unmergedRequestsFromId: Attr[Option[Long]]
  def sessionId: Attr[Option[Long]]
  def asRequest: Attr[Option[DBNode]]
}

trait SessionEventSourceAttrs {
  def asEvent: Attr[Option[DBNode]]
  def asInstantSession: Attr[Option[DBNode]]
  def sessionKey: Attr[Option[UUID]]
  def sessionId: Attr[Option[Long]]
  def asRequest: Attr[Option[DBNode]]
}
