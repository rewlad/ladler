package ee.cone.base.db

import ee.cone.base.connection_api.{Registration, ConnectionComponent}
import ee.cone.base.db.Types._
import ee.cone.base.util.{Setup, Never}

import scala.collection.mutable

case class DBNodeImpl(objId: Long)(val rawIndex: RawIndex, rw: Boolean) extends DBNode {
  def apply[Value](attr: Attr[Value]) = attr.get(this)
  def update[Value](attr: Attr[Value], value: Value) = {
    if(!rw) Never()
    attr.set(this, value)
  }
}

class NodeValueConverter(inner: InnerRawValueConverter, createNode: ObjId=>DBNode) extends RawValueConverter[Option[DBNode]] {
  def convert() = None
  def convert(valueA: Long, valueB: Long) = if(valueB==0) Some(createNode(valueA)) else Never()
  def convert(value: String) = Never()
  def allocWrite(before: Int, value: Option[DBNode], after: Int) =
    if(nonEmpty(value)) inner.allocWrite(before, value.get.objId, 0L, after) else Never()
  def nonEmpty(value: Option[DBNode]) = value.nonEmpty
  def same(valueA: Option[DBNode], valueB: Option[DBNode]) =
    valueA.map(_.objId) == valueB.map(_.objId)
}

class BooleanValueConverter(inner: InnerRawValueConverter) extends RawValueConverter[Boolean] {
  def convert() = false
  def convert(valueA: Long, valueB: Long) = true
  def convert(value: String) = true
  def allocWrite(before: Int, value: Boolean, after: Int) =
    if(nonEmpty(value)) inner.allocWrite(before, 1L, 0L, after) else Never()
  def nonEmpty(value: Boolean) = value
  def same(valueA: Boolean, valueB: Boolean) = valueA == valueB
}

trait ListByDBNode {
  def list(node: DBNode): List[Attr[_]]
}

class ListByDBNodeImpl(inner: FactIndex, attrFactory: AttrFactory, booleanValueConverter: BooleanValueConverter) extends ListByDBNode {
  def list(node: DBNode) = {
    val feed = new ListFeedImpl[Attr[_]](Long.MaxValue,attrFactory.apply(_,_,booleanValueConverter))
    inner.execute(node, feed)
    feed.result
  }
}

case class ListByValueImpl[Value](attrCalc: SearchAttrCalc[Value])(
  createNode: ObjId=>DBNode, searchIndex: SearchIndex
) extends ListByValue[Value] {
  def attrId = attrCalc.searchAttrId
  def list(value: Value): List[DBNode] = {
    val feed = new ListFeedImpl[DBNode](Long.MaxValue,(objId,_)=>createNode(objId))
    searchIndex.execute(attrId, value, feed)
    feed.result.reverse
  }
  def list(value: Value, fromObjId: ObjId, limit: Long): List[DBNode] = {
    val feed = new ListFeedImpl[DBNode](limit,(objId,_)=>createNode(objId))
    searchIndex.execute(attrId, value, fromObjId, feed)
    feed.result.reverse
  }
  def components = attrCalc :: Nil
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