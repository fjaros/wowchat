package wowchat.game

import wowchat.common.{WowChatConfig, WowExpansion}

trait GamePacketsCataclysm extends GamePackets {

  // i am actually pretty sure this just uses same bit obfuscating as some of the other variables like player guid
  // but mangos hardcodes the values.

  override val CMSG_CHAR_ENUM = 0x0502
  override val SMSG_CHAR_ENUM = 0x10B0
//  val CMSG_PLAYER_LOGIN = 0x3D
//  val CMSG_LOGOUT_REQUEST = 0x4B
//  val CMSG_NAME_QUERY = 0x50
//  val SMSG_NAME_QUERY = 0x51
//  val CMSG_WHO = 0x62
//  val SMSG_WHO = 0x63
//  val CMSG_GUILD_ROSTER = 0x89
//  val SMSG_GUILD_ROSTER = 0x8A
//  val SMSG_GUILD_EVENT = 0x92
//  val CMSG_CHATMESSAGE = 0x95
//  val SMSG_CHATMESSAGE = 0x96
//  val CMSG_JOIN_CHANNEL = 0x97
//  val SMSG_CHANNEL_NOTIFY = 0x99
//
//  val SMSG_NOTIFICATION = 0x01CB
//  val CMSG_PING = 0x01DC
  override val SMSG_AUTH_CHALLENGE = 0x4542
  override val CMSG_AUTH_CHALLENGE = 0x0449
  override val SMSG_AUTH_RESPONSE = 0x5DB6
//  val SMSG_LOGIN_VERIFY_WORLD = 0x0236

//  val SMSG_WARDEN_DATA = 0x02E6
//  val CMSG_WARDEN_DATA = 0x02E7

  // tbc/wotlk only
//  val SMSG_TIME_SYNC_REQ = 0x0390
//  val CMSG_TIME_SYNC_RESP = 0x0391

  // cataclysm
  // val WOW_CONNECTION = 0x4F57 // same hack as in mangos :D
}
