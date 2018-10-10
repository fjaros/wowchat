package wowchat.game

import java.security.MessageDigest

import io.netty.buffer.PooledByteBufAllocator
import wowchat.common._

import scala.util.Random

class GamePacketHandlerCataclysm(realmId: Int, sessionKey: Array[Byte], gameEventCallback: CommonConnectionCallback)
  extends GamePacketHandlerTBC(realmId, sessionKey, gameEventCallback) with GamePacketsCataclysm {


  override protected def channelParse(msg: Packet): Unit = {
    msg.id match {
      case WOW_CONNECTION => handle_WOW_CONNECTION(msg)
      case _ => super.channelParse(msg)
    }
  }

  override protected def parseAuthChallenge(msg: Packet): AuthChallengeMessage = {
    val account = Global.config.wow.account.toUpperCase

    msg.byteBuf.skipBytes(32) // 32 bytes of random data?
    val serverSeed = msg.byteBuf.readInt
    val clientSeed = Random.nextInt

    val md = MessageDigest.getInstance("SHA1")
    md.update(account.getBytes)
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
    out.writeShortLE(WowChatConfig.getBuild)
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
    out.writeBytes(account.getBytes)

    AuthChallengeMessage(sessionKey, out)
  }

  override protected def parseAuthResponse(msg: Packet): Byte = {
    msg.byteBuf.skipBytes(16)
    super.parseAuthResponse(msg)
  }

  override protected def parseCharEnum(msg: Packet): Option[CharEnumMessage] = {
    val unpack = new BitUnpack(msg.byteBuf)
    var guidBytes = new Array[Byte](8)

    msg.readBits(24) // unkn
    println(ByteUtils.toHexString(msg.byteBuf, true, false))
    val charactersNum = msg.readBits(17)

//    guidBytes(3) |= unpack.get



    None
  }

  private def handle_WOW_CONNECTION(msg: Packet): Unit = {
    val byteBuf = PooledByteBufAllocator.DEFAULT.buffer(48, 48)

    val connectionString = "RLD OF WARCRAFT CONNECTION - CLIENT TO SERVER"
    byteBuf.writeBytes(connectionString.getBytes)
    byteBuf.writeByte(0)
    ctx.get.writeAndFlush(Packet(WOW_CONNECTION, byteBuf))
  }
}
