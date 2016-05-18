package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.util.Never

class FindAttrsImpl(
  attr: AttrFactory,
  asDefined: AttrValueType[Boolean],
  asString: AttrValueType[String]
)(
  val justIndexed: Attr[String] = attr("e4a1ccbc-f039-4af1-a505-c6bee1b755fd", asString),
  val nonEmpty: Attr[Boolean] = attr("1cc81826-a1c0-4045-ab2a-e2501b4a71fc", asDefined)
) extends FindAttrs

class FindNodesImpl(
  at: FindAttrs,
  handlerLists: CoHandlerLists,
  nodeAttrs: NodeAttrs, noObj: Obj,
  attrFactory: AttrFactory, factIndex: FactIndex, objIdFactory: ObjIdFactory,
  dBObjValueConverter: RawValueConverter[ObjId], dbWrapType: WrapType[ObjId]
)(
  val noNode: Obj = noObj.wrap(dbWrapType, NoObjId)
) extends FindNodes  with CoHandlerProvider {
  def whereObjId(objId: ObjId): Obj = noObj.wrap(dbWrapType, objId)
  def zeroNode = whereObjId(dBObjValueConverter.convert(0L,0L))
  def nextNode(obj: Obj) = {
    val node = obj(nodeAttrs.objId)
    if(node.hi!=0L || node.lo == Long.MaxValue) Never()
    whereObjId(dBObjValueConverter.convert(node.hi, node.lo + 1L))
  }
  def where[Value](
    tx: BoundToTx, label: Attr[_], prop: Attr[Value], value: Value,
    options: List[SearchOption]
  ) = {
    var from: ObjId = objIdFactory.noObjId
    var upTo: ObjId = objIdFactory.noObjId
    var limit = Long.MaxValue
    var lastOnly = false
    var needSameValue = true
    options.foreach {
      case FindFirstOnly if limit == Long.MaxValue => limit = 1L
      case FindLastOnly => lastOnly = true
      case FindFrom(node) if !from.nonEmpty =>
        from = node(nodeAttrs.objId)
      case FindAfter(node) if !from.nonEmpty =>
        from = nextNode(node)(nodeAttrs.objId)
      case FindUpTo(node) if !upTo.nonEmpty =>
        upTo = node(nodeAttrs.objId)
      case FindNextValues ⇒ needSameValue = false
    }
    val searchKey = SearchByLabelProp[Value](attrFactory.attrId(label), attrFactory.attrId(prop))
    //println(s"searchKey: $searchKey")
    val handler = handlerLists.single(searchKey, ()⇒Never())
    //val feed = new NodeListFeedImpl(needSameValue, upTo, limit, nodeFactory)
    var result: List[Obj] = Nil
    val request = new SearchRequest[Value](tx, value, needSameValue, from, objId ⇒
      if(upTo.nonEmpty && (objId.hi > upTo.hi || objId.hi == upTo.hi && objId.lo > upTo.lo)) false else {
        result = whereObjId(objId) :: result
        limit -= 1L
        limit > 0L
      }
    )
    handler(request)
    if(lastOnly) result.headOption.toList else result.reverse
  }
  def justIndexed = "Y"
  def toObjId(uuid: UUID): ObjId = objIdFactory.toObjId(uuid)
  def toUUIDString(objId: ObjId) = new UUID(objId.hi,objId.lo).toString
  def handlers =
    CoHandler(GetValue(dbWrapType, at.nonEmpty))((obj,innerObj)⇒innerObj.data.nonEmpty) ::
    CoHandler(GetValue(dbWrapType, nodeAttrs.objId))((obj,innerObj)⇒innerObj.data) ::
    factIndex.handlers(at.justIndexed)
}

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

class AttrListFeedImpl(converter: RawValueConverter[Attr[Boolean]]) extends Feed {
  var result: List[Attr[Boolean]] = Nil
  def feed(diff: Long, valueA: Long, valueB: Long) = {
    result = converter.convert(valueA,valueB) :: result
    true
  }
}
*/





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