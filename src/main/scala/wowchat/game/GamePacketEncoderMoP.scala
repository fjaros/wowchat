package wowchat.game

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import wowchat.common.{ByteUtils, Packet, WowChatConfig, WowExpansion}

import scala.collection.mutable.ArrayBuffer

class GamePacketEncoderMoP extends GamePacketEncoderCataclysm with GamePacketsMoP {

  override def encode(ctx: ChannelHandlerContext, msg: Packet, out: ByteBuf): Unit = {
    val crypt = ctx.channel.attr(CRYPT).get
    val unencrypted = isUnencryptedPacket(msg.id)

    val headerSize = 4
    val size = msg.byteBuf.writerIndex

    val array = new ArrayBuffer[Byte](headerSize)
    val header = if (unencrypted) {
      array ++= ByteUtils.shortToBytesLE(size + 2)
      array ++= ByteUtils.shortToBytesLE(msg.id)
      array.toArray
    } else {
      array ++= ByteUtils.intToBytesLE((size << 13) | (msg.id & 0x1FFF))
      crypt.encrypt(array.toArray)
    }

    logger.debug(f"SEND PACKET: ${msg.id}%04X - ${ByteUtils.toHexString(msg.byteBuf, true, false)}")

    out.writeBytes(header)
    out.writeBytes(msg.byteBuf)
    msg.byteBuf.release
  }
}
