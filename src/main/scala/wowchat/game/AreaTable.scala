package wowchat.game

import wowchat.common.{WowChatConfig, WowExpansion}

import scala.io.Source

object AreaTable {

  lazy val AREA: Map[Int, String] = readAreaTableFile(WowChatConfig.getExpansion match {
    case WowExpansion.Vanilla | WowExpansion.TBC | WowExpansion.WotLK => "pre_cata_areas.csv"
    case WowExpansion.Cataclysm => "post_cata_areas.csv"
  })

  private def readAreaTableFile(file: String) = {
    Source
      .fromResource(file)
      .getLines
      .map(str => {
        val splt = str.split(",", 2)
        splt(0).toInt -> splt(1)
      })
      .toMap
  }
}
