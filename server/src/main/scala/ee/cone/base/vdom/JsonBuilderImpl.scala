package ee.cone.base.vdom

object JsonToString {
  def apply(value: Value): String = {
    val builder = new JsonBuilderImpl()
    value.appendJson(builder)
    builder.result.toString
  }
}

private object JsonBuilderImpl {
  val oddElementCount = 0x1L
  val nonEmpty        = 0x2L
  val isObject        = 0x4L
  val stateSize       = 3
  val maxStateCount   = 21 // 21*3=63
}

class JsonBuilderImpl(val result: StringBuilder = new StringBuilder) extends JsonBuilder {
  import JsonBuilderImpl._
  private var stateStack: Long = 0L
  private var stateCount = 1
  private def is(flag: Long) = (stateStack & flag) != 0L
  private def objectNeedsValue = is(isObject) && is(oddElementCount)

  private def push(flags: Long): Unit = {
    stateCount += 1
    if(stateCount > maxStateCount) new Exception("maxDepth")
    stateStack = (stateStack << stateSize) | flags
  }
  private def pop(): Unit = {
    stateCount -= 1
    if(stateCount < 0) new Exception("minDepth")
    stateStack = stateStack >>> stateSize
  }

  private def startElement(): Unit =
    if(is(nonEmpty)) result.append(if(objectNeedsValue) ':' else ',')
  private def endElement(): Unit = {
    stateStack = (stateStack | nonEmpty) ^ oddElementCount
  }

  private def start(flags: Long, c: Char): JsonBuilder = {
    startElement()
    push(flags)
    result.append(c)
    this
  }
  def startArray() = start(0L, '[')
  def startObject() = start(isObject, '{')
  def end() = {
    if(objectNeedsValue) throw new Exception("objectNeedsValue")
    result.append(if(is(isObject)) '}' else ']')
    pop()
    endElement()
    if(objectNeedsValue) throw new Exception("objectNeedsKey")
    this
  }
  def append(value: String) = {
    startElement()
    result.append('"')
    var j = 0
    while(j < value.length){
      val c = value(j)
      if(c == '\\' || c == '"' ||  c < '\u0020')
        result.append(if(c < '\u0010')"\\u000" else "\\u00").append(Integer.toHexString(c))
      else
        result.append(c)
      j += 1
    }
    result.append('"')
    endElement()
    this
  }
}
