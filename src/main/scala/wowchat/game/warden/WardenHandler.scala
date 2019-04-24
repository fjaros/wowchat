package wowchat.game.warden

import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.Inflater

import wowchat.common.{ByteUtils, Packet}
import wowchat.game.RC4
import com.typesafe.scalalogging.StrictLogging
import io.netty.buffer.{ByteBuf, PooledByteBufAllocator}

class WardenHandler(sessionKey: Array[Byte]) extends StrictLogging {

  protected val WARDEN_MODULE_LENGTH = 16

  private val sha1Randx = new SHA1Randx(sessionKey)
  private var clientCrypt = new RC4(sha1Randx.generate(16))
  private var serverCrypt = new RC4(sha1Randx.generate(16))
  private var moduleCrypt: RC4 = _

  private var moduleName = ""
  private val moduleSeed: Array[Byte] = new Array[Byte](16)
  private var moduleLength = 0
  private var module: Option[ByteBuf] = None

  def handle(msg: Packet): (Int, Option[ByteBuf]) = {
    val length = getEncryptedMessageLength(msg)
    val decrypted = serverCrypt.crypt(msg.byteBuf, length)
    logger.debug(s"WARDEN PACKET ($length): ${ByteUtils.toHexString(decrypted, true, false)}")

    val id = decrypted.readByte

    val ret = id match {
      case WardenPackets.WARDEN_SMSG_MODULE_USE => handle_WARDEN_SMSG_MODULE_USE(decrypted)
      case WardenPackets.WARDEN_SMSG_MODULE_CACHE => handle_WARDEN_SMSG_MODULE_CACHE(decrypted)
      case WardenPackets.WARDEN_SMSG_CHEAT_CHECKS_REQUEST => handle_WARDEN_SMSG_CHEAT_CHECKS_REQUEST(decrypted)
      case WardenPackets.WARDEN_SMSG_MODULE_INITIALIZE => None
      case WardenPackets.WARDEN_SMSG_MEM_CHECKS_REQUEST => None
      case WardenPackets.WARDEN_SMSG_HASH_REQUEST => handle_WARDEN_SMSG_HASH_REQUEST(decrypted)
      case _ => None
    }
    decrypted.release
    (id, ret)
  }

  protected def getEncryptedMessageLength(msg: Packet): Int = {
    msg.byteBuf.readableBytes
  }

  protected def formResponse(out: ByteBuf): ByteBuf = {
    out
  }

  protected def formCheatChecksRequestDigest(ret: ByteBuf, key: Array[Byte]): Unit = {
    val mdSHA1 = MessageDigest.getInstance("SHA1")
    mdSHA1.update(key)
    mdSHA1.update(ByteUtils.intToBytesLE(0xFEEDFACE))
    ret.writeBytes(mdSHA1.digest)

    val mdMD5 = MessageDigest.getInstance("MD5")
    mdMD5.update(key)
    ret.writeBytes(mdMD5.digest)
  }

  // sent by server at beginning of warden handshake. contains module name & its rc4 seed
  private def handle_WARDEN_SMSG_MODULE_USE(decrypted: ByteBuf): Option[ByteBuf] = {
    val moduleNameArray = new Array[Byte](WARDEN_MODULE_LENGTH)
    decrypted.readBytes(moduleNameArray)
    moduleName = moduleNameArray.map(byte => f"$byte%02X").mkString
    decrypted.readBytes(moduleSeed)
    moduleLength = decrypted.readIntLE
    moduleCrypt = new RC4(moduleSeed)

    // we can send WARDEN_CMSG_MODULE_MISSING if we need to download the module, or WARDEN_CMSG_MODULE_OK if we have it
    Some(formResponse(clientCrypt.crypt(WardenPackets.WARDEN_CMSG_MODULE_OK.toByte)))
  }

