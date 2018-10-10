package wowchat.game

import wowchat.common.{ByteUtils, Global, Packet}
import com.typesafe.scalalogging.StrictLogging
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

import scala.collection.mutable.ArrayBuffer

class GamePacketEncoder extends MessageToByteEncoder[Packet] with GamePackets with StrictLogging {

  override def encode(ctx: ChannelHandlerContext, msg: Packet, out: ByteBuf): Unit = {
    val crypt = ctx.channel.attr(CRYPT).get
    val unencrypted = isUnencryptedPacket(msg.id)

    val headerSize = if (unencrypted) 4 else 6

    val array = new ArrayBuffer[Byte](headerSize)
    array ++= ByteUtils.shortToBytes(msg.byteBuf.writerIndex + headerSize - 2)
    array ++= ByteUtils.shortToBytesLE(msg.id)
    val header = if (unencrypted) {
      array.toArray
    } else {
      array.append(0, 0)
      crypt.encrypt(array.toArray)
    }

    logger.debug(f"SEND PACKET: ${msg.id}%04X - ${ByteUtils.toHexString(msg.byteBuf, true, false)}")

    out.writeBytes(header)
    out.writeBytes(msg.byteBuf)
    msg.byteBuf.release
  }

  protected def isUnencryptedPacket(id: Int): Boolean = {
    id == CMSG_AUTH_CHALLENGE
  }
}
