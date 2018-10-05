package wowchat.realm

import wowchat.common.{ByteUtils, Packet}
import com.typesafe.scalalogging.StrictLogging
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class RealmPacketEncoder extends MessageToByteEncoder[Packet] with StrictLogging {

  override def encode(ctx: ChannelHandlerContext, msg: Packet, out: ByteBuf): Unit = {
    logger.debug(f"SEND REALM PACKET: ${msg.id}%04X - ${ByteUtils.toHexString(msg.byteBuf, true, false)}")

    out.writeByte(msg.id)
    out.writeBytes(msg.byteBuf)
  }
}
