package wowchat.commands

import net.dv8tion.jda.core.entities.MessageChannel
import wowchat.common.Global

import scala.util.Try

case class WhoRequest(messageChannel: MessageChannel, playerName: String)
case class WhoResponse(playerName: String, guildName: String, lvl: Int, cls: String, race: String, gender: Option[String], zone: String)

object CommandHandler {

  private val NOT_ONLINE = "Bot is not yet online."

  // make some of these configurable
  private val trigger = "?"

  // gross. rewrite
  var whoRequest: WhoRequest = _

  // returns back the message as an option if unhandled
  def apply(fromChannel: MessageChannel, message: String): Boolean = {
    if (!message.startsWith(trigger)) {
      return false
    }

    Global.game.fold({
      fromChannel.sendMessage(NOT_ONLINE).queue()
      true
    })(game => {
      val splt = message.substring(trigger.length).split(" ")
      val possibleCommand = splt(0)
      val arguments = if (splt.length > 1 && splt(1).length <= 16) Some(splt(1)) else None

      Try {
        possibleCommand match {
          case "reset" | "resets" =>
            // the construct only
            game.handleResets
          case "who" | "online" =>
            val whoSucceeded = game.handleWho(arguments)
            if (arguments.isDefined) {
              whoRequest = WhoRequest(fromChannel, arguments.get)
            }
            whoSucceeded
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
    })
  }

  // eww
  def handleWhoResponse(whoResponse: Option[WhoResponse]) = {
    val response = whoResponse.map(r => {
      s"${r.playerName} <${r.guildName}> is a level ${r.lvl}${r.gender.fold(" ")(g => s" $g ")}${r.race} ${r.cls} currently in ${r.zone}."
    }).getOrElse(s"No player named ${whoRequest.playerName} is currently playing.")
    whoRequest.messageChannel.sendMessage(response).queue()
  }
}
