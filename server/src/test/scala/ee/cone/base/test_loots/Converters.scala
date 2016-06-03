package ee.cone.base.test_loots

import java.time._
import java.time.format.DateTimeFormatter

import ee.cone.base.connection_api.{CoHandler, CoHandlerProvider}
import ee.cone.base.db._
import ee.cone.base.util.Never



abstract class RawValueConverterImpl[IValue] extends RawValueConverter[IValue] with CoHandlerProvider {
  type Value = IValue
  def valueType: AttrValueType[Value]
  def toUIString(value: Value): String
  def handlers = List(
    CoHandler(ToRawValueConverter(valueType))(this),
    CoHandler(ToUIStringConverter(valueType))(toUIString)
  )
  protected def zeroPad2(x: String) = x.length match {
    case 0 ⇒ "00"
    case 1 ⇒ s"0$x"
    case _ ⇒ x
  }
}

class DurationValueConverter(
  val valueType: AttrValueType[Option[Duration]], inner: RawConverter
) extends RawValueConverterImpl[Option[Duration]] {
  def convertEmpty() = None
  def convert(valueA: Long, valueB: Long) = Option(Duration.ofSeconds(valueA,valueB))
  def convert(value: String) = Never()
  def toBytes(preId: ObjId, value: Value, finId: ObjId) =
    if(value.nonEmpty) inner.toBytes(preId, value.get.getSeconds, value.get.getNano, finId) else Array()
  def toUIString(value: Value) = value.map(x =>
    s"${zeroPad2(x.abs.toHours.toString)}:${zeroPad2(x.abs.minusHours(x.abs.toHours).toMinutes.toString)}"
  ).getOrElse("")
}

class InstantValueConverter(
  val valueType: AttrValueType[Option[Instant]], inner: RawConverter
) extends RawValueConverterImpl[Option[Instant]] {
  def convertEmpty() = None
  def convert(valueA: Long, valueB: Long) = Option(Instant.ofEpochSecond(valueA,valueB))
  def convert(value: String) = Never()
  def toBytes(preId: ObjId, value: Value, finId: ObjId) =
    if(value.nonEmpty) inner.toBytes(preId, value.get.getEpochSecond, value.get.getNano, finId) else Array()
  def toUIString(value: Value) = value.map{ v ⇒
    val date = LocalDate.from(v.atZone(ZoneId.of("UTC")))
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    date.format(formatter)
  }.getOrElse("")
}

class LocalTimeValueConverter(
  val valueType: AttrValueType[Option[LocalTime]], inner: RawConverter) extends RawValueConverterImpl[Option[LocalTime]] {
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
}

