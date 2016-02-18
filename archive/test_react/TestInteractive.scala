package ee.cone.base.test_react

class FieldModel extends Model with StrProp {
  private var _value: String = ""
  private var _version: Int = 0
  def version = s"${synchronized(_version)}"
  def get: String = synchronized(_value)
  def set(value: String): Unit = synchronized{ _value = value; _version += 1 }
}

class InteractiveView(models: List[Model]) extends View {
  def modelVersion = model.version
  lazy val model = models.collectFirst{ case m: FieldModel => m }.get
  def generateDom = {
    import Tag._
    root(
      inputText(0, "send on change", model, deferSend=false) ::
        inputText(1, "send on blur", model, deferSend=true) ::
        resetButton(2,model) :: Nil
    )
  }
}
