package wowchat.common

import com.typesafe.scalalogging.StrictLogging
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.handler.timeout.{IdleState, IdleStateEvent}

class IdleStateCallback extends ChannelInboundHandlerAdapter with StrictLogging {

  override def userEventTriggered(ctx: ChannelHandlerContext, evt: scala.Any): Unit = {
    evt match {
      case event: IdleStateEvent =>
        val idler = event.state match {
          case IdleState.READER_IDLE => "reader"
          case IdleState.WRITER_IDLE => "writer"
          case _ => "all"
        }
        logger.error(s"Network state for $idler marked as idle!")
        ctx.close
      case _ =>
    }

    super.userEventTriggered(ctx, evt)
  }
}
