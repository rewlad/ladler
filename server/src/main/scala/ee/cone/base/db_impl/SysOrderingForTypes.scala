package ee.cone.base.db_impl

import java.time.{Instant, LocalTime}

import ee.cone.base.connection_api._
import ee.cone.base.util.Never

class ObjOrderingForAttrValueTypes(
  handlerLists: CoHandlerLists,
  objOrderingFactory: ObjOrderingFactoryI,
  asBoolean: AttrValueType[Boolean],
  asString: AttrValueType[String],
  asObj: AttrValueType[Obj],
  asInstant: AttrValueType[Option[Instant]],
  asLocalTime: AttrValueType[Option[LocalTime]],
  asBigDecimal: AttrValueType[Option[BigDecimal]]
) extends CoHandlerProvider {
  private def converter[From,To](from: AttrValueType[From], to: AttrValueType[To]) =
    handlerLists.single(ConverterKey(from,to), ()⇒Never())
  def handlers =
    objOrderingFactory.handlers(asBoolean) :::
      objOrderingFactory.handlers(asString) :::
      objOrderingFactory.handlers(asObj)(Ordering.by(obj⇒converter(asObj,asString)(obj))) :::
      objOrderingFactory.handlers(asInstant) :::
      objOrderingFactory.handlers(asLocalTime) :::
      objOrderingFactory.handlers(asBigDecimal)
}
