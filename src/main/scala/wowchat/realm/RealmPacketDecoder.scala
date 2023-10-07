package wowchat.realm

import java.util

import wowchat.common.{ByteUtils, Packet, WowChatConfig, WowExpansion}
import com.typesafe.scalalogging.StrictLogging
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder

class RealmPacketDecoder extends ByteToMessageDecoder with StrictLogging {

  private var size = 0
  private var id = 0

  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
    if (in.readableBytes == 0) {
      return
    }

    if (size == 0 && id == 0) {
      in.markReaderIndex
      id = in.readByte
      id match {
        case RealmPackets.CMD_AUTH_LOGON_CHALLENGE =>
          if (in.readableBytes < 2) {
            in.resetReaderIndex
            return
          }

          in.markReaderIndex
          in.skipBytes(1)
          val result = in.readByte
          size = if (RealmPackets.AuthResult.isSuccess(result)) {
            in.skipBytes(115)
            val securityFlags = in.readByte
            var securityLength = 0
            if ((securityFlags & 0x01) == 0x01) { // PIN
              securityLength += 20
            }
            if ((securityFlags & 0x02) == 0x02) { // Matrix
              securityLength += 12
            }
            if ((securityFlags & 0x04) == 0x04) { // Security token
              securityLength += 1
            }
            118 + securityLength
          } else {
            2
          }
          in.resetReaderIndex
        case RealmPackets.CMD_AUTH_LOGON_PROOF =>
          if (in.readableBytes < 1) {
            in.resetReaderIndex
            return
          }

          // size is error dependent
          in.markReaderIndex
          val result = in.readByte
          size = if (RealmPackets.AuthResult.isSuccess(result)) {
            if (WowChatConfig.getExpansion == WowExpansion.Vanilla) 25 else 31
          } else {
            // A failure authentication result should be 1 byte in length for vanilla and 3 bytes for other expansions.
            // Some servers send back a malformed 1 byte response even for later expansions.
            if (in.readableBytes == 0) 1 else 3
          }
          in.resetReaderIndex
        case RealmPackets.CMD_REALM_LIST =>
          if (in.readableBytes < 2) {
            in.resetReaderIndex
            return
          }

          size = in.readShortLE
      }
    }

    if (size > in.readableBytes) {
      return
    }

    val byteBuf = in.readBytes(size)
    val packet = Packet(id, byteBuf)

    logger.debug(f"RECV REALM PACKET: $id%04X - ${ByteUtils.toHexString(byteBuf, true, false)}")

    out.add(packet)
    size = 0
    id = 0
  }
}
