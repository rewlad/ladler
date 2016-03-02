
package ee.cone.base.db

import ee.cone.base.connection_api.BaseCoHandler


class UniqueAttrCalcList(
  preCommitCheck: PreCommitCheck=>BaseCoHandler,
  searchIndex: SearchIndex,
  values: ListByValueStart
) {
  def apply[Value](uniqueAttr: Attr[Value]) =
    searchIndex.handlers(uniqueAttr) :::
    preCommitCheck(new UniqueAttrCalc(uniqueAttr, values)) :: Nil
}

//uniqueAttrId must be indexed
class UniqueAttrCalc[Value](
  uniqueAttr: Attr[Value], values: ListByValueStart
) extends PreCommitCheck {
  def affectedBy = uniqueAttr :: Nil
  def check(nodes: Seq[DBNode]) = nodes.flatMap{ node =>
    if(!node(uniqueAttr.nonEmpty)) Nil
    else values.of(uniqueAttr).list(node(uniqueAttr)) match {
      case _ :: Nil => Nil
      case ns => ns.map(ValidationFailure(this,_))
    }
  }
}
