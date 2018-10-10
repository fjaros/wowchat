package wowchat.common

import io.netty.buffer.ByteBuf

class BitUnpack(in: ByteBuf) {

  var pos = 8
  var value: Byte = 0

  def get: Byte = {
    if (pos == 8) {
      value = in.readByte
      pos = 0
    }

    val ret = value
    value = (2 * ret).toByte
    pos += 1

    ((ret >> 7) & 1).toByte
  }

  def skip(bits: Int): Unit = {
    (0 until bits).foreach(i => {
      if (pos == 8) {
        in.readByte
        pos = 0
      }
      pos += 1
    })
  }
}
