package wowchat.game

import wowchat.common.{WowChatConfig, WowExpansion}
import io.netty.util.AttributeKey

trait GamePackets {

  val CRYPT: AttributeKey[GameHeaderCrypt] = AttributeKey.valueOf("CRYPT")

  val CMSG_CHAR_ENUM = 0x37
  val SMSG_CHAR_ENUM = 0x3B
  val CMSG_PLAYER_LOGIN = 0x3D
  val CMSG_LOGOUT_REQUEST = 0x4B
  val CMSG_NAME_QUERY = 0x50
  val SMSG_NAME_QUERY = 0x51
  val CMSG_GUILD_QUERY = 0x54
  val SMSG_GUILD_QUERY = 0x55
  val CMSG_WHO = 0x62
  val SMSG_WHO = 0x63
  val CMSG_GUILD_ROSTER = 0x89
  val SMSG_GUILD_ROSTER = 0x8A
  val SMSG_GUILD_EVENT = 0x92
  val CMSG_MESSAGECHAT = 0x95
  val SMSG_MESSAGECHAT = 0x96
  val CMSG_JOIN_CHANNEL = 0x97
  val SMSG_CHANNEL_NOTIFY = 0x99

  val SMSG_NOTIFICATION = 0x01CB
  val CMSG_PING = 0x01DC
  val SMSG_AUTH_CHALLENGE = 0x01EC
  val CMSG_AUTH_CHALLENGE = 0x01ED
  val SMSG_AUTH_RESPONSE = 0x01EE
  val SMSG_LOGIN_VERIFY_WORLD = 0x0236
  val SMSG_SERVER_MESSAGE = 0x0291

  val SMSG_WARDEN_DATA = 0x02E6
  val CMSG_WARDEN_DATA = 0x02E7

  // tbc/wotlk only
  val SMSG_TIME_SYNC_REQ = 0x0390
  val CMSG_TIME_SYNC_RESP = 0x0391

  object ChatEvents {
    // err...
    lazy val CHAT_MSG_SAY = if (WowChatConfig.getExpansion == WowExpansion.Vanilla) 0x00.toByte else 0x01.toByte
    lazy val CHAT_MSG_GUILD = if (WowChatConfig.getExpansion == WowExpansion.Vanilla) 0x03.toByte else 0x04.toByte
    lazy val CHAT_MSG_OFFICER = if (WowChatConfig.getExpansion == WowExpansion.Vanilla) 0x04.toByte else 0x05.toByte
    lazy val CHAT_MSG_YELL = if (WowChatConfig.getExpansion == WowExpansion.Vanilla) 0x05.toByte else 0x06.toByte
    lazy val CHAT_MSG_WHISPER = if (WowChatConfig.getExpansion == WowExpansion.Vanilla) 0x06.toByte else 0x07.toByte
    lazy val CHAT_MSG_EMOTE = if (WowChatConfig.getExpansion == WowExpansion.Vanilla) 0x08.toByte else 0x0A.toByte
    lazy val CHAT_MSG_TEXT_EMOTE = if (WowChatConfig.getExpansion == WowExpansion.Vanilla) 0x09.toByte else 0x0B.toByte
    lazy val CHAT_MSG_CHANNEL = if (WowChatConfig.getExpansion == WowExpansion.Vanilla) 0x0E.toByte else 0x11.toByte
    lazy val CHAT_MSG_SYSTEM = if (WowChatConfig.getExpansion == WowExpansion.Vanilla) 0x0A.toByte else 0x00.toByte
    lazy val CHAT_MSG_CHANNEL_JOIN = if (WowChatConfig.getExpansion == WowExpansion.Vanilla) 0x0F.toByte else 0x12.toByte
    lazy val CHAT_MSG_CHANNEL_LEAVE = if (WowChatConfig.getExpansion == WowExpansion.Vanilla) 0x10.toByte else 0x13.toByte
    lazy val CHAT_MSG_CHANNEL_LIST = if (WowChatConfig.getExpansion == WowExpansion.Vanilla) 0x11.toByte else 0x14.toByte
    lazy val CHAT_MSG_CHANNEL_NOTICE = if (WowChatConfig.getExpansion == WowExpansion.Vanilla) 0x12.toByte else 0x15.toByte
    lazy val CHAT_MSG_CHANNEL_NOTICE_USER = if (WowChatConfig.getExpansion == WowExpansion.Vanilla) 0x13.toByte else 0x16.toByte

