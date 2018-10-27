package wowchat.game

import java.util

import wowchat.common.{ByteUtils, Packet}
import com.typesafe.scalalogging.StrictLogging
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder

class GamePacketDecoder extends ByteToMessageDecoder with GamePackets with StrictLogging {

  protected val HEADER_LENGTH = 4

  private var size = 0
  private var id = 0

  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
    if (in.readableBytes < HEADER_LENGTH) {
      return
    }

    val crypt = ctx.channel.attr(CRYPT).get

    if (size == 0 && id == 0) {
      // decrypt if necessary
      val tuple = if (crypt.isInit) {
        parseGameHeaderEncrypted(in, crypt)
      } else {
        parseGameHeader(in)
      }
      id = tuple._1
      size = tuple._2
    }

    if (size > in.readableBytes) {
      return
    }

    val byteBuf = in.readBytes(size)

    // decompress if necessary
    val (newId, decompressed) = decompress(id, byteBuf)

    val packet = Packet(newId, decompressed)

    logger.debug(f"RECV PACKET: $newId%04X - ${ByteUtils.toHexString(decompressed, true, false)}")

    out.add(packet)
    size = 0
    id = 0
  }

  def parseGameHeader(in: ByteBuf): (Int, Int) = {
    val size = in.readShort - 2
    val id = in.readShortLE
    (id, size)
  }

  def parseGameHeaderEncrypted(in: ByteBuf, crypt: GameHeaderCrypt): (Int, Int) = {
    val header = new Array[Byte](HEADER_LENGTH)
    in.readBytes(header)
    val decrypted = crypt.decrypt(header)
    val size = ((decrypted(0) & 0xFF) << 8 | decrypted(1) & 0xFF) - 2
    val id = (decrypted(3) & 0xFF) << 8 | decrypted(2) & 0xFF
    (id, size)
  }

  // vanilla has no compression. starts in cata/mop
  def decompress(id: Int, in: ByteBuf): (Int, ByteBuf) = {
    (id, in)
  }
}
