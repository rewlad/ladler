package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api.{CoHandler, CoHandlerProvider, BaseCoHandler}
import ee.cone.base.db.Types._

// fail'll probably do nothing in case of outdated rel type

case class RelTypeInfo (
  label: Attr[Boolean], start: RelSideTypeInfo, end: RelSideTypeInfo,
  handlers: List[BaseCoHandler]
) extends CoHandlerProvider

case class RelSideTypeInfo(
  side: RelSideInfo,
  handlers: List[BaseCoHandler]
) extends CoHandlerProvider

class RelTypeInfoFactory(
  relStartSide: RelSideInfo,
  relEndSide: RelSideInfo,
  searchIndex: SearchIndex
){
  private def createForSide(label: Attr[Boolean], side: RelSideInfo) =
    RelSideTypeInfo(side, searchIndex.handlers(label, side.attrId))

  def apply(label: Attr[Boolean]) = {
    val start = createForSide(label, relStartSide)
    val end = createForSide(label, relEndSide)
    RelTypeInfo(label, start, end, start.handlers ::: end.handlers)
  }
}

case class RelSideInfo(
  attrId: Attr[Option[DBNode]],
  handlers: List[BaseCoHandler]
) extends CoHandlerProvider

class RelSideInfoFactory(
  preCommitCheck: PreCommitCheckAllOfConnection,
  hasTypeAttr: Attr[Boolean],
  searchIndex: SearchIndex,
  values: ListByValueStart[MainEnvKey]
){
  def apply[Value](attr: Attr[Option[DBNode]]) = {
    val handlers: List[BaseCoHandler] =
      searchIndex.handlers(attr) :::
        toHandler(new TypeRefIntegrityPreCommitCheck(hasTypeAttr, attr, values)) ::
        toHandler(new SideRefIntegrityPreCommitCheck(hasTypeAttr, attr)) :: Nil
    RelSideInfo(attr, handlers)
  }
  def toHandler(check: RefIntegrityPreCommitCheck) =
    CoHandler(AfterUpdate(check.affectedBy)::Nil)(preCommitCheck.create(check.check))
}

//toAttrId must be indexed
class TypeRefIntegrityPreCommitCheck(
  val hasTypeAttr: Attr[Boolean],
  val toAttr: Attr[Option[DBNode]],
  values: ListByValueStart[MainEnvKey]
) extends RefIntegrityPreCommitCheck {
  def affectedBy = hasTypeAttr
  def check(nodes: Seq[DBNode]) =
    nodes.flatMap(node => values.of(toAttr).list(Some(node))).flatMap(checkNode)
}

  //def affectedBy = typeAttr :: propAttr :: Nil

abstract class RefIntegrityPreCommitCheck {
  def affectedBy: Attr[Boolean]
  def check(nodes: Seq[DBNode]): Seq[ValidationFailure]
  protected def hasTypeAttr: Attr[Boolean]
  protected def toAttr: Attr[Option[DBNode]]
  protected def checkNode(node: DBNode): Option[ValidationFailure] = node(toAttr) match {
    case None ⇒ None
    case Some(toNode) if toNode(hasTypeAttr) ⇒ None
    case v => Some(ValidationFailure("refs",node)) //, s"attr $toAttrId should refer to valid object, but $v found")
  }
}

class SideRefIntegrityPreCommitCheck(
  val hasTypeAttr: Attr[Boolean], val toAttr: Attr[Option[DBNode]]
) extends RefIntegrityPreCommitCheck {
  def affectedBy = toAttr.nonEmpty
  def check(nodes: Seq[DBNode]) = nodes.flatMap(checkNode)
}
