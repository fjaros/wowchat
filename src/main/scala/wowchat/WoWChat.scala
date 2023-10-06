package wowchat

import java.util.concurrent.{Executors, TimeUnit}

import wowchat.common.{CommonConnectionCallback, Global, ReconnectDelay, WowChatConfig}
import wowchat.discord.Discord
import wowchat.game.GameConnector
import wowchat.realm.{RealmConnectionCallback, RealmConnector}
import com.typesafe.scalalogging.StrictLogging
import io.netty.channel.nio.NioEventLoopGroup

import scala.io.Source

object WoWChat extends StrictLogging {

  private val RELEASE = "v1.3.8"

  def main(args: Array[String]): Unit = {
    logger.info(s"Running WoWChat - $RELEASE")
    val confFile = if (args.nonEmpty) {
      args(0)
    } else {
      logger.info("No configuration file supplied. Trying with default wowchat.conf.")
      "wowchat.conf"
    }
    Global.config = WowChatConfig(confFile)

    try {
      checkForNewVersion
    } catch {
      case e: Exception => logger.error("Failed to check for a new version!", e)
    }

    val gameConnectionController: CommonConnectionCallback = new CommonConnectionCallback {

      private val reconnectExecutor = Executors.newSingleThreadScheduledExecutor
      private val reconnectDelay = new ReconnectDelay

      override def connect: Unit = {
        Global.group = new NioEventLoopGroup

        val realmConnector = new RealmConnector(new RealmConnectionCallback {
          override def success(host: String, port: Int, realmName: String, realmId: Int, sessionKey: Array[Byte]): Unit = {
            gameConnect(host, port, realmName, realmId, sessionKey)
          }

          override def disconnected: Unit = doReconnect

          override def error: Unit = sys.exit(1)
        })

        realmConnector.connect
      }

      private def gameConnect(host: String, port: Int, realmName: String, realmId: Int, sessionKey: Array[Byte]): Unit = {
        new GameConnector(host, port, realmName, realmId, sessionKey, this).connect
      }

      override def connected: Unit = reconnectDelay.reset

      override def disconnected: Unit = doReconnect

      def doReconnect: Unit = {
        Global.group.shutdownGracefully()
        Global.discord.changeRealmStatus("Connecting...")
        val delay = reconnectDelay.getNext
        logger.info(s"Disconnected from server! Reconnecting in $delay seconds...")

        reconnectExecutor.schedule(new Runnable {
          override def run(): Unit = connect
        }, delay, TimeUnit.SECONDS)
      }
    }

    logger.info("Connecting to Discord...")
    Global.discord = new Discord(new CommonConnectionCallback {
      override def connected: Unit = gameConnectionController.connect

      override def error: Unit = sys.exit(1)
    })
  }

  private def checkForNewVersion = {
    // This is JSON, but I really just didn't want to import a full blown JSON library for one string.
    val data = Source.fromURL("https://api.github.com/repos/fjaros/wowchat/releases/latest").mkString
    val regex = "\"tag_name\":\"(.+?)\",".r
    val repoTagName = regex
      .findFirstMatchIn(data)
      .map(_.group(1))
      .getOrElse("NOT FOUND")

    if (repoTagName != RELEASE) {
      logger.error( "~~~ !!!                YOUR WoWChat VERSION IS OUT OF DATE                !!! ~~~")
      logger.error(s"~~~ !!!                     Current Version:  $RELEASE                      !!! ~~~")
      logger.error(s"~~~ !!!                     Repo    Version:  $repoTagName                      !!! ~~~")
      logger.error( "~~~ !!! RUN git pull OR GO TO https://github.com/fjaros/wowchat TO UPDATE !!! ~~~")
      logger.error( "~~~ !!!                YOUR WoWChat VERSION IS OUT OF DATE                !!! ~~~")
    }
  }
}
