package ee.cone.base.db_impl

import java.time.{Instant, LocalTime}

import ee.cone.base.connection_api.{AttrValueType, CoHandlerProvider, Obj}

class ObjOrderingForAttrValueTypes(
  objOrderingFactory: ObjOrderingFactory,
  asBoolean: AttrValueType[Boolean],
  asString: AttrValueType[String],
  asObj: AttrValueType[Obj],
  asInstant: AttrValueType[Option[Instant]],
  asLocalTime: AttrValueType[Option[LocalTime]],
  asBigDecimal: AttrValueType[Option[BigDecimal]],
  uiStrings: UIStrings
) extends CoHandlerProvider {
  def handlers =
    objOrderingFactory.handlers(asBoolean) :::
      objOrderingFactory.handlers(asString) :::
      objOrderingFactory.handlers(asObj)(Ordering.by(obj⇒uiStrings.converter(asObj,asString)(obj))) :::
      objOrderingFactory.handlers(asInstant) :::
      objOrderingFactory.handlers(asLocalTime) :::
      objOrderingFactory.handlers(asBigDecimal)
}
