package wowchat.game

/**
  * This class was taken from JaNGOS project and ported to Scala.
  * Look at JaNGOSRealm for further documentation about the algorithm used here
  *
  * https://github.com/Warkdev/JaNGOSRealm
  */
class GameHeaderCrypt {

  private var _initialized = false
  private var _send_i = 0
  private var _send_j = 0
  private var _recv_i = 0
  private var _recv_j = 0
  protected var _key: Array[Byte] = _

  def decrypt(data: Array[Byte]): Array[Byte] = {
    if (!_initialized) {
      return data
    }

    data.indices.foreach(i => {
      _recv_i %= _key.length
      val x = ((data(i) - _recv_j) ^ _key(_recv_i)).toByte
      _recv_i += 1
      _recv_j = data(i)
      data(i) = x
    })

    data
  }

  def encrypt(data: Array[Byte]): Array[Byte] = {
    if (!_initialized) {
      return data
    }

    data.indices.foreach(i => {
      _send_i %= _key.length
      val x = ((data(i) ^ _key(_send_i)) + _send_j).toByte
      _send_i += 1
      data(i) = x
      _send_j = x
    })

    data
  }

  def init(key: Array[Byte]): Unit = {
    _key = key
    _send_i = 0
    _send_j = 0
    _recv_i = 0
    _recv_j = 0
    _initialized = true
  }

  def isInit: Boolean = {
    _initialized
  }
}
