package ee.cone.base.db

import java.util.UUID

import ee.cone.base.util.Never

// fail'll probably do nothing in case of outdated rel type

class RelSideAttrInfoList(
  preCommitCheck: PreCommitCheck=>AttrCalc,
  createSearchAttrInfo: IndexComposer,
  attrs: SearchByNode
) {
  def apply(
    relSideAttrInfo: List[SearchByValue[Option[DBNode]]], typeAttr: CalcIndex,
    relTypeAttrInfo: List[CalcIndex]
  ): List[AttrInfo] = relSideAttrInfo.flatMap{ searchToAttr ⇒
    val propInfo = searchToAttr.direct.ruled
    val relComposedAttrInfo: List[(CalcIndex,CalcIndex)] =
      relTypeAttrInfo.map(labelAttr⇒(labelAttr,createSearchAttrInfo(labelAttr, propInfo)))
    val relTypeAttrIdToComposedAttr =
      relComposedAttrInfo.map{ case(l,i) ⇒ l.attrId.toString → i }.toMap
    val indexedAttrIds = relTypeAttrIdToComposedAttr.values.toSet
    val calc = TypeIndexAttrCalc(typeAttr, propInfo, attrs)(relTypeAttrIdToComposedAttr, indexedAttrIds)
    val integrity = createRefIntegrityPreCommitCheckList(typeAttr, searchToAttr)
    calc :: propInfo :: relComposedAttrInfo.map(_._2) ::: integrity
  }
  private def createRefIntegrityPreCommitCheckList(
    typeAttr: CalcIndex,
    searchToAttrId: SearchByValue[Option[DBNode]]
  ): List[AttrCalc] =
    preCommitCheck(TypeRefIntegrityPreCommitCheck(typeAttr, searchToAttrId)) ::
      preCommitCheck(SideRefIntegrityPreCommitCheck(typeAttr, searchToAttrId.direct)) :: Nil
}

case class TypeIndexAttrCalc(
  typeAttr: CalcIndex, propAttr: CalcIndex, attrs: SearchByNode,
  version: String = "a6e93a68-1df8-4ee7-8b3f-1cb5ae768c42"
)(
  relTypeIdToAttr: String=>CalcIndex, indexed: CalcIndex=>Boolean // relTypeIdToAttr.getOrElse(typeIdStr, throw new Exception(s"bad rel type $typeIdStr of $objId never here"))
) extends AttrCalc {
  def affectedBy = typeAttr :: propAttr :: Nil
  def beforeUpdate(node: DBNode) = ()
  def afterUpdate(node: DBNode) = {
    node(attrs).foreach(attr => if(indexed(attr)) node(attr) = DBRemoved)
    (node(typeAttr), node(propAttr)) match {
      case (_,DBRemoved) | (DBRemoved,_) => ()
      case (DBStringValue(typeIdStr),value) => node(relTypeIdToAttr(typeIdStr)) = value
      case _ => Never()
    }
  }
}

abstract class RefIntegrityPreCommitCheck extends PreCommitCheck {
  protected def typeAttr: CalcIndex
  protected def toAttr: RuledIndexAdapter[Option[DBNode]]
  protected def checkNode(node: DBNode): Option[ValidationFailure] = node(toAttr) match {
    case None ⇒ None
    case Some(toNode) if toNode(typeAttr) != DBRemoved ⇒ None
    case v => Some(ValidationFailure(this,node)) //, s"attr $toAttrId should refer to valid object, but $v found")
  }
}

//toAttrId must be indexed
case class TypeRefIntegrityPreCommitCheck(
  typeAttr: CalcIndex,
  searchToAttr: SearchByValue[Option[DBNode]],
  version: String = "b2232ecf-734c-4cfa-a88f-78b066a01cd3"
) extends RefIntegrityPreCommitCheck {
  protected def toAttr = searchToAttr.direct
  def affectedBy = typeAttr :: Nil
  def check(nodes: Seq[DBNode]) =
    nodes.flatMap(node => searchToAttr.get(Some(node))).flatMap(checkNode)
}

case class SideRefIntegrityPreCommitCheck(
  typeAttr: CalcIndex, toAttr: RuledIndexAdapter[Option[DBNode]],
  version: String = "677f2fdc-b56e-4cf8-973f-db148ee3f0c4"
) extends RefIntegrityPreCommitCheck {
  def affectedBy = toAttr.ruled :: Nil
  def check(nodes: Seq[DBNode]) = nodes.flatMap(checkNode)
}
