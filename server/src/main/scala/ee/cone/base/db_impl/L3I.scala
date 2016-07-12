package ee.cone.base.db_impl

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.db._

case class ValidationFailure(hint: String, node: Obj)

case class FindAfter(node: Obj) extends SearchOption
case class FindFrom(node: Obj) extends SearchOption
case class FindUpTo(node: Obj) extends SearchOption
case object FindNextValues extends SearchOption
case object FindLastOnly extends SearchOption

trait FindNodesI extends FindNodes {
  def zeroNode: Obj
  def nextNode(obj: Obj): Obj
  def toObjId(uuid: UUID): ObjId
}

//trait ListByDBNode extends Attr[List[Attr[_]]]

trait PreCommitCheckAllOfConnection {
  def switchTx(tx: BoundToTx, on: Boolean): Unit
  def checkTx(tx: BoundToTx): Seq[ValidationFailure]
  def create(later: Seq[Obj]=>Seq[ValidationFailure]): Obj=>Unit
}

trait DefaultTxManager[DBEnvKey] {
  def roTx[R](f: () ⇒ R): R
  def rwTx[R](f: () ⇒ R): R
  def currentTx: CurrentTx[DBEnvKey]
}
trait SessionMainTxManager {
  def muxTx[R](recreate: Boolean)(f: ()⇒R): R
}

trait ObjOrderingFactoryI extends ObjOrderingFactory {
  def ordering[Value](valueType: AttrValueType[Value]): Option[Ordering[Value]]
  def handlers[Value](asType: AttrValueType[Value])(implicit ord: Ordering[Value]): List[BaseCoHandler]
}
