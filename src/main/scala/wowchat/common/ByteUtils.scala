package wowchat.common

import io.netty.buffer.ByteBuf

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

  def stringToInt(str: String): Int = {
    bytesToLong(str.getBytes).toInt
  }

  def bytesToLong(bytes: Array[Byte]): Long = {
    bytes
      .reverseIterator
      .zipWithIndex
      .foldLeft(0L) {
        case (result, (byte, i)) =>
          result | ((byte & 0xFF) << (i * 8))
      }
  }

  def bytesToLongLE(bytes: Array[Byte]): Long = {
    bytes
      .zipWithIndex
      .foldLeft(0L) {
        case (result, (byte, i)) =>
          result | ((byte & 0xFF) << (i * 8))
      }
  }

  def toHexString(byteBuf: ByteBuf, addSpaces: Boolean = false, resolvePlainText: Boolean = true): String = {
    val ret = StringBuilder.newBuilder

    val copy = byteBuf.copy()
    while (copy.readableBytes() > 0) {
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
}
