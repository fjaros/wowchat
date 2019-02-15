package wowchat.game

import java.lang.management.ManagementFactory
import java.nio.charset.Charset

import io.netty.buffer.{ByteBuf, PooledByteBufAllocator}
import wowchat.common.{CommonConnectionCallback, Global, Packet}

class GamePacketHandlerTBC(realmId: Int, realmName: String, sessionKey: Array[Byte], gameEventCallback: CommonConnectionCallback)
  extends GamePacketHandler(realmId, realmName, sessionKey, gameEventCallback) with GamePacketsTBC {

  override protected val addonInfo: Array[Byte] = Array(
    0xD0, 0x01, 0x00, 0x00, 0x78, 0x9C, 0x75, 0xCF, 0x3B, 0x0E, 0xC2, 0x30, 0x0C, 0x80, 0xE1, 0x72,
    0x0F, 0x2E, 0x43, 0x18, 0x50, 0xA5, 0x66, 0xA1, 0x65, 0x46, 0x26, 0x71, 0x2B, 0xAB, 0x89, 0x53,
    0x19, 0x87, 0x47, 0x4F, 0x0F, 0x0B, 0x62, 0x71, 0xBD, 0x7E, 0xD6, 0x6F, 0xD9, 0x25, 0x5A, 0x57,
    0x90, 0x78, 0x3D, 0xD4, 0xA0, 0x54, 0xF8, 0xD2, 0x36, 0xBB, 0xFC, 0xDC, 0x77, 0xCD, 0x77, 0xDC,
    0xCF, 0x1C, 0xA8, 0x26, 0x1C, 0x09, 0x53, 0xF4, 0xC4, 0x94, 0x61, 0xB1, 0x96, 0x88, 0x23, 0xF1,
    0x64, 0x06, 0x8E, 0x25, 0xDF, 0x40, 0xBB, 0x32, 0x6D, 0xDA, 0x80, 0x2F, 0xB5, 0x50, 0x60, 0x54,
    0x33, 0x79, 0xF2, 0x7D, 0x95, 0x07, 0xBE, 0x6D, 0xAC, 0x94, 0xA2, 0x03, 0x9E, 0x4D, 0x6D, 0xF9,
    0xBE, 0x60, 0xB0, 0xB3, 0xAD, 0x62, 0xEE, 0x4B, 0x98, 0x51, 0xB7, 0x7E, 0xF1, 0x10, 0xA4, 0x98,
    0x72, 0x06, 0x8A, 0x26, 0x0C, 0x90, 0x90, 0xED, 0x7B, 0x83, 0x40, 0xC4, 0x7E, 0xA6, 0x94, 0xB6,
    0x98, 0x18, 0xC5, 0x36, 0xCA, 0xE8, 0x81, 0x61, 0x42, 0xF9, 0xEB, 0x07, 0x63, 0xAB, 0x8B, 0xEC
  ).map(_.toByte)

  override protected def channelParse(msg: Packet): Unit = {
    msg.id match {
      case SMSG_GM_MESSAGECHAT => handle_SMSG_MESSAGECHAT(msg)
      case SMSG_MOTD => handle_SMSG_MOTD(msg)
      case SMSG_TIME_SYNC_REQ => handle_SMSG_TIME_SYNC_REQ(msg)
      case _ => super.channelParse(msg)
    }
  }

  override protected def parseCharEnum(msg: Packet): Option[CharEnumMessage] = {
    val charactersNum = msg.byteBuf.readByte

    // only care about guid and name here
    (0 until charactersNum).foreach(i => {
      val guid = msg.byteBuf.readLongLE
      val name = msg.readString
      val race = msg.byteBuf.readByte // will determine what language to use in chat

      msg.byteBuf.skipBytes(1) // class
      msg.byteBuf.skipBytes(1) // gender
      msg.byteBuf.skipBytes(1) // skin
      msg.byteBuf.skipBytes(1) // face
      msg.byteBuf.skipBytes(1) // hair style
      msg.byteBuf.skipBytes(1) // hair color
      msg.byteBuf.skipBytes(1) // facial hair
      msg.byteBuf.skipBytes(1) // level
      msg.byteBuf.skipBytes(4) // zone
      msg.byteBuf.skipBytes(4) // map - could be useful in the future to determine what city specific channels to join

      msg.byteBuf.skipBytes(12) // x + y + z

      val guildGuid = msg.byteBuf.readIntLE
      if (name.equalsIgnoreCase(Global.config.wow.character)) {
        return Some(CharEnumMessage(name, guid, race, guildGuid))
      }

      msg.byteBuf.skipBytes(4) // character flags
      msg.byteBuf.skipBytes(1) // first login
      msg.byteBuf.skipBytes(12) // pet info
      msg.byteBuf.skipBytes(19 * 9) // equipment info TBC has 9 slot equipment info
      msg.byteBuf.skipBytes(9) // first bag display info TBC has 9 slot equipment info
    })
    None
  }

  override protected def parseChatMessage(msg: Packet): Option[ChatMessage] = {
    val tp = msg.byteBuf.readByte

    val lang = msg.byteBuf.readIntLE
    // ignore addon messages
    if (lang == -1) {
      return None
    }

    // ignore messages from itself
    val guid = msg.byteBuf.readLongLE
    if (guid == selfCharacterId.get) {
      return None
    }

    msg.byteBuf.skipBytes(4)

    val channelName = if (tp == ChatEvents.CHAT_MSG_CHANNEL) {
      Some(msg.readString)
    } else {
      None
    }

    // ignore if from an unhandled channel
    if (!Global.wowToDiscord.contains((tp, channelName.map(_.toLowerCase)))) {
      return None
    }

    msg.byteBuf.skipBytes(8) // skip guid again

    val txtLen = msg.byteBuf.readIntLE
    val txt = msg.byteBuf.readCharSequence(txtLen - 1, Charset.forName("UTF-8")).toString
    msg.byteBuf.skipBytes(1) // null terminator

    Some(ChatMessage(guid, tp, txt, channelName))
  }

  private def handle_SMSG_MOTD(msg: Packet): Unit = {
    parseServerMotd(msg).foreach(sendChatMessage)
  }

  protected def parseServerMotd(msg: Packet): Seq[ChatMessage] = {
    val lineCount = msg.byteBuf.readIntLE
    (0 until lineCount).map(i => {
      val message = msg.readString
      ChatMessage(0, ChatEvents.CHAT_MSG_SYSTEM, message, None)
    })
  }

  override protected def parseGuildRoster(msg: Packet): Map[Long, GuildMember] = {
    val count = msg.byteBuf.readIntLE
    val motd = msg.readString
    val ginfo = msg.readString
    val rankscount = msg.byteBuf.readIntLE
    (0 until rankscount).foreach(i => {
      msg.byteBuf.skipBytes(8 + 48) // rank info + guild bank
    })
    (0 until count).map(i => {
      val guid = msg.byteBuf.readLongLE
      val isOnline = msg.byteBuf.readBoolean
      val name = msg.readString
      msg.byteBuf.skipBytes(4) // guild rank
      val level = msg.byteBuf.readByte
      val charClass = msg.byteBuf.readByte
      msg.byteBuf.skipBytes(1) // tbc unkn
      val zoneId = msg.byteBuf.readIntLE
      val lastLogoff = if (!isOnline) {
        msg.byteBuf.readFloatLE
      } else {
        0
      }
      msg.skipString
      msg.skipString

      guid -> GuildMember(name, isOnline, charClass, level, zoneId, lastLogoff)
    }).toMap
  }

  override protected def writeJoinChannel(out: ByteBuf, utf8ChannelBytes: Array[Byte]): Unit = {
    out.writeIntLE(0)
    out.writeByte(0)
    out.writeByte(1)
    super.writeJoinChannel(out, utf8ChannelBytes)
  }

  private def handle_SMSG_TIME_SYNC_REQ(msg: Packet): Unit = {
    // jvm uptime should work for this?
    val jvmUptime = ManagementFactory.getRuntimeMXBean.getUptime

    val counter = msg.byteBuf.readIntLE

    val byteBuf = PooledByteBufAllocator.DEFAULT.buffer(8, 8)
    byteBuf.writeIntLE(counter)
    byteBuf.writeIntLE(jvmUptime.toInt)

    ctx.get.writeAndFlush(Packet(CMSG_TIME_SYNC_RESP, byteBuf))
  }
}
