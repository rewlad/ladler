
package ee.cone.base.db

import scala.collection.mutable

import ee.cone.base.util.{HexDebug, MD5}

import ee.cone.base.db.LMTypes._

trait IndexingTx {
  def apply(objId: Long, attrId: Long): LMValue
  def update(objId: Long, attrId: Long, value: LMValue): Unit
  def isOriginal = 1L
  def set(objId: Long, attrId: Long, value: LMValue, valueSrcId: ValueSrcId): Boolean
}

class ImplIndexingTx(tx: RawTx, attrCalcInfo: AttrCalcInfo) extends IndexingTx {
  private def index(objId: Long, attrId: Long, value: LMValue, on: Boolean): Unit =
    if(attrCalcInfo.indexed(attrId) && value != LMRemoved)
      tx.set(LMIndex.key(attrId, value, objId), LMIndex.value(on))

  def apply(objId: Long, attrId: Long): LMValue =
    LMFact.valueFromBytes(tx.get(LMFact.key(objId, attrId)), None)

  def update(objId: Long, attrId: Long, value: LMValue): Unit =
    set(objId, attrId, value, isCalculated)

  private def isCalculated = 2L
  def set(objId: Long, attrId: Long, value: LMValue, valueSrcId: ValueSrcId): Boolean = {
    val key = LMFact.key(objId, attrId)
    val matchOrDie: Option[ValueSrcId⇒Boolean] =
      Some(wasValueSrcId⇒if(valueSrcId == wasValueSrcId) true else
        throw new Exception(s"attempt to set attr ${HexDebug(attrId.toInt)} src from $wasValueSrcId to $valueSrcId value $value")
      )
    val wasValue = LMFact.valueFromBytes(tx.get(key), matchOrDie)
    if (value == wasValue) {
      return false
    }
    index(objId, attrId, wasValue, on=false)
    tx.set(key, LMFact.value(value, valueSrcId))
    index(objId, attrId, value, on=true)
    for(prototypes <- attrCalcInfo.protoByAttrId.get(attrId); prototype <- prototypes)
      prototype.recalculate(objId)
    true
  }
}

case class ExtractedFact(objId: Long, attrId: Long, value: LMValue)
class Replay(db: IndexingTx) {
  private lazy val changedOriginalSet = mutable.SortedSet[(Long,Long)]()
  def set(facts: List[ExtractedFact]): Unit =
    facts.foreach(fact⇒set(fact.objId, fact.attrId, fact.value))
  def set(objId: Long, attrId: Long, value: LMValue): Unit =
    if(db.set(objId, attrId, value, db.isOriginal))
      changedOriginalSet += ((objId,attrId))
  def changedOriginalFacts: List[ExtractedFact] = changedOriginalSet.map{
    case (objId, attrId) ⇒ ExtractedFact(objId, attrId, db(objId, attrId))
  }.toList
}

/*NoGEN*/ trait IndexAttrInfo { def attrId: Long }

class AttrCalcInfo(attrInfoList: List[AttrInfo]) {
  lazy val indexed: Set[Long] =
    attrInfoList.collect { case i: IndexAttrInfo ⇒ i.attrId }.toSet
  lazy val protoByAttrId: Map[Long, List[AttrCalc]] =
    attrInfoList.collect { case attrCalc: AttrCalc ⇒
      attrCalc.affectedByAttrIds.map(attrId => (attrId, attrCalc))
    }.flatten.groupBy(_._1).mapValues(_.map(_._2))
  lazy val version = MD5(attrInfoList.collect {
    case i: IndexAttrInfo ⇒
      //println(s"ai: ${i.attrId.toString}")
      i.attrId.toString
    case i: AttrCalc ⇒
      //println(s"acc:${i.version}:$i")
      s"${i.version}:$i"
  }.sorted.mkString(","))
}

/*NoGEN*/ trait DBHas {
  def db: IndexingTx
  def dbHas(objId: Long, attrId: Long) = db(objId, attrId) != LMRemoved
}

/******************************************************************************/

trait PreCommitCalcContext {
  def preCommitCalcCollector: PreCommitCalcCollector
}

abstract class PreCommitCalc(context: PreCommitCalcContext) extends AttrCalc {
  import context._
  lazy val objIds = mutable.SortedSet[Long]()
  def recalculate(objId: Long) = preCommitCalcCollector.same(this).objIds += objId
  def apply(): Unit
}

/*NoGEN*/ class PreCommitCalcCollector {
  private val calcQueue = mutable.Queue[PreCommitCalc]()
  private val calcMap = mutable.Map[PreCommitCalc,PreCommitCalc]()
  def same(calc: PreCommitCalc): PreCommitCalc = calcMap.getOrElseUpdate(calc, {
    calcQueue.enqueue(calc)
    calc
  })
  def applyAll(): Unit = while (calcQueue.nonEmpty) calcQueue.dequeue().apply()/*
  abstract class PreCommitCalc extends AttrCalc {
    lazy val objIds = mutable.SortedSet[Long]()
    def apply(objId: Long) = same(this).objIds += objId
    def apply(): Unit
  }*/
}

/*class PreCommitCalcCollector {
  private val calcQueue = mutable.Queue[()=>Unit]()
  def applyAll(): Unit = while (calcQueue.nonEmpty) calcQueue.dequeue().apply()
  def aggregate(onPreCommit: Seq[Long]=>Unit): Long=>Unit = {
    val objIds = mutable.SortedSet[Long]()
    calcQueue.enqueue(()=>onPreCommit(objIds.toSeq))
    objId => objIds += objId
  }
  abstract class PreCommitCalc extends AttrCalc {
    def apply(objId: Long) = ???
  }
}*/