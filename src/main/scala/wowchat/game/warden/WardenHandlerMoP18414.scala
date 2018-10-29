package wowchat.game.warden

import java.security.MessageDigest

import io.netty.buffer.{ByteBuf, PooledByteBufAllocator}
import wowchat.common.Packet

class WardenHandlerMoP18414(sessionKey: Array[Byte]) extends WardenHandler(sessionKey) {

  override protected val WARDEN_MODULE_LENGTH = 32

  override protected def getEncryptedMessageLength(msg: Packet): Int = {
    msg.byteBuf.readIntLE
  }

  override protected def formResponse(out: ByteBuf): ByteBuf = {
    val newLength = out.readableBytes + 4
    val withHeader = PooledByteBufAllocator.DEFAULT.buffer(newLength, newLength)
    withHeader.writeIntLE(out.readableBytes)
    withHeader.writeBytes(out)
    out.release
    withHeader
  }

  override protected def formCheatChecksRequestDigest(ret: ByteBuf, key: Array[Byte]): Unit = {
    val mdSHA1 = MessageDigest.getInstance("SHA1")
    mdSHA1.update(key)
    ret.writeBytes(mdSHA1.digest)

    val mdSHA256 = MessageDigest.getInstance("SHA-256")
    mdSHA256.update(key)
    ret.writeBytes(mdSHA256.digest)
  }
}
