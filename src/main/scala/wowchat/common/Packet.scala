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
}
