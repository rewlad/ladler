package ee.cone.base.test_loots

import ee.cone.base.connection_api.{AttrCaption, _}
import ee.cone.base.db._
import ee.cone.base.framework.Users

class ErrorAttributes(
  attr: AttrFactory,
  label: LabelFactory,
  asObj: AttrValueType[Obj],
  asString: AttrValueType[String],
  asBoolean: AttrValueType[Boolean]
)(
  val asError: Attr[Obj] = label("40eed80a-f249-4e59-9193-2b27dffd8d6e"),
  val errorMsg: Attr[String] = attr("946913bd-e644-4ff0-8aa1-75ff95e6e7d9", asString),
  val realm: Attr[Obj] = attr("9dbb1e0f-523f-4e7b-b524-6dfd7a12a681", asObj),
  val show: Attr[Boolean] = attr("1626b864-21ae-4517-8006-c3d104852488", asBoolean)
)

class Errors(
  at: ErrorAttributes, searchIndex: SearchIndex, alien: Alien,
  users: Users, findNodes: FindNodes,
  listedFactory: IndexedObjCollectionFactory
)(
  val findAll: SearchByLabelProp[Obj] = searchIndex.create(at.asError, at.realm)
) extends CoHandlerProvider{
  def lastError: Obj = { // todo replace
    //val itemList2 = findNodes.where(mainTx(),errors.findByView,view,Nil)
    val listed = listedFactory.create(findAll,users.world)
    listed.toList.lastOption.getOrElse(findNodes.noNode) //order?
  }
  def handlers =
    CoHandler(AttrCaption(at.realm))("Error") ::
      CoHandler(AttrCaption(at.show))("Show") ::
      List(findAll).flatMap(searchIndex.handlers):::
      List(at.asError,at.errorMsg,at.realm,at.show).flatMap(alien.update(_))
}