    lazy val CHAT_MSG_ACHIEVEMENT = if (WowChatConfig.getExpansion == WowExpansion.MoP) 0x2E.toByte else 0x30.toByte
    lazy val CHAT_MSG_GUILD_ACHIEVEMENT = if (WowChatConfig.getExpansion == WowExpansion.MoP) 0x2F.toByte else 0x31.toByte

    def parse(tp: String): Byte = {
      (tp.toLowerCase match {
        case "system" => CHAT_MSG_SYSTEM
        case "say" => CHAT_MSG_SAY
        case "guild" => CHAT_MSG_GUILD
        case "officer" => CHAT_MSG_OFFICER
        case "yell" => CHAT_MSG_YELL
        case "emote" => CHAT_MSG_EMOTE
        case "whisper" => CHAT_MSG_WHISPER
        case "channel" | "custom" => CHAT_MSG_CHANNEL
        case _ => -1
      }).toByte
    }

    def valueOf(tp: Byte): String = {
      tp match {
        case CHAT_MSG_SAY => "Say"
        case CHAT_MSG_GUILD => "Guild"
        case CHAT_MSG_OFFICER => "Officer"
        case CHAT_MSG_YELL => "Yell"
        case CHAT_MSG_WHISPER => "Whisper"
        case CHAT_MSG_EMOTE | CHAT_MSG_TEXT_EMOTE => "Emote"
        case CHAT_MSG_CHANNEL => "Channel"
        case CHAT_MSG_SYSTEM => "System"
        case _ => "Unknown"
      }
    }
  }

  object GuildEvents {
    // quite a nice hack because MoP doesn't use these events directly. has separate packet for each.
    val GE_PROMOTED = if (WowChatConfig.getExpansion == WowExpansion.Cataclysm) 0x01 else 0x00
    val GE_DEMOTED = if (WowChatConfig.getExpansion == WowExpansion.Cataclysm) 0x02 else 0x01
    val GE_MOTD = if (WowChatConfig.getExpansion == WowExpansion.Cataclysm) 0x03 else 0x02
    val GE_JOINED = if (WowChatConfig.getExpansion == WowExpansion.Cataclysm) 0x04 else 0x03
    val GE_LEFT = if (WowChatConfig.getExpansion == WowExpansion.Cataclysm) 0x05 else 0x04
    val GE_REMOVED = if (WowChatConfig.getExpansion == WowExpansion.Cataclysm) 0x06 else 0x05
    val GE_SIGNED_ON = if (WowChatConfig.getExpansion == WowExpansion.Cataclysm) 0x10 else 0x0C
    val GE_SIGNED_OFF = if (WowChatConfig.getExpansion == WowExpansion.Cataclysm) 0x11 else 0x0D
  }

  object Races {
    val RACE_HUMAN = 0x01
    val RACE_ORC = 0x02
    val RACE_DWARF = 0x03
    val RACE_NIGHTELF = 0x04
    val RACE_UNDEAD = 0x05
    val RACE_TAUREN = 0x06
    val RACE_GNOME = 0x07
    val RACE_TROLL = 0x08
    val RACE_GOBLIN = 0x09
    val RACE_BLOODELF = 0x0A
    val RACE_DRAENEI = 0x0B
    val RACE_WORGEN = 0x16
    val RACE_PANDAREN_NEUTRAL = 0x18
    val RACE_PANDAREN_ALLIANCE = 0x19
    val RACE_PANDAREN_HORDE = 0x1A

