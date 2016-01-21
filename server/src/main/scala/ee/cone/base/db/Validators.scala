
package ee.cone.base.db

import java.util.UUID

/*NoGEN*/ trait ValidateFailReaction {
  def apply(objId: Long, comment: String): Unit
}

case class IgnoreValidateFailReaction() extends  ValidateFailReaction {
  def apply(objId: Long, comment: String): Unit = ()
}

case class ThrowValidateFailReaction() extends  ValidateFailReaction {
  def apply(objId: Long, comment: String): Unit = throw new Exception(s"ObjId: $objId: $comment")
}

//uniqueAttrId must be indexed
case class UniqueAttrCalc(uniqueAttrId: Long, fail: ValidateFailReaction) extends DBAttrCalc with IndexAttrInfo{
  def makeACopy = copy()
  def version = UUID.fromString("2a734606-11c4-4a7e-a5de-5486c6b788d2")
  def affectedByAttrIds = uniqueAttrId :: Nil
  def apply(objId: Long) = {
    val uniqueValue = db(objId, uniqueAttrId)
    if(uniqueValue != LMRemoved) {
      val objIds = IndexSearch()(uniqueAttrId, uniqueValue)
      if (objIds.length != 1)
        fail(objId, "uniqueAttr value must be unique!")
    }
  }
  def attrId: Long = uniqueAttrId
}

