package ee.cone.base.connection_api

trait Message
case class DictMessage(value: Map[String,String]) extends Message
