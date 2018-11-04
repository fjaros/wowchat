package wowchat.game

import java.net.InetSocketAddress

import wowchat.common._
import com.typesafe.scalalogging.StrictLogging
import io.netty.bootstrap.Bootstrap
import io.netty.channel.{Channel, ChannelInitializer, ChannelOption}
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.timeout.IdleStateHandler

class GameConnector(host: String,
                    port: Int,
                    realmName: String,
                    realmId: Int,
                    sessionKey: Array[Byte],
                    gameEventCallback: CommonConnectionCallback)
  extends GamePackets with StrictLogging {

  sys.addShutdownHook({
    disconnect
  })

  private var channel: Option[Channel] = None
  var handler: Option[GamePacketHandler] = None

  def connect: Unit = {
    if (channel.fold(false)(_.isActive)) {
      logger.error("Refusing to connect to game server. Connection already exists.")
      return
    }

    logger.info(s"Connecting to game server $realmName ($host:$port)")

    val bootstrap = new Bootstrap
    bootstrap.group(Global.group)
      .channel(classOf[NioSocketChannel])
      .option[java.lang.Integer](ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
      .option[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)
      .remoteAddress(new InetSocketAddress(host, port))
      .handler(new ChannelInitializer[SocketChannel]() {

        @throws[Exception]
        override protected def initChannel(socketChannel: SocketChannel): Unit = {
          val encoder = WowChatConfig.getExpansion match {
            case WowExpansion.Cataclysm => new GamePacketEncoderCataclysm
            case WowExpansion.MoP => new GamePacketEncoderMoP
            case _ => new GamePacketEncoder
          }

          val decoder = WowChatConfig.getExpansion match {
            case WowExpansion.Cataclysm => new GamePacketDecoderCataclysm
            case WowExpansion.MoP => new GamePacketDecoderMoP
            case _ => new GamePacketDecoder
          }

          handler = Some(
            WowChatConfig.getExpansion match {
              case WowExpansion.Vanilla =>
                socketChannel.attr(CRYPT).set(new GameHeaderCrypt)
                new GamePacketHandler(realmId, realmName, sessionKey, gameEventCallback)
              case WowExpansion.TBC =>
                socketChannel.attr(CRYPT).set(new GameHeaderCryptTBC)
                new GamePacketHandlerTBC(realmId, realmName, sessionKey, gameEventCallback)
              case WowExpansion.WotLK =>
                socketChannel.attr(CRYPT).set(new GameHeaderCryptWotLK)
                new GamePacketHandlerWotLK(realmId, realmName, sessionKey, gameEventCallback)
              case WowExpansion.Cataclysm =>
                socketChannel.attr(CRYPT).set(new GameHeaderCryptWotLK)
                new GamePacketHandlerCataclysm15595(realmId, realmName, sessionKey, gameEventCallback)
              case WowExpansion.MoP =>
                socketChannel.attr(CRYPT).set(new GameHeaderCryptMoP)
                new GamePacketHandlerMoP18414(realmId, realmName, sessionKey, gameEventCallback)
            }
          )

          socketChannel.pipeline.addLast(
            new IdleStateHandler(60, 120, 0),
            new IdleStateCallback,
            decoder,
            encoder,
            handler.get)
        }
      })

    channel = Some(bootstrap.connect.channel)
  }

  def disconnect: Unit = {
    if (Global.group.isShuttingDown || Global.group.isShutdown || Global.group.isTerminated) {
      return
    }

    channel.foreach(channel => {
      handler.get.sendLogout.foreach(_.await)
      channel.close
    })
    channel = None
  }
}
