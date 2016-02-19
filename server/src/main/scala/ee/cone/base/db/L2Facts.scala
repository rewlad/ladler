
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
  rawVisitor: RawVisitor,
  calcLists: AttrCalcLists
) extends FactIndex {
  var txOpt: Option[RawIndex] = None
  def tx = txOpt.get
  var srcObjId = 0L
  def get[Value](objId: ObjId, attrId: Attr[Value]) =
    rawFactConverter.valueFromBytes(attrId, tx.get(rawFactConverter.key(objId, attrId)))
  def set[Value](objId: ObjId, attrId: Attr[Value], value: Value): Unit = {
    val wasValue = get(objId, attrId)
    if (attrId.converter.same(wasValue,value)) { return }
    if (!rewritable && attrId.converter.nonEmpty(wasValue)) Never()

    val key = rawFactConverter.key(objId, attrId)
    val rawValue = rawFactConverter.value(attrId, value, srcObjId)
    val calcList = calcLists.value(attrId.nonEmpty)
    if(calcList.isEmpty) throw new Exception(s"$attrId is lost")
    for(calc <- calcList) calc.beforeUpdate(objId)
    tx.set(key, rawValue)
    for(calc <- calcList) calc.afterUpdate(objId)
  }
  def execute(objId: ObjId, feed: Feed): Unit = {
    val key = rawFactConverter.keyWithoutAttrId(objId)
    tx.seek(key)
    rawVisitor.execute(tx, key, feed)
  }
}

class AttrCalcLists(components: =>List[ConnectionComponent]) {
  lazy val value: Map[Attr[Boolean], List[AttrCalc]] =
    components.collect { case attrCalc: AttrCalc ⇒
      attrCalc.affectedBy.map(attrId => (attrId.nonEmpty, attrCalc))
    }.flatten.groupBy(_._1).mapValues(_.map(_._2))
}