    def getLanguage(race: Byte): Byte = {
      race match {
        case RACE_ORC | RACE_UNDEAD | RACE_TAUREN | RACE_TROLL | RACE_BLOODELF | RACE_GOBLIN | RACE_PANDAREN_HORDE => 0x01 // orcish
        case RACE_PANDAREN_NEUTRAL => 0x2A.toByte // pandaren neutral?
        case _ => 0x07 // common
      }
    }

    def valueOf(charClass: Byte): String = {
      charClass match {
        case RACE_HUMAN => "Human"
        case RACE_ORC => "Orc"
        case RACE_DWARF => "Dwarf"
        case RACE_NIGHTELF => "Night Elf"
        case RACE_UNDEAD => "Undead"
        case RACE_TAUREN => "Tauren"
        case RACE_GNOME => "Gnome"
        case RACE_TROLL => "Troll"
        case RACE_GOBLIN => "Goblin"
        case RACE_BLOODELF => "Blood Elf"
        case RACE_DRAENEI => "Draenei"
        case RACE_WORGEN => "Worgen"
        case RACE_PANDAREN_NEUTRAL => "Pandaren"
        case RACE_PANDAREN_ALLIANCE => "Alliance Pandaren"
        case RACE_PANDAREN_HORDE => "Horde Pandaren"
        case _ => "Unknown"
      }
    }
  }

  object Classes {
    val CLASS_WARRIOR = 0x01
    val CLASS_PALADIN = 0x02
    val CLASS_HUNTER = 0x03
    val CLASS_ROGUE = 0x04
    val CLASS_PRIEST = 0x05
    val CLASS_DEATH_KNIGHT = 0x06
    val CLASS_SHAMAN = 0x07
    val CLASS_MAGE = 0x08
    val CLASS_WARLOCK = 0x09
    val CLASS_MONK = 0x0A
    val CLASS_DRUID = 0x0B

    def valueOf(charClass: Byte): String = {
      charClass match {
        case CLASS_WARRIOR => "Warrior"
        case CLASS_PALADIN => "Paladin"
        case CLASS_HUNTER => "Hunter"
        case CLASS_ROGUE => "Rogue"
        case CLASS_DEATH_KNIGHT => "Death Knight"
        case CLASS_PRIEST => "Priest"
        case CLASS_SHAMAN => "Shaman"
        case CLASS_MAGE => "Mage"
        case CLASS_WARLOCK => "Warlock"
        case CLASS_MONK => "Monk"
        case CLASS_DRUID => "Druid"
        case _ => "Unknown"
      }
    }
  }

  object Genders {
    val GENDER_MALE = 0
    val GENDER_FEMALE = 1
    val GENDER_NONE = 2

    def valueOf(gender: Byte): String = {
      gender match {
        case GENDER_MALE => "Male"
        case GENDER_FEMALE => "Female"
        case _ => "Unknown"
      }
    }
  }

  object AuthResponseCodes {
    val AUTH_OK = 0x0C
    val AUTH_FAILED = 0x0D
    val AUTH_REJECT = 0x0E
    val AUTH_BAD_SERVER_PROOF = 0x0F
    val AUTH_UNAVAILABLE = 0x10
    val AUTH_SYSTEM_ERROR = 0x11
    val AUTH_BILLING_ERROR = 0x12
    val AUTH_BILLING_EXPIRED = 0x13
    val AUTH_VERSION_MISMATCH = 0x14
    val AUTH_UNKNOWN_ACCOUNT = 0x15
    val AUTH_INCORRECT_PASSWORD = 0x16
    val AUTH_SESSION_EXPIRED = 0x17
    val AUTH_SERVER_SHUTTING_DOWN = 0x18
    val AUTH_ALREADY_LOGGING_IN = 0x19
    val AUTH_LOGIN_SERVER_NOT_FOUND = 0x1A
    val AUTH_WAIT_QUEUE = 0x1B
    val AUTH_BANNED = 0x1C
    val AUTH_ALREADY_ONLINE = 0x1D
    val AUTH_NO_TIME = 0x1E
    val AUTH_DB_BUSY = 0x1F
    val AUTH_SUSPENDED = 0x20
    val AUTH_PARENTAL_CONTROL = 0x21

