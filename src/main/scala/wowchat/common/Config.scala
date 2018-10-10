package wowchat.common

import java.io.File

import wowchat.common.ChatDirection.ChatDirection
import wowchat.common.WowExpansion.WowExpansion
import wowchat.game.GamePackets
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConverters._
import scala.reflect.runtime.universe.{typeOf, TypeTag}

case class WowChatConfig(discord: DiscordConfig, wow: Wow, guildConfig: GuildConfig, channels: Seq[ChannelConfig])
case class DiscordConfig(token: String)
case class Wow(realmlist: RealmListConfig, account: String, password: String, character: String)
case class RealmListConfig(name: String, host: String, port: Int)
case class GuildConfig(notificationConfigs: Map[String, GuildNotificationConfig])
case class GuildNotificationConfig(enabled: Boolean, format: String)
case class ChannelConfig(chatDirection: ChatDirection, wow: WowChannelConfig, discord: DiscordChannelConfig)
case class WowChannelConfig(tp: Byte, channel: Option[String] = None, format: String)
case class DiscordChannelConfig(channel: String, format: String)

object WowChatConfig {

  private var version: String = _
  private var expansion: WowExpansion = _

  def apply(confFile: String): WowChatConfig = {
    val file = new File(confFile)
    val config = if (file.exists) {
      ConfigFactory.parseFile(file)
    } else {
      ConfigFactory.load(confFile)
    }

    val discordConf = config.getConfig("discord")
    val wowConf = config.getConfig("wow")
    val guildConf = if (config.hasPath("guild")) {
      Some(config.getConfig("guild"))
    } else {
      None
    }
    val channelsConf = config.getConfig("chat")

    // we gotta load this first to initialize constants that change between versions :OMEGALUL:
    version = getOpt(wowConf, "version").getOrElse("1.12.1")
    expansion = WowExpansion.valueOf(version)

    WowChatConfig(
      DiscordConfig(
        discordConf.getString("token")
      ),
      Wow(
        parseRealmlist(wowConf),
        wowConf.getString("account"),
        wowConf.getString("password"),
        wowConf.getString("character")
      ),
      parseGuildConfig(guildConf),
      parseChannels(channelsConf)
    )
  }

  lazy val getVersion = version
  lazy val getExpansion = expansion

  lazy val getBuild: Int = {
    version match {
      case "1.11.2" => 5464
      case "1.12.1" => 5875
      case "1.12.2" => 6005
      case "1.12.3" => 6141
      case "2.4.3" => 8606
      case "3.2.2" => 10505
      case "3.3.0" => 11159
      case "3.3.2" => 11403
      case "3.3.3" => 11723
      case "3.3.5" => 12340
    }
  }

  private def parseRealmlist(wowConf: Config): RealmListConfig = {
    val realmlist = wowConf.getString("realmlist")
    val splt = realmlist.split(":", 2)
    val (host, port) =
      if (splt.length == 1) {
        (splt.head, 3724)
      } else {
        (splt.head, splt(1).toInt)
      }

    RealmListConfig(wowConf.getString("realm"), host, port)
  }

  private def parseGuildConfig(guildConf: Option[Config]): GuildConfig = {
    // make reasonable defaults for old config
    val defaults = Map(
      "online" -> (false, "`[%user] has come online.`"),
      "offline" -> (false, "`[%user] has gone offline.`"),
      "joined" -> (true, "`[%user] has joined the guild.`"),
      "left" -> (true, "`[%user] has left the guild.`")
    )

    guildConf.fold({
      GuildConfig(defaults.mapValues {
        case (enabled, format) => GuildNotificationConfig(enabled, format)
      })
    })(guildConf => {
      GuildConfig(
        Seq("online", "offline", "joined", "left").map(key => {
          val conf = getConfigOpt(guildConf, key)
          val default = defaults(key)
          key -> conf.fold(GuildNotificationConfig(default._1, default._2))(conf => {
            GuildNotificationConfig(
              getOpt[Boolean](conf, "enabled").getOrElse(default._1),
              getOpt[String](conf, "format").getOrElse(default._2)
            )
          })
        })
          .toMap
      )
    })
  }

  private def parseChannels(channelsConf: Config): Seq[ChannelConfig] = {
    channelsConf.getObjectList("channels").asScala
      .map(_.toConfig)
      .map(channel => {
        val wowChannel = getOpt[String](channel, "wow.channel")

        ChannelConfig(
          ChatDirection.withName(channel.getString("direction")),
          WowChannelConfig(GamePackets.ChatEvents.parse(channel.getString("wow.type")), wowChannel, channel.getString("wow.format")),
          DiscordChannelConfig(channel.getString("discord.channel"), channel.getString("discord.format"))
        )
    })
  }

  private def getConfigOpt(cfg: Config, path: String): Option[Config] = {
    if (cfg.hasPath(path)) {
      Some(cfg.getConfig(path))
    } else {
      None
    }
  }

  private def getOpt[T : TypeTag](cfg: Config, path: String): Option[T] = {
    if (cfg.hasPath(path)) {
      // evil smiley face :)
      if (typeOf[T] =:= typeOf[Boolean]) {
        val str = cfg.getString(path).toLowerCase
        Some((str match {
          case "true" | "1" | "y" | "yes" => true
          case _ => false
        }).asInstanceOf[T])
      } else {
        Some(cfg.getAnyRef(path).asInstanceOf[T])
      }
    } else {
      None
    }
  }
}

object WowExpansion extends Enumeration {
  type WowExpansion = Value
  val Vanilla, TBC, WotLK = Value

  def valueOf(version: String): WowExpansion = {
    if (version.startsWith("1.")) {
      WowExpansion.Vanilla
    } else if (version.startsWith("2.")) {
      WowExpansion.TBC
    } else if (version.startsWith("3.")) {
      WowExpansion.WotLK
    } else {
      throw new IllegalArgumentException(s"Version $version not supported!")
    }
  }
}

object ChatDirection extends Enumeration {
  type ChatDirection = Value
  val both, wow_to_discord, discord_to_wow = Value
}
