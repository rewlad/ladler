
package ee.cone.base.db_impl

import ee.cone.base.connection_api.{Obj, Attr, CoHandler}
import ee.cone.base.db.{Unique, SearchIndex}

class UniqueImpl(
  attrFactory: AttrFactoryI,
  factIndex: FactIndexI,
  txSelector: TxSelector,
  preCommitCheck: PreCommitCheckAllOfConnection,
  searchIndex: SearchIndex,
  findNodes: FindNodesI
) extends Unique {
  def apply[Value](label: Attr[Obj], uniqueAttr: Attr[Value]) = {
    val attrIds = (label :: uniqueAttr :: Nil).map(attrFactory.attrId(_))
    val index = searchIndex.create(label,uniqueAttr)
    def checkNode(node: Obj): List[ValidationFailure] =
      if(attrIds.forall(attrId⇒node(factIndex.defined(attrId)))){
        findNodes
          .where(txSelector.txOf(node), index, node(uniqueAttr), Nil) match {
          case _ :: Nil => Nil
          case ns => ns.map(ValidationFailure("unique", _))
        }
      } else Nil

    searchIndex.handlers(index) :::
      attrIds.map{ a =>
        CoHandler(AfterUpdate(a))(
          preCommitCheck.create{ nodes => nodes.flatMap(checkNode) }
        )
      }
  }
}
