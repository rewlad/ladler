package ee.cone.base.db_impl

import java.time.{Instant, LocalTime}

import ee.cone.base.connection_api._
import ee.cone.base.util.Never

class ObjOrderingForAttrValueTypes(
  handlerLists: CoHandlerLists,
  objOrderingFactory: ObjOrderingFactoryI,
  vType: BasicValueTypes
) extends CoHandlerProvider {
  private def converter[From,To](from: AttrValueType[From], to: AttrValueType[To]) =
    handlerLists.single(ConverterKey(from,to), ()⇒Never())
  def handlers =
    objOrderingFactory.handlers(vType.asBoolean) :::
      objOrderingFactory.handlers(vType.asString) :::
      objOrderingFactory.handlers(vType.asObj)(Ordering.by(obj⇒converter(vType.asObj,vType.asString)(obj))) :::
      objOrderingFactory.handlers(vType.asInstant) :::
      objOrderingFactory.handlers(vType.asLocalTime) :::
      objOrderingFactory.handlers(vType.asBigDecimal)
}
