package ee.cone.base.db_impl

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.db.{SearchByLabelProp, SessionEventSourceOperations}
import ee.cone.base.db.Types._

trait SessionState {
  def sessionKey: UUID
}

////

case class ApplyEvent(attrId: ObjId) extends EventKey[Obj=>Unit]



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
  def findCommit: SearchByLabelProp[Obj]
  def findCommitByInstantSession: SearchByLabelProp[Obj]
  def findInstantSessionBySessionKey: SearchByLabelProp[Option[UUID]]
}

trait ForMergerEventSourceOperations extends EventSourceOperations {
  def addCommit(req: Obj): Unit
  def applyRequestedEvents(req: Obj): Unit
  def nextRequest(): Obj
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
}
