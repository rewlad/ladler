
package ee.cone.base.db

import ee.cone.base.connection_api.CoHandler

class UniqueImpl(
  preCommitCheck: PreCommitCheckAllOfConnection,
  searchIndex: SearchIndex,
  allNodes: DBNodes
) extends Unique {
  def apply[Value](label: Attr[_], uniqueAttr: Attr[Value]) = {
    def checkNode(node: DBNode): List[ValidationFailure] =
      if(!node(label.defined) || !node(uniqueAttr.defined)) Nil
      else allNodes.where(node.tx, label.defined, uniqueAttr, node(uniqueAttr)) match {
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
