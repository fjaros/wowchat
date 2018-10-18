package wowchat.realm

trait RealmConnectionCallback {

  def success(host: String, port: Int, realmName: String, realmId: Int, sessionKey: Array[Byte]): Unit
  def error: Unit
}
