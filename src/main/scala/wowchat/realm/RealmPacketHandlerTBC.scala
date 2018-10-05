package wowchat.realm
import wowchat.common.Packet

class RealmPacketHandlerTBC(realmConnectionCallback: RealmConnectionCallback)
  extends RealmPacketHandler(realmConnectionCallback) {

  override protected def parseRealmList(msg: Packet): Seq[RealmList] = {
    msg.byteBuf.readIntLE // unknown
    val numRealms = msg.byteBuf.readShortLE

    (0 until numRealms).map(i => {
      // BC/wotlk are slightly different
      msg.byteBuf.skipBytes(1) // realm type (pvp/pve)
      msg.byteBuf.skipBytes(1) // lock flag
      val realmFlags = msg.byteBuf.readByte // realm flags (offline/recommended/for newbs)
      val name = msg.readString
      val address = msg.readString
      msg.byteBuf.skipBytes(4) // population
      msg.byteBuf.skipBytes(1) // num characters
      msg.byteBuf.skipBytes(1) // timezone
      val realmId = msg.byteBuf.readByte

      // BC/wotlk include build information in the packet
      if ((realmFlags & 0x04) == 0x04) {
        // includes build information
        msg.byteBuf.skipBytes(5)
      }

      RealmList(name, address, realmId)
    })
  }
}
