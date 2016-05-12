
package ee.cone.base.db

import ee.cone.base.connection_api.{Obj, Attr, CoHandler}

class UniqueImpl(
  attrFactory: AttrFactory,
  nodeAttributes: NodeAttrs,
  preCommitCheck: PreCommitCheckAllOfConnection,
  searchIndex: SearchIndex,
  findNodes: FindNodes
) extends Unique {
  def apply[Value](label: Attr[_], uniqueAttr: Attr[Value]) = {
    val definedAttrs = (label :: uniqueAttr :: Nil).map(attrFactory.defined)
    def checkNode(node: Obj): List[ValidationFailure] =
      if(definedAttrs.forall(node(_))){
        findNodes
          .where(node(nodeAttributes.dbNode).tx, label, uniqueAttr, node(uniqueAttr), Nil) match {
          case _ :: Nil => Nil
          case ns => ns.map(ValidationFailure("unique", _))
        }
      } else Nil

    searchIndex.handlers(label,uniqueAttr) :::
      definedAttrs.map{ a =>
        CoHandler(AfterUpdate(a))(
          preCommitCheck.create{ nodes => nodes.flatMap(checkNode) }
        )
      }
  }
}
