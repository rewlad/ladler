package ee.cone.base.server

class Starter(components: List[AppComponent]) {
  def apply() = components.collect{ case c: CanStart => c.start() }
}


