package wowchat.game

import java.security.MessageDigest

import wowchat.common._
import io.netty.buffer.{ByteBuf, PooledByteBufAllocator}

import scala.util.Random

class GamePacketHandlerWotLK(realmId: Int, realmName: String, sessionKey: Array[Byte], gameEventCallback: CommonConnectionCallback)
  extends GamePacketHandlerTBC(realmId, realmName, sessionKey, gameEventCallback) {

  override protected def parseAuthChallenge(msg: Packet): AuthChallengeMessage = {
    val accountConfig = Global.config.wow.account.toUpperCase

    msg.byteBuf.skipBytes(4) // wotlk
    val serverSeed = msg.byteBuf.readInt
    val clientSeed = Random.nextInt
    val out = PooledByteBufAllocator.DEFAULT.buffer(200, 400)
    out.writeShortLE(0)
    out.writeIntLE(WowChatConfig.getBuild)
    out.writeIntLE(0)
    out.writeBytes(accountConfig.getBytes)
    out.writeByte(0)
    out.writeInt(0) // wotlk
    out.writeInt(clientSeed)
    out.writeIntLE(0) // wotlk
    out.writeIntLE(0) // wotlk
    out.writeIntLE(realmId) // wotlk
    out.writeLongLE(3) // wotlk

    val md = MessageDigest.getInstance("SHA1")
    md.update(accountConfig.getBytes)
    md.update(Array[Byte](0, 0, 0, 0))
    md.update(ByteUtils.intToBytes(clientSeed))
    md.update(ByteUtils.intToBytes(serverSeed))
    md.update(sessionKey)
    out.writeBytes(md.digest)

    out.writeBytes(addonInfo)

    AuthChallengeMessage(sessionKey, out)
  }

  override protected def parseNameQuery(msg: Packet): NameQueryMessage = {
    val guid = unpackGuid(msg.byteBuf)

    val nameKnown = msg.byteBuf.readByte // wotlk
    val (name, charClass) = if (nameKnown == 0) {
      val name = msg.readString
      msg.skipString // realm name for cross bg usage

      // wotlk changed the char info to bytes
      msg.byteBuf.skipBytes(1) // race
      msg.byteBuf.skipBytes(1) // gender
      val charClass = msg.byteBuf.readByte
      (name, charClass)
    } else {
      logger.error(s"RECV SMSG_NAME_QUERY - Name not known for guid $guid")
      ("UNKNOWN", 0xFF.toByte)
    }

    NameQueryMessage(guid, name, charClass)
  }

  // saving those single 0 bytes like whoa
  private def unpackGuid(byteBuf: ByteBuf): Long = {
    val set = byteBuf.readByte

    (0 until 8).foldLeft(0L) {
      case (result, i) =>
        val onBit = 1 << i
        if ((set & onBit) == onBit) {
          result | ((byteBuf.readByte & 0xFFL) << (i * 8))
        } else {
          result
        }
    }
  }
}
