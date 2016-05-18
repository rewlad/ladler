
package ee.cone.base.db

import ee.cone.base.connection_api.{Obj, Attr, CoHandler}

class UniqueImpl(
  attrFactory: AttrFactory,
  txSelector: TxSelector,
  preCommitCheck: PreCommitCheckAllOfConnection,
  searchIndex: SearchIndex,
  findNodes: FindNodes
) extends Unique {
  def apply[Value](label: Attr[_], uniqueAttr: Attr[Value]) = {
    val attrIds = (label :: uniqueAttr :: Nil).map(attrFactory.attrId(_))
    def checkNode(node: Obj): List[ValidationFailure] =
      if(attrIds.forall(attrId⇒node(attrFactory.defined(attrId)))){
        findNodes
          .where(txSelector.txOf(node), label, uniqueAttr, node(uniqueAttr), Nil) match {
          case _ :: Nil => Nil
          case ns => ns.map(ValidationFailure("unique", _))
        }
      } else Nil

    searchIndex.handlers(label,uniqueAttr) :::
      attrIds.map{ a =>
        CoHandler(AfterUpdate(a))(
          preCommitCheck.create{ nodes => nodes.flatMap(checkNode) }
        )
      }
  }
}
