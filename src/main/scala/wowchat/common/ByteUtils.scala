package wowchat.common

import io.netty.buffer.{ByteBuf, PooledByteBufAllocator}

object ByteUtils {

  def shortToBytes(short: Int): Array[Byte] = {
    Array(
      (short >> 8).toByte,
      short.toByte
    )
  }

  def shortToBytesLE(short: Int): Array[Byte] = {
    Array(
      short.toByte,
      (short >> 8).toByte,
    )
  }

  def intToBytes(int: Int): Array[Byte] = {
    Array(
      (int >> 24).toByte,
      (int >> 16).toByte,
      (int >> 8).toByte,
      int.toByte
    )
  }

  def intToBytesLE(int: Int): Array[Byte] = {
    Array(
      int.toByte,
      (int >> 8).toByte,
      (int >> 16).toByte,
      (int >> 24).toByte
    )
  }

  def longToBytes(long: Long): Array[Byte] = {
    Array(
      (long >> 56).toByte,
      (long >> 48).toByte,
      (long >> 40).toByte,
      (long >> 32).toByte,
      (long >> 24).toByte,
      (long >> 16).toByte,
      (long >> 8).toByte,
      long.toByte
    )
  }

  def longToBytesLE(long: Long): Array[Byte] = {
    Array(
      long.toByte,
      (long >> 8).toByte,
      (long >> 16).toByte,
      (long >> 24).toByte,
      (long >> 32).toByte,
      (long >> 40).toByte,
      (long >> 48).toByte,
      (long >> 56).toByte
    )
  }

  def stringToInt(str: String): Int = {
    bytesToLong(str.getBytes("UTF-8")).toInt
  }

  def bytesToLong(bytes: Array[Byte]): Long = {
    bytes
      .reverseIterator
      .zipWithIndex
      .foldLeft(0L) {
        case (result, (byte, i)) =>
          result | ((byte & 0xFFL) << (i * 8))
      }
  }

  def bytesToLongLE(bytes: Array[Byte]): Long = {
    bytes
      .zipWithIndex
      .foldLeft(0L) {
        case (result, (byte, i)) =>
          result | ((byte & 0xFFL) << (i * 8))
      }
  }

  def toHexString(bytes: Array[Byte]): String = {
    val byteBuf = PooledByteBufAllocator.DEFAULT.buffer(bytes.length, bytes.length)
    byteBuf.writeBytes(bytes)
    val ret = toHexString(byteBuf, true, false)
    byteBuf.release
    ret
  }

  def toHexString(byteBuf: ByteBuf, addSpaces: Boolean = false, resolvePlainText: Boolean = true): String = {
    val ret = StringBuilder.newBuilder

    val copy = byteBuf.copy
    while (copy.readableBytes > 0) {
      val byte = copy.readByte
      if (resolvePlainText && byte >= 0x20 && byte < 0x7F) {
        ret ++= byte.toChar + " "
      } else {
        ret ++= f"$byte%02X"
      }
      if (addSpaces)
        ret += ' '
    }
    copy.release
    ret.mkString.trim
  }

  def toBinaryString(byteBuf: ByteBuf): String = {
    val ret = StringBuilder.newBuilder

    val copy = byteBuf.copy
    var i = 0
    while (copy.readableBytes > 0) {
      val byte = copy.readByte
      if (i != 0 && i % 4 == 0) {
        ret ++= System.lineSeparator
      }
      ret ++= f"${(byte & 0xFF).toBinaryString.toInt}%08d "
      i += 1
    }
    copy.release
    ret.mkString.trim
  }
}
