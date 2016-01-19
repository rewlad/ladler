package io.github.rewlad.ladler.vdom

import io.github.rewlad.ladler.connection_api.SenderOfConnection

object Child extends ChildPairFactory(MapValueImpl)


/*
class ReactiveVDom(sender: SenderOfConnection) {
  var prevVDom: Value = WasNoValue
  private lazy val diff = new DiffImpl(MapValueImpl)
  def diffAndSend(vDom: Value) = {
    diff.diff(prevVDom, vDom).foreach(d=>sender.send("showDiff", JsonToString(d)))
    prevVDom = vDom
  }
}
*/