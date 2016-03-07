package ee.cone.base.test_react_db

import scala.collection.immutable.SortedMap

import ee.cone.base.connection_api.LifeCycle
import ee.cone.base.db.{RawIndex, NonEmptyUnmergedIndex,
UnsignedBytesOrdering, DBEnv}
import ee.cone.base.db.Types._
import ee.cone.base.util.Setup

class TestEnv extends DBEnv {
  private var data = SortedMap[RawKey, RawValue]()(UnsignedBytesOrdering)
  private def createRawIndex() = Setup(new NonEmptyUnmergedIndex) { i =>
    synchronized { i.data = data }
  }
  def roTx(txLifeCycle: LifeCycle) = createRawIndex()
  private object RW
  def rwTx[R](f: RawIndex ⇒ R): R = RW.synchronized{
    val index = createRawIndex()
    Setup(f(index))(_ ⇒ synchronized { data = index.data })
  }
  def start() = ()
}
