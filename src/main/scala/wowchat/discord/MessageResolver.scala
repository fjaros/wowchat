package wowchat.discord

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.TextChannel
import wowchat.common.{WowChatConfig, WowExpansion}
import wowchat.game.GameResources

import scala.collection.JavaConverters._
import scala.collection.mutable

object MessageResolver {

  def apply(jda: JDA): MessageResolver = {
    WowChatConfig.getExpansion match {
      case WowExpansion.Vanilla => new MessageResolver(jda)
      case WowExpansion.TBC => new MessageResolverTBC(jda)
      case WowExpansion.WotLK => new MessageResolverWotLK(jda)
      case WowExpansion.Cataclysm => new MessageResolverCataclysm(jda)
      case WowExpansion.MoP => new MessageResolverMoP(jda)
    }
  }
}

class MessageResolver(jda: JDA) {

  protected val linkRegexes = Seq(
    "item" -> "\\|.+?\\|Hitem:(\\d+):.+?\\|h\\[(.+?)\\]\\|h\\|r\\s?".r,
    "spell" -> "\\|.+?\\|(?:Hspell|Henchant)?:(\\d+).*?\\|h\\[(.+?)\\]\\|h\\|r\\s?".r,
    "quest" -> "\\|.+?\\|Hquest:(\\d+):.+?\\|h\\[(.+?)\\]\\|h\\|r\\s?".r
  )

  protected val linkSite = "http://database.turtle-wow.org"

  def resolveLinks(message: String): String = {
    linkRegexes.foldLeft(message) {
      case (result, (classicDbKey, regex)) =>
        regex.replaceAllIn(result, m => {
          s"[[${m.group(2)}]($linkSite?$classicDbKey=${m.group(1)})] "
        })
    }
  }

  def resolveAchievementId(achievementId: Int): String = {
    val name = GameResources.ACHIEVEMENT.getOrElse(achievementId, achievementId)
    s"[[$name]($linkSite?achievement=$achievementId)] "
  }

  def stripColorCoding(message: String): String = {
    val hex = "\\|c[0-9a-fA-F]{8}"
    val pass1 = s"$hex(.*?)\\|r".r
    val pass2 = hex.r

    pass2.replaceAllIn(pass1.replaceAllIn(message.replace("$", "\\$"), _.group(1)), "")
  }

  def resolveTags(discordChannel: TextChannel, message: String, onError: String => Unit): String = {
    // OR non-capturing regex didn't work for these for some reason
    val regexes = Seq("\"@(.+?)\"", "@([\\w]+)").map(_.r)

    val scalaMembers = discordChannel.getMembers.asScala
      // you don't want to tag yourself
      .filterNot(_.getUser.getIdLong == jda.getSelfUser.getIdLong)
    val effectiveNames = scalaMembers.map(member => {
      member.getEffectiveName -> member.getUser.getId
    })
    val userNames = scalaMembers.map(member => {
      val user = member.getUser
      s"${user.getName}#${user.getDiscriminator}" -> user.getId
    })
    val roleNames = jda.getRoles.asScala
      .filterNot(_.getName == "@everyone")
      .map(role => role.getName -> role.getId)

    // each group
    regexes.foldLeft(message) {
      case (result, regex) =>
        regex.replaceAllIn(result, m => {
          val tag = m.group(1)
          val matches = Seq(effectiveNames, userNames, roleNames).foldLeft(Seq.empty[(String, String)]) {
            case (result, members) =>
              val resolvedTags = resolveTagMatcher(members, tag, members == roleNames)
              if (result.isEmpty) {
                resolvedTags
              } else if (result.size == 1) {
                // if one group hit a match, just use that as the best match.
                result
              } else {
                result ++ resolvedTags
              }
          }

          if (matches.size == 1) {
            s"<@${matches.head._2}>"
          } else if (matches.size > 1 && matches.size < 5) {
            onError(s"Your tag @$tag matches multiple channel members: ${
              matches.map(_._1).mkString(", ")
            }. Be more specific in your tag!")
            m.group(0)
          } else if (matches.size >= 5) {
            onError(s"Your tag @$tag matches too many channel members. Be more specific in your tag!")
            m.group(0)
          } else {
            m.group(0)
          }
        })
    }
  }

