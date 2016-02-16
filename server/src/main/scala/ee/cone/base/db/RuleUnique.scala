
package ee.cone.base.db

class UniqueAttrCalcList(preCommitCheck: PreCommitCheck=>AttrCalc) {
  def apply[Value](uniqueAttr: Prop[Value], listUniqueAttr: ListByValue[Value]) =
    preCommitCheck(UniqueAttrCalc(uniqueAttr, listUniqueAttr)) :: Nil
}

//uniqueAttrId must be indexed
case class UniqueAttrCalc[Value](uniqueAttr: Prop[Value], listUniqueAttr: ListByValue[Value]) extends PreCommitCheck {
  def affectedBy = uniqueAttr :: Nil
  def check(nodes: Seq[DBNode]) = nodes.flatMap{ node =>
    if(!node(uniqueAttr.nonEmpty)) Nil
    else listUniqueAttr.list(node(uniqueAttr)) match {
      case _ :: Nil => Nil
      case ns => ns.map(ValidationFailure(this,_))
    }
  }
}
