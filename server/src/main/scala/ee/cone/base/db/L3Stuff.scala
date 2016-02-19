package ee.cone.base.db

import ee.cone.base.connection_api.ConnectionComponent
import ee.cone.base.db.Types._
import ee.cone.base.util.Never

import scala.collection.mutable

case class DBNodeImpl(objId: Long)(db: FactIndex) extends DBNode {
  def apply[Value](attr: Attr[Value]) = db.get(objId, attr)
  def update[Value](attr: Attr[Value], value: Value) = db.set(objId, attr, value)
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

class ListByDBNodeImpl(inner: FactIndex, index: (Long,Long)=>Attr[_]) extends ListByDBNode {
  def list(node: DBNode) = {
    val feed = new ListFeedImpl[Attr[_]](index)
    inner.execute(node.objId, feed)
    feed.result
  }
}

case class ListByValueImpl[Value](attrCalc: SearchAttrCalc[Value])(
  createNode: ObjId=>DBNode, searchIndex: SearchIndex
) extends ListByValue[Value] {
  def attrId = attrCalc.searchAttrId
  def list(value: Value): List[DBNode] = {
    val feed = new ListFeedImpl[DBNode]((objId,_)=>createNode(objId))
    searchIndex.execute(attrId, value, feed)
    feed.result.reverse
  }
  def list(value: Value, fromNode: DBNode): List[DBNode] = {
    val feed = new ListFeedImpl[DBNode]((objId,_)=>createNode(objId))
    searchIndex.execute(attrId, value, fromNode.objId, feed)
    feed.result.reverse
  }
  def components = attrCalc :: Nil
}

class ListFeedImpl[To](converter: (Long,Long)=>To) extends Feed {
  var result: List[To] = Nil
  def apply(valueA: Long, valueB: Long) = {
    result = converter(valueA,valueB) :: result
    true
  }
}

case class PreCommitCheckAttrCalcImpl(check: PreCommitCheck)(createNode: ObjId=>DBNode) extends PreCommitCheckAttrCalc {
  private lazy val objIds = mutable.SortedSet[ObjId]()
  def affectedBy = check.affectedBy.map(_.nonEmpty)
  def beforeUpdate(objId: ObjId) = ()
  def afterUpdate(objId: ObjId) = objIds += objId
  def checkAll() = check.check(objIds.toSeq.map(createNode(_)))
}

class CheckAll(components: =>List[ConnectionComponent]) {
  def apply(): Seq[ValidationFailure] =
    components.collect{ case i: PreCommitCheckAttrCalc => i.checkAll() }.flatten
}


class AttrCalcAdapter(inner: NodeAttrCalc)(createNode: ObjId=>DBNode) extends AttrCalc {
  def affectedBy = inner.affectedBy.map(_.nonEmpty)
  def beforeUpdate(objId: ObjId) = inner.beforeUpdate(createNode(objId))
  def afterUpdate(objId: ObjId) = inner.afterUpdate(createNode(objId))
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