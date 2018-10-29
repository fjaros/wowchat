package wowchat.game

class GamePacketEncoderCataclysm extends GamePacketEncoder with GamePacketsCataclysm15595 {

  override protected def isUnencryptedPacket(id: Int): Boolean = {
    super.isUnencryptedPacket(id) || id == WOW_CONNECTION
  }
}
