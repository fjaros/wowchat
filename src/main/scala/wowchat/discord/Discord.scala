package wowchat.discord

import wowchat.commands.CommandHandler
import wowchat.common._
import com.typesafe.scalalogging.StrictLogging
import com.vdurmont.emoji.EmojiParser
import net.dv8tion.jda.core.JDA.Status
import net.dv8tion.jda.core.entities.{ChannelType, Game}
import net.dv8tion.jda.core.entities.Game.GameType
import net.dv8tion.jda.core.events.StatusChangeEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import net.dv8tion.jda.core.{AccountType, JDABuilder}
import wowchat.game.GamePackets

import scala.collection.JavaConverters._

class Discord(discordConnectionCallback: CommonConnectionCallback) extends ListenerAdapter
  with GamePackets with StrictLogging {

  private val jda = new JDABuilder(AccountType.BOT)
    .setToken(Global.config.discord.token)
    .addEventListener(this)
    .build

  private val messageResolver = MessageResolver(jda)

  def changeStatus(gameType: GameType, message: String): Unit = {
    jda.getPresence.setGame(Game.of(gameType, message))
  }

  def changeGuildStatus(message: String): Unit = {
    changeStatus(GameType.WATCHING, message)
  }

  def changeRealmStatus(message: String): Unit = {
    changeStatus(GameType.DEFAULT, message)
  }

  def sendMessageFromWow(from: Option[String], message: String, wowType: Byte, wowChannel: Option[String]): Unit = {
    val discordChannels = Global.wowToDiscord((wowType, wowChannel.map(_.toLowerCase)))
    val parsedLinks = messageResolver.stripColorCoding(messageResolver.resolveLinks(message))

    discordChannels.foreach {
      case (channel, channelConfig) =>

        val parsedResolvedTags = from.map(from => {
          messageResolver.resolveTags(channel, parsedLinks, error => {
            Global.game.foreach(_.sendMessageToWow(ChatEvents.CHAT_MSG_WHISPER, error, Some(from)))
          })
        })
          .getOrElse(parsedLinks)

        val formatted = channelConfig
          .format
          .replace("%time", Global.getTime)
          .replace("%user", from.getOrElse(""))
          .replace("%message", parsedResolvedTags)
          .replace("%target", wowChannel.getOrElse(""))

        logger.info(s"WoW->Discord(${channel.getName}): $formatted")
        channel.sendMessage(formatted).queue()
    }
  }

  def sendGuildNotification(message: String): Unit = {
    Global.wowToDiscord.get((ChatEvents.CHAT_MSG_GUILD, None))
      .foreach(_.foreach {
        case (discordChannel, _) => discordChannel.sendMessage(message).queue()
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
        // we only care about this once. after we build the channel maps,
        // there is no reason to worry about the discord driver automatically reconnecting and changing status
        if (Global.discordToWow.nonEmpty || Global.wowToDiscord.nonEmpty) {
          discordConnectionCallback.reconnected
          return
        }

        // getNext seq of needed channels from config
        val configChannels = Global.config.channels.map(channelConfig => {
          channelConfig.discord.channel.toLowerCase -> channelConfig
        })

        val eligibleDiscordChannels = event.getEntity.getTextChannels.asScala
          .filter(channel => configChannels.map(_._1).contains(channel.getName.toLowerCase))

        // build directional maps
        eligibleDiscordChannels.foreach(channel => {
          configChannels
            .filter(_._1.equalsIgnoreCase(channel.getName))
            .foreach {
              case (name, channelConfig) =>
                if (channelConfig.chatDirection == ChatDirection.both ||
                  channelConfig.chatDirection == ChatDirection.discord_to_wow) {
                  Global.discordToWow.addBinding(
                    name.toLowerCase,
                    (channel, channelConfig.wow)
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

        if (Global.discordToWow.nonEmpty || Global.wowToDiscord.nonEmpty) {
          discordConnectionCallback.connected
        } else {
          logger.error("No discord channels configured!")
        }
      case Status.DISCONNECTED =>
        discordConnectionCallback.disconnected
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

    val channel = event.getChannel
    val effectiveName = event.getMember.getEffectiveName
    val message = sanitizeMessage(event.getMessage.getContentDisplay)
    logger.debug(s"RECV DISCORD MESSAGE: [${channel.getName}] [$effectiveName]: $message")

    if (!CommandHandler(channel, message)) {
      // send to all configured wow channels
      Global.discordToWow
        .get(event.getMessage.getChannel.getName.toLowerCase)
        .foreach(_.foreach {
          case (_, channelConfig) =>
            val finalMessage = if (Global.config.discord.enableDotCommands && message.startsWith(".")) {
              message
            } else {
              channelConfig.format
                .replace("%time", Global.getTime)
                .replace("%user", effectiveName)
                .replace("%message", message)
            }

            Global.game.foreach(handler => {
              handler.sendMessageToWow(channelConfig.tp, finalMessage, channelConfig.channel)
            })
        })
    }
  }

  def sanitizeMessage(message: String): String = {
    EmojiParser.removeAllEmojis(message)
  }
}
