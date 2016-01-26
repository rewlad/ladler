package ee.cone.base.db

object IgnoreValidateFailReaction extends ValidateFailReaction {
  def apply(objId: Long, comment: String): Unit = ()
}

object ThrowValidateFailReaction extends ValidateFailReaction {
  def apply(objId: Long, comment: String): Unit = throw new Exception(s"ObjId: $objId: $comment")
}
