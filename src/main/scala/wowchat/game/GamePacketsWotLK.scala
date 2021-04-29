package wowchat.game

trait GamePacketsWotLK extends GamePacketsTBC {

  override val SMSG_GM_MESSAGECHAT = 0x03B3
  override val CMSG_KEEP_ALIVE = 0x0407
}
