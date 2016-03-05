package ee.cone.base.db

import ee.cone.base.connection_api.{EventKey, CoHandlerLists}
import ee.cone.base.util.Single

class ListByDBNodeImpl(inner: FactIndex, attrFactory: AttrFactory, definedValueConverter: RawValueConverter[Boolean]) extends ListByDBNode {
  def list(node: DBNode) = {
    val feed = new ListFeedImpl[Attr[_]](Long.MaxValue,attrFactory.apply(_,_,definedValueConverter))
    inner.execute(node, feed)
    feed.result
  }
}

class SysAttrs(
  attr: AttrFactory, nodeValueConverter: RawValueConverter[DBNode]
)(
  val seq: Attr[DBNode] = attr(0, 0x0001, nodeValueConverter)
)

class DBNodesImpl(
  handlerLists: CoHandlerLists,
  nodeFactory: NodeFactory, at: SysAttrs
) extends DBNodes {
  def where[Value](tx: BoundToTx, label: Attr[Boolean], prop: Attr[Value], value: Value) =
    where(tx, label, prop, value, None, Long.MaxValue)
  def where[Value](tx: BoundToTx, label: Attr[Boolean], prop: Attr[Value], value: Value, from: Option[Long], limit: Long) = {
    val searchKey = SearchByLabelProp[Value](label.defined, prop.defined)
    val handler = Single(handlerLists.list(searchKey))
    val feed = new ListFeedImpl[DBNode](limit,(objId,_)=>nodeFactory.toNode(tx,objId))
    val request = new SearchRequest[Value](tx, value, None, feed)
    handler(request)
    feed.result.reverse
  }
  def create(tx: BoundToTx, label: Attr[DBNode]): DBNode = {
    val seqNode = nodeFactory.seqNode(tx)
    val lastNode = seqNode(at.seq)
    val nextObjId = if(lastNode.nonEmpty) lastNode.objId + 1L else 1L
    val res = nodeFactory.toNode(tx,nextObjId)
    seqNode(at.seq) = res
    res(label) = res
    res
  }
}

class ListFeedImpl[To](var limit: Long, converter: (Long,Long)=>To) extends Feed {
  var result: List[To] = Nil
  def apply(valueA: Long, valueB: Long) = {
    result = converter(valueA,valueB) :: result
    limit -= 1L
    limit > 0L
  }
}






//lazy val keyForValue: String = propOpt.orElse(labelOpt).flatMap(_.nameOpt).get

/*
  lazy val version = MD5(Bytes(attrInfoList.collect {
    //case i: RuledIndex if i.indexed ⇒
      //println(s"ai: ${i.attrId.toString}")
    //  i.attrId.toString
    case i: AttrCalc ⇒
      //println(s"acc:${i.version}:$i")
      i.toString
  }.sorted.mkString(",")))*/


/*
case class ExtractedFact(objId: Long, attrId: Long, value: DBValue)
class Replay(db: IndexingTx) {
  private lazy val changedOriginalSet = mutable.SortedSet[(Long,Long)]()
  def set(facts: List[ExtractedFact]): Unit =
    facts.foreach(fact⇒set(fact.objId, fact.attrId, fact.value))
  def set(objId: Long, attrId: Long, value: DBValue): Unit =
    if(db.set(objId, attrId, value, db.isOriginal))
      changedOriginalSet += ((objId,attrId))
  def changedOriginalFacts: List[ExtractedFact] = changedOriginalSet.map{
    case (objId, attrId) ⇒ ExtractedFact(objId, attrId, db(objId, attrId))
  }.toList
}
*/