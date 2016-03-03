package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api.{EventKey, CoHandlerLists, BaseCoHandler}
import ee.cone.base.db.Types._
import ee.cone.base.util.{Single, Never}

case object NoDBNode extends DBNode {
  def nonEmpty = false
  def objId = Never()
  def apply[Value](attr: Attr[Value]) = Never()
  def update[Value](attr: Attr[Value], value: Value) = Never()
  def tx = Never()
}

case class DBNodeImpl(objId: Long)(val tx: RawTx) extends DBNode {
  def nonEmpty = true
  def apply[Value](attr: Attr[Value]) = attr.get(this)
  def update[Value](attr: Attr[Value], value: Value) = {
    if(!tx.rw) Never()
    attr.set(this, value)
  }
}

class NodeValueConverter(inner: InnerRawValueConverter, createNode: ObjId=>DBNode) extends RawValueConverter[DBNode] {
  def convert() = NoDBNode
  def convert(valueA: Long, valueB: Long) = if(valueB==0) createNode(valueA) else Never()
  def convert(value: String) = Never()
  def allocWrite(before: Int, value: DBNode, after: Int) =
    if(nonEmpty(value)) inner.allocWrite(before, value.objId, 0L, after) else Never()
  def nonEmpty(value: DBNode) = value.nonEmpty
}

class UUIDValueConverter(inner: InnerRawValueConverter) extends RawValueConverter[UUID] {
  def convert() = Never()
  def convert(valueA: Long, valueB: Long) = new UUID(valueA,valueB)
  def convert(value: String) = Never()
  def allocWrite(before: Int, value: UUID, after: Int) =
    if(nonEmpty(value)) inner.allocWrite(before, value.getMostSignificantBits, value.getLeastSignificantBits, after) else Never()
  def nonEmpty(value: UUID) = true
}

// for true Boolean converter? if(nonEmpty(value)) inner.allocWrite(before, 1L, 0L, after) else Never()
class DefinedValueConverter(inner: InnerRawValueConverter) extends RawValueConverter[Boolean] {
  def convert() = false
  def convert(valueA: Long, valueB: Long) = true
  def convert(value: String) = true
  def allocWrite(before: Int, value: Boolean, after: Int) = Never()
  def nonEmpty(value: Boolean) = value
}

class ListByDBNodeImpl(inner: FactIndex, attrFactory: AttrFactory, definedValueConverter: DefinedValueConverter) extends ListByDBNode {
  def list(node: DBNode) = {
    val feed = new ListFeedImpl[Attr[_]](Long.MaxValue,attrFactory.apply(_,_,definedValueConverter))
    inner.execute(node, feed)
    feed.result
  }
}

class ListByValueStartImpl[DBEnvKey](
  handlerLists: CoHandlerLists,
  searchIndex: SearchIndex, txManager: TxManager[DBEnvKey], createNode: ObjId=>DBNode
) extends ListByValueStart[DBEnvKey] {
  def of[Value](attr: Attr[Value]) = of(SearchByAttr[Value](attr.defined))
  def of[Value](label: Attr[Boolean], prop: Attr[Value]) =
    of(SearchByLabelProp[Value](label.defined, prop.defined))
  private def of[Value](searchKey: EventKey[SearchRequest[Value],Unit]) = {
    val handler = Single(handlerLists.list(searchKey))
    val tx = txManager.tx
    new ListByValue[Value] {
      def list(value: Value) = {
        val feed = new ListFeedImpl[DBNode](Long.MaxValue,(objId,_)=>createNode(objId))
        val request = new SearchRequest[Value](tx, value, None, feed)
        handler(request)
        feed.result.reverse
      }
    }
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

class ObjIdSequence(
  seqAttr: Attr[DBNode],
  createNode: ObjId=>DBNode
) {
  def inc(): DBNode = {
    val seqNode = createNode(0L)
    val lastNode = seqNode(seqAttr)
    val nextObjId = if(lastNode.nonEmpty) lastNode.objId + 1L else 1L
    val res = createNode(nextObjId)
    seqNode(seqAttr) = res
    res
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