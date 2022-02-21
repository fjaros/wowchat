package wowchat.realm

import java.security.MessageDigest

import wowchat.common._
import com.typesafe.scalalogging.StrictLogging
import io.netty.buffer.{ByteBuf, PooledByteBufAllocator}
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}

private[realm] case class RealmList(name: String, address: String, realmId: Byte)

class RealmPacketHandler(realmConnectionCallback: RealmConnectionCallback)
  extends ChannelInboundHandlerAdapter with StrictLogging {

  private val srpClient = new SRPClient
  private var ctx: Option[ChannelHandlerContext] = None
  private var expectedDisconnect = false
  private var sessionKey: Array[Byte] = _
  // Issue 57, certain servers return logon proof packet for the 2nd time when asking for friends list with an error code.
  // Implement a state to ignore it if/when it comes a second time
  private var logonState = 0

  private val buildCrcHashes = Map(
    (4544, Platform.Windows) // 1.6.1
      -> Array(0xD7, 0xAC, 0x29, 0x0C, 0xC2, 0xE4, 0x2F, 0x9C, 0xC8, 0x3A, 0x90, 0x23, 0x80, 0x3A, 0x43, 0x24, 0x43, 0x59, 0xF0, 0x30).map(_.toByte),
    (4565, Platform.Windows) // 1.6.2
      -> Array(0x1A, 0xC0, 0x2C, 0xE9, 0x3E, 0x7B, 0x82, 0xD1, 0x7E, 0x87, 0x18, 0x75, 0x8D, 0x67, 0xF5, 0x9F, 0xB0, 0xCA, 0x4B, 0x5D).map(_.toByte),
    (4620, Platform.Windows) // 1.6.3
      -> Array(0x3C, 0x77, 0xED, 0x95, 0xD6, 0x00, 0xF9, 0xD4, 0x27, 0x0D, 0xA1, 0xA2, 0x91, 0xC7, 0xF6, 0x45, 0xCA, 0x4F, 0x2A, 0xAC).map(_.toByte),
    (4695, Platform.Windows) // 1.7.1
      -> Array(0x37, 0xC0, 0x12, 0x91, 0x27, 0x1C, 0xBB, 0x89, 0x1D, 0x8F, 0xEE, 0xC1, 0x5B, 0x2F, 0x14, 0x7A, 0xA3, 0xE4, 0x0C, 0x80).map(_.toByte),
    (4878, Platform.Windows) // 1.8.4
      -> Array(0x03, 0xDF, 0xB3, 0xC3, 0xF7, 0x24, 0x79, 0xF9, 0xBC, 0xC5, 0xED, 0xD8, 0xDC, 0xA1, 0x02, 0x5E, 0x8D, 0x11, 0xAF, 0x0F).map(_.toByte),
    (5086, Platform.Windows) // 1.9.4
      -> Array(0xC5, 0x61, 0xB5, 0x2B, 0x3B, 0xDD, 0xDD, 0x17, 0x6A, 0x46, 0x43, 0x3C, 0x6D, 0x06, 0x7B, 0xA7, 0x45, 0xE6, 0xB0, 0x00).map(_.toByte),
    (5302, Platform.Windows) // 1.10.2
      -> Array(0x70, 0xDD, 0x18, 0x3C, 0xE6, 0x71, 0xE7, 0x99, 0x09, 0xE0, 0x25, 0x54, 0xE9, 0x4C, 0xBE, 0x3F, 0x2C, 0x33, 0x8C, 0x55).map(_.toByte),
    (5464, Platform.Windows) // 1.11.2
      -> Array(0x4D, 0xF8, 0xA5, 0x05, 0xE4, 0xFE, 0x8D, 0x83, 0x33, 0x50, 0x8C, 0x0E, 0x85, 0x84, 0x65, 0xE3, 0x57, 0x17, 0x86, 0x83).map(_.toByte),
    (5875, Platform.Mac)  // 1.12.1
      -> Array(0x8D, 0x17, 0x3C, 0xC3, 0x81, 0x96, 0x1E, 0xEB, 0xAB, 0xF3, 0x36, 0xF5, 0xE6, 0x67, 0x5B, 0x10, 0x1B, 0xB5, 0x13, 0xE5).map(_.toByte),
    (5875, Platform.Windows)  // 1.12.1
      -> Array(0x95, 0xED, 0xB2, 0x7C, 0x78, 0x23, 0xB3, 0x63, 0xCB, 0xDD, 0xAB, 0x56, 0xA3, 0x92, 0xE7, 0xCB, 0x73, 0xFC, 0xCA, 0x20).map(_.toByte),
    (6005, Platform.Windows)  // 1.12.2
      -> Array(0x06, 0x97, 0x32, 0x38, 0x76, 0x56, 0x96, 0x41, 0x48, 0x79, 0x28, 0xFD, 0xC7, 0xC9, 0xE3, 0x3B, 0x44, 0x70, 0xC8, 0x80).map(_.toByte),
    (6141, Platform.Windows)  // 1.12.3
      -> Array(0x2E, 0x52, 0x36, 0xE5, 0x66, 0xAE, 0xA9, 0xBF, 0xFA, 0x0C, 0xC0, 0x41, 0x67, 0x9C, 0x2D, 0xB5, 0x2E, 0x21, 0xC9, 0xDC).map(_.toByte),
    (8606, Platform.Mac)  // 2.4.3
      -> Array(0xD8, 0xB0, 0xEC, 0xFE, 0x53, 0x4B, 0xC1, 0x13, 0x1E, 0x19, 0xBA, 0xD1, 0xD4, 0xC0, 0xE8, 0x13, 0xEE, 0xE4, 0x99, 0x4F).map(_.toByte),
    (8606, Platform.Windows)  // 2.4.3
      -> Array(0x31, 0x9A, 0xFA, 0xA3, 0xF2, 0x55, 0x96, 0x82, 0xF9, 0xFF, 0x65, 0x8B, 0xE0, 0x14, 0x56, 0x25, 0x5F, 0x45, 0x6F, 0xB1).map(_.toByte),
    (12340, Platform.Mac)  // 3.3.5
      -> Array(0xB7, 0x06, 0xD1, 0x3F, 0xF2, 0xF4, 0x01, 0x88, 0x39, 0x72, 0x94, 0x61, 0xE3, 0xF8, 0xA0, 0xE2, 0xB5, 0xFD, 0xC0, 0x34).map(_.toByte),
    (12340, Platform.Windows)  // 3.3.5
      -> Array(0xCD, 0xCB, 0xBD, 0x51, 0x88, 0x31, 0x5E, 0x6B, 0x4D, 0x19, 0x44, 0x9D, 0x49, 0x2D, 0xBC, 0xFA, 0xF1, 0x56, 0xA3, 0x47).map(_.toByte)
  )

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    if (!expectedDisconnect) {
      realmConnectionCallback.disconnected
    }
    super.channelInactive(ctx)
  }

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    logger.info(s"Connected! Sending account login information...")
    this.ctx = Some(ctx)
    val version = WowChatConfig.getVersion.split("\\.").map(_.toByte)
    val accountConfig = Global.config.wow.account
    val platformString = Global.config.wow.platform match {
      case Platform.Windows => "Win"
      case Platform.Mac => "OSX"
    }
    val localeString = Global.config.wow.locale

    val byteBuf = PooledByteBufAllocator.DEFAULT.buffer(50, 100)

    // seems to be 3 for vanilla and 8 for bc/wotlk
    if (WowChatConfig.getExpansion == WowExpansion.Vanilla) {
      byteBuf.writeByte(3)
    } else {
      byteBuf.writeByte(8)
    }
    byteBuf.writeShortLE(30 + accountConfig.length) // size
    byteBuf.writeIntLE(ByteUtils.stringToInt("WoW"))
    byteBuf.writeByte(version(0))
    byteBuf.writeByte(version(1))
    byteBuf.writeByte(version(2))
    byteBuf.writeShortLE(WowChatConfig.getBuild)
    byteBuf.writeIntLE(ByteUtils.stringToInt("x86"))
    byteBuf.writeIntLE(ByteUtils.stringToInt(platformString))
    byteBuf.writeIntLE(ByteUtils.stringToInt(localeString))
    byteBuf.writeIntLE(0)
    byteBuf.writeByte(127)
    byteBuf.writeByte(0)
    byteBuf.writeByte(0)
    byteBuf.writeByte(1)
    byteBuf.writeByte(accountConfig.length)
    byteBuf.writeBytes(accountConfig)

    ctx.writeAndFlush(Packet(RealmPackets.CMD_AUTH_LOGON_CHALLENGE, byteBuf))

    super.channelActive(ctx)
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    msg match {
      case msg: Packet =>
        msg.id match {
          case RealmPackets.CMD_AUTH_LOGON_CHALLENGE if logonState == 0 => handle_CMD_AUTH_LOGON_CHALLENGE(msg)
          case RealmPackets.CMD_AUTH_LOGON_PROOF if logonState == 1 => handle_CMD_AUTH_LOGON_PROOF(msg)
          case RealmPackets.CMD_REALM_LIST if logonState == 2 => handle_CMD_REALM_LIST(msg)
          case _ =>
            logger.info(f"Received packet ${msg.id}%04X in unexpected logonState $logonState")
            msg.byteBuf.release
            return
        }
        msg.byteBuf.release
        logonState += 1
      case msg =>
        logger.error(s"Packet is instance of ${msg.getClass}")
    }
  }

  private def handle_CMD_AUTH_LOGON_CHALLENGE(msg: Packet): Unit = {
    val error = msg.byteBuf.readByte // ?
    val result = msg.byteBuf.readByte
    if (!RealmPackets.AuthResult.isSuccess(result)) {
      logger.error(RealmPackets.AuthResult.getMessage(result))
      ctx.get.close
      realmConnectionCallback.error
      return
    }

    val B = toArray(msg.byteBuf, 32)
    val gLength = msg.byteBuf.readByte
    val g = toArray(msg.byteBuf, gLength)
    val nLength = msg.byteBuf.readByte
    val n = toArray(msg.byteBuf, nLength)
    val salt = toArray(msg.byteBuf, 32)
    val unk3 = toArray(msg.byteBuf, 16)
    val securityFlag = msg.byteBuf.readByte
    if (securityFlag != 0) {
      logger.error(s"Two factor authentication is enabled for this account. Please disable it or use another account.")
      ctx.get.close
      realmConnectionCallback.error
      return
    }

    srpClient.step1(
      Global.config.wow.account,
      Global.config.wow.password,
      BigNumber(B),
      BigNumber(g),
      BigNumber(n),
      BigNumber(salt)
    )

    sessionKey = srpClient.K.asByteArray(40)

    val aArray = srpClient.A.asByteArray(32)
    val ret = PooledByteBufAllocator.DEFAULT.buffer(74, 74)
    ret.writeBytes(aArray)
    ret.writeBytes(srpClient.M.asByteArray(20, false))
    val md = MessageDigest.getInstance("SHA1")
    md.update(aArray)
    md.update(buildCrcHashes.getOrElse((WowChatConfig.getBuild, Global.config.wow.platform), new Array[Byte](20)))
    ret.writeBytes(md.digest)
    ret.writeByte(0)
    ret.writeByte(0)

    ctx.get.writeAndFlush(Packet(RealmPackets.CMD_AUTH_LOGON_PROOF, ret))
  }

  private def handle_CMD_AUTH_LOGON_PROOF(msg: Packet): Unit = {
    val result = msg.byteBuf.readByte

    if (!RealmPackets.AuthResult.isSuccess(result)) {
      logger.error(RealmPackets.AuthResult.getMessage(result))
      expectedDisconnect = true
      ctx.get.close
      if (result == RealmPackets.AuthResult.WOW_FAIL_UNKNOWN_ACCOUNT) {
        // seems sometimes this error happens even on a legit connect. so just run regular reconnect loop
        realmConnectionCallback.disconnected
      } else {
        realmConnectionCallback.error
      }
      return
    }

    val proof = toArray(msg.byteBuf, 20, false)
    if (!proof.sameElements(srpClient.generateHashLogonProof)) {
      logger.error("Logon proof generated by client and server differ. Something is very wrong! Will try to reconnect in a moment.")
      expectedDisconnect = true
      ctx.get.close
      // Also sometimes happens on a legit connect.
      realmConnectionCallback.disconnected
      return
    }

    val accountFlag = msg.byteBuf.readIntLE

    // ask for realm list
    logger.info(s"Successfully logged into realm server. Looking for realm ${Global.config.wow.realmlist.name}")
    val ret = PooledByteBufAllocator.DEFAULT.buffer(4, 4)
    ret.writeIntLE(0)
    ctx.get.writeAndFlush(Packet(RealmPackets.CMD_REALM_LIST, ret))
  }

  private def handle_CMD_REALM_LIST(msg: Packet): Unit = {
    val configRealm = Global.config.wow.realmlist.name

    val parsedRealmList = parseRealmList(msg)
    val realms = parsedRealmList
      .filter {
        case RealmList(name, _, _) => name.equalsIgnoreCase(configRealm)
      }

    if (realms.isEmpty) {
      logger.error(s"Realm $configRealm not found!")
      logger.error(s"${parsedRealmList.length} possible realms:")
      parsedRealmList.foreach(realm => logger.error(realm.name))
    } else if (realms.length > 1) {
      logger.error("Too many realms returned. Something is very wrong! This should never happen.")
    } else {
      val splt = realms.head.address.split(":")
      val port = splt(1).toInt & 0xFFFF // some servers "overflow" the port on purpose to dissuade rudimentary bots
      realmConnectionCallback.success(splt(0), port, realms.head.name, realms.head.realmId, sessionKey)
    }
    expectedDisconnect = true
    ctx.get.close
  }

  protected def parseRealmList(msg: Packet): Seq[RealmList] = {
    msg.byteBuf.readIntLE // unknown
    val numRealms = msg.byteBuf.readByte

    (0 until numRealms).map(i => {
      msg.byteBuf.skipBytes(4) // realm type (pvp/pve)
      val realmFlags = msg.byteBuf.readByte // realm flags (offline/recommended/for newbs)
      val name = if ((realmFlags & 0x04) == 0x04) {
        // On Vanilla MaNGOS, there is some string manipulation to insert the build information into the name itself
        // if realm flags specify to do so. But that is counter-intuitive to matching the config, so let's remove it.
        msg.readString.replaceAll(" \\(\\d+,\\d+,\\d+\\)", "")
      } else {
        msg.readString
      }
      val address = msg.readString
      msg.byteBuf.skipBytes(4) // population
      msg.byteBuf.skipBytes(1) // num characters
      msg.byteBuf.skipBytes(1) // timezone
      val realmId = msg.byteBuf.readByte

      RealmList(name, address, realmId)
    })
  }

  // Helper functions
  private def toArray(byteBuf: ByteBuf, size: Int, reverse: Boolean = true): Array[Byte] = {
    val ret = Array.newBuilder[Byte]
    (0 until size).foreach(_ => ret += byteBuf.readByte)
    if (reverse) {
      ret.result().reverse
    } else {
      ret.result()
    }
  }
}
