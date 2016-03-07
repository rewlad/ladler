package ee.cone.base.test_react_db

import java.nio.file.Paths
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import ee.cone.base.util.{Single, Never, Setup}
import scala.collection.immutable.SortedMap

import ee.cone.base.connection_api.DictMessage
import ee.cone.base.db._
import ee.cone.base.db.Types.{RawValue, RawKey}
import ee.cone.base.server._
import ee.cone.base.vdom._

/*
class LifeCacheState[C] {
  private var state: Option[C] = None
  def apply() = state
  def set(lifeCycle: LifeCycle, value: =>C) = {
    if(state.nonEmpty) Never()
    lifeCycle.setup()(_ => state = None)
    state = Option(value)
  }
}

class LifeCache[C] {
  lazy val lifeCycle = new LifeCacheState[LifeCycle]
  def apply(create: =>C): ()=>C = {
    val base = new LifeCacheState[C]
    () =>
      if(base().isEmpty) base.set(lifeCycle().get, create)
      base().get
  }
}
*/

////

/*
class FindOrCreateSrcId(
  srcId: Attr[UUID],
  searchSrcId: ListByValue[UUID],
  seq: ObjIdSequence
) {
  def apply(value: UUID) = Single.option(searchSrcId.list(value))
    .getOrElse(Setup(seq.inc()){ node => node(srcId) = value })
}
*/

////

/*
trait Model
trait View {
  def modelVersion: String
  def generateDom: Value
}

class IndexView extends View {
  def modelVersion = "index"
  def generateDom = {
    import Tag._
    root(
      anchor(0,"#big","[big]")::
        anchor(1,"#interactive","[interactive]")::
        Nil
    )
  }
}
*/

// ? periodic re-snap
// ? periodicFullReset = new OncePer(1000, reset)
// notify session using db



class A {
  lazy val diff = new DiffImpl(MapValueImpl)
  lazy val vDom = new Cached[Value] { () =>

    ???
  }
  private var vDomDeferReset = false

  def transformMessage(path: List[String], message: DictMessage): Message = {
    val node = ResolveValue(vDom(), path)
      .getOrElse(throw new Exception(s"$path not found"))
    val transformer = node match {
      case v: MessageReceiver => v
    }
    transformer.transformMessage.lift(message).get
  }

  def receive = {
    case message@WithVDomPath(path) => receive(transformMessage(path, message))
    case ev@DBEvent(data) =>
      dbEventList.add(ev)
      vDom.reset()
    // non-db events here, vDomDeferReset = true
    // hash
    case ResetTxMessage => reset() // msg from merger can reset full context
    case PeriodicMessage =>
      periodicFullReset() // ?vDom if fail?
      if (vDomDeferReset) {
        vDomDeferReset = false
        vDom.reset()
      }
      connection.diff.diff(vDom()).foreach(d =>
        connection.context.sender.send("showDiff", JsonToString(d))
      )
  }
}

