package wowchat.game

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class GameHeaderCryptTBC extends GameHeaderCrypt {

  override def init(key: Array[Byte]): Unit = {
    super.init(key)

    val hmacSeed = Array(
      0x38, 0xA7, 0x83, 0x15, 0xF8, 0x92, 0x25, 0x30, 0x71, 0x98, 0x67, 0xB1, 0x8C, 0x04, 0xE2, 0xAA
    ).map(_.toByte)
    val md = Mac.getInstance("HmacSHA1")
    md.init(new SecretKeySpec(hmacSeed, "HmacSHA1"))
    md.update(key)
    _key = md.doFinal()
  }
}
