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

  private var lastStatus: Option[Game] = None
  private var firstConnect = true

  def changeStatus(gameType: GameType, message: String): Unit = {
    lastStatus = Some(Game.of(gameType, message))
    jda.getPresence.setGame(lastStatus.get)
  }

  def changeGuildStatus(message: String): Unit = {
    changeStatus(GameType.WATCHING, message)
  }

  def changeRealmStatus(message: String): Unit = {
    changeStatus(GameType.DEFAULT, message)
  }

  def sendMessageFromWow(from: Option[String], message: String, wowType: Byte, wowChannel: Option[String]): Unit = {
    val discordChannels = Global.wowToDiscord((wowType, wowChannel.map(_.toLowerCase)))
    val parsedLinks = messageResolver.resolveEmojis(messageResolver.stripColorCoding(messageResolver.resolveLinks(message)))

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
        lastStatus.foreach(game => changeStatus(game.getType, game.getName))
        // this is a race condition if already connected to wow, reconnect to discord, and bot tries to send
        // wow->discord message. alternatively it was throwing already garbage collected exceptions if trying
        // to use the previous connection's channel references. I guess need to refill these maps on discord reconnection
        Global.discordToWow.clear
        Global.wowToDiscord.clear

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
    val channelName = event.getChannel.getName.toLowerCase
    val effectiveName = event.getMember.getEffectiveName
    val message = sanitizeMessage(event.getMessage.getContentDisplay)
    val enableCommandsChannels = Global.config.discord.enableCommandsChannels
    logger.debug(s"RECV DISCORD MESSAGE: [${channel.getName}] [$effectiveName]: $message")

    if ((enableCommandsChannels.nonEmpty && !enableCommandsChannels.contains(channelName)) || !CommandHandler(channel, message)) {
      // send to all configured wow channels
      Global.discordToWow
        .get(channelName)
        .foreach(_.foreach(channelConfig => {
            val finalMessage = if (Global.config.discord.enableDotCommands && message.startsWith(".")) {
              message
            } else {
              channelConfig.format
                .replace("%time", Global.getTime)
                .replace("%user", effectiveName)
                .replace("%message", message)
            }

            channelConfig.channel.fold(
              logger.info(s"Discord->WoW(${ChatEvents.valueOf(channelConfig.tp)}): $message")
            )(target => {
              logger.info(s"Discord->WoW($target): $message")
            })
            Global.game.fold(logger.error("Cannot send message! Not connected to WoW!"))(handler => {
              handler.sendMessageToWow(channelConfig.tp, finalMessage, channelConfig.channel)
            })
        }))
    }
  }

  def sanitizeMessage(message: String): String = {
    EmojiParser.parseToAliases(message, EmojiParser.FitzpatrickAction.REMOVE)
  }
}
