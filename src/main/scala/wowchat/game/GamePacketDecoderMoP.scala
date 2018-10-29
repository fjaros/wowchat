package wowchat.game

import java.util.zip.Inflater

import io.netty.buffer.ByteBuf
import wowchat.common.ByteUtils

class GamePacketDecoderMoP extends GamePacketDecoderCataclysm with GamePacketsMoP18414 {

  // MoP compression does not have zlib header
  override protected val inflater: Inflater = new Inflater(true)

  override def parseGameHeader(in: ByteBuf): (Int, Int) = {
    val size = in.readShortLE - 2
    val id = in.readShortLE
    (id, size)
  }

  override def parseGameHeaderEncrypted(in: ByteBuf, crypt: GameHeaderCrypt): (Int, Int) = {
    val header = new Array[Byte](HEADER_LENGTH)
    in.readBytes(header)
    val decrypted = crypt.decrypt(header)
    val raw = ByteUtils.bytesToLongLE(decrypted).toInt
    val id = raw & 0x1FFF
    val size = raw >>> 13
    (id, size)
  }

  override def getDecompressedSize(byteBuf: ByteBuf): Int = {
    val size = byteBuf.readIntLE
    byteBuf.skipBytes(8) // skip adler checksums
    size
  }

  override def getDecompressedId(id: Int, buf: ByteBuf): Int = {
    val newId = buf.readShortLE
    buf.skipBytes(2)
    newId
  }

  override def isCompressed(id: Int): Boolean = {
    id == SMSG_COMPRESSED_DATA
  }
}
