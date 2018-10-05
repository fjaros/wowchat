package wowchat

import wowchat.common.{CommonConnectionCallback, Global, WowChatConfig}
import wowchat.discord.Discord
import wowchat.game.GameConnector
import wowchat.realm.{RealmConnectionCallback, RealmConnector}
import com.typesafe.scalalogging.StrictLogging

import scala.io.Source

object WoWChat extends StrictLogging {

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
      override def success(host: String, port: Int, realmId: Int, sessionKey: Array[Byte]): Unit =
        new GameConnector(host, port, realmId, sessionKey, gameEventCallback).connect

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
    val gitProperties = getClass.getClassLoader.getResourceAsStream("git.properties")
    Option(gitProperties).fold({
      logger.error("Failed to find git.properties file to check for new version!")
    })(gitProperties => {
      // This is JSON, but I really just didn't want to import a full blown JSON library for one string.
      val myCommitIdRegex = "\"git\\.commit\\.id\"\\s+:\\s+\"(.+?)\"".r
      val myCommitId = myCommitIdRegex
        .findFirstMatchIn(Source.fromInputStream(gitProperties).mkString)
        .map(m => m.group(1))
        .getOrElse("NOT FOUND")

      val repoCommitId = Source
        .fromURL("https://api.github.com/repos/fjaros/wowchat/commits/master")
        .dropWhile(_ != ':')
        .takeWhile(_ != ',')
        .drop(2)
        .takeWhile(_ != '"')
        .mkString

      if (myCommitId != repoCommitId) {
        logger.error( "~~~ !!!                YOUR WoWChat VERSION IS OUT OF DATE                !!! ~~~")
        logger.error(s"~~~ !!!  Current Version hash: $myCommitId   !!! ~~~")
        logger.error(s"~~~ !!!  Repo    Version hash: $repoCommitId   !!! ~~~")
        logger.error( "~~~ !!! RUN git pull OR GO TO https://github.com/fjaros/wowchat TO UPDATE !!! ~~~")
        logger.error( "~~~ !!!                YOUR WoWChat VERSION IS OUT OF DATE                !!! ~~~")
      }
    })
    gitProperties.close()
  }
}
