package wowchat.game

import io.netty.buffer.ByteBuf
import wowchat.common.ByteUtils

class GamePacketDecoderMoP18414 extends GamePacketDecoder {

  override def parseGameHeader(in: ByteBuf): (Int, Int) = {
    val size = in.readShortLE - 2
    val id = in.readShortLE
    (id, size)
  }

  override def parseGameHeaderEncrypted(decrypted: Array[Byte]): (Int, Int) = {
    val raw = ByteUtils.bytesToLongLE(decrypted).toInt
    val id = raw & 0x1FFF
    val size = raw >>> 13
    (id, size)
  }
}
