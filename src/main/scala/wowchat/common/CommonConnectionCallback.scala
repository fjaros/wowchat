package wowchat.common

trait CommonConnectionCallback {

  def connect: Unit = {}
  def connected: Unit = {}
  def reconnected: Unit = {}
  def disconnected: Unit = {}
  def error: Unit = {}
}
