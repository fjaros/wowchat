package wowchat.commands

import com.typesafe.scalalogging.StrictLogging
import net.dv8tion.jda.core.entities.MessageChannel
import wowchat.common.Global
import wowchat.game.{GamePackets, GameResources, GuildInfo, GuildMember}

import scala.collection.mutable
import scala.util.Try

case class WhoRequest(messageChannel: MessageChannel, playerName: String)
case class WhoResponse(playerName: String, guildName: String, lvl: Int, cls: String, race: String, gender: Option[String], zone: String)

object CommandHandler extends StrictLogging {

  private val NOT_ONLINE = "Bot is not online."

  // make some of these configurable
  private val trigger = "?"

  // gross. rewrite
  var whoRequest: WhoRequest = _

  // returns back the message as an option if unhandled
  // needs to be refactored into a Map[String, <Intelligent Command Handler Function>]
  def apply(fromChannel: MessageChannel, message: String): Boolean = {
    if (!message.startsWith(trigger)) {
      return false
    }

    val splt = message.substring(trigger.length).split(" ")
    val possibleCommand = splt(0).toLowerCase
    val arguments = if (splt.length > 1 && splt(1).length <= 16) Some(splt(1)) else None

    Try {
      possibleCommand match {
        case "who" | "online" =>
          Global.game.fold({
            fromChannel.sendMessage(NOT_ONLINE).queue()
            return true
          })(game => {
            val whoSucceeded = game.handleWho(arguments)
            if (arguments.isDefined) {
              whoRequest = WhoRequest(fromChannel, arguments.get)
            }
            whoSucceeded
          })
        case "gmotd" =>
          Global.game.fold({
            fromChannel.sendMessage(NOT_ONLINE).queue()
            return true
          })(_.handleGmotd())
      }
    }.fold(throwable => {
      // command not found, should send to wow chat
      false
    }, opt => {
      // command found, do not send to wow chat
      if (opt.isDefined) {
        fromChannel.sendMessage(opt.get).queue()
      }
      true
    })
  }

  // eww
  def handleWhoResponse(whoResponse: Option[WhoResponse], guildInfo: GuildInfo, guildRoster: mutable.Map[Long, GuildMember]) = {
    val response = whoResponse.map(r => {
      s"${r.playerName} ${if (r.guildName.nonEmpty) s"<${r.guildName}> " else ""}is a level ${r.lvl}${r.gender.fold(" ")(g => s" $g ")}${r.race} ${r.cls} currently in ${r.zone}."
    }).getOrElse({
      // Check guild roster
      guildRoster
        .values
        .find(_.name.equalsIgnoreCase(whoRequest.playerName))
        .fold(s"No player named ${whoRequest.playerName} is currently playing.")(guildMember => {
          val cls = new GamePackets{}.Classes.valueOf(guildMember.charClass) // ... should really move that out
          val days = guildMember.lastLogoff.toInt
          val hours = ((guildMember.lastLogoff * 24) % 24).toInt
          val minutes = ((guildMember.lastLogoff * 24 * 60) % 60).toInt
          val minutesStr = s" $minutes minute${if (minutes != 1) "s" else ""}"
          val hoursStr = if (hours > 0) s" $hours hour${if (hours != 1) "s" else ""}," else ""
          val daysStr = if (days > 0) s" $days day${if (days != 1) "s" else ""}," else ""

          val guildNameStr = if (guildInfo != null) {
            s" <${guildInfo.name}>"
          } else {
            // Welp, some servers don't set guild guid in character selection packet.
            // The only other way to get this information is through parsing SMSG_UPDATE_OBJECT
            // and its compressed version which is quite annoying especially across expansions.
            ""
          }

          s"${guildMember.name}$guildNameStr is a level ${guildMember.level} $cls currently offline. " +
            s"Last seen$daysStr$hoursStr$minutesStr ago in ${GameResources.AREA.getOrElse(guildMember.zoneId, "Unknown Zone")}."
        })
    })
    whoRequest.messageChannel.sendMessage(response).queue()
  }
}
