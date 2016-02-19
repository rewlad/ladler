package ee.cone.base.test_react_db

import java.nio.file.Paths
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import ee.cone.base.util.{Single, Never, Setup}
import scala.collection.immutable.SortedMap

import ee.cone.base.connection_api.{DictMessage, Message}
import ee.cone.base.db._
import ee.cone.base.db.Types.{RawValue, RawKey}
import ee.cone.base.server._
import ee.cone.base.vdom._

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











class TestTx(connection: TestConnection) extends ReceiverOf[Message] {
  /*def regenerateDom() = {
  if(unmergedReadModelIndex.nonEmpty && unmergedReadModelIndex.get.baseVersion < sharedIndex.version)
    unmergedReadModelIndex = None


  connection.mainDB.withTx(rw=false){
    connection.tempDB.withTx(rw=false){

    }

  }

  ???
}*/
  def mainTx: DBAppliedTx

  private lazy val periodicFullReset = new OncePer(1000, reset)
  private def reset() = {
    lifeCycle.close()
    connection.tx.reset()
  }
  //lazy val eventFactConverter = new RawFactConverterImpl(DBLayers.eventFacts, 0L)


}



class DBDelete(db: UpdatableAttrIndex, search: ListObjIdsByValue) {
  def apply(objId: DBNode) =
    search(objId).foreach{ attrId => db(objId, attrId) = DBRemoved }
}

class TxContext {
  def rawTx: RawTx
  class TxLayerContext(level: Long) {
    private lazy val rawIndex = rawTx.rawIndex
    private lazy val rawFactConverter = new RawFactConverterImpl(0L)
    private lazy val rawIndexConverter = new RawSearchConverterImpl
    protected lazy val db =
      new AttrIndexImpl(rawFactConverter, rawIndexConverter, rawIndex, ???) // indexed SysAttrId.sessionId
    protected lazy val search =
      new IndexListObjIdsImpl(rawFactConverter, rawIndexConverter, RawKeyMatcherImpl, rawIndex)
    protected lazy val seq = new ObjIdSequence(db, SysAttrId.lastObjId)
    protected lazy val delete = new DBDelete(db, search)
  }
  class EventTxLayerContext extends TxLayerContext(1L)
  class TempEventTxLayerContext(main: MainEventTxLayer) extends EventTxLayerContext {
    private lazy val dbEvents = new TempEventTxLayer(db, search, seq, delete, main)
  }
  class MainEventTxLayerContext extends EventTxLayerContext {
    lazy val applied = new MainEventTxLayer(db, search)
  }


}

class MainEventTxLayer(db: UpdatableAttrIndex, search: ListObjIdsByValue) {
  def isApplied(tempEventId: DBNode): Boolean =
    search(SysAttrId.tempEventId, DBLongValue(tempEventId)).nonEmpty
  ???
}

class TempEventTxLayer(
  db: UpdatableAttrIndex, search: ListObjIdsByValue, seq: ObjIdSequence, delete: DBDelete,
  main: MainEventTxLayer
){
  private def loadEvent(objId: DBNode) =
    DBEvent(search(objId).map(attrId => (attrId, db(objId, attrId))))

  def purgeAndLoad(sessionKey: String): SessionState =
    if(sessionKey.isEmpty) SessionState(loaded=true, DBRemoved, Nil)
    else search(SysAttrId.sessionKey, DBStringValue(sessionKey)) match {
      case Nil =>
        val objId = seq.inc()
        db(objId, SysAttrId.sessionKey) = DBStringValue(sessionKey)
        val sessionId = DBLongValue(objId)
        SessionState(loaded=true, sessionId, Nil)
      case objId :: Nil =>
        val sessionId = DBLongValue(objId)
        val (past, future) =
          search(SysAttrId.sessionId, sessionId).partition(main.isApplied)
        past.foreach(delete(_))
        SessionState(loaded=true, sessionId, future.map(loadEvent))
    }

  def add(state: SessionState, ev: DBEvent): SessionState = {
    if(state.sessionId == DBRemoved) Never()
    val objId = seq.inc()
    db(objId, SysAttrId.sessionId) = state.sessionId
    ev.data.foreach { case (attrId, value) => db(objId, attrId) = value }
    state.copy(eventList = ev :: state.eventList)
  }
}

case class SessionState(loaded: Boolean, sessionId: DBValue, eventList: List[DBEvent])
class MutableSessionState(
  lifeCycle: LifeCycle, env: TestEnv, sessionKey: String,
  createTxContext: (RawTx,)=>TxContext
){
  private var state = SessionState(loaded=false, DBRemoved, Nil)
  private def withTx(rw: Boolean)(f: ()=>Unit): Unit = lifeCycle.sub{ mTxLifeCycle =>
    val rawTx = env.createTx(mTxLifeCycle, rw)
    // todo: set rawTx for mTxLifeCycle
    f()
    rawTx.commit()
  }
  private def loaded() =
    if(!state.loaded) withTx{ tx => state = tx.purgeAndLoad(sessionKey) }
  def get = { loaded(); state }


  //  withTx(rw=true){ dispatch  }

}

case class DBEvent(data: Map[Attr,DBValue])

case object ResetTxMessage extends Message

/*
class SysProps(db: Index, indexSearch: IndexSearch) {
  class Prop(attrId: Long) {
    def apply(objId: Long) = db(objId, attrId)
    def update(objId: Long, value: DBValue) = db(objId, attrId) = value
    def search(value: DBValue) = indexSearch(attrId, value)
  }
  lazy val lastObjId      = new Prop(0x01)
  lazy val sessionId      = new Prop(0x02)
  lazy val appliedId      = new Prop(0x03)
}
*/
/*
object SysAttrId {
  def lastObjId      = new AttrId(0x01)
  def sessionId      = new AttrId(0x02)
  def sessionKey     = new AttrId(0x03)
  def tempEventId    = new AttrId(0x04)
}
*/

////


class TestConnection0(
  val context: ContextOfConnection,
  val tempDB: TestEnv, //apply and clear on db startup
  val mainDB: TestEnv
) extends ReceiverOf[Message] {
  lazy val diff = new DiffImpl(MapValueImpl)
  lazy val vDom = new Cached[Value] { () =>

    ???
  }
  private var vDomDeferReset = false
  lazy val lifeCycle = connection.context.lifeCycle.sub()
  lazy val dbEventList = new DBEventList(lifeCycle, connection.tempDB, rw=true, connection.sessionId, mainTx)
  def transformMessage(path: List[String], message: DictMessage): Message = {
    val node = ResolveValue(vDom(), path)
      .getOrElse(throw new Exception(s"$path not found"))
    val transformer = node match {
      case v: MessageTransformer => v
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

class TestConnection1(
  val context: ContextOfConnection,
  val tempDB: TestEnv, //apply and clear on db startup
  val mainDB: TestEnv
){



}





