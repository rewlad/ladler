package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.db.Types._

case class ValidationFailure(hint: String, node: Obj)

trait SearchOption
case class FindAfter(node: Obj) extends SearchOption
case class FindFrom(node: Obj) extends SearchOption
case class FindUpTo(node: Obj) extends SearchOption
case object FindFirstOnly extends SearchOption
case object FindLastOnly extends SearchOption
case object FindNextValues extends SearchOption

trait FindNodes {
  def where[Value](tx: BoundToTx, label: Attr[Boolean], prop: Attr[Value], value: Value, options: List[SearchOption]): List[Obj]
  def justIndexed: String
}

trait UniqueNodes {
  def create(tx: BoundToTx, label: Attr[Obj], srcId: UUID): Obj
  def whereSrcId(tx: BoundToTx, srcId: UUID): Obj
  def srcId: Attr[Option[UUID]]
  def seqNode(tx: BoundToTx): Obj
  def noNode: Obj
}

trait ListByDBNode extends Attr[List[Attr[_]]]

trait PreCommitCheckAllOfConnection {
  def switchTx(tx: BoundToTx, on: Boolean): Unit
  def checkTx(tx: BoundToTx): Seq[ValidationFailure]
  def create(later: Seq[Obj]=>Seq[ValidationFailure]): Obj=>Unit
}

trait CurrentTx[DBEnvKey] {
  def dbId: Long
  def value: Option[BoundToTx]
  def apply(): BoundToTx
}

trait DBEnv[DBEnvKey] extends CanStart {
  def dbId: Long
  def roTx(txLifeCycle: LifeCycle): RawIndex
  def rwTx[R](f: RawIndex⇒R): R
}

trait MainEnvKey
trait InstantEnvKey

trait DefaultTxManager[DBEnvKey] {
  def roTx[R](f: () ⇒ R): R
  def rwTx[R](f: () ⇒ R): R
  def currentTx: CurrentTx[DBEnvKey]
}
trait SessionMainTxManager {
  def muxTx[R](recreate: Boolean)(f: ()⇒R): R
}

trait LabelFactory {
  def apply(id: Long): Attr[Obj]
}