package wowchat.discord

import wowchat.commands.CommandHandler
import wowchat.common._
import com.typesafe.scalalogging.StrictLogging
import com.vdurmont.emoji.EmojiParser
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.JDA.Status
import net.dv8tion.jda.api.entities.{Activity, ChannelType, MessageType}
import net.dv8tion.jda.api.entities.Activity.ActivityType
import net.dv8tion.jda.api.events.{ShutdownEvent, StatusChangeEvent}
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.{CloseCode, GatewayIntent}
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import wowchat.game.GamePackets

import scala.collection.JavaConverters._
import scala.collection.mutable

class Discord(discordConnectionCallback: CommonConnectionCallback) extends ListenerAdapter
  with GamePackets with StrictLogging {

  private val jda = JDABuilder
    .createDefault(Global.config.discord.token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_EMOJIS)
    .setMemberCachePolicy(MemberCachePolicy.ALL)
    .disableCache(CacheFlag.VOICE_STATE)
    .addEventListeners(this)
    .build

  private val messageResolver = MessageResolver(jda)

  private var lastStatus: Option[Activity] = None
  private var firstConnect = true

  def changeStatus(gameType: ActivityType, message: String): Unit = {
    lastStatus = Some(Activity.of(gameType, message))
    jda.getPresence.setActivity(lastStatus.get)
  }

  def changeGuildStatus(message: String): Unit = {
    changeStatus(ActivityType.WATCHING, message)
  }

  def changeRealmStatus(message: String): Unit = {
    changeStatus(ActivityType.DEFAULT, message)
  }

  def sendMessageFromWow(from: Option[String], message: String, wowType: Byte, wowChannel: Option[String]): Unit = {
    Global.wowToDiscord.get((wowType, wowChannel.map(_.toLowerCase))).foreach(discordChannels => {
      val parsedLinks = messageResolver.resolveEmojis(messageResolver.stripColorCoding(messageResolver.resolveLinks(message)))

      discordChannels.foreach {
        case (channel, channelConfig) =>
          var errors = mutable.ArrayBuffer.empty[String]
          val parsedResolvedTags = from.map(_ => {
            messageResolver.resolveTags(channel, parsedLinks, errors += _)
          })
            .getOrElse(parsedLinks)
            .replace("`", "\\`")
            .replace("*", "\\*")
            .replace("_", "\\_")
            .replace("~", "\\~")

          val formatted = channelConfig
            .format
            .replace("%time", Global.getTime)
            .replace("%user", from.getOrElse(""))
            .replace("%message", parsedResolvedTags)
            .replace("%target", wowChannel.getOrElse(""))

          val filter = shouldFilter(channelConfig.filters, formatted)
          logger.info(s"${if (filter) "FILTERED " else ""}WoW->Discord(${channel.getName}) $formatted")
          if (!filter) {
            channel.sendMessage(formatted).queue()
          }
          if (Global.config.discord.enableTagFailedNotifications) {
            errors.foreach(error => {
              Global.game.foreach(_.sendMessageToWow(ChatEvents.CHAT_MSG_WHISPER, error, from))
              channel.sendMessage(error).queue()
            })
          }
      }
    })
  }

  def sendGuildNotification(eventKey: String, message: String): Unit = {
    Global.guildEventsToDiscord
      .getOrElse(eventKey, Global.wowToDiscord.getOrElse(
          (ChatEvents.CHAT_MSG_GUILD, None), mutable.Set.empty
        ).map(_._1)
      )
      .foreach(channel => {
        logger.info(s"WoW->Discord(${channel.getName}) $message")
        channel.sendMessage(message).queue()
      })
  }

  def sendAchievementNotification(name: String, achievementId: Int): Unit = {
    val notificationConfig = Global.config.guildConfig.notificationConfigs("achievement")
    if (!notificationConfig.enabled) {
      return
    }

    Global.wowToDiscord.get((ChatEvents.CHAT_MSG_GUILD, None))
      .foreach(_.foreach {
        case (discordChannel, _) =>
          val formatted = notificationConfig
            .format
            .replace("%time", Global.getTime)
            .replace("%user", name)
            .replace("%achievement", messageResolver.resolveAchievementId(achievementId))

          discordChannel.sendMessage(formatted).queue()
      })
  }

  override def onStatusChange(event: StatusChangeEvent): Unit = {
    event.getNewStatus match {
      case Status.CONNECTED =>
        lastStatus.foreach(game => changeStatus(game.getType, game.getName))
        // this is a race condition if already connected to wow, reconnect to discord, and bot tries to send
        // wow->discord message. alternatively it was throwing already garbage collected exceptions if trying
        // to use the previous connection's channel references. I guess need to refill these maps on discord reconnection
        Global.discordToWow.clear
        Global.wowToDiscord.clear
        Global.guildEventsToDiscord.clear

        // getNext seq of needed channels from config
        val configChannels = Global.config.channels.map(channelConfig => {
          channelConfig.discord.channel.toLowerCase -> channelConfig
        })
        val configChannelsNames = configChannels.map(_._1)

        val discordTextChannels = event.getEntity.getTextChannels.asScala
        val eligibleDiscordChannels = discordTextChannels
          .filter(channel =>
            configChannelsNames.contains(channel.getName.toLowerCase) ||
            configChannelsNames.contains(channel.getId)
          )

        // build directional maps
        eligibleDiscordChannels.foreach(channel => {
          configChannels
            .filter {
              case (name, _) =>
                name.equalsIgnoreCase(channel.getName) ||
                name == channel.getId
            }
            .foreach {
              case (name, channelConfig) =>
                if (channelConfig.chatDirection == ChatDirection.both ||
                  channelConfig.chatDirection == ChatDirection.discord_to_wow) {
                  Global.discordToWow.addBinding(
                    name.toLowerCase, channelConfig.wow
                  )
                }

                if (channelConfig.chatDirection == ChatDirection.both ||
                  channelConfig.chatDirection == ChatDirection.wow_to_discord) {
                  Global.wowToDiscord.addBinding(
                    (channelConfig.wow.tp, channelConfig.wow.channel.map(_.toLowerCase)),
                    (channel, channelConfig.discord)
                  )
                }
            }
          })

        // build guild notification maps
        val guildEventChannels = Global.config.guildConfig.notificationConfigs
          .filter {
            case (_, notificationConfig) =>
              notificationConfig.enabled
          }
          .flatMap {
            case (key, notificationConfig) =>
              notificationConfig.channel.map(key -> _)
          }

        discordTextChannels.foreach(channel => {
          guildEventChannels
            .filter {
              case (_, name) =>
                name.equalsIgnoreCase(channel.getName) ||
                name == channel.getId
            }
            .foreach {
              case (notificationKey, _) =>
                Global.guildEventsToDiscord.addBinding(notificationKey, channel)
            }
        })

        if (Global.discordToWow.nonEmpty || Global.wowToDiscord.nonEmpty) {
          if (firstConnect) {
            discordConnectionCallback.connected
            firstConnect = false
          } else {
            discordConnectionCallback.reconnected
          }
        } else {
          logger.error("No discord channels configured!")
        }
      case Status.DISCONNECTED =>
        discordConnectionCallback.disconnected
      case _ =>
    }
  }

  override def onShutdown(event: ShutdownEvent): Unit = {
    event.getCloseCode match {
      case CloseCode.DISALLOWED_INTENTS =>
        logger.error("Per new Discord rules, you must check the PRESENCE INTENT, SERVER MEMBERS INTENT, and MESSAGE CONTENT INTENT boxes under \"Privileged Gateway Intents\" for this bot in the developer portal. You can find more info at https://discord.com/developers/docs/topics/gateway#privileged-intents")
      case _ =>
    }
  }

  override def onMessageReceived(event: MessageReceivedEvent): Unit = {
    // ignore messages received from self
    if (event.getAuthor.getIdLong == jda.getSelfUser.getIdLong) {
      return
    }

    // ignore messages from non-text channels
    if (event.getChannelType != ChannelType.TEXT) {
      return
    }

    // ignore non-default messages
    val messageType = event.getMessage.getType
    if (messageType != MessageType.DEFAULT && messageType != MessageType.INLINE_REPLY) {
      return
    }

    val channel = event.getChannel
    val channelId = channel.getId
    val channelName = event.getChannel.getName.toLowerCase
    val effectiveName = event.getMember.getEffectiveName
    val message = (sanitizeMessage(event.getMessage.getContentDisplay) +: event.getMessage.getAttachments.asScala.map(_.getUrl))
      .filter(_.nonEmpty)
      .mkString(" ")
    val enableCommandsChannels = Global.config.discord.enableCommandsChannels
    logger.debug(s"RECV DISCORD MESSAGE: [${channel.getName}] [$effectiveName]: $message")
    if (message.isEmpty) {
      logger.error(s"Received a message in channel ${channel.getName} but the content was empty. You likely forgot to enable MESSAGE CONTENT INTENT for your bot in the Discord Developers portal.")
    }

    if ((enableCommandsChannels.nonEmpty && !enableCommandsChannels.contains(channelName)) || !CommandHandler(channel, message)) {
      // send to all configured wow channels
      Global.discordToWow
        .get(channelName)
        .fold(Global.discordToWow.get(channelId))(Some(_))
        .foreach(_.foreach(channelConfig => {
          val finalMessages = if (shouldSendDirectly(message)) {
            Seq(message)
          } else {
            splitUpMessage(channelConfig.format, effectiveName, message)
          }

          finalMessages.foreach(finalMessage => {
            val filter = shouldFilter(channelConfig.filters, finalMessage)
            logger.info(s"${if (filter) "FILTERED " else ""}Discord->WoW(${
              channelConfig.channel.getOrElse(ChatEvents.valueOf(channelConfig.tp))
            }) $finalMessage")
            if (!filter) {
              Global.game.fold(logger.error("Cannot send message! Not connected to WoW!"))(handler => {
                handler.sendMessageToWow(channelConfig.tp, finalMessage, channelConfig.channel)
              })
            }
          })
        }))
    }
  }

  def shouldSendDirectly(message: String): Boolean = {
    val discordConf = Global.config.discord
    val trimmed = message.drop(1).toLowerCase

    message.startsWith(".") &&
    discordConf.enableDotCommands &&
      (
        discordConf.dotCommandsWhitelist.isEmpty ||
        discordConf.dotCommandsWhitelist.contains(trimmed) ||
        // Theoretically it would be better to construct a prefix tree for this.
        !discordConf.dotCommandsWhitelist.forall(item => {
          if (item.endsWith("*")) {
            !trimmed.startsWith(item.dropRight(1).toLowerCase)
          } else {
            true
          }
        })
      )
  }

  def shouldFilter(filtersConfig: Option[FiltersConfig], message: String): Boolean = {
    filtersConfig
      .fold(Global.config.filters)(Some(_))
      .exists(filters => filters.enabled && filters.patterns.exists(message.matches))
  }

  def sanitizeMessage(message: String): String = {
    EmojiParser.parseToAliases(message, EmojiParser.FitzpatrickAction.REMOVE)
  }

  def splitUpMessage(format: String, name: String, message: String): Seq[String] = {
    val retArr = mutable.ArrayBuffer.empty[String]
    val maxTmpLen = 255 - format
      .replace("%time", Global.getTime)
      .replace("%user", name)
      .replace("%message", "")
      .length

    var tmp = message
    while (tmp.length > maxTmpLen) {
      val subStr = tmp.substring(0, maxTmpLen)
      val spaceIndex = subStr.lastIndexOf(' ')
      tmp = if (spaceIndex == -1) {
        retArr += subStr
        tmp.substring(maxTmpLen)
      } else {
        retArr += subStr.substring(0, spaceIndex)
        tmp.substring(spaceIndex + 1)
      }
    }

    if (tmp.nonEmpty) {
      retArr += tmp
    }

    retArr
      .map(message => {
        val formatted = format
          .replace("%time", Global.getTime)
          .replace("%user", name)
          .replace("%message", message)

        // If the final formatted message is a dot command, it should be disabled. Add a space in front.
        if (formatted.startsWith(".")) {
          s" $formatted"
        } else {
          formatted
        }
      })
  }
}
