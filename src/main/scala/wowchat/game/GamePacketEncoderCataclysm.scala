package wowchat.game

class GamePacketEncoderCataclysm extends GamePacketEncoder with GamePacketsCataclysm {

  override protected def isUnencryptedPacket(id: Int): Boolean = {
    super.isUnencryptedPacket(id) || id == WOW_CONNECTION
  }
}
