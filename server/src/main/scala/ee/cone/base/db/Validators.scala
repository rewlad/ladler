
package ee.cone.base.db

import java.util.UUID

object IgnoreValidateFailReaction extends ValidateFailReaction {
  def apply(objId: Long, comment: String): Unit = ()
}

object ThrowValidateFailReaction extends ValidateFailReaction {
  def apply(objId: Long, comment: String): Unit = throw new Exception(s"ObjId: $objId: $comment")
}

class UniqueAttrCalcFactory(sysAttrCalcFactory: SysAttrCalcFactory){
  def apply(uniqueAttrId: Long) = UniqueAttrCalc(uniqueAttrId)(sysAttrCalcFactory)
}

//uniqueAttrId must be indexed
case class UniqueAttrCalc(uniqueAttrId: Long)
  (context: SysAttrCalcFactory)
  extends AttrCalc with IndexAttrInfo
{
  import context._
  def version = UUID.fromString("2a734606-11c4-4a7e-a5de-5486c6b788d2")
  def affectedByAttrIds = uniqueAttrId :: Nil
  def recalculate(objId: Long) = {
    val uniqueValue = db(objId, uniqueAttrId)
    if(uniqueValue != LMRemoved) {
      val objIds = indexSearch(uniqueAttrId, uniqueValue)
      if (objIds.length != 1)
        fail(objId, "uniqueAttr value must be unique!")
    }
  }
  def attrId: Long = uniqueAttrId
}

