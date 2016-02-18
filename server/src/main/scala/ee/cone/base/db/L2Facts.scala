
package ee.cone.base.db

import ee.cone.base.connection_api.{ConnectionComponent, Registration}
import ee.cone.base.db.Types._
import ee.cone.base.util.Never

class SrcObjIdRegistration(index: FactIndexImpl, objId: ObjId) extends Registration {
  def open() = index.srcObjId = objId
  def close() = index.srcObjId = 0L
}

class FactRawIndexRegistration(index: FactIndexImpl, tx: RawIndex) extends Registration {
  def open() = index.txOpt = Option(tx)
  def close() = index.txOpt = None
}

class FactIndexImpl(
  rewritable: Boolean,
  rawFactConverter: RawFactConverter,
  rawVisitor: RawVisitor[AttrId],
  calcLists: AttrCalcLists
) extends FactIndex {
  var txOpt: Option[RawIndex] = None
  def tx = txOpt.get
  var srcObjId = 0L
  def get(objId: ObjId, attrId: AttrId) =
    rawFactConverter.valueFromBytes(tx.get(rawFactConverter.key(objId, attrId)))
  def set(objId: ObjId, attrId: AttrId, value: DBValue): Unit = {
    val wasValue = get(objId, attrId)
    if (value == wasValue) { return }
    if (!rewritable && wasValue != DBRemoved) Never()
    val key = rawFactConverter.key(objId, attrId)
    val rawValue = rawFactConverter.value(value, srcObjId)
    val calcList = calcLists.value(attrId)
    if(calcList.isEmpty) throw new Exception(s"$attrId is lost")
    for(calc <- calcList) calc.beforeUpdate(objId)
    tx.set(key, rawValue)
    for(calc <- calcList) calc.afterUpdate(objId)
  }
  def execute(objId: ObjId, feed: Feed[AttrId]): Unit = {
    val key = rawFactConverter.keyWithoutAttrId(objId)
    tx.seek(key)
    rawVisitor.execute(tx, key, feed)
  }
}

class AttrCalcLists(components: =>List[ConnectionComponent]) {
  lazy val value: Map[AttrId, List[AttrCalc]] =
    components.collect { case attrCalc: AttrCalc ⇒
      attrCalc.affectedBy.map(attrId => (attrId, attrCalc))
    }.flatten.groupBy(_._1).mapValues(_.map(_._2))
}