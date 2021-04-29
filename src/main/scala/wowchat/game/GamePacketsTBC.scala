package wowchat.game

trait GamePacketsTBC extends GamePackets {

  val SMSG_GM_MESSAGECHAT = 0x03B2
  val SMSG_MOTD = 0x033D
  val CMSG_KEEP_ALIVE = 0x0406
}
