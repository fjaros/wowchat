package wowchat.common

import java.io.File
import java.util

import wowchat.common.ChatDirection.ChatDirection
import wowchat.common.WowExpansion.WowExpansion
import com.typesafe.config.{Config, ConfigFactory}
import wowchat.game.GamePackets

import scala.collection.JavaConverters._
import scala.reflect.runtime.universe.{TypeTag, typeOf}

case class WowChatConfig(discord: DiscordConfig, wow: Wow, guildConfig: GuildConfig, channels: Seq[ChannelConfig], filters: Option[FiltersConfig])
case class DiscordConfig(token: String, enableDotCommands: Boolean, dotCommandsWhitelist: Set[String], enableCommandsChannels: Set[String], enableTagFailedNotifications: Boolean, itemDatabase: Option[String])
case class Wow(locale: String, platform: Platform.Value, realmBuild: Option[Int], gameBuild: Option[Int], realmlist: RealmListConfig, account: Array[Byte], password: String, character: String, enableServerMotd: Boolean)
case class RealmListConfig(name: String, host: String, port: Int)
case class GuildConfig(notificationConfigs: Map[String, GuildNotificationConfig])
case class GuildNotificationConfig(enabled: Boolean, format: String, channel: Option[String])
case class ChannelConfig(chatDirection: ChatDirection, wow: WowChannelConfig, discord: DiscordChannelConfig)
case class WowChannelConfig(id: Option[Int], tp: Byte, channel: Option[String] = None, format: String, filters: Option[FiltersConfig])
case class DiscordChannelConfig(channel: String, format: String, filters: Option[FiltersConfig])
case class FiltersConfig(enabled: Boolean, patterns: Seq[String])

object WowChatConfig extends GamePackets {

  private var version: String = _
  private var expansion: WowExpansion = _

  def apply(confFile: String): WowChatConfig = {
    val file = new File(confFile)
    val config = (if (file.exists) {
      ConfigFactory.parseFile(file)
    } else {
      ConfigFactory.load(confFile)
    }).resolve

    val discordConf = config.getConfig("discord")
    val wowConf = config.getConfig("wow")
    val guildConf = getConfigOpt(config, "guild")
    val channelsConf = config.getConfig("chat")
    val filtersConf = getConfigOpt(config, "filters")

    // we gotta load this first to initialize constants that change between versions :OMEGALUL:
    version = getOpt(wowConf, "version").getOrElse("1.12.1")
    expansion = WowExpansion.valueOf(version)

    WowChatConfig(
      DiscordConfig(
        discordConf.getString("token"),
        getOpt[Boolean](discordConf, "enable_dot_commands").getOrElse(true),
        getOpt[util.List[String]](discordConf, "dot_commands_whitelist")
          .getOrElse(new util.ArrayList[String]()).asScala.map(_.toLowerCase).toSet,
        getOpt[util.List[String]](discordConf, "enable_commands_channels")
          .getOrElse(new util.ArrayList[String]()).asScala.map(_.toLowerCase).toSet,
        getOpt[Boolean](discordConf, "enable_tag_failed_notifications").getOrElse(true),
        getOpt[String](discordConf, "item_database")
      ),
      Wow(
        getOpt[String](wowConf, "locale").getOrElse("enUS"),
        Platform.valueOf(getOpt[String](wowConf, "platform").getOrElse("Mac")),
        getOpt[Int](wowConf, "realm_build").orElse(getOpt[Int](wowConf, "build")),
        getOpt[Int](wowConf, "game_build").orElse(getOpt[Int](wowConf, "build")),
        parseRealmlist(wowConf),
        convertToUpper(wowConf.getString("account")),
        wowConf.getString("password"),
        wowConf.getString("character"),
        getOpt[Boolean](wowConf, "enable_server_motd").getOrElse(true)
      ),
      parseGuildConfig(guildConf),
      parseChannels(channelsConf),
      parseFilters(filtersConf)
    )
  }

  lazy val getVersion = version
  lazy val getExpansion = expansion

  private lazy val buildFromVersion: Int =
    version match {
      case "1.6.1" => 4544
      case "1.6.2" => 4565
      case "1.6.3" => 4620
      case "1.7.1" => 4695
      case "1.8.4" => 4878
      case "1.9.4" => 5086
      case "1.10.2" => 5302
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
      case _ => throw new IllegalArgumentException(s"Build $version not supported!")
    }

  lazy val getRealmBuild: Int = Global.config.wow.realmBuild.getOrElse(buildFromVersion)
  lazy val getGameBuild: Int = Global.config.wow.gameBuild.getOrElse(buildFromVersion)

  private def convertToUpper(account: String): Array[Byte] = {
    account.map(c => {
      if (c >= 'a' && c <= 'z') {
        c.toUpper
      } else {
        c
      }
    }).getBytes("UTF-8")
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
        case (enabled, format) => GuildNotificationConfig(enabled, format, None)
      })
    })(guildConf => {
      GuildConfig(
        defaults.keysIterator.map(key => {
          val conf = getConfigOpt(guildConf, key)
          val default = defaults(key)
          key -> conf.fold(GuildNotificationConfig(default._1, default._2, None))(conf => {
            GuildNotificationConfig(
              getOpt[Boolean](conf, "enabled").getOrElse(default._1),
              getOpt[String](conf, "format").getOrElse(default._2),
              getOpt[String](conf, "channel")
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
          WowChannelConfig(
            getOpt[Int](channel, "wow.id"),
            ChatEvents.parse(channel.getString("wow.type")),
            wowChannel,
            getOpt[String](channel, "wow.format").getOrElse(""),
            parseFilters(getConfigOpt(channel, "wow.filters"))
          ),
          DiscordChannelConfig(
            channel.getString("discord.channel"),
            channel.getString("discord.format"),
            parseFilters(getConfigOpt(channel, "discord.filters"))
          )
        )
    })
  }

  private def parseFilters(filtersConf: Option[Config]): Option[FiltersConfig] = {
    filtersConf.map(config => {
      FiltersConfig(
        getOpt[Boolean](config, "enabled").getOrElse(false),
        getOpt[util.List[String]](config, "patterns").getOrElse(new util.ArrayList[String]()).asScala
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
        } else if (typeOf[T] =:= typeOf[String]) {
          cfg.getString(path)
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
