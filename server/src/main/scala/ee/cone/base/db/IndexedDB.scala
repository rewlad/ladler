
package ee.cone.base.db

import scala.collection.mutable

import ee.cone.base.util.{Bytes, HexDebug, MD5}

import ee.cone.base.db.Types._

class IndexingTxImpl(
  rawFactConverter: RawFactConverter,
  rawIndexConverter: RawIndexConverter,
  tx: RawTx,
  attrCalcInfo: AttrCalcInfo
) extends IndexingTx {
  private def index(objId: Long, attrId: Long, value: DBValue, on: Boolean): Unit =
    if(attrCalcInfo.indexed(attrId) && value != DBRemoved)
      tx.set(rawIndexConverter.key(attrId, value, objId), rawIndexConverter.value(on))

  def apply(objId: Long, attrId: Long): DBValue =
    rawFactConverter.valueFromBytes(tx.get(rawFactConverter.key(objId, attrId)), None)

  def update(objId: Long, attrId: Long, value: DBValue): Unit =
    set(objId, attrId, value, isCalculated)

  private def isCalculated = 2L
  def set(objId: Long, attrId: Long, value: DBValue, valueSrcId: ValueSrcId): Boolean = {
    val key = rawFactConverter.key(objId, attrId)
    val matchOrDie: Option[ValueSrcId⇒Boolean] =
      Some(wasValueSrcId⇒if(valueSrcId == wasValueSrcId) true else
        throw new Exception(s"attempt to set attr ${HexDebug(attrId.toInt)} src from $wasValueSrcId to $valueSrcId value $value")
      )
    val wasValue = rawFactConverter.valueFromBytes(tx.get(key), matchOrDie)
    if (value == wasValue) {
      return false
    }
    index(objId, attrId, wasValue, on=false)
    tx.set(key, rawFactConverter.value(value, valueSrcId))
    index(objId, attrId, value, on=true)
    for(calcList <- attrCalcInfo.calcListByAttrId.get(attrId); calc <- calcList)
      calc.recalculate(objId)
    true
  }
}

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

class AttrCalcInfo(attrInfoList: List[AttrInfo]) {
  lazy val indexed: Set[Long] =
    attrInfoList.collect { case i: IndexAttrInfo ⇒ i.attrId }.toSet
  lazy val calcListByAttrId: Map[Long, List[AttrCalc]] =
    attrInfoList.collect { case attrCalc: AttrCalc ⇒
      attrCalc.affectedByAttrIds.map(attrId => (attrId, attrCalc))
    }.flatten.groupBy(_._1).mapValues(_.map(_._2))
  lazy val version = MD5(Bytes(attrInfoList.collect {
    case i: IndexAttrInfo ⇒
      //println(s"ai: ${i.attrId.toString}")
      i.attrId.toString
    case i: AttrCalc ⇒
      //println(s"acc:${i.version}:$i")
      s"${i.version}:$i"
  }.sorted.mkString(",")))
}
