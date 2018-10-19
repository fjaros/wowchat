package wowchat.game

trait GamePacketsMoP extends GamePacketsCataclysm {

  override val CMSG_MESSAGECHAT_AFK = 0x0EAB
  override val CMSG_MESSAGECHAT_CHANNEL = 0x00BB
  override val CMSG_MESSAGECHAT_DND = 0x002E
  override val CMSG_MESSAGECHAT_EMOTE = 0x103E
  override val CMSG_MESSAGECHAT_GUILD = 0x0CAE
  override val CMSG_MESSAGECHAT_OFFICER = 0x0ABF
  override val CMSG_MESSAGECHAT_PARTY = 0x109A
  override val CMSG_MESSAGECHAT_SAY = 0x0A9A
  override val CMSG_MESSAGECHAT_WHISPER = 0x123E
  override val CMSG_MESSAGECHAT_YELL = 0x04AA

  override val CMSG_CHAR_ENUM = 0x00E0
  override val SMSG_CHAR_ENUM = 0x11C3
  override val CMSG_PLAYER_LOGIN = 0x158F
  override val CMSG_LOGOUT_REQUEST = 0x1349
  override val CMSG_NAME_QUERY = 0x0328
  override val SMSG_NAME_QUERY = 0x169B
  override val CMSG_WHO = 0x18A3
  override val SMSG_WHO = 0x161B
  override val CMSG_GUILD_ROSTER = 0x1459
  override val SMSG_GUILD_ROSTER = 0x0BE0
  override val SMSG_GUILD_EVENT = 0x0705 // needs to be implemented in separate packets. obsolete in mop
  override val SMSG_CHATMESSAGE = 0x1A9A
  override val CMSG_JOIN_CHANNEL = 0x148E
  override val SMSG_CHANNEL_NOTIFY = 0x0F06

  override val SMSG_NOTIFICATION = 0x0C2A
  override val CMSG_PING = 0x0012
  override val SMSG_AUTH_CHALLENGE = 0x0949
  override val CMSG_AUTH_CHALLENGE = 0x00B2
  override val SMSG_AUTH_RESPONSE = 0x0ABA
  override val SMSG_LOGIN_VERIFY_WORLD = 0x1C0F

  override val SMSG_WARDEN_DATA = 0x0C0A
  override val CMSG_WARDEN_DATA = 0x1816

  override val SMSG_TIME_SYNC_REQ = 0x1A8F
  override val CMSG_TIME_SYNC_RESP = 0x01DB
}
