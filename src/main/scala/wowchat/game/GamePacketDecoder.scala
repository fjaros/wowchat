package wowchat.game

import java.util

import wowchat.common.{ByteUtils, Packet}
import com.typesafe.scalalogging.StrictLogging
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder

class GamePacketDecoder extends ByteToMessageDecoder with GamePackets with StrictLogging {

  private val HEADER_LENGTH = 4

  private var size = 0
  private var id = 0

  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
    if (in.readableBytes < HEADER_LENGTH) {
      return
    }

    val crypt = ctx.channel.attr(CRYPT).get

    if (size == 0 && id == 0) {
      if (crypt.isInit) {
        val header = new Array[Byte](HEADER_LENGTH)
        in.readBytes(header)
        val decrypted = crypt.decrypt(header)
        size = (decrypted(0) << 8 | decrypted(1) & 0xFF) - 2
        id = decrypted(3) << 8 | decrypted(2) & 0xFF
      } else {
        size = in.readShort() - 2
        id = in.readShortLE
      }
    }

    if (size > in.readableBytes) {
      return
    }

    val byteBuf = in.readBytes(size)
    val packet = Packet(id, byteBuf)

    logger.debug(f"RECV PACKET: $id%04X - ${ByteUtils.toHexString(byteBuf, true, false)}")

    out.add(packet)
    size = 0
    id = 0
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    logger.error("EXCEPTION CAUGHT: " + cause.getMessage)

    super.exceptionCaught(ctx, cause)
  }
}
