package wowchat.common

import com.typesafe.scalalogging.StrictLogging
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.handler.timeout.{IdleState, IdleStateEvent}

// i have no idea if this is a good idea to handle reconnections like this in netty
@Sharable
class ReconnectDelay extends ChannelInboundHandlerAdapter with StrictLogging {

  private var reconnectDelay: Option[Int] = None

  def getNext: Int = {
    synchronized {
      reconnectDelay = reconnectDelay.map {
        case 10 => 30
        case 30 => 60
        case 60 => 180
        case _ => 300
      }.orElse(Some(10))

      val result = reconnectDelay.get
      logger.debug(s"GET RECONNECT DELAY $result")
      result
    }
  }

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    reconnectDelay = None
    logger.debug("GAME RECONNECTOR ACTIVE")
    super.channelActive(ctx)
  }

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

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    println("Excsdfp caught")
  }
}
