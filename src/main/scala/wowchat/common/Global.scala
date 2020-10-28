package wowchat.common

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import io.netty.channel.EventLoopGroup
import net.dv8tion.jda.api.entities.TextChannel
import wowchat.discord.Discord
import wowchat.game.GameCommandHandler

import scala.collection.mutable

object Global {

  var group: EventLoopGroup = _
  var config: WowChatConfig = _

  var discord: Discord = _
  var game: Option[GameCommandHandler] = None

  val discordToWow = new mutable.HashMap[String, mutable.Set[WowChannelConfig]]
    with mutable.MultiMap[String, WowChannelConfig]
  val wowToDiscord = new mutable.HashMap[(Byte, Option[String]), mutable.Set[(TextChannel, DiscordChannelConfig)]]
    with mutable.MultiMap[(Byte, Option[String]), (TextChannel, DiscordChannelConfig)]
  val guildEventsToDiscord = new mutable.HashMap[String, mutable.Set[TextChannel]]
    with mutable.MultiMap[String, TextChannel]

  def getTime: String = {
    LocalDateTime.now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
  }
}
