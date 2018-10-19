package wowchat.game

import java.nio.charset.Charset
import java.security.MessageDigest

import io.netty.buffer.{ByteBuf, PooledByteBufAllocator}
import wowchat.common._

import scala.util.Random

class GamePacketHandlerMoP18414(realmId: Int, sessionKey: Array[Byte], gameEventCallback: CommonConnectionCallback)
  extends GamePacketHandlerCataclysm(realmId, sessionKey, gameEventCallback) with GamePacketsMoP {

  override protected val addonInfo: Array[Byte] = Array(
    0x30, 0x05, 0x00, 0x00, 0x78, 0x9C, 0x75, 0x93, 0x61, 0x6E, 0x83, 0x30, 0x0C, 0x85, 0xD9, 0x3D,
    0x76, 0x84, 0x5D, 0xA2, 0xED, 0x56, 0xD4, 0xA9, 0x48, 0xAC, 0xD0, 0xFE, 0x9D, 0x4C, 0xE2, 0x82,
    0x45, 0x88, 0x91, 0x09, 0x6C, 0xED, 0xE5, 0xB7, 0xA0, 0x49, 0xD3, 0x26, 0x39, 0xFC, 0xE4, 0x7B,
    0x76, 0xE2, 0x97, 0xE7, 0xA7, 0x2C, 0xCB, 0xB6, 0x8E, 0xEE, 0x77, 0x10, 0xFB, 0xBE, 0x31, 0x1D,
    0xE1, 0x82, 0x03, 0xFA, 0x70, 0x3E, 0x64, 0x0F, 0xC3, 0xC7, 0xE3, 0x31, 0xFB, 0xC7, 0xC5, 0x74,
    0x80, 0xEC, 0xB8, 0xBD, 0x25, 0x38, 0x7A, 0xD0, 0xC9, 0x6C, 0x02, 0xB1, 0x57, 0xD9, 0x16, 0xA4,
    0x41, 0x99, 0x3A, 0x1E, 0x13, 0x38, 0x04, 0x87, 0x57, 0x42, 0x67, 0x0B, 0xF2, 0x34, 0xC0, 0xA8,
    0x89, 0xC8, 0x5B, 0xF2, 0xAD, 0xDE, 0xC0, 0x81, 0xE9, 0x0B, 0x90, 0x1E, 0xF5, 0xA9, 0x76, 0xE0,
    0xD0, 0x5B, 0x10, 0x0D, 0x75, 0xE0, 0x22, 0x6C, 0x71, 0xD2, 0x2B, 0x1D, 0x45, 0xAB, 0x2A, 0x58,
    0xD0, 0x5E, 0x40, 0x08, 0x1A, 0x87, 0x93, 0x26, 0xE3, 0xA1, 0x81, 0x70, 0xE4, 0x36, 0xC9, 0x6A,
    0xFC, 0x0C, 0x3A, 0x1C, 0xC1, 0x84, 0x13, 0x90, 0xDD, 0x0B, 0x0C, 0x7A, 0xF3, 0xF3, 0xBE, 0x14,
    0xBE, 0x92, 0x7E, 0xF4, 0x33, 0x36, 0x73, 0x5B, 0x33, 0x3B, 0x0D, 0xBE, 0x78, 0xC3, 0xB3, 0x0F,
    0x28, 0xAF, 0x3C, 0x8B, 0x07, 0xA7, 0x48, 0x72, 0x77, 0x1B, 0x3B, 0x75, 0xF6, 0xBC, 0x88, 0xE6,
    0xE8, 0x86, 0xE6, 0x45, 0x35, 0xCB, 0x82, 0x7A, 0x46, 0xF2, 0x99, 0x9C, 0xDD, 0x82, 0xEF, 0xD3,
    0x74, 0xC7, 0x3E, 0x08, 0xBB, 0xB4, 0x40, 0x25, 0x07, 0x3F, 0x8D, 0x68, 0xF4, 0x1B, 0x1D, 0x02,
    0x0E, 0x1B, 0x17, 0x27, 0x85, 0x64, 0x0A, 0x57, 0x49, 0xC5, 0x26, 0x86, 0x24, 0x95, 0xA3, 0x55,
    0x71, 0x1E, 0x5B, 0x01, 0x8B, 0x2A, 0x3F, 0x32, 0xF7, 0xB1, 0x76, 0xCF, 0x92, 0xBE, 0x64, 0x01,
    0x46, 0x58, 0x27, 0xBC, 0x60, 0x09, 0x56, 0x21, 0x25, 0x86, 0x9F, 0x1D, 0x50, 0xEB, 0x22, 0x4D,
    0x3F, 0xDF, 0x9A, 0x1C, 0xB5, 0xEA, 0x84, 0x57, 0x96, 0x36, 0x35, 0x69, 0xBD, 0x6E, 0x84, 0xEE,
    0x64, 0x4D, 0x03, 0x16, 0xE0, 0xA1, 0x45, 0x6D, 0x5F, 0x6A, 0xEE, 0x51, 0xB7, 0xB7, 0x5E, 0x6D,
    0xAB, 0x7A, 0x72, 0xFA, 0xB3, 0x46, 0x4C, 0x1E, 0x45, 0x65, 0x17, 0x26, 0x5B, 0x05, 0x96, 0x78,
    0xA6, 0xEE, 0xC0, 0xA5, 0x54, 0xFF, 0xBF, 0xCD, 0x38, 0x85, 0x5D, 0xC7, 0x64, 0x50, 0xA1, 0x6B,
    0x47, 0xBD, 0xDF, 0x66, 0x0E, 0xDD, 0xEF, 0xE2, 0xFF, 0x55, 0x7C, 0xC5, 0xEF, 0x1B, 0x7A, 0xEB,
    0x96, 0xC4
  ).map(_.toByte)

  override def sendMessageToWow(tp: Byte, message: String, target: Option[String]): Unit = {
    // channel only todo impl whisper
    ctx.foreach(ctx => {
      val out = PooledByteBufAllocator.DEFAULT.buffer(100, 4096)
      out.writeIntLE(languageId)
      target.fold(logger.info(s"Discord->WoW(${ChatEvents.valueOf(tp)}): $message"))(target => {
        logger.info(s"Discord->WoW($target): $message")
        writeBits(out, target.length, 9)
      })
      writeBits(out, message.length, 8)
      flushBits(out)
      // note for whispers (if the bot ever supports them, the order is opposite, person first then msg
      out.writeBytes(message.getBytes)
      if (target.isDefined) {
        out.writeBytes(target.get.getBytes)
      }
      ctx.writeAndFlush(Packet(getChatPacketFromType(tp), out))
    })
  }

  private def getChatPacketFromType(tp: Byte): Int = {
    tp match {
      case ChatEvents.CHAT_MSG_CHANNEL => CMSG_MESSAGECHAT_CHANNEL
      case ChatEvents.CHAT_MSG_EMOTE => CMSG_MESSAGECHAT_EMOTE
      case ChatEvents.CHAT_MSG_GUILD => CMSG_MESSAGECHAT_GUILD
      case ChatEvents.CHAT_MSG_OFFICER => CMSG_MESSAGECHAT_OFFICER
      case ChatEvents.CHAT_MSG_SAY => CMSG_MESSAGECHAT_SAY
      case ChatEvents.CHAT_MSG_WHISPER => CMSG_MESSAGECHAT_WHISPER
      case ChatEvents.CHAT_MSG_YELL => CMSG_MESSAGECHAT_YELL
    }
  }

  override protected def parseAuthChallenge(msg: Packet): AuthChallengeMessage = {
    val account = Global.config.wow.account.toUpperCase

    msg.byteBuf.skipBytes(35) // MoP - 35 bytes random data
    val serverSeed = msg.byteBuf.readInt
    val clientSeed = Random.nextInt

    val md = MessageDigest.getInstance("SHA1")
    md.update(account.getBytes)
    md.update(Array[Byte](0, 0, 0, 0))
    md.update(ByteUtils.intToBytes(clientSeed))
    md.update(ByteUtils.intToBytes(serverSeed))
    md.update(sessionKey)
    val digest = md.digest

    val out = PooledByteBufAllocator.DEFAULT.buffer(200, 1000)
    out.writeShortLE(0)
    out.writeLongLE(0)
    out.writeByte(digest(18))
    out.writeByte(digest(14))
    out.writeByte(digest(3))
    out.writeByte(digest(4))
    out.writeByte(digest(0))
    out.writeIntLE(1)
    out.writeByte(digest(11))
    out.writeInt(clientSeed)
    out.writeByte(digest(19))
    out.writeShortLE(1)
    out.writeByte(digest(2))
    out.writeByte(digest(9))
    out.writeByte(digest(12))
    out.writeLongLE(0)
    out.writeIntLE(0)
    out.writeByte(digest(16))
    out.writeByte(digest(5))
    out.writeByte(digest(6))
    out.writeByte(digest(8))
    out.writeShortLE(WowChatConfig.getBuild)
    out.writeByte(digest(17))
    out.writeByte(digest(7))
    out.writeByte(digest(13))
    out.writeByte(digest(15))
    out.writeByte(digest(1))
    out.writeByte(digest(10))

    out.writeIntLE(addonInfo.length)
    out.writeBytes(addonInfo)

    writeBit(out, 0)
    writeBits(out, account.length, 11)
    flushBits(out)
    out.writeBytes(account.getBytes)

    AuthChallengeMessage(sessionKey, out)
  }

  override protected def parseAuthResponse(msg: Packet): Byte = {
    (if (msg.readBit == 1) AuthResponseCodes.AUTH_OK else AuthResponseCodes.AUTH_FAILED).toByte
  }

  override protected def parseCharEnum(msg: Packet): Option[CharEnumMessage] = {
    msg.readBits(21) // unkn
    val charactersNum = msg.readBits(16)

    val guids = new Array[Array[Byte]](charactersNum)
    val guildGuids = new Array[Array[Byte]](charactersNum)
    val nameLenghts = new Array[Int](charactersNum)

    (0 until charactersNum).foreach(i => {
      guids(i) = new Array[Byte](8)
      guildGuids(i) = new Array[Byte](8)
      guildGuids(i)(4) = msg.readBit
      guids(i)(0) = msg.readBit
      guildGuids(i)(3) = msg.readBit
      guids(i)(3) = msg.readBit
      guids(i)(7) = msg.readBit
      msg.readBits(2)
      guids(i)(6) = msg.readBit
      guildGuids(i)(6) = msg.readBit
      nameLenghts(i) = msg.readBits(6)
      guids(i)(1) = msg.readBit
      guildGuids(i)(1) = msg.readBit
      guildGuids(i)(0) = msg.readBit
      guids(i)(4) = msg.readBit
      guildGuids(i)(7) = msg.readBit
      guids(i)(2) = msg.readBit
      guids(i)(5) = msg.readBit
      guildGuids(i)(2) = msg.readBit
      guildGuids(i)(5) = msg.readBit
    })

    msg.readBit // packet success flag?

    (0 until charactersNum).foreach(i => {
      msg.byteBuf.skipBytes(4) // unkn
      guids(i)(1) = msg.readXorByte(guids(i)(1))
      msg.byteBuf.skipBytes(2) // slot + hairstyle
      guildGuids(i)(2) = msg.readXorByte(guildGuids(i)(2))
      guildGuids(i)(0) = msg.readXorByte(guildGuids(i)(0))
      guildGuids(i)(6) = msg.readXorByte(guildGuids(i)(6))
      val name = msg.byteBuf.readCharSequence(nameLenghts(i), Charset.defaultCharset).toString
      guildGuids(i)(3) = msg.readXorByte(guildGuids(i)(3))
      msg.byteBuf.skipBytes(10) // x + unkn + face + class
      guildGuids(i)(5) = msg.readXorByte(guildGuids(i)(5))
      msg.byteBuf.skipBytes(207) // inventory
      msg.byteBuf.skipBytes(4) // customization flag
      guids(i)(3) = msg.readXorByte(guids(i)(3))
      guids(i)(5) = msg.readXorByte(guids(i)(5))
      msg.byteBuf.skipBytes(4) // pet family
      guildGuids(i)(4) = msg.readXorByte(guildGuids(i)(4))
      msg.byteBuf.readIntLE // map
      val race = msg.byteBuf.readByte
      msg.byteBuf.skipBytes(1) // skin
      guildGuids(i)(1) = msg.readXorByte(guildGuids(i)(1))
      msg.byteBuf.skipBytes(1) // level
      guids(i)(0) = msg.readXorByte(guids(i)(0))
      guids(i)(2) = msg.readXorByte(guids(i)(2))
      msg.byteBuf.skipBytes(3) // hair color + gender + facial hair
      msg.byteBuf.skipBytes(4) // pet level
      guids(i)(4) = msg.readXorByte(guids(i)(4))
      guids(i)(7) = msg.readXorByte(guids(i)(7))
      msg.byteBuf.skipBytes(12) // y + pet display id + unkn
      guids(i)(6) = msg.readXorByte(guids(i)(6))
      msg.byteBuf.skipBytes(8) // char flags + zone id
      guildGuids(i)(7) = msg.readXorByte(guildGuids(i)(7))
      msg.byteBuf.skipBytes(4) // z

      if (name.equalsIgnoreCase(Global.config.wow.character)) {
        return Some(CharEnumMessage(ByteUtils.bytesToLongLE(guids(i)), race))
      }
    })

    None
  }

  override protected def writePlayerLogin(out: ByteBuf): Unit = {
    val bytes = ByteUtils.longToBytesLE(selfCharacterId.get)

    out.writeIntLE(0x43480000) // unkn

    writeBit(out, bytes(1))
    writeBit(out, bytes(4))
    writeBit(out, bytes(7))
    writeBit(out, bytes(3))
    writeBit(out, bytes(2))
    writeBit(out, bytes(6))
    writeBit(out, bytes(5))
    writeBit(out, bytes(0))

    writeXorByte(out, bytes(5))
    writeXorByte(out, bytes(1))
    writeXorByte(out, bytes(0))
    writeXorByte(out, bytes(6))
    writeXorByte(out, bytes(2))
    writeXorByte(out, bytes(4))
    writeXorByte(out, bytes(7))
    writeXorByte(out, bytes(3))
  }

  override protected def writeJoinChannel(out: ByteBuf, channel: String): Unit = {
    out.writeIntLE(0) // channel id
    writeBit(out, 0) // unkn
    writeBits(out, channel.length, 7)
    writeBits(out, 0, 7)
    writeBit(out, 0) // unkn
    flushBits(out)

    out.writeBytes(channel.getBytes)
  }

  override def updateGuildRoster: Unit = {
    // it apparently sends 2 masked guids,
    // but in fact MaNGOS does not do anything with them so we can just send 0s
    val byteBuf = PooledByteBufAllocator.DEFAULT.buffer(18, 18)
    byteBuf.writeBytes(new Array[Byte](18))
    ctx.get.writeAndFlush(Packet(CMSG_GUILD_ROSTER, byteBuf))
  }

  override protected def parseGuildRoster(msg: Packet): Map[Long, Player] = {
    val motdLength = msg.readBits(11)
    val count = msg.readBits(18)
    val guids = new Array[Array[Byte]](count)
    val pNoteLengths = new Array[Int](count)
    val oNoteLengths = new Array[Int](count)
    val nameLengths = new Array[Int](count)

    (0 until count).foreach(i => {
      guids(i) = new Array[Byte](8)
      guids(i)(3) = msg.readBit
      guids(i)(4) = msg.readBit
      msg.readBits(2) // bnet client flags
      pNoteLengths(i) = msg.readBits(8)
      oNoteLengths(i) = msg.readBits(8)
      guids(i)(0) = msg.readBit
      nameLengths(i) = msg.readBits(7)
      guids(i)(1) = msg.readBit
      guids(i)(2) = msg.readBit
      guids(i)(6) = msg.readBit
      guids(i)(5) = msg.readBit
      guids(i)(7) = msg.readBit
    })

    val gInfoLength = msg.readBits(12)

    (0 until count).flatMap(i => {
      val charClass = msg.byteBuf.readByte
      msg.byteBuf.skipBytes(4) // unkn
      guids(i)(0) = msg.readXorByte(guids(i)(0))
      msg.byteBuf.skipBytes(40) // weekly activity, achievments, professions
      guids(i)(2) = msg.readXorByte(guids(i)(2))
      val flags = msg.byteBuf.readByte
      msg.byteBuf.skipBytes(4) // zone id
      msg.byteBuf.skipBytes(8) // total activity (0)
      guids(i)(7) = msg.readXorByte(guids(i)(7))
      msg.byteBuf.skipBytes(4) // guild rep?
      msg.byteBuf.skipBytes(pNoteLengths(i)) // public note
      guids(i)(3) = msg.readXorByte(guids(i)(3))
      msg.byteBuf.skipBytes(1) // level
      msg.byteBuf.skipBytes(4) // unkn
      guids(i)(5) = msg.readXorByte(guids(i)(5))
      guids(i)(4) = msg.readXorByte(guids(i)(4))
      msg.byteBuf.skipBytes(1) // unkn
      guids(i)(1) = msg.readXorByte(guids(i)(1))
      msg.byteBuf.skipBytes(4) // last logoff time
      msg.byteBuf.skipBytes(oNoteLengths(i)) // officer note
      guids(i)(6) = msg.readXorByte(guids(i)(6))
      val name = msg.byteBuf.readCharSequence(nameLengths(i), Charset.defaultCharset).toString

      if ((flags & 0x01) == 0x01) {
        Some(ByteUtils.bytesToLongLE(guids(i)) -> Player(name, charClass))
      } else {
        None
      }
    }).toMap
  }

  override protected def parseNotification(msg: Packet): String = {
    val length = msg.readBits(12)
    msg.byteBuf.readCharSequence(length, Charset.defaultCharset).toString
  }
}
