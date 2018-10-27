package wowchat.game

import java.util.zip.Inflater

import io.netty.buffer.{ByteBuf, PooledByteBufAllocator}
import io.netty.channel.ChannelHandlerContext

class GamePacketDecoderCataclysm extends GamePacketDecoder with GamePacketsCataclysm15595 {

  protected val inflater: Inflater = new Inflater

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    inflater.end()
    super.channelInactive(ctx)
  }

  override def parseGameHeaderEncrypted(in: ByteBuf, crypt: GameHeaderCrypt): (Int, Int) = {
    val header = new Array[Byte](HEADER_LENGTH)
    in.readBytes(header)
    val decrypted = crypt.decrypt(header)
    if ((decrypted.head & 0x80) == 0x80) {
      val nextByte = crypt.decrypt(Array(in.readByte)).head
      val size = ((decrypted(0) & 0x7F) << 16) | (decrypted(1) << 8) | (decrypted(2) & 0xFF)
      val id = (nextByte & 0xFF) << 8 | decrypted(3) & 0xFF
      (id, size)
    } else {
      val size = ((decrypted(0) & 0xFF) << 8 | decrypted(1) & 0xFF) - 2
      val id = (decrypted(3) & 0xFF) << 8 | decrypted(2) & 0xFF
      (id, size)
    }
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
