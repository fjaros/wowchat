package wowchat

import wowchat.common.{CommonConnectionCallback, Global, WowChatConfig}
import wowchat.discord.Discord
import wowchat.game.GameConnector
import wowchat.realm.{RealmConnectionCallback, RealmConnector}
import com.typesafe.scalalogging.StrictLogging

import scala.io.Source

object WoWChat extends StrictLogging {

  private val RELEASE = "v1.2.0"

  def main(args: Array[String]): Unit = {
    val confFile = if (args.nonEmpty) {
      args(0)
    } else {
      logger.info("No configuration file supplied. Trying with default wowchat.conf.")
      "wowchat.conf"
    }
    Global.config = WowChatConfig(confFile)

    checkForNewVersion

    val gameEventCallback = new CommonConnectionCallback {

      override def reconnected: Unit = Global.discord.sendGuildNotification("Reconnected to WoW!")

      override def disconnected: Unit = Global.discord.sendGuildNotification("Disconnected from WoW!")
    }

    val realmConnector = new RealmConnector(new RealmConnectionCallback {
      override def success(host: String, port: Int, realmName: String, realmId: Int, sessionKey: Array[Byte]): Unit =
        new GameConnector(host, port, realmName, realmId, sessionKey, gameEventCallback).connect

      override def error: Unit = sys.exit(1)
    })

    Global.discord = new Discord(new CommonConnectionCallback {
      override def connected: Unit = realmConnector.connect

      override def reconnected: Unit = Global.game.foreach(_.sendNotification("Reconnected to Discord!"))

      override def disconnected: Unit = Global.game.foreach(_.sendNotification("Disconnected from Discord!"))

      override def error: Unit = sys.exit(1)
    })
  }

  def checkForNewVersion: Unit = {
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
