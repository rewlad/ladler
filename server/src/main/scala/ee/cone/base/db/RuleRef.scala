package ee.cone.base.db

import ee.cone.base.connection_api.{Obj, Attr, CoHandler}

class RefIntegrityImpl(
    preCommitCheck: PreCommitCheckAllOfConnection,
    searchIndex: SearchIndex,
    findNodes: FindNodes,
    mandatory: Mandatory
) extends RefIntegrity {
  def apply(existsA: Attr[Boolean], toAttr: Attr[Obj], existsB: Attr[Boolean]) = {
    // if A exists and there's link from A to B then B should exist
    def checkPairs(nodesA: Seq[Obj]): Seq[ValidationFailure] =
      nodesA.flatMap{ nodeA =>
        val nodeB = nodeA(toAttr)
        if(!nodeA(existsA) || !nodeB.nonEmpty || nodeB(existsB)) None
        else Some(ValidationFailure("refs",nodeA)) //, s"attr $toAttrId should refer to valid object, but $v found")
      }
    mandatory(toAttr,existsA, mutual=false) :::
      searchIndex.handlers(existsA,toAttr) :::
      CoHandler(AfterUpdate(existsB))(
        preCommitCheck.create{ nodesB =>
          checkPairs(nodesB.flatMap(nodeB => findNodes.where(nodeB.tx,existsA,toAttr,nodeB,Nil)))
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
