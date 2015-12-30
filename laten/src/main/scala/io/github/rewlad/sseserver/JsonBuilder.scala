package io.github.rewlad.sseserver

trait JsonBuilder {
  def startArray(): Unit
  def startObject(): Unit
  def end(): Unit
  def append(value: String): Unit
}
