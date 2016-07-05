
package ee.cone.base.test_loots //demo

import java.time.Duration

import ee.cone.base.connection_api.{AttrCaption, CoHandler, CoHandlerProvider, ValidationState, _}
import ee.cone.base.db.{LazyObjFactory, SearchByLabelProp, UIStrings, ValidationFactory, _}

class FuelingAttrs(
  attr: AttrFactory,
  label: LabelFactory,
  objIdFactory: ObjIdFactory,
  asString: AttrValueType[String],
  asDuration: AttrValueType[Option[Duration]],
  asBigDecimal: AttrValueType[Option[BigDecimal]],
  asObjId: AttrValueType[ObjId]
)(
  // 00 08 RF 24
  val asFueling: Attr[Obj] = label("8fc310bc-0ae7-4ad7-90f1-2dacdc6811ad"),
  //val meHoursStr: Attr[String] = attr("5415aa5e-efec-4f05-95fa-4954fee2dd2e", asString),
  val meHours: Attr[Option[Duration]] = attr("9be17c9f-6689-44ca-badf-7b55cc53a6b0", asDuration),
  val fuel: Attr[Option[BigDecimal]] = attr("f29cdc8a-4a93-4212-bb23-b966047c7c4d", asBigDecimal),
  val comment: Attr[String] = attr("2589cfd4-b125-4e4d-b3e9-9200690ddbc9", asString),
  val engineer: Attr[String] = attr("e5fe80e5-274a-41ab-b8b8-1909310b5a17", asString),
  val master: Attr[String] = attr("b85d4572-8cc5-42ad-a2f1-a3406352800a", asString),
  val time: Attr[String] = attr("df695468-c2bd-486c-9e58-f83da9566940", asString),
  val time00: ObjId = objIdFactory.toObjId("2b4c0bbf-fd24-4df8-b57a-29c26af11b23"),
  val time08: ObjId = objIdFactory.toObjId("a281aafc-b32d-4cf0-8599-eee8448c937d"),
  val time24: ObjId = objIdFactory.toObjId("f6bdcef8-179e-4da3-8c3d-ec7f51716ee6"),
  val fullKey: Attr[ObjId] = attr("37b8c873-e897-49b2-9802-5c4d87f5a272",asObjId)
)

class FuelingItems(
  at: FuelingAttrs,
  findAttrs: FindAttrs,
  alienAttrs: AlienAttributes,
  nodeAttrs: NodeAttrs,
  factIndex: FactIndex,
  searchIndex: SearchIndex,
  alien: Alien,
  onUpdate: OnUpdate,
  attrFactory: AttrFactory,
  dbWrapType: WrapType[ObjId],
  validationFactory: ValidationFactory,
  uiStrings: UIStrings,
  lazyObjFactory: LazyObjFactory
)(
  val fuelingByFullKey: SearchByLabelProp[ObjId] = searchIndex.create(at.asFueling,at.fullKey),
  val times: List[ObjId] = List(at.time00,at.time08,at.time24)
) extends CoHandlerProvider {
  def handlers =
    attrFactory.handlers(at.time)((obj,objId)⇒
      if(objId == at.time00) "00:00" else
      if(objId == at.time08) "08:00" else
      if(objId == at.time24) "24:00" else throw new Exception(s"? $objId")
    ) :::
      uiStrings.captions(at.asFueling, Nil)(_⇒"") :::
      CoHandler(AttrCaption(at.asFueling))("Fueling") ::
      CoHandler(AttrCaption(at.meHours))("ME Hours.Min") ::
      CoHandler(AttrCaption(at.fuel))("Fuel rest/quantity") ::
      CoHandler(AttrCaption(at.comment))("Comment") ::
      CoHandler(AttrCaption(at.engineer))("Engineer") ::
      CoHandler(AttrCaption(at.master))("Master") ::
      searchIndex.handlers(fuelingByFullKey) :::
      List(
        at.asFueling, at.meHours, at.fuel, at.comment, at.engineer, at.master
      ).flatMap(alien.update(_))
  def fueling(entry: Obj, time: ObjId, wrapForEdit: Boolean) =
    lazyObjFactory.create(fuelingByFullKey,List(entry(nodeAttrs.objId),time),wrapForEdit)
  def validation(entry: Obj): List[ValidationState] = {
    val fuelingList = times.map(time⇒fueling(entry, time, wrapForEdit = false))
    fuelingList.flatMap { obj ⇒
      validationFactory.need[Option[BigDecimal]](obj,at.fuel,v⇒if(v.isEmpty) Some("") else None) :::
        validationFactory.need[String](obj,at.engineer,v⇒if(v.isEmpty) Some("") else None) :::
        validationFactory.need[String](obj,at.master,v⇒if(v.isEmpty) Some("") else None)
    } :::
      fuelingList.sliding(2).toList.flatMap{ fuelingPair ⇒ fuelingPair.map(_(at.meHours)) match {
        case Some(a) :: Some(b) :: Nil if a.compareTo(b) <= 0 ⇒ Nil
        case _ ⇒ fuelingPair.flatMap(fueling⇒
          validationFactory.need[Option[Duration]](fueling,at.meHours,v⇒Some("to increase"))
        )
      }}
  }
}

