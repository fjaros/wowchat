package wowchat.common

import io.netty.buffer.{ByteBuf, ByteBufAllocator, EmptyByteBuf}

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

case class Packet(
  id: Int,
  byteBuf: ByteBuf = new EmptyByteBuf(ByteBufAllocator.DEFAULT)
) {

  def readString: String = {
    import scala.util.control.Breaks._

    val ret = ArrayBuffer.newBuilder[Byte]
    breakable {
      while (byteBuf.readableBytes > 0) {
        val value = byteBuf.readByte
        if (value == 0) {
          break
        }
        ret += value
      }
    }

    Source.fromBytes(ret.result.toArray, "UTF-8").mkString
  }

  def skipString: Packet = {
    while (byteBuf.readableBytes > 0 && byteBuf.readByte != 0) {}
    this
  }

  // bit manipulation for cata+
  private var bitPosition = 7
  private var byte: Byte = 0

  def readBit: Byte = {
    bitPosition += 1
    if (bitPosition > 7) {
      bitPosition = 0
      byte = byteBuf.readByte
    }

    (byte >> (7 - bitPosition) & 1).toByte
  }

  def readBits(length: Int): Int = {
    (length - 1 to 0 by -1).foldLeft(0) {
      case (result, i) => result | (readBit << i)
    }
  }

  def readXorByte(mask: Byte): Byte = {
    if (mask != 0) {
      (mask ^ byteBuf.readByte).toByte
    } else {
      mask
    }
  }
}