  private def resolveTagMatcher(names: Seq[(String, String)], tag: String, isRole: Boolean): Seq[(String, String)] = {
    val lTag = tag.toLowerCase
    if (lTag == "here") {
      return Seq.empty
    }

    val matchesInitial = names
      .filter {
        case (name, _) =>
          name.toLowerCase.contains(lTag)
      }

    (if (matchesInitial.size > 1 && !lTag.contains(" ")) {
      // Multiple matches found. Prefer exact match first and a match where the tag is a whole word in the Discord name second.
      matchesInitial.find {
        case (name, _) => name.toLowerCase == lTag
      }.fold({
        // Exact match not found. Try to find the tag as a whole word within the name.
        val namesWithMatchedWord = matchesInitial.filter {
          case (name, _) => name.toLowerCase.split("\\W+").contains(lTag)
        }
        if (namesWithMatchedWord.nonEmpty) {
          namesWithMatchedWord
        } else {
          matchesInitial
        }
      })(_ :: Nil)
    } else {
      matchesInitial
    }).map {
      case (name, id) =>
        name -> (if (isRole) s"&$id" else id)
    }
  }

  def resolveEmojis(message: String): String = {
    val regex = "(?<=:).*?(?=:)".r

    // could do some caching here later
    val emojiMap = jda.getEmotes.asScala.map(emote => {
      emote.getName.toLowerCase -> emote.getId
    }).toMap

    val alreadyResolved = mutable.Set.empty[String]
    regex.findAllIn(message).foldLeft(message) {
      case (result, possibleEmoji) =>
        val lPossibleEmoji = possibleEmoji.toLowerCase
        if (alreadyResolved(lPossibleEmoji)) {
          result
        } else {
          emojiMap.get(lPossibleEmoji).fold(result)(id => {
            alreadyResolved += lPossibleEmoji
            result.replace(s":$possibleEmoji:", s"<:$possibleEmoji:$id>")
          })
        }
    }
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
    "achievement" -> "\\|.+?\\|Hachievement:(\\d+):.+?\\|h\\[(.+?)\\]\\|h\\|r\\s?".r,
    "spell" -> "\\|Htrade:(\\d+):.+?\\|h\\[(.+?)\\]\\|h\\s?".r
  )

  override protected val linkSite = "http://wotlk-twinhead.twinstar.cz"
}

class MessageResolverCataclysm(jda: JDA) extends MessageResolverWotLK(jda) {

  override protected val linkSite = "https://cata-twinhead.twinstar.cz/"
}

class MessageResolverMoP(jda: JDA) extends MessageResolverCataclysm(jda) {

  override protected val linkRegexes = Seq(
    "item" -> "\\|.+?\\|Hitem:(\\d+):.+?\\|h\\[(.+?)\\]\\|h\\|r\\s?".r,
    "spell" -> "\\|.+?\\|(?:Hspell|Henchant|Htalent)?:(\\d+).*?\\|h\\[(.+?)\\]\\|h\\|r\\s?".r,
    "quest" -> "\\|.+?\\|Hquest:(\\d+):.+?\\|h\\[(.+?)\\]\\|h\\|r\\s?".r,
    "achievement" -> "\\|.+?\\|Hachievement:(\\d+):.+?\\|h\\[(.+?)\\]\\|h\\|r\\s?".r,
    "spell" -> "\\|Htrade:.+?:(\\d+):.+?\\|h\\[(.+?)\\]\\|h\\s?".r
  )

  override protected val linkSite = "http://mop-shoot.tauri.hu"
}
