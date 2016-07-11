package ee.cone.base.db_impl

import java.time.{Duration, Instant, LocalTime}
import java.util.UUID

import ee.cone.base.connection_api.{Attr, AttrValueType, BasicValueTypes, Obj}
import ee.cone.base.db.{AttrFactory, LabelFactory, ObjIdFactory}

class BasicValueTypesImpl(
  objIdFactory: ObjIdFactory
)(
  val asString: AttrValueType[String] = AttrValueType[String](objIdFactory.toObjId("1e94f9bc-a34d-4fab-8a01-eb3dd98795d2")),
  val asObj: AttrValueType[Obj] = AttrValueType[Obj](objIdFactory.toObjId("275701ec-cb9b-4474-82e6-69f2e1f28c87")),
  val asUUID: AttrValueType[Option[UUID]] = AttrValueType[Option[UUID]](objIdFactory.toObjId("13c5769d-f120-4a1a-9fce-c56df8835f08")),
  val asBoolean: AttrValueType[Boolean] = AttrValueType[Boolean](objIdFactory.toObjId("fa03f6f1-90ef-460d-a4dd-2279269a4d79")),
  val asBigDecimal: AttrValueType[Option[BigDecimal]] = AttrValueType[Option[BigDecimal]](objIdFactory.toObjId("4d5894bc-e913-4d90-8b7f-32bd1d3893ea")),
  val asInstant: AttrValueType[Option[Instant]] = AttrValueType[Option[Instant]](objIdFactory.toObjId("ce152d2d-d783-439f-a21b-e175663f2650")),
  val asDuration: AttrValueType[Option[Duration]] = AttrValueType[Option[Duration]](objIdFactory.toObjId("356068df-ac9d-44cf-871b-036fa0ac05ad")),
  val asLocalTime: AttrValueType[Option[LocalTime]] = AttrValueType[Option[LocalTime]](objIdFactory.toObjId("8489d9a9-37ec-4206-be73-89287d0282e3"))
) extends BasicValueTypes

class LabelFactoryImpl(
  attr: AttrFactory,
  basicValueTypes: BasicValueTypes
) extends LabelFactory {
  def apply(uuid: String) = attr(uuid, basicValueTypes.asObj)
}
