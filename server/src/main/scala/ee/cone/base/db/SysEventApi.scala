package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api.{Obj, Attr, EventKey, BaseCoHandler}
import ee.cone.base.db.Types._

trait SessionState {
  def sessionKey: UUID
}

////

case class ApplyEvent(attr: Attr[Boolean]) extends EventKey[Obj=>Unit]

/*
trait Ref[Value] {
  def apply(): Value
  def update(value: Value): Unit
}*/

trait EventSourceOperations {
  def undo(ev: Obj): Unit
}

trait ForSessionEventSourceOperations extends EventSourceOperations {
  def unmergedEvents(instantSession: Obj): List[Obj]
  def applyEvents(instantSession: Obj): Unit
  def addInstant(instantSession: Obj, label: Attr[Obj]): Obj
}

trait ForMergerEventSourceOperations extends EventSourceOperations {
  def addCommit(req: Obj): Unit
  def applyRequestedEvents(req: Obj): Unit
  def nextRequest(): Obj
}

case object SessionEventSource extends EventKey[SessionEventSourceOperations]

trait SessionEventSourceOperations {
  var decoupled: Boolean
  def incrementalApplyAndView[R](view: ()=>R): R
  def unmergedEvents: List[Obj]
  def addEvent(setup: Obj=>(Attr[Boolean],String)): Unit
  def addRequest(): Unit
  def addUndo(eventSrcId: UUID): Unit
  def sessionKey: UUID
  def comment: Attr[String]
}

trait SessionEventSourceAttrs {
  def asInstantSession: Attr[Obj]
  def instantSession: Attr[Obj]
  def sessionKey: Attr[Option[UUID]]
  def mainSessionSrcId: Attr[Option[UUID]]
  def asEvent: Attr[Obj]
  def requested: Attr[Boolean]
  def asCommit: Attr[Obj]
  def applyAttr: Attr[Attr[Boolean]]
  def comment: Attr[String]
}
