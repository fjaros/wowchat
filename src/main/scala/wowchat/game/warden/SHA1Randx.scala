package wowchat.game.warden

import java.security.MessageDigest

class SHA1Randx(sessionKey: Array[Byte]) {

  private val md = MessageDigest.getInstance("SHA1")
  private val keyHalfSize = sessionKey.length / 2

  md.update(sessionKey, 0, keyHalfSize)
  private val o1 = md.digest

  md.update(sessionKey, keyHalfSize, keyHalfSize)
  private val o2 = md.digest

  private var o0 = new Array[Byte](20)
  private var index = 0
  fillUp

  private def fillUp: Unit = {
    md.update(o1)
    md.update(o0)
    md.update(o2)

    o0 = md.digest
    index = 0
  }

  def generate(size: Int): Array[Byte] = {
    (0 until size).map(i => {
      if (index >= o0.length) {
        fillUp
      }
      val ret = o0(index)
      index += 1
      ret
    }).toArray
  }
}
