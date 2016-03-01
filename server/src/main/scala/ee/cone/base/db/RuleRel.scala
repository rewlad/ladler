package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api.ConnectionComponent
import ee.cone.base.db.Types._

// fail'll probably do nothing in case of outdated rel type

case class RelTypeInfo (
  label: Attr[Boolean], start: RelSideTypeInfo, end: RelSideTypeInfo,
  components: List[ConnectionComponent]
) extends ComponentProvider

case class RelSideTypeInfo(
  side: RelSideInfo, listByValue: ListByValue[Option[DBNode]],
  components: List[ConnectionComponent]
) extends ComponentProvider

class RelTypeInfoFactory(
  relStartSide: RelSideInfo,
  relEndSide: RelSideInfo,
  createList: (Attr[Option[DBNode]],List[ConnectionComponent]) => ListByValue[Option[DBNode]],
  searchIndex: SearchIndex
){
  private def createForSide(label: Attr[Boolean], side: RelSideInfo) = {
    val (attr, components) = searchIndex.attrCalc(label, side.attrId)
    val listByValue = createList(attr, components)
    RelSideTypeInfo(side, listByValue, listByValue.components)
  }
  def apply(label: Attr[Boolean]) = {
    val start = createForSide(label, relStartSide)
    val end = createForSide(label, relEndSide)
    RelTypeInfo(label, start, end, start.components ::: end.components)
  }
}

case class RelSideInfo(
  attrId: Attr[Option[DBNode]],
  listByValue: ListByValue[Option[DBNode]],
  components: List[ConnectionComponent]
) extends ComponentProvider

class RelSideInfoFactory(
  preCommitCheck: PreCommitCheck=>CoHandler[DBNode,Unit],
  createList: (Attr[Option[DBNode]],List[ConnectionComponent]) => ListByValue[Option[DBNode]],
  hasTypeAttr: Attr[Boolean],
  searchIndex: SearchIndex
){
  def apply[Value](attr: Attr[Option[DBNode]]) = {
    val listByValue = createList(attr, searchIndex.attrCalc(attr))
    val components: List[ConnectionComponent] = listByValue.components :::
        preCommitCheck(TypeRefIntegrityPreCommitCheck(hasTypeAttr, attr, listByValue)) ::
        preCommitCheck(SideRefIntegrityPreCommitCheck(hasTypeAttr, attr)) :: Nil


    RelSideInfo(attr, listByValue, components)
  }
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

//toAttrId must be indexed
case class TypeRefIntegrityPreCommitCheck(
  hasTypeAttr: Attr[Boolean],
  toAttr: Attr[Option[DBNode]],
  listToAttr: ListByValue[Option[DBNode]]
) extends RefIntegrityPreCommitCheck {
  def affectedBy = hasTypeAttr :: Nil
  def check(nodes: Seq[DBNode]) =
    nodes.flatMap(node => listToAttr.list(Some(node))).flatMap(checkNode)
}

case class SideRefIntegrityPreCommitCheck(
  hasTypeAttr: Attr[Boolean], toAttr: Attr[Option[DBNode]]
) extends RefIntegrityPreCommitCheck {
  def affectedBy = toAttr :: Nil
  def check(nodes: Seq[DBNode]) = nodes.flatMap(checkNode)
}