    def getMessage(authResult: Int): String = {
      authResult match {
        case AUTH_OK => "Success!"
        case AUTH_UNKNOWN_ACCOUNT | AUTH_INCORRECT_PASSWORD => "Incorrect username or password!"
        case AUTH_VERSION_MISMATCH => "Invalid game version for this server!"
        case AUTH_BANNED => "Your account has been banned!"
        case AUTH_ALREADY_LOGGING_IN | AUTH_ALREADY_ONLINE => "Your account is already online! Log it off or wait a minute if already logging off."
        case AUTH_SUSPENDED => "Your account has been suspended!"
        case x => f"Failed to login! Error code: $x%02X"
      }
    }
  }

  object ChatNotify {
    val CHAT_JOINED_NOTICE = 0x00
    val CHAT_LEFT_NOTICE = 0x01
    val CHAT_YOU_JOINED_NOTICE = 0x02
    val CHAT_YOU_LEFT_NOTICE = 0x03
    val CHAT_WRONG_PASSWORD_NOTICE = 0x04
    val CHAT_NOT_MEMBER_NOTICE = 0x05
    val CHAT_NOT_MODERATOR_NOTICE = 0x06
    val CHAT_PASSWORD_CHANGED_NOTICE = 0x07
    val CHAT_OWNER_CHANGED_NOTICE = 0x08
    val CHAT_PLAYER_NOT_FOUND_NOTICE = 0x09
    val CHAT_NOT_OWNER_NOTICE = 0x0A
    val CHAT_CHANNEL_OWNER_NOTICE = 0x0B
    val CHAT_MODE_CHANGE_NOTICE = 0x0C
    val CHAT_ANNOUNCEMENTS_ON_NOTICE = 0x0D
    val CHAT_ANNOUNCEMENTS_OFF_NOTICE = 0x0E
    val CHAT_MODERATION_ON_NOTICE = 0x0F
    val CHAT_MODERATION_OFF_NOTICE = 0x10
    val CHAT_MUTED_NOTICE = 0x11
    val CHAT_PLAYER_KICKED_NOTICE = 0x12
    val CHAT_BANNED_NOTICE = 0x13
    val CHAT_PLAYER_BANNED_NOTICE = 0x14
    val CHAT_PLAYER_UNBANNED_NOTICE = 0x15
    val CHAT_PLAYER_NOT_BANNED_NOTICE = 0x16
    val CHAT_PLAYER_ALREADY_MEMBER_NOTICE = 0x17
    val CHAT_INVITE_NOTICE = 0x18
    val CHAT_INVITE_WRONG_FACTION_NOTICE = 0x19
    val CHAT_WRONG_FACTION_NOTICE = 0x1A
    val CHAT_INVALID_NAME_NOTICE = 0x1B
    val CHAT_NOT_MODERATED_NOTICE = 0x1C
    val CHAT_PLAYER_INVITED_NOTICE = 0x1D
    val CHAT_PLAYER_INVITE_BANNED_NOTICE = 0x1E
    val CHAT_THROTTLED_NOTICE = 0x1F
    val CHAT_NOT_IN_AREA_NOTICE = 0x20
    val CHAT_NOT_IN_LFG_NOTICE = 0x21
    val CHAT_VOICE_ON_NOTICE = 0x22
    val CHAT_VOICE_OFF_NOTICE = 0x23
  }

  object ServerMessageType {
    val SERVER_MSG_SHUTDOWN_TIME = 0x01
    val SERVER_MSG_RESTART_TIME = 0x02
    val SERVER_MSG_CUSTOM = 0x03
    val SERVER_MSG_SHUTDOWN_CANCELLED = 0x04
    val SERVER_MSG_RESTART_CANCELLED = 0x05
  }
}
