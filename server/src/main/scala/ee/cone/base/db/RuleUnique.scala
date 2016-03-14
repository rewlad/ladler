
package ee.cone.base.db

import ee.cone.base.connection_api.{Obj, Attr, CoHandler}

class UniqueImpl(
  preCommitCheck: PreCommitCheckAllOfConnection,
  searchIndex: SearchIndex,
  findNodes: FindNodes
) extends Unique {
  def apply[Value](label: Attr[_], uniqueAttr: Attr[Value]) = {
    def checkNode(node: Obj): List[ValidationFailure] =
      if(!node(label.defined) || !node(uniqueAttr.defined)) Nil
      else findNodes.where(node.tx, label.defined, uniqueAttr, node(uniqueAttr),Nil) match {
        case _ :: Nil => Nil
        case ns => ns.map(ValidationFailure("unique",_))
      }

    searchIndex.handlers(label,uniqueAttr) :::
      (label :: uniqueAttr :: Nil).map{ a =>
        CoHandler(AfterUpdate(a.defined))(
          preCommitCheck.create{ nodes => nodes.flatMap(checkNode) }
        )
      }
  }
}
