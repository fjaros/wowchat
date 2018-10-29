package wowchat.game

import io.netty.buffer.{ByteBuf, PooledByteBufAllocator}

// used for warden and wotlk header encryption
class RC4(key: Array[Byte]) {
  private val SBOX_LENGTH = 256
  private var sbox = initSBox(key)
  private var i = 0
  private var j = 0

  def cryptToByteArray(msg: Array[Byte]): Array[Byte] = {
    val code = new Array[Byte](msg.length)
    msg.indices.foreach(n => {
      i = (i + 1) % SBOX_LENGTH
      j = (j + sbox(i)) % SBOX_LENGTH
      swap(i, j, sbox)
      val rand = sbox((sbox(i) + sbox(j)) % SBOX_LENGTH)
      code(n) = (rand ^ msg(n).toInt).toByte
    })
    code
  }

  def crypt(msg: Array[Byte]): ByteBuf = {
    val byteBuf = PooledByteBufAllocator.DEFAULT.buffer(msg.length, msg.length)
    byteBuf.writeBytes(cryptToByteArray(msg))
  }

  def crypt(msg: Byte): ByteBuf = {
    crypt(Array(msg))
  }

  def crypt(msg: ByteBuf, length: Int): ByteBuf = {
    val arr = new Array[Byte](length)
    msg.readBytes(arr)
    crypt(arr)
  }

  private def initSBox(key: Array[Byte]) = {
    val sbox = new Array[Int](SBOX_LENGTH)
    var j = 0
    (0 until SBOX_LENGTH).foreach(i => sbox(i) = i)
    (0 until SBOX_LENGTH).foreach(i => {
      j = (j + sbox(i) + key(i % key.length) + SBOX_LENGTH) % SBOX_LENGTH
      swap(i, j, sbox)
    })
    sbox
  }

  private def swap(i: Int, j: Int, sbox: Array[Int]): Unit = {
    val temp = sbox(i)
    sbox(i) = sbox(j)
    sbox(j) = temp
  }
}
