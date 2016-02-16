package ee.cone.base.db

import ee.cone.base.db.Types._

case class CalcSearchIndexImpl(direct: CalcFactIndex)(db: SearchIndex) extends CalcSearchIndex with AttrCalc {
  def execute(value: DBValue, feed: Feed[ObjId]) =
    db.execute(direct.attrId, value, feed)
  def execute(value: DBValue, objId: ObjId, feed: Feed[ObjId]) =
    db.execute(direct.attrId, value, objId, feed)
  def beforeUpdate(objId: ObjId) =
    db.set(direct.attrId, direct.get(objId), objId, on=false)
  def afterUpdate(objId: ObjId) =
    db.set(direct.attrId, direct.get(objId), objId, on=true)
  def affectedBy = direct :: Nil
}
