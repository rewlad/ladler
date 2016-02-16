package ee.cone.base.db

import java.util.UUID

import ee.cone.base.util.Never

// fail'll probably do nothing in case of outdated rel type

case class RelTypeInfo(
  label: Prop[Option[Boolean]], start: RelSideTypeInfo, end: RelSideTypeInfo,
  attrInfoList: List[AttrInfo]
)

case class RelSideTypeInfo(
  side: RelSideInfo, listByValue: ListByValue[Option[DBNode]],
  attrInfoList: List[AttrInfo]
)

class RelTypeInfoFactory(
  relStartSide: RelSideInfo,
  relEndSide: RelSideInfo,
  createProp: (AttrId,DBValueConverter[Boolean]) => Prop[Option[Boolean]],
  createList: (SearchAttrCalc,DBValueConverter[Option[DBNode]]) => ListByValue[Option[DBNode]],
  converter: DBValueConverter[Boolean],
  searchIndex: SearchIndex
){
  private def createForSide(labelAttrId: AttrId, side: RelSideInfo) = {
    val composedAttrId = searchIndex.composeAttrId(labelAttrId, side.attrId)
    val listByValue = createList(searchIndex.attrCalc(composedAttrId), side.converter)
    RelSideTypeInfo(side, listByValue, listByValue :: Nil)
  }
  def apply(labelAttrId: AttrId) = {
    val label = createProp(labelAttrId,converter)
    val start = createForSide(labelAttrId, relStartSide)
    val end = createForSide(labelAttrId, relEndSide)
    RelTypeInfo(label, start, end, label :: start.attrInfoList ::: end.attrInfoList)
  }
}

case class RelSideInfo(
  attrId: AttrId,
  prop: Prop[Option[DBNode]],
  listByValue: ListByValue[Option[DBNode]],
  attrInfoList: List[AttrInfo]
)(val converter: DBValueConverter[Option[DBNode]])

class RelSideInfoFactory(
  preCommitCheck: PreCommitCheck=>AttrCalc,
  converter: DBValueConverter[Option[DBNode]],
  createProp: (AttrId,DBValueConverter[Option[DBNode]]) => Prop[Option[DBNode]],
  createList: (SearchAttrCalc,DBValueConverter[Option[DBNode]]) => ListByValue[Option[DBNode]],
  hasTypeAttr: Prop[Boolean],
  searchIndex: SearchIndex
){
  def apply(attrId: AttrId) = {
    val prop = createProp(attrId,converter)
    val listByValue = createList(searchIndex.attrCalc(attrId),converter)
    val attrInfoList: List[AttrInfo] = prop :: listByValue :: new AttrInfo {
      def attrCalcList =
        preCommitCheck(TypeRefIntegrityPreCommitCheck(hasTypeAttr, prop, listByValue)) ::
        preCommitCheck(SideRefIntegrityPreCommitCheck(hasTypeAttr, prop)) :: Nil
    } :: Nil

    RelSideInfo(attrId, prop, listByValue, attrInfoList)(converter)
  }
}

  //def affectedBy = typeAttr :: propAttr :: Nil


abstract class RefIntegrityPreCommitCheck extends PreCommitCheck {
  protected def hasTypeAttr: Prop[Boolean]
  protected def toAttr: Prop[Option[DBNode]]
  protected def checkNode(node: DBNode): Option[ValidationFailure] = node(toAttr) match {
    case None ⇒ None
    case Some(toNode) if toNode(hasTypeAttr) ⇒ None
    case v => Some(ValidationFailure(this,node)) //, s"attr $toAttrId should refer to valid object, but $v found")
  }
}

//toAttrId must be indexed
case class TypeRefIntegrityPreCommitCheck(
  hasTypeAttr: Prop[Boolean],
  toAttr: Prop[Option[DBNode]],
  listToAttr: ListByValue[Option[DBNode]]
) extends RefIntegrityPreCommitCheck {
  def affectedBy = hasTypeAttr :: Nil
  def check(nodes: Seq[DBNode]) =
    nodes.flatMap(node => listToAttr.list(Some(node))).flatMap(checkNode)
}

case class SideRefIntegrityPreCommitCheck(
  hasTypeAttr: Prop[Boolean], toAttr: Prop[Option[DBNode]]
) extends RefIntegrityPreCommitCheck {
  def affectedBy = toAttr :: Nil
  def check(nodes: Seq[DBNode]) = nodes.flatMap(checkNode)
}
