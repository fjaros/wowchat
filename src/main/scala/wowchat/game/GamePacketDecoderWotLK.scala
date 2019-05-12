package wowchat.game

import io.netty.buffer.ByteBuf

class GamePacketDecoderWotLK extends GamePacketDecoder with GamePacketsWotLK {

  override def parseGameHeaderEncrypted(in: ByteBuf, crypt: GameHeaderCrypt): (Int, Int) = {
    val header = new Array[Byte](HEADER_LENGTH)
    in.readBytes(header)
    val decrypted = crypt.decrypt(header)

    // WotLK and later expansions have a variable size header. An extra byte is included if the size is > 0x7FFF
    if ((decrypted.head & 0x80) == 0x80) {
      val nextByte = crypt.decrypt(Array(in.readByte)).head
      val size = (((decrypted(0) & 0x7F) << 16) | ((decrypted(1) & 0xFF) << 8) | (decrypted(2) & 0xFF)) - 2
      val id = (nextByte & 0xFF) << 8 | decrypted(3) & 0xFF
      (id, size)
    } else {
      val size = ((decrypted(0) & 0xFF) << 8 | decrypted(1) & 0xFF) - 2
      val id = (decrypted(3) & 0xFF) << 8 | decrypted(2) & 0xFF
      (id, size)
    }
  }
}
