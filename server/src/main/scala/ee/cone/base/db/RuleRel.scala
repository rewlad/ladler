package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api.{CoHandlerProvider, BaseCoHandler}
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
  preCommitCheck: PreCommitCheck=>BaseCoHandler,
  hasTypeAttr: Attr[Boolean],
  searchIndex: SearchIndex,
  values: ListByValueStart
){
  def apply[Value](attr: Attr[Option[DBNode]]) = {
    val handlers: List[BaseCoHandler] =
      searchIndex.handlers(attr) :::
        preCommitCheck(new TypeRefIntegrityPreCommitCheck(hasTypeAttr, attr, values)) ::
        preCommitCheck(SideRefIntegrityPreCommitCheck(hasTypeAttr, attr)) :: Nil
    RelSideInfo(attr, handlers)
  }
}

//toAttrId must be indexed
class TypeRefIntegrityPreCommitCheck(
  val hasTypeAttr: Attr[Boolean],
  val toAttr: Attr[Option[DBNode]],
  values: ListByValueStart
) extends RefIntegrityPreCommitCheck {
  def affectedBy = hasTypeAttr :: Nil
  def check(nodes: Seq[DBNode]) =
    nodes.flatMap(node => values.of(toAttr).list(Some(node))).flatMap(checkNode)
}

  //def affectedBy = typeAttr :: propAttr :: Nil


abstract class RefIntegrityPreCommitCheck extends PreCommitCheck {
  protected def hasTypeAttr: Attr[Boolean]
  protected def toAttr: Attr[Option[DBNode]]
  protected def checkNode(node: DBNode): Option[ValidationFailure] = node(toAttr) match {
    case None ⇒ None
    case Some(toNode) if toNode(hasTypeAttr) ⇒ None
    case v => Some(ValidationFailure(this,node)) //, s"attr $toAttrId should refer to valid object, but $v found")
  }
}



case class SideRefIntegrityPreCommitCheck(
  hasTypeAttr: Attr[Boolean], toAttr: Attr[Option[DBNode]]
) extends RefIntegrityPreCommitCheck {
  def affectedBy = toAttr :: Nil
  def check(nodes: Seq[DBNode]) = nodes.flatMap(checkNode)
}
