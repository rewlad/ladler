
package ee.cone.base.db

import ee.cone.base.connection_api.{CoHandler, BaseCoHandler}


class UniqueImpl[DBEnvKey](
  preCommitCheck: PreCommitCheckAllOfConnection,
  searchIndex: SearchIndex,
  allNodes: DBNodes[DBEnvKey]
) {
  def apply[Value](uniqueAttr: Attr[Value]) =
    searchIndex.handlers(uniqueAttr) :::
    CoHandler(AfterUpdate(uniqueAttr.defined) :: Nil)(preCommitCheck.create{ nodes =>
      nodes.flatMap{ node =>
        if(!node(uniqueAttr.defined)) Nil
        else allNodes.where(uniqueAttr,node(uniqueAttr)) match {
          case _ :: Nil => Nil
          case ns => ns.map(ValidationFailure("unique",_))
        }
      }
    }) :: Nil
}
