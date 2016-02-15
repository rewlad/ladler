package ee.cone.base.db

import ee.cone.base.util.{Bytes, MD5, LongFits, Never}

import scala.collection.mutable

//lazy val keyForValue: String = propOpt.orElse(labelOpt).flatMap(_.nameOpt).get

// fails = attrCalcList.collect{ case i: PreCommitCheckAttrCalc => i.checkAll() }.flatten
case class PreCommitCheckAttrCalcImpl(check: PreCommitCheck) extends PreCommitCheckAttrCalc {
  private lazy val nodes = mutable.Set[DBNode]()
  def version = check.version
  def affectedBy = check.affectedBy
  def beforeUpdate(node: DBNode) = ()
  def afterUpdate(node: DBNode) = nodes += node
  def checkAll() = check.check(nodes.toSeq.sortBy(_.objId))
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
    //case i: RuledIndex if i.indexed ⇒
      //println(s"ai: ${i.attrId.toString}")
    //  i.attrId.toString
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