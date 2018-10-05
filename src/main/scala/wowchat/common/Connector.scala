package wowchat.common

trait Connector {

  def isConnected: Boolean
  def connect: Unit
  def disconnect: Unit
}
