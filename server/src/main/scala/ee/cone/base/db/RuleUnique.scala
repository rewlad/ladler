
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
      CoHandler(AfterUpdate(label.defined) :: AfterUpdate(uniqueAttr.defined) :: Nil)(
        preCommitCheck.create{ nodes => nodes.flatMap(checkNode) }
      ) :: Nil
  }
}
