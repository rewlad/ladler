package ee.cone.base.test_loots

import java.time._
import java.time.format.DateTimeFormatter

import ee.cone.base.connection_api.{CoHandler, CoHandlerProvider}
import ee.cone.base.db._
import ee.cone.base.util.Never

abstract class TimeRawValueConverterImpl[IValue] extends RawValueConverter[IValue] with CoHandlerProvider {
  type Value = IValue
  def valueType: AttrValueType[Value]
  def asString: AttrValueType[String]
  def toUIString(value: Value): String
  def fromUIString(value: String): Value
  def handlers = List(
    CoHandler(ToRawValueConverter(valueType))(this),
    CoHandler(ToUIStringConverter(valueType,asString))(toUIString),
    CoHandler(ToUIStringConverter(asString,valueType))(fromUIString)
  )
  protected def zeroPad2(x: String) = x.length match {
    case 0 ⇒ "00"
    case 1 ⇒ s"0$x"
    case _ ⇒ x
  }
  protected def strToPair[To](value: String, by: (Int,Int)⇒To): Option[To] = if(value.nonEmpty) {
    val Array(h,m) = value.split(":")
    Some(by(h.toInt,m.toInt))
  } else None
}

class DurationValueConverter(
  val valueType: AttrValueType[Option[Duration]], inner: RawConverter, val asString: AttrValueType[String]
) extends TimeRawValueConverterImpl[Option[Duration]] {
  def convertEmpty() = None
  def convert(valueA: Long, valueB: Long) = Option(Duration.ofSeconds(valueA,valueB))
  def convert(value: String) = Never()
  def toBytes(preId: ObjId, value: Value, finId: ObjId) =
    if(value.nonEmpty) inner.toBytes(preId, value.get.getSeconds, value.get.getNano, finId) else Array()
  def toUIString(value: Value) = value.map(x =>
    s"${zeroPad2(x.abs.toHours.toString)}:${zeroPad2(x.abs.minusHours(x.abs.toHours).toMinutes.toString)}"
  ).getOrElse("")
  def fromUIString(value: String): Value =
    strToPair(value, (h,m)⇒Duration.ofMinutes(h*60+m))
}

class InstantValueConverter(
  val valueType: AttrValueType[Option[Instant]], inner: RawConverter, val asString: AttrValueType[String]
) extends TimeRawValueConverterImpl[Option[Instant]] {
  def convertEmpty() = None
  def convert(valueA: Long, valueB: Long) = Option(Instant.ofEpochSecond(valueA,valueB))
  def convert(value: String) = Never()
  def toBytes(preId: ObjId, value: Value, finId: ObjId) =
    if(value.nonEmpty) inner.toBytes(preId, value.get.getEpochSecond, value.get.getNano, finId) else Array()

  private def zoneId = ZoneId.of("UTC")
  def toUIString(value: Value) = value.map{ v ⇒
    val date = LocalDate.from(v.atZone(zoneId))
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    date.format(formatter)
  }.getOrElse("")
  def fromUIString(value: String) = if(value.isEmpty) None else {
    val DateRe = """(\d{1,2})\.(\d{1,2})\.(\d{4})""".r
    val DateRe(d,m,y) = value
    val date = LocalDate.of(y.toInt,m.toInt,d.toInt)
    Some(Instant.from(ZonedDateTime.of(date,LocalTime.MIN,zoneId)))
  }
}

class LocalTimeValueConverter(
  val valueType: AttrValueType[Option[LocalTime]], inner: RawConverter, val asString: AttrValueType[String]
) extends TimeRawValueConverterImpl[Option[LocalTime]] {
  def convertEmpty()=None
  def convert(valueA: Long, valueB: Long) = {
    if(valueB != 0L) Never()
    Option(LocalTime.ofSecondOfDay(valueA))
  }
  def convert(value: String) = Never()
  def toBytes(preId: ObjId, value: Value, finId: ObjId) =
    if(value.nonEmpty) inner.toBytes(preId, value.get.toSecondOfDay,0L,finId) else Array()
  def toUIString(value: Value) = value.map(v ⇒
    s"${zeroPad2(v.getHour.toString)}:${zeroPad2(v.getMinute.toString)}"
  ).getOrElse("")
  def fromUIString(value: String) = strToPair(value, (h,m)⇒LocalTime.of(h,m))
}

class BigDecimalValueConverter(
                                valueType: AttrValueType[Option[BigDecimal]], inner: RawConverter, asString: AttrValueType[String]
                              ) extends RawValueConverter[Option[BigDecimal]] with CoHandlerProvider  {
  def convertEmpty() = None
  def convert(valueA: Long, valueB: Long) = {
    val scale=8
    Option(BigDecimal(valueB.toLong,scale))
  }
  def convert(value: String) = Never()
  def toBytes(preId: ObjId, value: Option[BigDecimal], finId: ObjId) ={
    if(value.nonEmpty){
      val scale=8
      val bgd=value.get.setScale(scale)
      val unscaledVal=bgd.bigDecimal.unscaledValue()
      val valueA=scale
      val valueB= unscaledVal.longValueExact()
      inner.toBytes(preId,valueA,valueB,finId)
    }
    else Array()
  }
  def toUIString(value: Option[BigDecimal]) = value.map(v ⇒v.bigDecimal.setScale(2,BigDecimal.RoundingMode.HALF_UP).toString()).getOrElse("")
  def fromUIString(value: String):Option[BigDecimal] = if(value.nonEmpty) Some(BigDecimal(value)) else None
  def handlers = List(
    CoHandler(ToRawValueConverter(valueType))(this),
    CoHandler(ToUIStringConverter(valueType,asString))(toUIString),
    CoHandler(ToUIStringConverter(asString,valueType))(fromUIString)
  )
}