package wowchat.game

import java.nio.charset.Charset
import java.security.MessageDigest

import io.netty.buffer.{ByteBuf, PooledByteBufAllocator}
import wowchat.commands.{CommandHandler, WhoResponse}
import wowchat.common._
import wowchat.game.warden.{WardenHandler, WardenHandlerMoP18414}

import scala.util.Random

class GamePacketHandlerMoP18414(realmId: Int, sessionKey: Array[Byte], gameEventCallback: CommonConnectionCallback)
  extends GamePacketHandlerCataclysm15595(realmId, sessionKey, gameEventCallback) with GamePacketsMoP18414 {

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


  override protected def channelParse(msg: Packet): Unit = {
    msg.id match {
      case SMSG_GUILD_INVITE_ACCEPT => handle_SMSG_GUILD_INVITE_ACCEPT(msg)
      case SMSG_GUILD_MEMBER_LOGGED => handle_SMSG_GUILD_MEMBER_LOGGED(msg)
      case SMSG_GUILD_LEAVE => handle_SMSG_GUILD_LEAVE(msg)
      case 0x1568 => // seems to be opcode for compressed data and is sent if account has >= 4 characters for SMSG_CHAR_ENUM
        if (!inWorld) {
          logger.error("You are trying to use this bot on an account with 4 or more characters. This is NOT supported!")
          ctx.foreach(_.close)
          gameEventCallback.error
        }
      case _ => super.channelParse(msg)
    }
  }

  override def sendMessageToWow(tp: Byte, message: String, target: Option[String]): Unit = {
    ctx.foreach(ctx => {
      val out = PooledByteBufAllocator.DEFAULT.buffer(100, 4096)
      out.writeIntLE(languageId)
      target.fold(logger.info(s"Discord->WoW(${ChatEvents.valueOf(tp)}): $message"))(target => {
        logger.info(s"Discord->WoW($target): $message")
        if (tp == ChatEvents.CHAT_MSG_CHANNEL) {
          writeBits(out, target.length, 9)
        }
      })
      writeBits(out, message.length, 8)
      if (tp == ChatEvents.CHAT_MSG_WHISPER) {
        writeBits(out, target.get.length, 9)
      }
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

  // technically MoP sends sender name as part of chat message,
  // but i'll just stick to the old system to keep it consistent.
  override def sendNameQuery(guid: Long): Unit = {
    ctx.foreach(ctx => {
      val out = PooledByteBufAllocator.DEFAULT.buffer(10, 10)
      val guidBytes = ByteUtils.longToBytesLE(guid)

      writeBit(out, guidBytes(4))
      writeBit(out, 0)
      writeBit(out, guidBytes(6))
      writeBit(out, guidBytes(0))
      writeBit(out, guidBytes(7))
      writeBit(out, guidBytes(1))
      writeBit(out, 0)
      writeBit(out, guidBytes(5))
      writeBit(out, guidBytes(2))
      writeBit(out, guidBytes(3))
      flushBits(out)

      writeXorByteSeq(out, guidBytes, 7, 5, 1, 2, 6, 3, 0, 4)

      ctx.writeAndFlush(Packet(CMSG_NAME_QUERY, out))
    })
  }

  override protected def parseNameQuery(msg: Packet): NameQueryMessage = {
    val guid = new Array[Byte](8)
    val guid2 = new Array[Byte](8) // ??
    val guid3 = new Array[Byte](8) // ??

    msg.readBitSeq(guid, 3, 6, 7, 2, 5, 4, 0, 1)
    msg.readXorByteSeq(guid, 5, 4, 7, 6, 1, 2)

    val hasNameData = msg.readBit == 0
    val charClass = if (hasNameData) {
      msg.byteBuf.readBytes(8) // realm id, acc id?
      val charClass = msg.byteBuf.readByte
      msg.byteBuf.skipBytes(3) // race, level, gender
      charClass
    } else {
      0xFF.toByte
    }

    msg.readXorByteSeq(guid, 0, 3)

    if (!hasNameData) {
      val longGuid = ByteUtils.bytesToLongLE(guid)
      logger.error(s"RECV SMSG_NAME_QUERY - Name not known for guid $longGuid")
      return NameQueryMessage(longGuid, "UNKNOWN", charClass)
    }

    msg.resetBitReader
    guid2(2) = msg.readBit
    guid2(7) = msg.readBit
    guid3(7) = msg.readBit
    guid3(2) = msg.readBit
    guid3(0) = msg.readBit
    msg.readBit // unkn
    guid2(4) = msg.readBit
    guid3(5) = msg.readBit
    guid2(1) = msg.readBit
    guid2(3) = msg.readBit
    guid2(0) = msg.readBit

    msg.readBits(7 * 5) // declined names

    guid3(6) = msg.readBit
    guid3(3) = msg.readBit
    guid2(5) = msg.readBit
    guid3(1) = msg.readBit
    guid3(4) = msg.readBit
    val nameLength = msg.readBits(6)
    guid2(6) = msg.readBit

    guid3(6) = msg.readXorByte(guid3(6))
    guid3(0) = msg.readXorByte(guid3(0))
    val name = msg.byteBuf.readCharSequence(nameLength, Charset.defaultCharset).toString

    // can't be bothered to parse the rest of this crap
    NameQueryMessage(ByteUtils.bytesToLongLE(guid), name, charClass)
  }

  override def handleWho(arguments: Option[String]): Option[String] = {
    val characterName = Global.config.wow.character

    if (arguments.isDefined) {
      val byteBuf = PooledByteBufAllocator.DEFAULT.buffer(64, 128)
      byteBuf.writeIntLE(0xFFFFFFFF) // class mask (all classes)
      byteBuf.writeIntLE(0xFFFFFFFF) // race mask (all races)
      byteBuf.writeIntLE(100) // level max
      byteBuf.writeIntLE(0) // level min
      writeBit(byteBuf, 1) // show enemies
      writeBit(byteBuf, 1) // exact name
      writeBit(byteBuf, 0) // request server info
      writeBits(byteBuf, 0, 9) // guild realm name length
      writeBit(byteBuf, 1) // show arena players
      writeBits(byteBuf, arguments.get.length, 6) // name length
      writeBits(byteBuf, 0, 4) // zones count
      writeBits(byteBuf, 0, 9) // realm length name
      writeBits(byteBuf, 0, 7) // guild name length
      writeBits(byteBuf, 0, 3) // word count
      flushBits(byteBuf)
      byteBuf.writeBytes(arguments.get.getBytes)
      ctx.get.writeAndFlush(Packet(CMSG_WHO, byteBuf))
      None
    } else {
      Some(playerRoster
        .valuesIterator
        .filter(!_.name.equalsIgnoreCase(characterName))
        .toSeq
        .sortBy(_.name)
        .map(m => {
          s"${m.name} (${Classes.valueOf(m.charClass)})"
        })
        .mkString(getGuildiesOnlineMessage(false), ", ", ""))
    }
  }

  override protected def handle_SMSG_WHO(msg: Packet): Unit = {
    val displayCount = msg.readBits(6)

    if (displayCount == 0) {
      CommandHandler.handleWhoResponse(None)
    } else {
      val fetchCount = Math.min(displayCount, 3)
      val accountId = new Array[Array[Byte]](fetchCount)
      val playerGuid = new Array[Array[Byte]](fetchCount)
      val guildGuid = new Array[Array[Byte]](fetchCount)
      val guildNameLengths = new Array[Int](fetchCount)
      val playerNameLengths = new Array[Int](fetchCount)

      (0 until fetchCount).foreach(i => {
        accountId(i) = new Array[Byte](8)
        playerGuid(i) = new Array[Byte](8)
        guildGuid(i) = new Array[Byte](8)

        accountId(i)(2) = msg.readBit
        playerGuid(i)(2) = msg.readBit
        accountId(i)(7) = msg.readBit
        guildGuid(i)(5) = msg.readBit
        guildNameLengths(i) = msg.readBits(7)
        accountId(i)(1) = msg.readBit
        accountId(i)(5) = msg.readBit
        guildGuid(i)(7) = msg.readBit
        playerGuid(i)(5) = msg.readBit
        msg.readBit // unkn
        guildGuid(i)(1) = msg.readBit
        playerGuid(i)(6) = msg.readBit
        guildGuid(i)(2) = msg.readBit
        playerGuid(i)(4) = msg.readBit
        guildGuid(i)(0) = msg.readBit
        guildGuid(i)(3) = msg.readBit
        accountId(i)(6) = msg.readBit
        msg.readBit // unkn
        playerGuid(i)(1) = msg.readBit
        guildGuid(i)(4) = msg.readBit
        accountId(i)(0) = msg.readBit
        msg.readBits(7 * 5) // declined names
        playerGuid(i)(3) = msg.readBit
        guildGuid(i)(6) = msg.readBit
        playerGuid(i)(0) = msg.readBit
        accountId(i)(4) = msg.readBit
        accountId(i)(3) = msg.readBit
        playerGuid(i)(7) = msg.readBit
        playerNameLengths(i) = msg.readBits(6)
      })

      // skip rest
      (fetchCount until displayCount).foreach(i => msg.readBits(74))

      (0 until fetchCount).foreach(i => {
        playerGuid(i)(1) = msg.readXorByte(playerGuid(i)(1))
        msg.byteBuf.skipBytes(4) // realm id
        playerGuid(i)(7) = msg.readXorByte(playerGuid(i)(7))
        msg.byteBuf.skipBytes(4) // realm id
        playerGuid(i)(4) = msg.readXorByte(playerGuid(i)(4))
        val playerName = msg.byteBuf.readCharSequence(playerNameLengths(i), Charset.defaultCharset).toString
        guildGuid(i)(1) = msg.readXorByte(guildGuid(i)(1))
        playerGuid(i)(0) = msg.readXorByte(playerGuid(i)(0))
        guildGuid(i)(2) = msg.readXorByte(guildGuid(i)(2))
        guildGuid(i)(0) = msg.readXorByte(guildGuid(i)(0))
        guildGuid(i)(4) = msg.readXorByte(guildGuid(i)(4))
        playerGuid(i)(3) = msg.readXorByte(playerGuid(i)(3))
        guildGuid(i)(6) = msg.readXorByte(guildGuid(i)(6))
        msg.byteBuf.skipBytes(4) // account id?
        val guildName = msg.byteBuf.readCharSequence(guildNameLengths(i), Charset.defaultCharset).toString
        guildGuid(i)(3) = msg.readXorByte(guildGuid(i)(3))
        accountId(i)(4) = msg.readXorByte(accountId(i)(4))
        val cls = Classes.valueOf(msg.byteBuf.readByte)
        accountId(i)(7) = msg.readXorByte(accountId(i)(7))
        playerGuid(i)(6) = msg.readXorByte(playerGuid(i)(6))
        playerGuid(i)(2) = msg.readXorByte(playerGuid(i)(2))

        // assume no declined names

        accountId(i)(2) = msg.readXorByte(accountId(i)(2))
        accountId(i)(3) = msg.readXorByte(accountId(i)(3))
        val race = Races.valueOf(msg.byteBuf.readByte)
        guildGuid(i)(7) = msg.readXorByte(guildGuid(i)(7))
        accountId(i)(1) = msg.readXorByte(accountId(i)(1))
        accountId(i)(5) = msg.readXorByte(accountId(i)(5))
        accountId(i)(6) = msg.readXorByte(accountId(i)(6))
        playerGuid(i)(5) = msg.readXorByte(playerGuid(i)(5))
        accountId(i)(0) = msg.readXorByte(accountId(i)(0))
        val gender = Some(Genders.valueOf(msg.byteBuf.readByte))
        guildGuid(i)(5) = msg.readXorByte(guildGuid(i)(5))
        val lvl = msg.byteBuf.readByte
        val zone = msg.byteBuf.readIntLE

        CommandHandler.handleWhoResponse(Some(WhoResponse(
          playerName,
          guildName,
          lvl,
          cls,
          race,
          gender,
          AreaTable.AREA.getOrElse(zone, "Unknown Zone")))
        )
      })
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

    writeBitSeq(out, bytes, 1, 4, 7, 3, 2, 6, 5, 0)
    writeXorByteSeq(out, bytes, 5, 1, 0, 6, 2, 4, 7, 3)
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

  override protected def parseChatMessage(msg: Packet): Option[ChatMessage] = {
    val hasSenderName = msg.readBit == 0
    msg.readBit // hide in chat log

    val senderNameLength = if (hasSenderName) {
      msg.readBits(11)
    } else {
      0
    }

    msg.readBit // unkn
    val hasChannelName = msg.readBit == 0
    msg.readBit // unkn
    msg.readBit // send fake time?
    val hasChatTag = msg.readBit == 0
    val hasRealmId = msg.readBit == 0

    val groupGuid = new Array[Byte](8)
    msg.readBitSeq(groupGuid, 0, 1, 5, 4, 3, 2, 6, 7)

    if (hasChatTag) {
      msg.readBits(9)
    }

    msg.readBit // unkn

    val receiverGuid = new Array[Byte](8)
    msg.readBitSeq(receiverGuid, 7, 6, 1, 4, 0, 2, 3, 5)

    msg.readBit // unkn
    val hasLanguage = msg.readBit == 0
    val hasPrefix = msg.readBit == 0

    val senderGuid = new Array[Byte](8)
    msg.readBitSeq(senderGuid, 0, 3, 7, 2, 1, 5, 4, 6)

    val hasAchievement = msg.readBit == 0
    val hasMessage = msg.readBit == 0

    val channelNameLength = if (hasChannelName) {
      msg.readBits(7)
    } else {
      0
    }

    val messageLength = if (hasMessage) {
      msg.readBits(12)
    } else {
      0
    }

    if (!hasMessage || messageLength == 0) {
      return None
    }

    val hasReceiver = msg.readBit == 0

    val addonPrefixLength = if (hasPrefix) {
      msg.readBits(5)
    } else {
      0
    }

    msg.readBit // realm id?

    val receiverLength = if (hasReceiver) {
      msg.readBits(11)
    } else {
      0
    }

    msg.readBit // unkn

    val guildGuid = new Array[Byte](8)
    msg.readBitSeq(guildGuid, 2, 5, 7, 4, 0, 1, 3, 6)

    msg.readXorByteSeq(guildGuid, 4, 5, 7, 3, 2, 6, 0, 1)

    val channelName = if (hasChannelName) {
      Some(msg.byteBuf.readCharSequence(channelNameLength, Charset.defaultCharset).toString)
    } else {
      None
    }

    if (hasPrefix) {
      msg.byteBuf.readBytes(addonPrefixLength)
    }

    msg.readXorByteSeq(senderGuid, 4, 7, 1, 5, 0, 6, 2, 3)

    // ignore messages from itself
    if (ByteUtils.bytesToLongLE(senderGuid) == selfCharacterId.get) {
      return None
    }

    val tp = msg.byteBuf.readByte

    // ignore if from an unhandled channel
    if (!Global.wowToDiscord.contains((tp, channelName.map(_.toLowerCase)))) {
      return None
    }

    if (hasAchievement) {
      msg.byteBuf.skipBytes(4)
    }

    msg.readXorByteSeq(groupGuid, 1, 3, 4, 6, 0, 2, 5, 7)

    msg.readXorByteSeq(receiverGuid, 4, 7, 1, 5, 0, 6, 2, 3)

    val language = if (hasLanguage) {
      msg.byteBuf.readByte
    } else {
      0
    }

    // ignore addon messages
    if (language == -1) {
      return None
    }

    // i am not sure about this
    if (hasRealmId) {
      msg.byteBuf.skipBytes(4)
    }

    val txt = if (hasMessage) {
      msg.byteBuf.readCharSequence(messageLength, Charset.defaultCharset).toString
    } else {
      ""
    }

    Some(ChatMessage(ByteUtils.bytesToLongLE(senderGuid), tp, txt, channelName))
  }

  override def updateGuildRoster: Unit = {
    // it apparently sends 2 masked guids,
    // but in fact MaNGOS does not do anything with them so we can just send 0s
    val byteBuf = PooledByteBufAllocator.DEFAULT.buffer(18, 18)
    byteBuf.writeBytes(new Array[Byte](18))
    ctx.get.writeAndFlush(Packet(CMSG_GUILD_ROSTER, byteBuf))
  }

  override protected def parseGuildRoster(msg: Packet): Map[Long, Player] = {
    val count = msg.readBits(17)
    val motdLength = msg.readBits(10)
    val guids = new Array[Array[Byte]](count)
    val pNoteLengths = new Array[Int](count)
    val oNoteLengths = new Array[Int](count)
    val nameLengths = new Array[Int](count)

    (0 until count).foreach(i => {
      guids(i) = new Array[Byte](8)
      oNoteLengths(i) = msg.readBits(8)
      guids(i)(5) = msg.readBit
      msg.readBit // scroll of resurrect
      pNoteLengths(i) = msg.readBits(8)
      guids(i)(7) = msg.readBit
      guids(i)(0) = msg.readBit
      guids(i)(6) = msg.readBit
      nameLengths(i) = msg.readBits(6)
      msg.readBit // has authenticator
      guids(i)(3) = msg.readBit
      guids(i)(4) = msg.readBit
      guids(i)(1) = msg.readBit
      guids(i)(2) = msg.readBit
    })

    val gInfoLength = msg.readBits(11)

    (0 until count).flatMap(i => {
      val charClass = msg.byteBuf.readByte
      msg.byteBuf.skipBytes(4) // total reputation
      val name = msg.byteBuf.readCharSequence(nameLengths(i), Charset.defaultCharset).toString
      guids(i)(0) = msg.readXorByte(guids(i)(0))
      msg.byteBuf.skipBytes(24) // professions
      msg.byteBuf.skipBytes(1) // level
      val flags = msg.byteBuf.readByte
      msg.byteBuf.skipBytes(4) // zone id
      msg.byteBuf.skipBytes(4) // rep cap
      guids(i)(3) = msg.readXorByte(guids(i)(3))
      msg.byteBuf.skipBytes(8) // total activity
      msg.byteBuf.skipBytes(oNoteLengths(i)) // officer note
      msg.byteBuf.skipBytes(4) // logout time
      msg.byteBuf.skipBytes(1) // gender? always 0?
      msg.byteBuf.skipBytes(4) // rank
      msg.byteBuf.skipBytes(4) // realm id
      guids(i)(5) = msg.readXorByte(guids(i)(5))
      guids(i)(7) = msg.readXorByte(guids(i)(7))
      msg.byteBuf.skipBytes(pNoteLengths(i)) // public note
      guids(i)(4) = msg.readXorByte(guids(i)(4))
      msg.byteBuf.skipBytes(8) // weekly activity
      msg.byteBuf.skipBytes(4) // achievement points
      guids(i)(6) = msg.readXorByte(guids(i)(6))
      guids(i)(1) = msg.readXorByte(guids(i)(1))
      guids(i)(2) = msg.readXorByte(guids(i)(2))

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

  override protected def initializeWardenHandler: WardenHandler = {
    new WardenHandlerMoP18414(sessionKey)
  }

  private def handle_SMSG_GUILD_INVITE_ACCEPT(msg: Packet): Unit = {
    val guid = new Array[Byte](8)
    msg.readBitSeq(guid, 6, 1, 3)
    val nameLength = msg.readBits(6)
    msg.readBitSeq(guid, 7, 4, 2, 5, 0)

    msg.readXorByteSeq(guid, 2, 4, 1, 6, 5)
    msg.byteBuf.skipBytes(4) // unkn
    msg.readXorByteSeq(guid, 3, 0)
    val name = msg.byteBuf.readCharSequence(nameLength, Charset.defaultCharset).toString
    msg.readXorByteSeq(guid, 7)

    handleGuildEvent(GuildEvents.GE_JOINED.toByte, name)
  }

  private def handle_SMSG_GUILD_MEMBER_LOGGED(msg: Packet): Unit = {
    val guid = new Array[Byte](8)
    msg.readBitSeq(guid, 0, 6)
    msg.readBit // unkn
    msg.readBitSeq(guid, 2, 5, 3)
    val nameLength = msg.readBits(6)
    msg.readBitSeq(guid, 1, 7, 4)
    val isOnline = msg.readBit == 1

    msg.readXorByteSeq(guid, 3, 2, 0)
    msg.byteBuf.skipBytes(4) // unkn
    msg.readXorByteSeq(guid, 6)
    val name = msg.byteBuf.readCharSequence(nameLength, Charset.defaultCharset).toString
    msg.readXorByteSeq(guid, 4, 5, 7, 1)

    val event = (if (isOnline) {
      GuildEvents.GE_SIGNED_ON
    } else {
      GuildEvents.GE_SIGNED_OFF
    }).toByte

    handleGuildEvent(event, name)
  }

  private def handle_SMSG_GUILD_LEAVE(msg: Packet): Unit = {
    val guid = new Array[Byte](8)

    guid(2) = msg.readBit
    val nameLength = msg.readBits(6)
    guid(6) = msg.readBit
    guid(5) = msg.readBit
    val kicked = msg.readBit == 1

    val (kickerNameLength, kickerGuid) = if (kicked) {
      msg.readBits(2) // unkn
      val kickerNameLength = msg.readBits(6) // kicker name length
      val kickerGuid = new Array[Byte](8)
      msg.readBitSeq(kickerGuid, 1, 3, 4, 2, 5, 7, 6, 0)
      msg.readBit // unkn
      (kickerNameLength, kickerGuid)
    } else {
      (0, null)
    }

    guid(1) = msg.readBit
    guid(0) = msg.readBit
    guid(3) = msg.readBit
    guid(4) = msg.readBit
    guid(7) = msg.readBit

    if (kicked) {
      msg.readXorByteSeq(kickerGuid, 1, 3, 5, 2, 0, 4, 6, 7)
      msg.byteBuf.skipBytes(kickerNameLength)
      msg.byteBuf.skipBytes(4) // unkn
    }

    val name = msg.byteBuf.readCharSequence(nameLength, Charset.defaultCharset).toString
    msg.readXorByteSeq(guid, 1)
    msg.byteBuf.skipBytes(4) // unkn
    msg.readXorByteSeq(guid, 0, 4, 2, 3, 6, 5, 7)

    val event = (if (kicked) {
      GuildEvents.GE_REMOVED
    } else {
      GuildEvents.GE_LEFT
    }).toByte

    handleGuildEvent(event, name)
  }
}
