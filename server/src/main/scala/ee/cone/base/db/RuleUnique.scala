
package ee.cone.base.db

import java.util.UUID

class UniqueAttrCalcList(context: SysAttrCalcContext) {
  def apply(uniqueAttrId: AttrId) = UniqueAttrCalc(uniqueAttrId)(context) :: Nil
}

//uniqueAttrId must be indexed
case class UniqueAttrCalc(uniqueAttrId: AttrId)
  (context: SysAttrCalcContext)
  extends AttrCalc with IndexAttrInfo
{
  import context._
  def version = UUID.fromString("2a734606-11c4-4a7e-a5de-5486c6b788d2")
  def affectedByAttrIds = uniqueAttrId :: Nil
  def recalculate(objId: ObjId) = {
    val uniqueValue = db(objId, uniqueAttrId)
    if(uniqueValue != DBRemoved) {
      val objIds = listObjIdsByValue(uniqueAttrId, uniqueValue)
      if (objIds.length != 1)
        fail(objId, s"$uniqueAttrId value must be unique!")
    }
  }
  def attrId: AttrId = uniqueAttrId
}
