package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.db.Types.ObjId
import ee.cone.base.util.{Never, Single}

class ListByDBNodeImpl(
  inner: FactIndex, attrValueConverter: RawValueConverter[Attr[Boolean]]
) extends ListByDBNode {
  def defined = Never()
  def get(node: Obj) = {
    val feed = new AttrListFeedImpl(attrValueConverter)
    inner.execute(node, feed)
    feed.result
  }
  def set(node: Obj, value: List[Attr[_]]) = if(value.nonEmpty) Never()
    else node(this).foreach(attr => node(attr.defined) = false)
}

class SysAttrs(
  attr: AttrFactory,
  searchIndex: SearchIndex,
  nodeValueConverter: RawValueConverter[Obj],
  uuidValueConverter: RawValueConverter[Option[UUID]],
  mandatory: Mandatory
)(
  val seq: Attr[Obj] = attr(0, 0x0001, nodeValueConverter),
  val asSrcIdentifiable: Attr[Obj] = attr(0x0002, 0, nodeValueConverter),
  val srcId: Attr[Option[UUID]] = attr(0, 0x0003, uuidValueConverter)
)(val handlers: List[BaseCoHandler] =
  mandatory(asSrcIdentifiable, srcId, mutual = true) :::
  searchIndex.handlers(asSrcIdentifiable, srcId) :::
  Nil
) extends CoHandlerProvider

class DBNodesImpl(
  handlerLists: CoHandlerLists, converter: RawValueConverter[Obj],
  nodeFactory: NodeFactory, at: SysAttrs
) extends DBNodes {
  def where[Value](
    tx: BoundToTx, label: Attr[Boolean], prop: Attr[Value], value: Value,
    options: List[SearchOption]
  ) = {
    var from: Option[ObjId] = None
    var upTo = Long.MaxValue
    var limit = Long.MaxValue
    var lastOnly = false
    options.foreach{
      case FindFirstOnly if limit == Long.MaxValue => limit = 1L
      case FindLastOnly => lastOnly = true
      case FindFrom(node) if from.isEmpty => from = Some(node(nodeFactory.objId))
      case FindAfter(node) if from.isEmpty => from = Some(node(nodeFactory.objId)+1L)
      case FindUpTo(node) if upTo == Long.MaxValue => upTo = node(nodeFactory.objId)
    }
    val searchKey = SearchByLabelProp[Value](label.defined, prop.defined)
    val handler = Single(handlerLists.list(searchKey))
    val feed = new NodeListFeedImpl(upTo,limit,converter)
    val request = new SearchRequest[Value](tx, value, from, feed)
    handler(request)
    if(lastOnly) feed.result.head :: Nil else feed.result.reverse
  }
  def whereSrcId(tx: BoundToTx, srcId: UUID): Obj =
    where(tx, at.asSrcIdentifiable.defined, at.srcId, Some(srcId), Nil) match {
      case Nil => converter.convert()
      case node :: Nil => node
      case _ => Never()
    }
  def srcId = at.srcId
  def seqNode(tx: BoundToTx) = nodeFactory.toNode(tx,0L)
  def create(tx: BoundToTx, label: Attr[Obj]): Obj = {
    val sNode = seqNode(tx)
    val lastNode = sNode(at.seq)
    val nextObjId = if(lastNode.nonEmpty) lastNode(nodeFactory.objId) + 1L else 1L
    val res = nodeFactory.toNode(tx,nextObjId)
    sNode(at.seq) = res
    res(label) = res
    res(at.asSrcIdentifiable) = res
    res(at.srcId) = Some(UUID.randomUUID)
    res
  }
}

class NodeListFeedImpl(upTo: Long, var limit: Long, converter: RawValueConverter[Obj]) extends Feed {
  var result: List[Obj] = Nil
  def apply(valueA: Long, valueB: Long): Boolean = {
    if(valueB > upTo){ return false }
    result = converter.convert(valueA,valueB) :: result
    limit -= 1L
    limit > 0L
  }
}

class AttrListFeedImpl(converter: RawValueConverter[Attr[Boolean]]) extends Feed {
  var result: List[Attr[Boolean]] = Nil
  def apply(valueA: Long, valueB: Long) = {
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