package wowchat.game

import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.concurrent.{Executors, TimeUnit}

import wowchat.common._
import wowchat.game.warden.WardenHandler
import com.typesafe.scalalogging.StrictLogging
import io.netty.buffer.{ByteBuf, PooledByteBufAllocator}
import io.netty.channel.{ChannelFuture, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import wowchat.commands.{CommandHandler, WhoResponse}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

case class Player(name: String, charClass: Byte)
case class ChatMessage(guid: Long, tp: Byte, message: String, channel: Option[String] = None)
case class NameQueryMessage(guid: Long, name: String, charClass: Byte)
case class AuthChallengeMessage(sessionKey: Array[Byte], byteBuf: ByteBuf)
case class CharEnumMessage(guid: Long, race: Byte, guildGuid: Long)

class GamePacketHandler(realmId: Int, realmName: String, sessionKey: Array[Byte], gameEventCallback: CommonConnectionCallback)
  extends ChannelInboundHandlerAdapter with GameCommandHandler with GamePackets with StrictLogging {

  protected val addonInfo: Array[Byte] = Array(
    0x56, 0x01, 0x00, 0x00, 0x78, 0x9C, 0x75, 0xCC, 0xBD, 0x0E, 0xC2, 0x30, 0x0C, 0x04, 0xE0, 0xF2,
    0x1E, 0xBC, 0x0C, 0x61, 0x40, 0x95, 0xC8, 0x42, 0xC3, 0x8C, 0x4C, 0xE2, 0x22, 0x0B, 0xC7, 0xA9,
    0x8C, 0xCB, 0x4F, 0x9F, 0x1E, 0x16, 0x24, 0x06, 0x73, 0xEB, 0x77, 0x77, 0x81, 0x69, 0x59, 0x40,
    0xCB, 0x69, 0x33, 0x67, 0xA3, 0x26, 0xC7, 0xBE, 0x5B, 0xD5, 0xC7, 0x7A, 0xDF, 0x7D, 0x12, 0xBE,
    0x16, 0xC0, 0x8C, 0x71, 0x24, 0xE4, 0x12, 0x49, 0xA8, 0xC2, 0xE4, 0x95, 0x48, 0x0A, 0xC9, 0xC5,
    0x3D, 0xD8, 0xB6, 0x7A, 0x06, 0x4B, 0xF8, 0x34, 0x0F, 0x15, 0x46, 0x73, 0x67, 0xBB, 0x38, 0xCC,
    0x7A, 0xC7, 0x97, 0x8B, 0xBD, 0xDC, 0x26, 0xCC, 0xFE, 0x30, 0x42, 0xD6, 0xE6, 0xCA, 0x01, 0xA8,
    0xB8, 0x90, 0x80, 0x51, 0xFC, 0xB7, 0xA4, 0x50, 0x70, 0xB8, 0x12, 0xF3, 0x3F, 0x26, 0x41, 0xFD,
    0xB5, 0x37, 0x90, 0x19, 0x66, 0x8F
  ).map(_.toByte)

  protected var selfCharacterId: Option[Long] = None
  protected var languageId: Byte = _
  protected var inWorld: Boolean = false
  protected var guildGuid: Long = _
  protected var guildName: String = ""

  var motd: String = ""
  var ginfo: String = ""

  protected var ctx: Option[ChannelHandlerContext] = None
  protected val playerRoster = mutable.Map.empty[Long, Player]

  // cannot use multimap here because need deterministic order
  private val queuedChatMessages = new mutable.HashMap[Long, mutable.ListBuffer[ChatMessage]]
  private var wardenHandler: Option[WardenHandler] = None

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    pingExecutor.shutdown()
    gameEventCallback.disconnected
    super.channelInactive(ctx)
  }

  private val pingExecutor = Executors.newSingleThreadScheduledExecutor

  private def runPingExecutor: Unit = {
    pingExecutor.scheduleAtFixedRate(new Runnable {
      var pingId = 0

      override def run(): Unit = {
        val latency = Random.nextInt(50) + 90

        val byteBuf = PooledByteBufAllocator.DEFAULT.buffer(8, 8)
        byteBuf.writeIntLE(pingId)
        byteBuf.writeIntLE(latency)

        ctx.get.writeAndFlush(Packet(CMSG_PING, byteBuf))
        pingId += 1
      }
    }, 30, 30, TimeUnit.SECONDS)
  }

  def buildGuildiesOnline: String = {
    val characterName = Global.config.wow.character

    playerRoster
      .valuesIterator
      .filter(!_.name.equalsIgnoreCase(characterName))
      .toSeq
      .sortBy(_.name)
      .map(m => {
        s"${m.name} (${Classes.valueOf(m.charClass)})"
      })
      .mkString(getGuildiesOnlineMessage(false), ", ", "")
  }

  def getGuildiesOnlineMessage(isStatus: Boolean): String = {
    val size = playerRoster.size - 1
    val guildies = s"guildie${if (size != 1) "s" else ""}"

    if (isStatus) {
      s"$size $guildies online"
    } else {
      if (size <= 0) {
        "Currently no guildies online."
      } else {
        s"Currently $size $guildies online:\n"
      }
    }
  }

  protected def updateGuildiesOnline: Unit = {
    Global.discord.changeGuildStatus(getGuildiesOnlineMessage(true))
  }

  protected def queryGuildName: Unit = {
    val out = PooledByteBufAllocator.DEFAULT.buffer(4, 4)
    out.writeIntLE(guildGuid.toInt)
    ctx.get.writeAndFlush(Packet(CMSG_GUILD_QUERY, out))
  }

  protected def updateGuildRoster: Unit = {
    ctx.get.writeAndFlush(Packet(CMSG_GUILD_ROSTER))
  }

  def sendLogout: Option[ChannelFuture] = {
    ctx.flatMap(ctx => {
      if (ctx.channel.isActive) {
        Some(ctx.writeAndFlush(Packet(CMSG_LOGOUT_REQUEST)))
      } else {
        None
      }
    })
  }

  override def sendMessageToWow(tp: Byte, message: String, target: Option[String]): Unit = {
    ctx.foreach(ctx => {
      val out = PooledByteBufAllocator.DEFAULT.buffer(128, 8192)
      out.writeIntLE(tp)
      out.writeIntLE(languageId)
      target.fold(logger.info(s"Discord->WoW(${ChatEvents.valueOf(tp)}): $message"))(target => {
        logger.info(s"Discord->WoW($target): $message")
        out.writeCharSequence(target, Charset.forName("UTF-8"))
        out.writeByte(0)
      })
      out.writeCharSequence(message, Charset.forName("UTF-8"))
      out.writeByte(0)
      ctx.writeAndFlush(Packet(CMSG_CHATMESSAGE, out))
    })
  }

  override def sendNotification(message: String): Unit = {
    sendMessageToWow(ChatEvents.CHAT_MSG_GUILD, message, None)
  }

  def sendNameQuery(guid: Long): Unit = {
    ctx.foreach(ctx => {
      val out = PooledByteBufAllocator.DEFAULT.buffer(8, 8)
      out.writeLongLE(guid)
      ctx.writeAndFlush(Packet(CMSG_NAME_QUERY, out))
    })
  }

  override def handleWho(arguments: Option[String]): Option[String] = {
    if (arguments.isDefined) {
      val byteBuf = buildWhoMessage(arguments.get)
      ctx.get.writeAndFlush(Packet(CMSG_WHO, byteBuf))
      None
    } else {
      Some(buildGuildiesOnline)
    }
  }

  protected def buildWhoMessage(name: String): ByteBuf = {
    val byteBuf = PooledByteBufAllocator.DEFAULT.buffer(64, 64)
    byteBuf.writeIntLE(0)  // level min
    byteBuf.writeIntLE(100) // level max
    byteBuf.writeBytes(name.getBytes)
    byteBuf.writeByte(0) // ?
    byteBuf.writeByte(0) // ?
    byteBuf.writeIntLE(0xFFFFFFFF) // race mask (all races)
    byteBuf.writeIntLE(0xFFFFFFFF) // class mask (all classes)
    byteBuf.writeIntLE(0) // zones count
    byteBuf.writeIntLE(0) // strings count
  }

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    logger.info("Connected! Authenticating...")
    this.ctx = Some(ctx)
    Global.game = Some(this)
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    msg match {
      case msg: Packet =>
        channelParse(msg)
        msg.byteBuf.release
      case msg => logger.error(s"Packet is instance of ${msg.getClass}")
    }
  }

  protected def channelParse(msg: Packet): Unit = {
    msg.id match {
      case SMSG_AUTH_CHALLENGE => handle_SMSG_AUTH_CHALLENGE(msg)
      case SMSG_AUTH_RESPONSE => handle_SMSG_AUTH_RESPONSE(msg)
      case SMSG_NAME_QUERY => handle_SMSG_NAME_QUERY(msg)
      case SMSG_CHAR_ENUM => handle_SMSG_CHAR_ENUM(msg)
      case SMSG_LOGIN_VERIFY_WORLD => handle_SMSG_LOGIN_VERIFY_WORLD(msg)
      case SMSG_GUILD_QUERY => handle_SMSG_GUILD_QUERY(msg)
      case SMSG_GUILD_EVENT => handle_SMSG_GUILD_EVENT(msg)
      case SMSG_GUILD_ROSTER => handle_SMSG_GUILD_ROSTER(msg)
      case SMSG_CHATMESSAGE => handle_SMSG_CHATMESSAGE(msg)
      case SMSG_CHANNEL_NOTIFY => handle_SMSG_CHANNEL_NOTIFY(msg)
      case SMSG_NOTIFICATION => handle_SMSG_NOTIFICATION(msg)
      case SMSG_WHO => handle_SMSG_WHO(msg)

      case SMSG_WARDEN_DATA => handle_SMSG_WARDEN_DATA(msg)

      case unhandled =>
    }
  }

  private def handle_SMSG_AUTH_CHALLENGE(msg: Packet): Unit = {
    val authChallengeMessage = parseAuthChallenge(msg)

    ctx.get.channel.attr(CRYPT).get.init(authChallengeMessage.sessionKey)

    ctx.get.writeAndFlush(Packet(CMSG_AUTH_CHALLENGE, authChallengeMessage.byteBuf))
  }

  protected def parseAuthChallenge(msg: Packet): AuthChallengeMessage = {
    val account = Global.config.wow.account.toUpperCase

    val serverSeed = msg.byteBuf.readInt
    val clientSeed = Random.nextInt
    val out = PooledByteBufAllocator.DEFAULT.buffer(200, 400)
    out.writeShortLE(0)
    out.writeIntLE(WowChatConfig.getBuild)
    out.writeIntLE(0)
    out.writeBytes(account.getBytes)
    out.writeByte(0)
    out.writeInt(clientSeed)

    val md = MessageDigest.getInstance("SHA1")
    md.update(account.getBytes)
    md.update(Array[Byte](0, 0, 0, 0))
    md.update(ByteUtils.intToBytes(clientSeed))
    md.update(ByteUtils.intToBytes(serverSeed))
    md.update(sessionKey)
    out.writeBytes(md.digest)

    out.writeBytes(addonInfo)

    AuthChallengeMessage(sessionKey, out)
  }

  private def handle_SMSG_AUTH_RESPONSE(msg: Packet): Unit = {
    val code = parseAuthResponse(msg)
    if (code == AuthResponseCodes.AUTH_OK) {
      logger.info("Successfully logged in!")
      ctx.get.writeAndFlush(Packet(CMSG_CHAR_ENUM))
    } else {
      logger.error(AuthResponseCodes.getMessage(code))
      ctx.foreach(_.close)
      gameEventCallback.error
    }
  }

  protected def parseAuthResponse(msg: Packet): Byte = {
    msg.byteBuf.readByte
  }

  private def handle_SMSG_NAME_QUERY(msg: Packet): Unit = {
    val nameQueryMessage = parseNameQuery(msg)

    queuedChatMessages
      .remove(nameQueryMessage.guid)
      .foreach(messages => {
        messages.foreach(message => {
          Global.discord.sendMessageFromWow(Some(nameQueryMessage.name), message.message, message.tp, message.channel)
        })
        playerRoster += nameQueryMessage.guid -> Player(nameQueryMessage.name, nameQueryMessage.charClass.toByte)
    })
  }

  protected def parseNameQuery(msg: Packet): NameQueryMessage = {
    val guid = msg.byteBuf.readLongLE
    val name = msg.readString
    msg.skipString // realm name for cross bg usage
    msg.byteBuf.skipBytes(4) // race
    msg.byteBuf.skipBytes(4) // gender
    val charClass = msg.byteBuf.readIntLE.toByte

    NameQueryMessage(guid, name, charClass)
  }

  private def handle_SMSG_CHAR_ENUM(msg: Packet): Unit = {
    parseCharEnum(msg).fold({
      logger.error(s"Character ${Global.config.wow.character} not found!")
    })(character => {
      selfCharacterId = Some(character.guid)
      languageId = Races.getLanguage(character.race)
      guildGuid = character.guildGuid

      val out = PooledByteBufAllocator.DEFAULT.buffer(16, 16) // increase to 16 for MoP
      writePlayerLogin(out)
      ctx.get.writeAndFlush(Packet(CMSG_PLAYER_LOGIN, out))
    })
  }

  protected def parseCharEnum(msg: Packet): Option[CharEnumMessage] = {
    val charactersNum = msg.byteBuf.readByte

    // only care about guid and name here
    (0 until charactersNum).foreach(i => {
      val guid = msg.byteBuf.readLongLE
      val name = msg.readString
      val race = msg.byteBuf.readByte // will determine what language to use in chat

      msg.byteBuf.skipBytes(1) // class
      msg.byteBuf.skipBytes(1) // gender
      msg.byteBuf.skipBytes(1) // skin
      msg.byteBuf.skipBytes(1) // face
      msg.byteBuf.skipBytes(1) // hair style
      msg.byteBuf.skipBytes(1) // hair color
      msg.byteBuf.skipBytes(1) // facial hair
      msg.byteBuf.skipBytes(1) // level
      msg.byteBuf.skipBytes(4) // zone
      msg.byteBuf.skipBytes(4) // map - could be useful in the future to determine what city specific channels to join

      msg.byteBuf.skipBytes(12) // x + y + z

      val guildGuid = msg.byteBuf.readIntLE
      if (name.equalsIgnoreCase(Global.config.wow.character)) {
        return Some(CharEnumMessage(guid, race, guildGuid))
      }

      msg.byteBuf.skipBytes(4) // character flags
      msg.byteBuf.skipBytes(1) // first login
      msg.byteBuf.skipBytes(12) // pet info
      msg.byteBuf.skipBytes(19 * 5) // equipment info
      msg.byteBuf.skipBytes(5) // first bag display info
    })
    None
  }

  protected def writePlayerLogin(out: ByteBuf): Unit = {
    out.writeLongLE(selfCharacterId.get)
  }

  private def handle_SMSG_LOGIN_VERIFY_WORLD(msg: Packet): Unit = {
    // for some reason some servers send this packet more than once.
    if (inWorld) {
      return
    }

    logger.info("Successfully joined the world!")
    inWorld = true
    Global.discord.changeRealmStatus(realmName)
    gameEventCallback.connected
    runPingExecutor
    if (guildGuid != 0) {
      queryGuildName
      updateGuildRoster
    }

    // join channels
    Global.config.channels
      .flatMap(_.wow.channel)
      .foreach(channel => {
        logger.info(s"Joining channel $channel")
        val byteBuf = PooledByteBufAllocator.DEFAULT.buffer(50, 200)
        writeJoinChannel(byteBuf, channel)
        ctx.get.writeAndFlush(Packet(CMSG_JOIN_CHANNEL, byteBuf))
      })
  }

  protected def writeJoinChannel(out: ByteBuf, channel: String): Unit = {
    out.writeBytes(channel.getBytes)
    out.writeByte(0)
    out.writeByte(0)
  }

  private def handle_SMSG_GUILD_QUERY(msg: Packet): Unit = {
    guildName = handleGuildQuery(msg)
  }

  protected def handleGuildQuery(msg: Packet): String = {
    msg.byteBuf.skipBytes(4)
    msg.readString
  }

  private def handle_SMSG_GUILD_EVENT(msg: Packet): Unit = {
    val event = msg.byteBuf.readByte
    // number of strings for other events, our cared-about events always have 1
    msg.byteBuf.skipBytes(1)
    val message = msg.readString

    handleGuildEvent(event, message)
  }

  protected def handleGuildEvent(event: Byte, message: String): Unit = {
    // ignore events from self
    if (event != GuildEvents.GE_MOTD && Global.config.wow.character.equalsIgnoreCase(message)) {
      return
    }

    val guildNotificationConfig = Global.config.guildConfig.notificationConfigs(
      event match {
        case GuildEvents.GE_MOTD => "motd"
        case GuildEvents.GE_JOINED => "joined"
        case GuildEvents.GE_LEFT | GuildEvents.GE_REMOVED => "left"
        case GuildEvents.GE_SIGNED_ON => "online"
        case GuildEvents.GE_SIGNED_OFF => "offline"
        case _ => return
      }
    )

    if (guildNotificationConfig.enabled) {
      val formatted = guildNotificationConfig
        .format
        .replace("%time", Global.getTime)
        .replace("%user", message)
        .replace("%message", message)

      Global.discord.sendGuildNotification(formatted)
    }

    updateGuildRoster
  }

  private def handle_SMSG_GUILD_ROSTER(msg: Packet): Unit = {
    playerRoster.clear
    playerRoster ++= parseGuildRoster(msg)
    updateGuildiesOnline
  }

  protected def parseGuildRoster(msg: Packet): Map[Long, Player] = {
    val count = msg.byteBuf.readIntLE
    val motd = msg.readString
    val ginfo = msg.readString
    val rankscount = msg.byteBuf.readIntLE
    (0 until rankscount).foreach(i => msg.byteBuf.skipBytes(4))
    (0 until count).flatMap(i => {
      val guid = msg.byteBuf.readLongLE
      val isOnline = msg.byteBuf.readBoolean
      val name = msg.readString
      msg.byteBuf.skipBytes(4) // guild rank
      msg.byteBuf.skipBytes(1) // level
      val charClass = msg.byteBuf.readByte
      msg.byteBuf.skipBytes(4) // zone id
      if (!isOnline) {
        // last logoff time
        msg.byteBuf.skipBytes(4)
      }
      msg.skipString
      msg.skipString
      if (isOnline) {
        Some(guid -> Player(name, charClass))
      } else {
        None
      }
    }).toMap
  }

  private def handle_SMSG_CHATMESSAGE(msg: Packet): Unit = {
    logger.debug(s"RECV CHAT: ${ByteUtils.toHexString(msg.byteBuf, true, true)}")

    parseChatMessage(msg).foreach(chatMessage => {
      if (chatMessage.guid == 0) {
        Global.discord.sendMessageFromWow(None, chatMessage.message, chatMessage.tp, None)
      } else {
        playerRoster.get(chatMessage.guid).fold({
          queuedChatMessages.get(chatMessage.guid).fold({
            queuedChatMessages += chatMessage.guid -> ListBuffer(chatMessage)
            sendNameQuery(chatMessage.guid)
          })(_ += chatMessage)
        })(name => {
          Global.discord.sendMessageFromWow(Some(name.name), chatMessage.message, chatMessage.tp, chatMessage.channel)
        })
      }
    })
  }

  protected def parseChatMessage(msg: Packet): Option[ChatMessage] = {
    val tp = msg.byteBuf.readByte

    val lang = msg.byteBuf.readIntLE
    // ignore addon messages
    if (lang == -1) {
      return None
    }

    val channelName = if (tp == ChatEvents.CHAT_MSG_CHANNEL) {
      val ret = Some(msg.readString)
      msg.byteBuf.skipBytes(4)
      ret
    } else {
      None
    }

    // ignore if from an unhandled channel
    if (!Global.wowToDiscord.contains((tp, channelName.map(_.toLowerCase)))) {
      return None
    }

    // ignore messages from itself
    val guid = msg.byteBuf.readLongLE
    if (guid == selfCharacterId.get) {
      return None
    }

    // these events have a "target" guid we need to skip
    tp match {
      case ChatEvents.CHAT_MSG_SAY |
           ChatEvents.CHAT_MSG_YELL =>
        msg.byteBuf.skipBytes(8)
      case _ =>
    }

    val txtLen = msg.byteBuf.readIntLE
    val txt = msg.byteBuf.readCharSequence(txtLen - 1, Charset.defaultCharset).toString

    Some(ChatMessage(guid, tp, txt, channelName))
  }

  private def handle_SMSG_CHANNEL_NOTIFY(msg: Packet): Unit = {
    val id = msg.byteBuf.readByte

    if (id == 0x02) {
      logger.info(s"Joined channel: ${msg.readString}")
    }
  }

  private def handle_SMSG_NOTIFICATION(msg: Packet): Unit = {
    logger.info(s"Notification: ${parseNotification(msg)}")
  }

  protected def parseNotification(msg: Packet): String = {
    msg.readString
  }

  // This is actually really hard to map back to a specific request
  // because the packet doesn't include a cookie/id/requested name if none found
  protected def handle_SMSG_WHO(msg: Packet): Unit = {
    val displayCount = msg.byteBuf.readIntLE
    val matchCount = msg.byteBuf.readIntLE

    if (matchCount == 0) {
      CommandHandler.handleWhoResponse(None)
    } else {
      (0 until Math.min(matchCount, 3)).foreach(i => {
        val playerName = msg.readString
        val guildName = msg.readString
        val lvl = msg.byteBuf.readIntLE
        val cls = Classes.valueOf(msg.byteBuf.readIntLE.toByte)
        val race = Races.valueOf(msg.byteBuf.readIntLE.toByte)
        val gender = if (WowChatConfig.getExpansion != WowExpansion.Vanilla) {
          Some(Genders.valueOf(msg.byteBuf.readByte)) // tbc/wotlk only
        } else {
          None
        }
        val zone = msg.byteBuf.readIntLE
        CommandHandler.handleWhoResponse(Some(WhoResponse(
          playerName,
          guildName,
          lvl,
          cls,
          race,
          gender,
          GameResources.AREA.getOrElse(zone, "Unknown Zone")))
        )
      })
    }
  }

  private def handle_SMSG_WARDEN_DATA(msg: Packet): Unit = {
    if (wardenHandler.isEmpty) {
      wardenHandler = Some(initializeWardenHandler)
    }

    val out = wardenHandler.get.handle(msg)
    if (out.isDefined) {
      ctx.get.writeAndFlush(Packet(CMSG_WARDEN_DATA, out.get))
    }
  }

  protected def initializeWardenHandler: WardenHandler = {
    new WardenHandler(sessionKey)
  }
}
