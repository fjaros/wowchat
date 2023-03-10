package wowchat.game

import java.nio.charset.Charset
import java.security.MessageDigest

import io.netty.buffer.{ByteBuf, PooledByteBufAllocator}
import wowchat.common._

import scala.util.Random

class GamePacketHandlerCataclysm15595(realmId: Int, realmName: String, sessionKey: Array[Byte], gameEventCallback: CommonConnectionCallback)
  extends GamePacketHandlerWotLK(realmId, realmName, sessionKey, gameEventCallback) with GamePacketsCataclysm15595 {

  override protected def channelParse(msg: Packet): Unit = {
    msg.id match {
      case WOW_CONNECTION => handle_WOW_CONNECTION(msg)
      case _ => super.channelParse(msg)
    }
  }

  override def buildChatMessage(tp: Byte, utf8MessageBytes: Array[Byte], utf8TargetBytes: Option[Array[Byte]]): Packet = {
    val out = PooledByteBufAllocator.DEFAULT.buffer(128, 8192)
    out.writeIntLE(languageId)
    utf8TargetBytes.foreach(utf8TargetBytes => {
      writeBits(out, utf8TargetBytes.length, 10)
    })
    writeBits(out, utf8MessageBytes.length, 9)
    flushBits(out)
    // note for whispers (if the bot ever supports them, the order is opposite, person first then msg)
    out.writeBytes(utf8MessageBytes)
    if (utf8TargetBytes.isDefined) {
      out.writeBytes(utf8TargetBytes.get)
    }
    Packet(getChatPacketFromType(tp), out)
  }

  protected def getChatPacketFromType(tp: Byte): Int = {
    tp match {
      case ChatEvents.CHAT_MSG_CHANNEL => CMSG_MESSAGECHAT_CHANNEL
      case ChatEvents.CHAT_MSG_EMOTE => CMSG_MESSAGECHAT_EMOTE
      case ChatEvents.CHAT_MSG_GUILD => CMSG_MESSAGECHAT_GUILD
      case ChatEvents.CHAT_MSG_OFFICER => CMSG_MESSAGECHAT_OFFICER
      case ChatEvents.CHAT_MSG_SAY => CMSG_MESSAGECHAT_SAY
      case ChatEvents.CHAT_MSG_WHISPER => CMSG_MESSAGECHAT_WHISPER
      case ChatEvents.CHAT_MSG_YELL => CMSG_MESSAGECHAT_YELL
      case _ => throw new UnsupportedOperationException(s"Type ${ChatEvents.valueOf(tp)} cannot be sent to WoW!")
    }
  }

  override protected def parseAuthChallenge(msg: Packet): AuthChallengeMessage = {
    val account = Global.config.wow.account

    msg.byteBuf.skipBytes(32) // 32 bytes of random data?
    val serverSeed = msg.byteBuf.readInt
    val clientSeed = Random.nextInt

    val md = MessageDigest.getInstance("SHA1")
    md.update(account)
    md.update(Array[Byte](0, 0, 0, 0))
    md.update(ByteUtils.intToBytes(clientSeed))
    md.update(ByteUtils.intToBytes(serverSeed))
    md.update(sessionKey)
    val digest = md.digest

    val out = PooledByteBufAllocator.DEFAULT.buffer(200, 400)
    out.writeShortLE(0)
    out.writeBytes(new Array[Byte](9))
    out.writeByte(digest(10))
    out.writeByte(digest(18))
    out.writeByte(digest(12))
    out.writeByte(digest(5))
    out.writeBytes(new Array[Byte](8))
    out.writeByte(digest(15))
    out.writeByte(digest(9))
    out.writeByte(digest(19))
    out.writeByte(digest(4))
    out.writeByte(digest(7))
    out.writeByte(digest(16))
    out.writeByte(digest(3))
    out.writeShortLE(WowChatConfig.getGameBuild)
    out.writeByte(digest(8))
    out.writeBytes(new Array[Byte](5))
    out.writeByte(digest(17))
    out.writeByte(digest(6))
    out.writeByte(digest(0))
    out.writeByte(digest(1))
    out.writeByte(digest(11))
    out.writeInt(clientSeed)
    out.writeByte(digest(2))
    out.writeIntLE(0)
    out.writeByte(digest(14))
    out.writeByte(digest(13))

    out.writeIntLE(addonInfo.length)
    out.writeBytes(addonInfo)

    out.writeByte(account.length >> 5)
    out.writeByte(account.length << 3)
    out.writeBytes(account)

    AuthChallengeMessage(sessionKey, out)
  }

  override protected def parseAuthResponse(msg: Packet): Byte = {
    msg.byteBuf.skipBytes(16)
    super.parseAuthResponse(msg)
  }

  override protected def parseCharEnum(msg: Packet): Option[CharEnumMessage] = {
    val characterBytes = Global.config.wow.character.toLowerCase.getBytes("UTF-8")
    msg.readBits(24) // unkn
    val charactersNum = msg.readBits(17)

    val guids = new Array[Array[Byte]](charactersNum)
    val guildGuids = new Array[Array[Byte]](charactersNum)
    val nameLenghts = new Array[Int](charactersNum)

    (0 until charactersNum).foreach(i => {
      guids(i) = new Array[Byte](8)
      guildGuids(i) = new Array[Byte](8)
      msg.readBitSeq(guids(i), 3)
      msg.readBitSeq(guildGuids(i), 1, 7, 2)
      nameLenghts(i) = msg.readBits(7)
      msg.readBitSeq(guids(i), 4, 7)
      msg.readBitSeq(guildGuids(i), 3)
      msg.readBitSeq(guids(i), 5)
      msg.readBitSeq(guildGuids(i), 6)
      msg.readBitSeq(guids(i), 1)
      msg.readBitSeq(guildGuids(i), 5, 4)
      msg.readBit // is first login
      msg.readBitSeq(guids(i), 0, 2, 6)
      msg.readBitSeq(guildGuids(i), 0)
    })

    (0 until charactersNum).foreach(i => {
      msg.byteBuf.skipBytes(1) // char class
      msg.byteBuf.skipBytes(207) // inventory
      msg.byteBuf.readIntLE
      msg.readXorByteSeq(guildGuids(i), 2)
      msg.byteBuf.skipBytes(2)
      msg.readXorByteSeq(guildGuids(i), 3)
      msg.byteBuf.skipBytes(9)
      msg.readXorByteSeq(guids(i), 4)
      msg.byteBuf.readIntLE // map
      msg.readXorByteSeq(guildGuids(i), 5)
      msg.byteBuf.skipBytes(4)
      msg.readXorByteSeq(guildGuids(i), 6)
      msg.byteBuf.skipBytes(4)
      msg.readXorByteSeq(guids(i), 3)
      msg.byteBuf.skipBytes(9)
      msg.readXorByteSeq(guids(i), 7)
      msg.byteBuf.skipBytes(1)
      val name = msg.byteBuf.readCharSequence(nameLenghts(i), Charset.forName("UTF-8")).toString
      msg.byteBuf.skipBytes(1)
      msg.readXorByteSeq(guids(i), 0, 2)
      msg.readXorByteSeq(guildGuids(i), 1, 7)
      msg.byteBuf.skipBytes(5)
      val race = msg.byteBuf.readByte
      msg.byteBuf.skipBytes(1) // char level
      msg.readXorByteSeq(guids(i), 6)
      msg.readXorByteSeq(guildGuids(i), 4, 0)
      msg.readXorByteSeq(guids(i), 5, 1)

      if (name.toLowerCase.getBytes("UTF-8").sameElements(characterBytes)) {
        return Some(CharEnumMessage(name, ByteUtils.bytesToLongLE(guids(i)), race, ByteUtils.bytesToLongLE(guildGuids(i))))
      }

      msg.byteBuf.skipBytes(4) // zone
    })

    None
  }

  override protected def writePlayerLogin(out: ByteBuf): Unit = {
    val bytes = ByteUtils.longToBytesLE(selfCharacterId.get)
    writeBitSeq(out, bytes, 2, 3, 0, 6, 4, 5, 1, 7)
    writeXorByteSeq(out, bytes, 2, 7, 0, 3, 5, 6, 1, 4)
  }

  override protected def writeJoinChannel(out: ByteBuf, id: Int, utf8ChannelBytes: Array[Byte]): Unit = {
    out.writeIntLE(id) // channel id
    writeBit(out, 0) // has voice
    writeBit(out, 0) // zone update
    writeBits(out, utf8ChannelBytes.length, 8)
    writeBits(out, 0, 8)
    flushBits(out)

    out.writeBytes(utf8ChannelBytes)
  }

  override protected def queryGuildName: Unit = {
    val out = PooledByteBufAllocator.DEFAULT.buffer(16, 16)
    out.writeLongLE(guildGuid)
    out.writeLongLE(selfCharacterId.get)
    ctx.get.writeAndFlush(Packet(CMSG_GUILD_QUERY, out))
  }

  override protected def handleGuildQuery(msg: Packet): GuildInfo = {
    msg.byteBuf.skipBytes(4) // first part of guid, the vanilla handler can handle the rest
    super.handleGuildQuery(msg)
  }

  override protected def buildGuildRosterPacket: Packet = {
    // it apparently sends 2 masked guids,
    // but in fact MaNGOS does not do anything with them so we can just send 0s
    val byteBuf = PooledByteBufAllocator.DEFAULT.buffer(18, 18)
    byteBuf.writeBytes(new Array[Byte](18))
    Packet(CMSG_GUILD_ROSTER, byteBuf)
  }

  override protected def parseGuildRoster(msg: Packet): Map[Long, GuildMember] = {
    val motdLength = msg.readBits(11)
    val count = msg.readBits(18)
    val guids = new Array[Array[Byte]](count)
    val pNoteLengths = new Array[Int](count)
    val oNoteLengths = new Array[Int](count)
    val nameLengths = new Array[Int](count)

    (0 until count).foreach(i => {
      guids(i) = new Array[Byte](8)
      msg.readBitSeq(guids(i), 3, 4)
      msg.readBits(2) // bnet client flags
      pNoteLengths(i) = msg.readBits(8)
      oNoteLengths(i) = msg.readBits(8)
      msg.readBitSeq(guids(i), 0)
      nameLengths(i) = msg.readBits(7)
      msg.readBitSeq(guids(i), 1, 2, 6, 5, 7)
    })

    val gInfoLength = msg.readBits(12)

    val guildRosterMap = (0 until count).map(i => {
      val charClass = msg.byteBuf.readByte
      msg.byteBuf.skipBytes(4) // unkn
      msg.readXorByteSeq(guids(i), 0)
      msg.byteBuf.skipBytes(40) // weekly activity, achievments, professions
      msg.readXorByteSeq(guids(i), 2)
      val flags = msg.byteBuf.readByte
      val zoneId = msg.byteBuf.readIntLE
      msg.byteBuf.skipBytes(8) // total activity (0)
      msg.readXorByteSeq(guids(i), 7)
      msg.byteBuf.skipBytes(4) // guild rep?
      msg.byteBuf.skipBytes(pNoteLengths(i)) // public note
      msg.readXorByteSeq(guids(i), 3)
      val level = msg.byteBuf.readByte
      msg.byteBuf.skipBytes(4) // unkn
      msg.readXorByteSeq(guids(i), 5, 4)
      msg.byteBuf.skipBytes(1) // unkn
      msg.readXorByteSeq(guids(i), 1)
      val lastLogoff = msg.byteBuf.readFloatLE
      msg.byteBuf.skipBytes(oNoteLengths(i)) // officer note
      msg.readXorByteSeq(guids(i), 6)
      val name = msg.byteBuf.readCharSequence(nameLengths(i), Charset.forName("UTF-8")).toString
      val isOnline = (flags & 0x01) == 0x01

      ByteUtils.bytesToLongLE(guids(i)) -> GuildMember(name, isOnline, charClass, level, zoneId, lastLogoff)
    }).toMap

    msg.byteBuf.skipBytes(gInfoLength)
    guildMotd = Some(msg.byteBuf.readCharSequence(motdLength, Charset.forName("UTF-8")).toString)

    guildRosterMap
  }

  override protected def parseNotification(msg: Packet): String = {
    val length = msg.readBits(13)
    msg.byteBuf.readCharSequence(length, Charset.forName("UTF-8")).toString
  }

  private def handle_WOW_CONNECTION(msg: Packet): Unit = {
    val byteBuf = PooledByteBufAllocator.DEFAULT.buffer(48, 48)

    val connectionString = "RLD OF WARCRAFT CONNECTION - CLIENT TO SERVER"
    byteBuf.writeBytes(connectionString.getBytes)
    byteBuf.writeByte(0)
    ctx.get.writeAndFlush(Packet(WOW_CONNECTION, byteBuf))
  }

  // bit manipulation for cata+
  private var bitPosition = 8
  private var byte = 0

  def writeBits(out: ByteBuf, value: Int, bitCount: Int): Unit = {
    (bitCount - 1 to 0 by -1).foreach(i => {
      writeBit(out, (value >> i) & 1)
    })
  }

  def writeBit(out: ByteBuf, bit: Int): Unit = {
    bitPosition -= 1
    if (bit != 0) {
      byte |= (1 << bitPosition)
    }

    if (bitPosition == 0) {
      flushBits(out)
    }
  }

  def writeBitSeq(out: ByteBuf, bytes: Array[Byte], indices: Int*): Unit = {
    indices.foreach(i => {
      writeBit(out, bytes(i))
    })
  }

  def writeXorByte(out: ByteBuf, byte: Byte): Unit = {
    if (byte != 0) {
      out.writeByte((byte ^ 1).toByte)
    }
  }

  def writeXorByteSeq(out: ByteBuf, bytes: Array[Byte], indices: Int*): Unit = {
    indices.foreach(i => {
      writeXorByte(out, bytes(i))
    })
  }

  def flushBits(out: ByteBuf): Unit = {
    if (bitPosition == 8) {
      return
    }

    out.writeByte((byte & 0xFF).toByte)
    bitPosition = 8
    byte = 0
  }
}
