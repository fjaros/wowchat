package wowchat.game

trait GamePacketsCataclysm15595 extends GamePacketsWotLK {

  override val CMSG_CHAR_ENUM = 0x0502
  override val SMSG_CHAR_ENUM = 0x10B0
  override val CMSG_PLAYER_LOGIN = 0x05B1
  override val CMSG_LOGOUT_REQUEST = 0x0A25
  override val CMSG_NAME_QUERY = 0x2224
  override val SMSG_NAME_QUERY = 0x6E04
  override val CMSG_WHO = 0x6C15
  override val SMSG_WHO = 0x6907
  override val CMSG_GUILD_QUERY = 0x4426
  override val SMSG_GUILD_QUERY = 0x0E06
  override val CMSG_GUILD_ROSTER = 0x1226
  override val SMSG_GUILD_ROSTER = 0x3DA3
  override val SMSG_GUILD_EVENT = 0x0705
  override val SMSG_MESSAGECHAT = 0x2026
  override val SMSG_GM_MESSAGECHAT = 0x13B4
  override val CMSG_JOIN_CHANNEL = 0x0156
  override val SMSG_CHANNEL_NOTIFY = 0x0825

  override val SMSG_NOTIFICATION = 0x14A0
  override val CMSG_PING = 0x444D
  override val SMSG_AUTH_CHALLENGE = 0x4542
  override val CMSG_AUTH_CHALLENGE = 0x0449
  override val SMSG_AUTH_RESPONSE = 0x5DB6
  override val SMSG_LOGIN_VERIFY_WORLD = 0x2005

  override val SMSG_WARDEN_DATA = 0x12E7
  override val CMSG_WARDEN_DATA = 0x12E8

  override val SMSG_TIME_SYNC_REQ = 0x3CA4
  override val CMSG_TIME_SYNC_RESP = 0x3B0C

  val WOW_CONNECTION = 0x4F57 // same hack as in mangos :D

  val CMSG_MESSAGECHAT_AFK = 0x0D44
  val CMSG_MESSAGECHAT_BATTLEGROUND = 0x2156
  val CMSG_MESSAGECHAT_CHANNEL = 0x1D44
  val CMSG_MESSAGECHAT_DND = 0x2946
  val CMSG_MESSAGECHAT_EMOTE = 0x1156
  val CMSG_MESSAGECHAT_GUILD = 0x3956
  val CMSG_MESSAGECHAT_OFFICER = 0x1946
  val CMSG_MESSAGECHAT_PARTY = 0x1D46
  val CMSG_MESSAGECHAT_SAY = 0x1154
  val CMSG_MESSAGECHAT_WHISPER = 0x0D56
  val CMSG_MESSAGECHAT_YELL = 0x3544

  final val COMPRESSED_DATA_MASK = 0x8000
}
