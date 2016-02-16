package ee.cone.base.db

import ee.cone.base.db.Types._
import ee.cone.base.util.{Bytes, MD5, LongFits, Never}

import scala.collection.mutable

//lazy val keyForValue: String = propOpt.orElse(labelOpt).flatMap(_.nameOpt).get

// fails = attrCalcList.collect{ case i: PreCommitCheckAttrCalc => i.checkAll() }.flatten
case class PreCommitCheckAttrCalcImpl(check: PreCommitCheck) extends PreCommitCheckAttrCalc {
  private lazy val objIds = mutable.SortedSet[ObjId]()
  def affectedBy = check.affectedBy
  def beforeUpdate(objId: ObjId) = ()
  def afterUpdate(objId: ObjId) = objIds += objId
  def checkAll() = check.check(objIds.toSeq)
}

////

class AttrInfoRegistry(attrInfoList: List[AttrInfo]) {
  lazy val calcLists: Map[CalcFactIndex, List[AttrCalc]] =
    attrInfoList.collect { case attrCalc: AttrCalc ⇒
      attrCalc.affectedBy.flatMap(calcFactIndex => calcFactIndex.affects.map(calcFactIndex => (calcFactIndex, attrCalc)))
    }.flatten.groupBy(_._1).mapValues(_.map(_._2))
  def checkAll(): Seq[ValidationFailure] =
    attrInfoList.collect{ case i: PreCommitCheckAttrCalc => i.checkAll() }.flatten
}



/*
  lazy val version = MD5(Bytes(attrInfoList.collect {
    //case i: RuledIndex if i.indexed ⇒
      //println(s"ai: ${i.attrId.toString}")
    //  i.attrId.toString
    case i: AttrCalc ⇒
      //println(s"acc:${i.version}:$i")
      i.toString
  }.sorted.mkString(",")))*/


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