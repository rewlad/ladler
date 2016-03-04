package ee.cone.base.db

import ee.cone.base.connection_api.CoHandler

class RefIntegrityImpl(
    preCommitCheck: PreCommitCheckAllOfConnection,
    searchIndex: SearchIndex,
    allNodes: DBNodes,
    mandatory: Mandatory
) extends RefIntegrity {
  def apply(existsA: Attr[Boolean], toAttr: Attr[DBNode], existsB: Attr[Boolean]) = {
    // if A exists and there's link from A to B then B should exist
    def checkPairs(nodesA: Seq[DBNode]): Seq[ValidationFailure] =
      nodesA.flatMap{ nodeA =>
        val nodeB = nodeA(toAttr)
        if(!nodeA(existsA) || !nodeB.nonEmpty || nodeB(existsB)) None
        else Some(ValidationFailure("refs",nodeA)) //, s"attr $toAttrId should refer to valid object, but $v found")
      }
    mandatory(toAttr,existsA, mutual=false) :::
      searchIndex.handlers(existsA,toAttr) :::
      CoHandler(AfterUpdate(existsB))(
        preCommitCheck.create{ nodesB =>
          checkPairs(nodesB.flatMap(nodeB => allNodes.where(nodeB.tx,existsA,toAttr,nodeB)))
        }
      ) ::
      CoHandler(AfterUpdate(existsA))(
        preCommitCheck.create(checkPairs)
      ) ::
      CoHandler(AfterUpdate(toAttr.defined))(
        preCommitCheck.create(checkPairs)
      ) :: Nil
  }
}
