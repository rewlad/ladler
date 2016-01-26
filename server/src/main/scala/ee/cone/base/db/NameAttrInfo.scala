package ee.cone.base.db

import ee.cone.base.util.{LongFits, Never}

import scala.collection.mutable

object AttrIdCompose { // do not use for original facts
  def hexSz = 4
  def idSz = 4 * hexSz
  def apply(a: Long, b: Long): Long =
    LongFits(a,idSz, isUnsigned = true, idSz) |
      LongFits(b,idSz, isUnsigned = true, 0)
}

case class NoNameAttrInfo(attrId: Long) extends BaseNameAttrInfo {
  def nameOpt = None
}

case class NameAttrInfoImpl(attrId: Long, name: String) extends NameAttrInfo {
  lazy val nameOpt = Some(name)
}

case class SearchAttrInfoImpl(
  labelOpt: Option[NameAttrInfo], propOpt: Option[NameAttrInfo]
) extends SearchAttrInfo with IndexAttrInfo {
  def attrId = composedInfo.attrId
  def nameOpt = composedInfo.nameOpt
  private lazy val composedInfo: BaseNameAttrInfo = (labelOpt,propOpt) match {
    case (None,Some(info)) ⇒ info
    case (Some(info),None) ⇒ info
    case (Some(NameAttrInfoImpl(labelAttrId,_)),Some(NameAttrInfoImpl(propAttrId,_))) ⇒
      NoNameAttrInfo(AttrIdCompose(labelAttrId,propAttrId))
    case (None,None) ⇒ Never()
  }
  lazy val keyForValue: String = propOpt.orElse(labelOpt).flatMap(_.nameOpt).get
  def labelAttrId = labelOpt.get.attrId
}

class PreCommitCalcCollectorImpl extends PreCommitCalcCollector {
  private var calcList: List[()=>Unit] = Nil
  def recalculateAll(): Unit = calcList.reverseIterator.foreach(_.apply())
  def apply(thenDo: Seq[Long]=>Unit): Long=>Unit = {
    val objIds = mutable.SortedSet[Long]()
    calcList = (()=>thenDo(objIds.toSeq)) :: calcList
    objId => objIds += objId
  }
}