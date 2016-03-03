package ee.cone.base.db



import ee.cone.base.connection_api.{CoHandler, CoHandlerProvider, BaseCoHandler}


// fail'll probably do nothing in case of outdated rel type

case class RelTypeInfo (
  label: Attr[Boolean], start: RelSideTypeInfo, end: RelSideTypeInfo,
  handlers: List[BaseCoHandler]
) extends CoHandlerProvider

case class RelSideTypeInfo(
  side: RelSideInfo,
  handlers: List[BaseCoHandler]
) extends CoHandlerProvider

class RelTypeInfoFactory(
  relStartSide: RelSideInfo,
  relEndSide: RelSideInfo,
  searchIndex: SearchIndex
){
  private def createForSide(label: Attr[Boolean], side: RelSideInfo) =
    RelSideTypeInfo(side, searchIndex.handlers(label, side.attrId))

  def apply(label: Attr[Boolean]) = {
    val start = createForSide(label, relStartSide)
    val end = createForSide(label, relEndSide)
    RelTypeInfo(label, start, end, start.handlers ::: end.handlers)
  }
}

////

case class RelSideInfo(
  attrId: Attr[DBNode],
  handlers: List[BaseCoHandler]
) extends CoHandlerProvider

class RelSideInfoFactory(
  preCommitCheck: PreCommitCheckAllOfConnection,
  existsA: Attr[Boolean],
  existsB: Attr[Boolean],
  searchIndex: SearchIndex,
  allNodes: DBNodes[MainEnvKey]
){
  def apply[Value](attr: Attr[DBNode]) = {
    val handlers: List[BaseCoHandler] =
      searchIndex.handlers(attr) :::
        toHandler(new TypeRefIntegrityPreCommitCheck(hasTypeAttr, attr, allNodes)) ::
        toHandler(new SideRefIntegrityPreCommitCheck(hasTypeAttr, attr)) :: Nil
    RelSideInfo(attr, handlers)
  }
  def toHandler(check: Seq[DBNode]=>Seq[ValidationFailure]) =
    CoHandler(AfterUpdate(check.affectedBy)::Nil)(preCommitCheck.create(check))




  def apply[Value](toAttr: Attr[DBNode]) =
    searchIndex.handlers(existsA,toAttr) :::
      CoHandler(AfterUpdate(existsB)::Nil)(
        preCommitCheck.create{ nodesB =>
          val nodesA = nodesB.flatMap(nodeB => allNodes.where(toAttr,nodeB))
          checkPairs(nodesA, toAttr)
        }
      ) ::
      CoHandler(AfterUpdate(existsA)::AfterUpdate(toAttr.defined)::Nil)(
        preCommitCheck.create{ nodesA =>
          checkPairs(nodesA, toAttr)
        }
      ) :: Nil

  // if there's link from A to B then B should exist (have type)
  def checkPairs(nodesA: Seq[DBNode], toAttr: Attr[DBNode]): Seq[ValidationFailure] =
    nodesA.flatMap{ nodeA =>
      val nodeB = nodeA(toAttr)
      if(!nodeB.nonEmpty || nodeB(existsB)) None
      else Some(ValidationFailure("refs",nodeA)) //, s"attr $toAttrId should refer to valid object, but $v found")
    }
}
