
package ee.cone.base.db

import java.time.Instant
import java.util.UUID

import ee.cone.base.connection_api._

//L1
object Types {
  type RawKey = Array[Byte]
  type RawValue = Array[Byte]
}

//Sys Transient
case object TransientChanged extends EventKey[()=>Unit]
trait Transient {
  def update[R](attr: Attr[R]): List[BaseCoHandler]
}

//Sys ES
case class ApplyEvent(attrId: ObjId) extends EventKey[Obj=>Unit]
trait SessionEventSourceOperations {
  def handleSessionKey(uuid: UUID): Unit
  def incrementalApplyAndView[R](view: ()=>R): R
  def unmergedEvents: List[Obj]
  def addEvent(setup: Obj=>ObjId): Unit
  def addRequest(): Unit
  def addUndo(event: Obj): Unit
  def mainSession: Obj
}
case object SessionEventSource extends EventKey[SessionEventSourceOperations]
case object SessionInstantAdded extends EventKey[()=>Unit]

//Sys Alien
trait AlienAttributes {
  def objIdStr: Attr[String]
  def isEditing: Attr[Boolean]
  def targetObj: Attr[Obj]
  def comment: Attr[String]
  def createdAt: Attr[Option[Instant]]
}
trait Alien {
  def update[Value](attr: Attr[Value]): List[BaseCoHandler]
  def wrapForUpdate(obj: Obj): Obj
  def demanded(setup: Obj⇒Unit): Obj
}

//Sys UIStrings
case class ObjIdCaption(objId: ObjId) extends EventKey[String]

trait UIStrings {
  def captions(label: Attr[Obj], attributes: List[Attr[_]])(calculate: Obj⇒String): List[BaseCoHandler]
}