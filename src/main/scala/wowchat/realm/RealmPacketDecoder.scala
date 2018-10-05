package wowchat.realm

import java.util

import wowchat.common.{ByteUtils, Packet, WowChatConfig, WowExpansion}
import com.typesafe.scalalogging.StrictLogging
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder

class RealmPacketDecoder extends ByteToMessageDecoder with StrictLogging {

  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
    if (in.readableBytes == 0) {
      return
    }

    val id = in.readByte
    var size = 0
    id match {
      case RealmPackets.CMD_AUTH_LOGON_CHALLENGE =>
        if (in.readableBytes < 2) {
          return
        }

        in.markReaderIndex
        in.skipBytes(1)
        val result = in.readByte
        size = if (RealmPackets.AuthResult.isSuccess(result)) {
          118
        } else {
          2
        }
        in.resetReaderIndex
      case RealmPackets.CMD_AUTH_LOGON_PROOF =>
        if (in.readableBytes < 1) {
          return
        }

        // size is error dependent
        in.markReaderIndex
        val result = in.readByte
        size = if (RealmPackets.AuthResult.isSuccess(result)) {
          25
        } else {
          if (WowChatConfig.getExpansion == WowExpansion.Vanilla) 1 else 3
        }
        in.resetReaderIndex
      case RealmPackets.CMD_REALM_LIST =>
        if (in.readableBytes < 2) {
          return
        }

        size = in.readShortLE
    }

    if (size > in.readableBytes) {
      return
    }

    if (size > 0) {
      val byteBuf = in.readBytes(size)

      logger.debug(f"RECV REALM PACKET: $id%04X - ${ByteUtils.toHexString(byteBuf, true, false)}")

      out.add(Packet(id, byteBuf))
    }
  }
}
