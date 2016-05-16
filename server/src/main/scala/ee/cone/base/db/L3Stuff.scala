package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.util.{Never, Single}

/*
class ListByDBNodeImpl(
  inner: FactIndex, attrValueConverter: RawValueConverter[Attr[Boolean]]
) extends ListByDBNode {
  def get(node: Obj) = {
    val feed = new AttrListFeedImpl(attrValueConverter)
    inner.execute(node, feed)
    feed.result
  }
  def set(node: Obj, value: List[Attr[_]]) = if(value.nonEmpty) Never()
    else node(this).foreach(attr => node(attr) = false)
}
*/



class FindAttrsImpl(
  attr: AttrFactory,
  asString: AttrValueType[String]
)(
  val justIndexed: Attr[String] = attr("e4a1ccbc-f039-4af1-a505-c6bee1b755fd", asString)
) extends FindAttrs

class FindNodesImpl(
  at: FindAttrs,
  handlerLists: CoHandlerLists,
  nodeAttrs: NodeAttrs, nodeFactory: NodeFactory,
  attrFactory: AttrFactory, factIndex: FactIndex
) extends FindNodes  with CoHandlerProvider {
  def noNode = nodeFactory.noNode
  def zeroNode = nodeFactory.toNode(0L,0L)
  def nextNode(obj: Obj) = {
    val node = obj(nodeAttrs.objId)
    if(node.hiObjId!=0L || node.loObjId == Long.MaxValue) Never()
    nodeFactory.toNode(node.hiObjId, node.loObjId + 1L)
  }
  def where[Value](
    tx: BoundToTx, label: Attr[_], prop: Attr[Value], value: Value,
    options: List[SearchOption]
  ) = {
    var from: Option[ObjId] = None
    var upTo: Option[ObjId] = None
    var limit = Long.MaxValue
    var lastOnly = false
    var needSameValue = true
    options.foreach {
      case FindFirstOnly if limit == Long.MaxValue => limit = 1L
      case FindLastOnly => lastOnly = true
      case FindFrom(node) if from.isEmpty =>
        from = Some(node(nodeAttrs.objId))
      case FindAfter(node) if from.isEmpty =>
        from = Some(nextNode(node)(nodeAttrs.objId))
      case FindUpTo(node) if upTo.isEmpty =>
        upTo = Some(node(nodeAttrs.objId))
      case FindNextValues ⇒ needSameValue = false
    }
    val searchKey = SearchByLabelProp[Value](attrFactory.defined(label), attrFactory.defined(prop))
    //println(s"searchKey: $searchKey")
    val handler = handlerLists.single(searchKey, ()⇒Never())
    val feed = new NodeListFeedImpl(needSameValue, upTo, limit, nodeFactory)
    val request = new SearchRequest[Value](tx, value, from, feed)
    handler(request)
    if(lastOnly) feed.result.headOption.toList else feed.result.reverse
  }
  def justIndexed = "Y"
  def whereObjId(objId: ObjId): Obj = nodeFactory.toNode(objId)
  def toObjId(uuid: UUID): ObjId =
    nodeFactory.toObjId(uuid.getMostSignificantBits, uuid.getLeastSignificantBits)
  def toUUIDString(objId: ObjId) = new UUID(objId.hiObjId,objId.loObjId).toString
  def handlers = factIndex.handlers(at.justIndexed)
}

class NodeListFeedImpl(needSameValue: Boolean, upTo: Option[ObjId], var limit: Long, nodeFactory: NodeFactory) extends Feed {
  var result: List[Obj] = Nil
  def feed(diff: Long, valueA: Long, valueB: Long): Boolean = {
    if(needSameValue && diff > 0 || upTo.nonEmpty && (valueA > upTo.get.hiObjId || valueA == upTo.get.hiObjId && valueB > upTo.get.loObjId)){ return false }
    result = nodeFactory.toNode(valueA,valueB) :: result
    limit -= 1L
    limit > 0L
  }
}

class AttrListFeedImpl(converter: RawValueConverter[Attr[Boolean]]) extends Feed {
  var result: List[Attr[Boolean]] = Nil
  def feed(diff: Long, valueA: Long, valueB: Long) = {
    result = converter.convert(valueA,valueB) :: result
    true
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