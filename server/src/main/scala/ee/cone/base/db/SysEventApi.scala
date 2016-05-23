package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api.{Obj, Attr, EventKey, BaseCoHandler}
import ee.cone.base.db.Types._

trait SessionState {
  def sessionKey: UUID
}

////

case class ApplyEvent(attrId: ObjId) extends EventKey[Obj=>Unit]

case object SessionInstantProbablyAdded extends EventKey[()=>Unit]

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
  def findCommit: SearchByLabelProp[String]
  def findCommitByInstantSession: SearchByLabelProp[Obj]
  def findInstantSessionBySessionKey: SearchByLabelProp[Option[UUID]]
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
  def addEvent(setup: Obj=>(ObjId,String)): Unit
  def addRequest(): Unit
  def addUndo(event: Obj): Unit
  def sessionKey: UUID
  def comment: Attr[String]
}

trait SessionEventSourceAttrs {
  def asInstantSession: Attr[Obj]
  def instantSession: Attr[Obj]
  def sessionKey: Attr[Option[UUID]]
  def mainSession: Attr[Obj]
  def asEvent: Attr[Obj]
  def requested: ObjId
  def asCommit: Attr[Obj]
  def applyAttr: Attr[ObjId]
  def comment: Attr[String]
}
