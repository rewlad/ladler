package ee.cone.base.db

import ee.cone.base.util.{Bytes, MD5, LongFits, Never}

import scala.collection.mutable

class IndexComposerImpl(
  ruledIndexById: AttrId=>RuledIndex //create indexed rewritable
) extends IndexComposer {
  def apply(labelAttr: RuledIndex, propAttr: RuledIndex) = {
    val AttrId(labelAttrId,0) = labelAttr.attrId
    val AttrId(0,propAttrId) = propAttr.attrId
    ruledIndexById(new AttrId(labelAttrId,propAttrId))
  }
}

//lazy val keyForValue: String = propOpt.orElse(labelOpt).flatMap(_.nameOpt).get

// fails = attrCalcList.collect{ case i: PreCommitCheckAttrCalc => i.checkAll() }.flatten
case class PreCommitCheckAttrCalcImpl(check: PreCommitCheck) extends PreCommitCheckAttrCalc {
  private lazy val objIds = mutable.SortedSet[ObjId]()
  def version = check.version
  def affectedBy = check.affectedBy
  def recalculate(objId: ObjId) = objIds += objId
  def checkAll() = check.check(objIds.toSeq)
}

////

class AttrInfoRegistry(attrInfoList: List[AttrInfo]) {
  private lazy val calcLists: Map[Affecting, List[AttrCalc]] =
    attrInfoList.collect { case attrCalc: AttrCalc ⇒
      attrCalc.affectedBy.map(attrId => (attrId, attrCalc))
    }.flatten.groupBy(_._1).mapValues(_.map(_._2))
  /*def updates(attrId: AttrId) =
    AttrUpdate(attrId, indexed(attrId), rewritable = ???, calcListByAttrId.getOrElse(attrId,Nil))*/
  lazy val version = MD5(Bytes(attrInfoList.collect {
    case i: RuledIndex if i.indexed ⇒
      //println(s"ai: ${i.attrId.toString}")
      i.attrId.toString
    case i: AttrCalc ⇒
      //println(s"acc:${i.version}:$i")
      i.toString
  }.sorted.mkString(",")))
}






/*
case class ExtractedFact(objId: Long, attrId: Long, value: DBValue)
class Replay(db: IndexingTx) {
  private lazy val changedOriginalSet = mutable.SortedSet[(Long,Long)]()
  def set(facts: List[ExtractedFact]): Unit =
    facts.foreach(fact⇒set(fact.objId, fact.attrId, fact.value))
  def set(objId: Long, attrId: Long, value: DBValue): Unit =
    if(db.set(objId, attrId, value, db.isOriginal))
      changedOriginalSet += ((objId,attrId))
  def changedOriginalFacts: List[ExtractedFact] = changedOriginalSet.map{
    case (objId, attrId) ⇒ ExtractedFact(objId, attrId, db(objId, attrId))
  }.toList
}
*/