package ee.cone.base.db

import ee.cone.base.db.Types.ObjId

case class CalcFactIndexImpl(attrId: AttrId)(db: FactIndex, calcList: CalcFactIndex=>List[AttrCalc]) extends CalcFactIndex {
  def affects = this :: Nil
  def get(objId: ObjId): DBValue = db.get(objId, attrId)
  def set(objId: ObjId, value: DBValue): Unit =
    db.updating(objId, attrId, value).foreach{ update =>
      for(calc <- calcList(this)) calc.beforeUpdate(objId)
      update()
      for(calc <- calcList(this)) calc.afterUpdate(objId)
    }
}

class CalcFactIndexByObjIdImpl(db: FactIndex, index: AttrId=>CalcFactIndex) extends CalcFactIndexByObjId {
  def execute(objId: ObjId, feed: Feed[CalcFactIndex]): Unit =
    db.execute(objId, new Feed[AttrId] {
      def apply(attrId: AttrId) = feed(index(attrId))
    })
}
