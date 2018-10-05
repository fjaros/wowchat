package wowchat.realm

trait RealmConnectionCallback {

  def success(host: String, port: Int, realmId: Int, sessionKey: Array[Byte]): Unit
  def error: Unit
}
