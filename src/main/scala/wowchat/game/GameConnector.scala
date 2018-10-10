package wowchat.game

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import wowchat.common._
import com.typesafe.scalalogging.StrictLogging
import io.netty.bootstrap.Bootstrap
import io.netty.channel.{Channel, ChannelInitializer}
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.timeout.IdleStateHandler
import io.netty.util.concurrent.{Future, GenericFutureListener}

import scala.util.Try

class GameConnector(host: String,
                    port: Int,
                    realmId: Int,
                    sessionKey: Array[Byte],
                    gameEventCallback: CommonConnectionCallback)
  extends Connector with GamePackets with StrictLogging {

  sys.addShutdownHook({
    disconnect
  })

  private val connectionProblemListener = new ConnectionProblemListener
  private val disconnectListener = new DisconnectListener
  private var channel: Option[Channel] = None
  private var connected: Boolean = false
  var handler: Option[GamePacketHandler] = None

  // ugliest code to handle this crap here probably. i just don't know how to use netty.
  // seems way to complicated to detect connection problems
  private class ConnectionProblemListener extends GenericFutureListener[Future[_ >: Void]] {

    override def operationComplete(future: Future[_ >: Void]): Unit = {
      Try {
        future.get(10, TimeUnit.SECONDS)
      }.fold(throwable => {
        logger.error("Failed to connect to game server! " + throwable.getMessage)
        disconnectListener.operationComplete(future)
      }, _ => Unit)
    }
  }

  private class DisconnectListener extends GenericFutureListener[Future[_ >: Void]] {

    val reconnectDelay = new ReconnectDelay

    override def operationComplete(future: Future[_ >: Void]): Unit = {
      connected = false
      val delay = reconnectDelay.getNext
      logger.info(s"Disconnected from game server! Reconnecting in $delay seconds...")
      channel.get.eventLoop().schedule(new Runnable {
        override def run(): Unit = connect
      }, delay, TimeUnit.SECONDS)
    }
  }

  override def isConnected: Boolean = connected

  override def connect: Unit = {
    if (isConnected) {
      logger.error("Refusing to connect to game server. Connection already exists.")
      return
    }

    logger.info(s"Logging into game server ${Global.config.wow.realmlist.name} ($host:$port)")

    val bootstrap = new Bootstrap
    bootstrap.group(Global.group)
      .channel(classOf[NioSocketChannel])
      .remoteAddress(new InetSocketAddress(host, port))
      .handler(new ChannelInitializer[SocketChannel]() {

        @throws[Exception]
        override protected def initChannel(socketChannel: SocketChannel): Unit = {
          val encoder = WowChatConfig.getExpansion match {
            case WowExpansion.Cataclysm => new GamePacketEncoderCataclysm
            case _ => new GamePacketEncoder
          }

          handler = Some(
            WowChatConfig.getExpansion match {
              case WowExpansion.Vanilla =>
                socketChannel.attr(CRYPT).set(new GameHeaderCrypt)
                new GamePacketHandler(realmId, sessionKey, gameEventCallback)
              case WowExpansion.TBC =>
                socketChannel.attr(CRYPT).set(new GameHeaderCryptTBC)
                new GamePacketHandlerTBC(realmId, sessionKey, gameEventCallback)
              case WowExpansion.WotLK =>
                socketChannel.attr(CRYPT).set(new GameHeaderCryptWotLK)
                new GamePacketHandlerWotLK(realmId, sessionKey, gameEventCallback)
              case WowExpansion.Cataclysm =>
                socketChannel.attr(CRYPT).set(new GameHeaderCryptWotLK)
                new GamePacketHandlerCataclysm(realmId, sessionKey, gameEventCallback)
            }
          )

          socketChannel.pipeline.addLast(
            new IdleStateHandler(60, 120, 0),
            disconnectListener.reconnectDelay,
            new GamePacketDecoder,
            encoder,
            handler.get)
        }
      })

    channel = Some(bootstrap.connect.addListener(connectionProblemListener).sync.channel)
    connected = channel.get.isActive
    // do not sync on close future here. this will thread lock rest of the program
    channel.get.closeFuture.addListener(disconnectListener)
  }

  override def disconnect: Unit = {
    channel.foreach(channel => {
      channel.closeFuture.removeListener(disconnectListener)
      handler.get.sendLogout.foreach(_.await)
      channel.close
    })
    channel = None
  }
}
