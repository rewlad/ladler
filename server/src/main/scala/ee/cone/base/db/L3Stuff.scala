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

trait SysAttrs {
  def seq: Attr[Obj]
  def justIndexed: Attr[String]
}

class SysAttrsImpl(
  attr: AttrFactory,
  label: LabelFactory,
  searchIndex: SearchIndex,
  nodeValueConverter: RawValueConverter[Obj],
  uuidValueConverter: RawValueConverter[Option[UUID]],
  stringValueConverter: RawValueConverter[String],
  mandatory: Mandatory,
  unique: Unique
)(
  val seq: Attr[Obj] = attr("a6479f10-5a99-47d1-a6e9-2c1713b44e3a", nodeValueConverter),
  val asSrcIdentifiable: Attr[Obj] = label("d3576ce3-100f-437c-bd00-febe9c0f1906"),
  val srcId: Attr[Option[UUID]] = attr("54d45fef-e9ee-44f7-8522-aec1cd78743e", uuidValueConverter),
  val justIndexed: Attr[String] = attr("e4a1ccbc-f039-4af1-a505-c6bee1b755fd", stringValueConverter)
)(val handlers: List[BaseCoHandler] =
  unique(asSrcIdentifiable, srcId) :::
  mandatory(asSrcIdentifiable, srcId, mutual = true) :::
  // searchIndex.handlers(asSrcIdentifiable, srcId) ::: // inside unique
  Nil
) extends SysAttrs with CoHandlerProvider

class FindNodesImpl(
  handlerLists: CoHandlerLists, nodeFactory: NodeFactory, attrFactory: AttrFactory
) extends FindNodes {
  def where[Value](
    tx: BoundToTx, label: Attr[_], prop: Attr[Value], value: Value,
    options: List[SearchOption]
  ) = {
    var from: Option[ObjId] = None
    var upTo = new ObjId(Long.MaxValue)
    var limit = Long.MaxValue
    var lastOnly = false
    var needSameValue = true
    options.foreach {
      case FindFirstOnly if limit == Long.MaxValue => limit = 1L
      case FindLastOnly => lastOnly = true
      case FindFrom(node) if from.isEmpty =>
        from = Some(node(nodeFactory.objId)
      )
      case FindAfter(node) if from.isEmpty =>
        from = Some(node(nodeFactory.nextObjId)
      )
      case FindUpTo(node) if upTo.value == Long.MaxValue =>
        upTo = node(nodeFactory.objId)
      case FindNextValues ⇒ needSameValue = false
    }
    val searchKey = SearchByLabelProp[Value](attrFactory.defined(label), attrFactory.defined(prop))
    //println(s"searchKey: $searchKey")
    val handler = handlerLists.single(searchKey)
    val feed = new NodeListFeedImpl(needSameValue, upTo, limit, nodeFactory, tx)
    val request = new SearchRequest[Value](tx, value, from, feed)
    handler(request)
    if(lastOnly) feed.result.headOption.toList else feed.result.reverse
  }
  def justIndexed = "Y"
}

class UniqueNodesImpl(
  converter: RawValueConverter[Obj], nodeFactory: NodeFactory, at: SysAttrsImpl,
  findNodes: FindNodes
) extends UniqueNodes {
  def whereSrcId(tx: BoundToTx, srcId: UUID): Obj =
    findNodes.where(tx, at.asSrcIdentifiable, at.srcId, Some(srcId), Nil) match {
      case Nil => converter.convertEmpty()
      case node :: Nil => node
      case _ => Never()
    }
  def srcId = at.srcId
  def seqNode(tx: BoundToTx) = nodeFactory.toNode(tx,new ObjId(0L))
  def create(tx: BoundToTx, label: Attr[Obj], srcId: UUID): Obj = {
    val sNode = seqNode(tx)
    val lastNode = sNode(at.seq)
    val nextObjId = (if(lastNode.nonEmpty) lastNode else sNode)(nodeFactory.nextObjId)
    val res = nodeFactory.toNode(tx,nextObjId)
    sNode(at.seq) = res

    res(label) = res
    res(at.asSrcIdentifiable) = res
    res(at.srcId) = Some(srcId)

    res
  }
  def noNode = nodeFactory.noNode
}

class NodeListFeedImpl(needSameValue: Boolean, upTo: ObjId, var limit: Long, nodeFactory: NodeFactory, tx: BoundToTx) extends Feed {
  var result: List[Obj] = Nil
  def apply(diff: Long, objId: Long): Boolean = {
    if(needSameValue && diff > 0 || objId > upTo.value){ return false }
    result = nodeFactory.toNode(tx,new ObjId(objId)) :: result
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