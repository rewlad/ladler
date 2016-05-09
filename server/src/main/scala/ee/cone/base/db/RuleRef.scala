package ee.cone.base.db

import ee.cone.base.connection_api.{Obj, Attr, CoHandler}

class RefIntegrityImpl(
    attrFactory: AttrFactory,
    nodeFactory: NodeFactory,
    preCommitCheck: PreCommitCheckAllOfConnection,
    searchIndex: SearchIndex,
    findNodes: FindNodes,
    uniqueNodes: UniqueNodes,
    mandatory: Mandatory
) extends RefIntegrity {
  def apply(existsA: Attr[Boolean], toAttr: Attr[Obj], existsB: Attr[Boolean]) = {
    // if A exists and there's link from A to B then B should exist
    def checkPairs(nodesA: Seq[Obj]): Seq[ValidationFailure] =
      nodesA.flatMap{ nodeA =>
        val nodeB = if(nodeA(existsA)) nodeA(toAttr) else uniqueNodes.noNode
        if(!nodeB.nonEmpty || nodeB(existsB)) None
        else Some(ValidationFailure("refs",nodeA)) //, s"attr $toAttrId should refer to valid object, but $v found")
      }
    mandatory(toAttr,existsA, mutual=false) :::
      searchIndex.handlers(existsA,toAttr) :::
      CoHandler(AfterUpdate(existsB))(
        preCommitCheck.create{ nodesB =>
          checkPairs(nodesB.flatMap(nodeB => findNodes.where(nodeB(nodeFactory.boundToTx),existsA,toAttr,nodeB,Nil)))
        }
      ) ::
      CoHandler(AfterUpdate(existsA))(
        preCommitCheck.create(checkPairs)
      ) ::
      CoHandler(AfterUpdate(attrFactory.defined(toAttr)))(
        preCommitCheck.create(checkPairs)
      ) :: Nil
  }
}