  // sent by server while sending us the module payload in case we do not yet have it
  private def handle_WARDEN_SMSG_MODULE_CACHE(decrypted: ByteBuf): Option[ByteBuf] = {
    val length = decrypted.readShortLE
    val compressedBytes = decrypted.readBytes(length)

    if (module.isEmpty) {
      module = Some(PooledByteBufAllocator.DEFAULT.buffer(moduleLength, moduleLength))
    }

    module.get.writeBytes(compressedBytes)
    if (module.get.writableBytes == 0) {
      // downloading module is completed, unzip it
      val moduleArr = new Array[Byte](moduleLength)
      module.get.readBytes(moduleArr)
      val unencryptedModule = moduleCrypt.crypt(moduleArr)
      val decompressedLength = unencryptedModule.readIntLE
      val unencryptedModuleArr = new Array[Byte](moduleLength - 4)
      unencryptedModule.readBytes(unencryptedModuleArr)

      val inflater = new Inflater
      inflater.setInput(unencryptedModuleArr)
      val decompressedData = new Array[Byte](decompressedLength)
      val count = inflater.inflate(decompressedData)
      inflater.end()
      val fos = new FileOutputStream(moduleName + ".bin")
      fos.write(decompressedData)
      fos.close()

      Some(formResponse(clientCrypt.crypt(WardenPackets.WARDEN_CMSG_MODULE_OK.toByte)))
    } else {
      None
    }
  }

  private def handle_WARDEN_SMSG_CHEAT_CHECKS_REQUEST(decrypted: ByteBuf): Option[ByteBuf] = {
    val ret = PooledByteBufAllocator.DEFAULT.buffer(53, 53)

    val strLength = decrypted.readByte
    val strArray = new Array[Byte](strLength)
    decrypted.readBytes(strArray)

    ret.writeByte(WardenPackets.WARDEN_CMSG_CHEAT_CHECKS_RESULT)

    formCheatChecksRequestDigest(ret, strArray)
    val encrypted = clientCrypt.crypt(ret, ret.readableBytes)
    ret.release
    Some(formResponse(encrypted))
  }

  private def handle_WARDEN_SMSG_HASH_REQUEST(decrypted: ByteBuf): Option[ByteBuf] = {
    val ret = PooledByteBufAllocator.DEFAULT.buffer(21, 21)

    val clientKey = new Array[Int](4)
    val serverKey = new Array[Int](4)
    var serverKey1 = 0
    var serverKey2 = 0
    (0 until 4).foreach(clientKey(_) = decrypted.readIntLE)

    serverKey(0) = clientKey(0)
    clientKey(0) = clientKey(0) ^ 0xDEADBEEF
    serverKey1 = clientKey(1)
    clientKey(1) -= 0x35014542
    serverKey2 = clientKey(2)
    clientKey(2) += 0x5313F22
    clientKey(3) *= 0x1337F00D
    serverKey(1) = serverKey1 - 0x6A028A84
    serverKey(2) = serverKey2 + 0xA627E44
    serverKey(3) = 0x1337F00D * clientKey(3)

    val clientKeyBuf = PooledByteBufAllocator.DEFAULT.buffer(16, 16)
    clientKey.foreach(clientKeyBuf.writeIntLE)

    val clientKeyBytes = new Array[Byte](16)
    clientKeyBuf.readBytes(clientKeyBytes)
    clientKeyBuf.release
    val md = MessageDigest.getInstance("SHA1")
    md.update(clientKeyBytes)

    ret.writeByte(WardenPackets.WARDEN_CMSG_HASH_RESULT)
    ret.writeBytes(md.digest)

    val encrypted = clientCrypt.crypt(ret, ret.readableBytes)
    ret.release

    // change crypto keys based on module for next message
    clientCrypt = new RC4(clientKeyBytes)

    val serverKeyBuf = PooledByteBufAllocator.DEFAULT.buffer(16, 16)
    serverKey.foreach(serverKeyBuf.writeIntLE)
    val serverKeyOutBytes = new Array[Byte](16)
    serverKeyBuf.readBytes(serverKeyOutBytes)
    serverKeyBuf.release
    serverCrypt = new RC4(serverKeyOutBytes)

    Some(formResponse(encrypted))
  }
}
