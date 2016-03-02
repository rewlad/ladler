
package ee.cone.base.db

import ee.cone.base.connection_api.BaseCoHandler


class UniqueAttrCalcList(preCommitCheck: PreCommitCheck=>BaseCoHandler) {
  def apply[Value](uniqueAttr: Attr[Value], listUniqueAttr: ListByValue[Value]) =
    preCommitCheck(UniqueAttrCalc(uniqueAttr, listUniqueAttr)) :: Nil
}

//uniqueAttrId must be indexed
case class UniqueAttrCalc[Value](uniqueAttr: Attr[Value], listUniqueAttr: ListByValue[Value]) extends PreCommitCheck {
  def affectedBy = uniqueAttr :: Nil
  def check(nodes: Seq[DBNode]) = nodes.flatMap{ node =>
    if(!node(uniqueAttr.nonEmpty)) Nil
    else listUniqueAttr.list(node(uniqueAttr)) match {
      case _ :: Nil => Nil
      case ns => ns.map(ValidationFailure(this,_))
    }
  }
}
