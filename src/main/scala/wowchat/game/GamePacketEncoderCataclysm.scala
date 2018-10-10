package wowchat.game

class GamePacketEncoderCataclysm extends GamePacketEncoder with GamePacketsCataclysm {

  override protected def isUnencryptedPacket(id: Int): Boolean = {
    id == CMSG_AUTH_CHALLENGE || id == WOW_CONNECTION
  }
}
