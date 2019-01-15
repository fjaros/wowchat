package wowchat.common

import java.io.File
import java.util

import wowchat.common.ChatDirection.ChatDirection
import wowchat.common.WowExpansion.WowExpansion
import com.typesafe.config.{Config, ConfigFactory}
import wowchat.game.GamePackets

import scala.collection.JavaConverters._
import scala.reflect.runtime.universe.{TypeTag, typeOf}

case class WowChatConfig(discord: DiscordConfig, wow: Wow, guildConfig: GuildConfig, channels: Seq[ChannelConfig])
case class DiscordConfig(token: String, enableDotCommands: Boolean, enableCommandsChannels: Set[String])
case class Wow(platform: Platform.Value, realmlist: RealmListConfig, account: String, password: String, character: String)
case class RealmListConfig(name: String, host: String, port: Int)
case class GuildConfig(notificationConfigs: Map[String, GuildNotificationConfig])
case class GuildNotificationConfig(enabled: Boolean, format: String)
case class ChannelConfig(chatDirection: ChatDirection, wow: WowChannelConfig, discord: DiscordChannelConfig)
case class WowChannelConfig(tp: Byte, channel: Option[String] = None, format: String)
case class DiscordChannelConfig(channel: String, format: String)

object WowChatConfig extends GamePackets {

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
        discordConf.getString("token"),
        getOpt[Boolean](discordConf, "enable_dot_commands").getOrElse(true),
        getOpt[util.List[String]](discordConf, "enable_commands_channels")
          .getOrElse(new util.ArrayList[String]()).asScala.map(_.toLowerCase).toSet
      ),
      Wow(
        Platform.valueOf(getOpt[String](wowConf, "platform").getOrElse("Mac")),
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
      case "4.3.4" => 15595
      case "5.4.8" => 18414
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
      "promoted" -> (true, "`[%user] has promoted [%target] to [%rank].`"),
      "demoted" -> (true, "`[%user] has demoted [%target] to [%rank].`"),
      "online" -> (false, "`[%user] has come online.`"),
      "offline" -> (false, "`[%user] has gone offline.`"),
      "joined" -> (true, "`[%user] has joined the guild.`"),
      "left" -> (true, "`[%user] has left the guild.`"),
      "removed" -> (true, "`[%target] has been kicked out of the guild by [%user].`"),
      "motd" -> (true, "`Guild Message of the Day: %message`"),
      "achievement" -> (true, "%user has earned the achievement %achievement!")
    )

    guildConf.fold({
      GuildConfig(defaults.mapValues {
        case (enabled, format) => GuildNotificationConfig(enabled, format)
      })
    })(guildConf => {
      GuildConfig(
        defaults.keysIterator.map(key => {
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
          WowChannelConfig(ChatEvents.parse(channel.getString("wow.type")), wowChannel, getOpt[String](channel, "wow.format").getOrElse("")),
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
      Some(
        (if (typeOf[T] =:= typeOf[Boolean]) {
          cfg.getString(path).toLowerCase match {
            case "true" | "1" | "y" | "yes" => true
            case _ => false
          }
        } else {
          cfg.getAnyRef(path)
        }).asInstanceOf[T]
      )
    } else {
      None
    }
  }
}

object Platform extends Enumeration {
  type Platform = Value
  val Windows, Mac = Value

  def valueOf(platform: String): Platform = {
    platform.toLowerCase match {
      case "win" | "windows" => Windows
      case _ => Mac
    }
  }
}

object WowExpansion extends Enumeration {
  type WowExpansion = Value
  val Vanilla, TBC, WotLK, Cataclysm, MoP = Value

  def valueOf(version: String): WowExpansion = {
    if (version.startsWith("1.")) {
      WowExpansion.Vanilla
    } else if (version.startsWith("2.")) {
      WowExpansion.TBC
    } else if (version.startsWith("3.")) {
      WowExpansion.WotLK
    } else if (version == "4.3.4") {
      WowExpansion.Cataclysm
    } else if (version == "5.4.8") {
      WowExpansion.MoP
    } else {
      throw new IllegalArgumentException(s"Version $version not supported!")
    }
  }
}

object ChatDirection extends Enumeration {
  type ChatDirection = Value
  val both, wow_to_discord, discord_to_wow = Value
}
