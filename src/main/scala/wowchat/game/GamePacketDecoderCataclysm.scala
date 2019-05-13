package wowchat.game

import java.util.zip.Inflater

import io.netty.buffer.{ByteBuf, PooledByteBufAllocator}
import io.netty.channel.ChannelHandlerContext

class GamePacketDecoderCataclysm extends GamePacketDecoderWotLK with GamePacketsCataclysm15595 {

  protected val inflater: Inflater = new Inflater

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    inflater.end()
    super.channelInactive(ctx)
  }

  override def decompress(id: Int, byteBuf: ByteBuf): (Int, ByteBuf) = {
    if (isCompressed(id)) {
      val decompressedSize = getDecompressedSize(byteBuf)

      val compressed = new Array[Byte](byteBuf.readableBytes)
      byteBuf.readBytes(compressed)
      byteBuf.release
      val decompressed = new Array[Byte](decompressedSize)

      inflater.setInput(compressed)
      inflater.inflate(decompressed)

      val ret = PooledByteBufAllocator.DEFAULT.buffer(decompressed.length, decompressed.length)
      ret.writeBytes(decompressed)
      (getDecompressedId(id, ret), ret)
    } else {
      (id, byteBuf)
    }
  }

  def getDecompressedSize(byteBuf: ByteBuf): Int = {
    byteBuf.readIntLE
  }

  def getDecompressedId(id: Int, buf: ByteBuf): Int = {
    id ^ COMPRESSED_DATA_MASK
  }

  def isCompressed(id: Int): Boolean = {
    (id & COMPRESSED_DATA_MASK) == COMPRESSED_DATA_MASK
  }
}
