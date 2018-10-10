package wowchat.discord

import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.TextChannel
import wowchat.common.{WowChatConfig, WowExpansion}

import scala.collection.JavaConverters._
import scala.util.control.Breaks.{break, breakable}

object MessageResolver {

  def apply(jda: JDA): MessageResolver = {
    WowChatConfig.getExpansion match {
      case WowExpansion.Vanilla => new MessageResolver(jda)
      case WowExpansion.TBC => new MessageResolverTBC(jda)
      case WowExpansion.WotLK => new MessageResolverWotLK(jda)
    }
  }
}

class MessageResolver(jda: JDA) {

  protected val linkRegexes = Seq(
    "item" -> "\\|.+?\\|Hitem:(\\d+):.+?\\|h\\[(.+?)\\]\\|h\\|r\\s?".r,
    "spell" -> "\\|.+?\\|(?:Hspell|Henchant)?:(\\d+).*?\\|h\\[(.+?)\\]\\|h\\|r\\s?".r,
    "quest" -> "\\|.+?\\|Hquest:(\\d+):.+?\\|h\\[(.+?)\\]\\|h\\|r\\s?".r
  )

  protected val linkSite = "http://classicdb.ch"

  def resolveLinks(message: String): String = {
    linkRegexes.foldLeft(message) {
      case (result, (classicDbKey, regex)) =>
        regex.replaceAllIn(result, m => {
          s"[${m.group(2)}] ($linkSite?$classicDbKey=${m.group(1)}) "
        })
    }
  }

  def resolveTags(discordChannel: TextChannel, message: String, onError: String => Unit): String = {
    // OR non-capturing regex didn't work for these for some reason
    val regex1 = "\"@(.+?)\"".r
    val regex2 = "@([^\\s]+)".r

    val scalaMembers = discordChannel.getMembers.asScala
    val effectiveNames = scalaMembers.map(member => {
      member.getEffectiveName -> member.getUser.getId
    })
    val userNames = scalaMembers.map(member => {
      val user = member.getUser
      s"${user.getName}#${user.getDiscriminator}" -> user.getId
    })
    val roleNames = jda.getRoles.asScala.map(role => {
      role.getName -> role.getId
    })

    // each group
    Seq(regex1, regex2).foldLeft(message) {
      case (result, regex) =>
        regex.replaceAllIn(result, m => {
          val tag = m.group(1)
          var success = ""
          breakable {
            Seq(effectiveNames, userNames, roleNames).foreach(members => {
              if (resolveTagMatcher(members, tag, success = _, onError, members == roleNames)) {
                break
              } else {
                success = m.group(0)
              }
            })
          }
          success
        })
    }
  }

  private def resolveTagMatcher(names: Seq[(String, String)], tag: String,
                                onSuccess: String => Unit,
                                onError: String => Unit, isRole: Boolean = false): Boolean = {
    val lTag = tag.toLowerCase
    val matchesInitial = names
      .filter {
        case (name, id) =>
          name.toLowerCase.contains(lTag)
      }

    val matches = if (matchesInitial.size > 1 && !lTag.contains(" ")) {
      matchesInitial.filterNot {
        case (name, id) => name.contains(" ")
      }
    } else {
      matchesInitial
    }

    if (matches.size == 1) {
      onSuccess(s"<@${if (isRole) "&" else ""}${matches.head._2}>")
    } else if (matches.size > 1 && matches.size < 5) {
      onError(s"Your tag @$tag matches multiple channel members: ${
        matches.map(_._1).mkString(", ")
      }. Be more specific in your tag!")
    } else if (matches.size >= 5) {
      onError(s"Your tag @$tag matches too many channel members. Be more specific in your tag!")
    }

    matches.nonEmpty
  }
}

// ADD MORE HERE AS NEEDED
class MessageResolverTBC(jda: JDA) extends MessageResolver(jda) {

  override protected val linkRegexes = Seq(
    "item" -> "\\|.+?\\|Hitem:(\\d+):.+?\\|h\\[(.+?)\\]\\|h\\|r\\s?".r,
    "spell" -> "\\|.+?\\|(?:Hspell|Henchant|Htalent)?:(\\d+).*?\\|h\\[(.+?)\\]\\|h\\|r\\s?".r,
    "quest" -> "\\|.+?\\|Hquest:(\\d+):.+?\\|h\\[(.+?)\\]\\|h\\|r\\s?".r
  )

  override protected val linkSite = "http://tbc-twinhead.twinstar.cz"
}

class MessageResolverWotLK(jda: JDA) extends MessageResolverTBC(jda) {

  override protected val linkRegexes = Seq(
    "item" -> "\\|.+?\\|Hitem:(\\d+):.+?\\|h\\[(.+?)\\]\\|h\\|r\\s?".r,
    "spell" -> "\\|.+?\\|(?:Hspell|Henchant|Htalent)?:(\\d+).*?\\|h\\[(.+?)\\]\\|h\\|r\\s?".r,
    "quest" -> "\\|.+?\\|Hquest:(\\d+):.+?\\|h\\[(.+?)\\]\\|h\\|r\\s?".r,
    "achievement" -> "\\|.+?\\|Hachievement:(\\d+):.+?\\|h\\[(.+?)\\]\\|h\\|r\\s?".r
  )

  override protected val linkSite = "http://wotlk-twinhead.twinstar.cz"
}