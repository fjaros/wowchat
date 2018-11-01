package wowchat.game

import wowchat.common.{WowChatConfig, WowExpansion}

import scala.io.Source

object GameResources {

  lazy val AREA: Map[Int, String] = readIDNameFile(WowChatConfig.getExpansion match {
    case WowExpansion.Vanilla | WowExpansion.TBC | WowExpansion.WotLK => "pre_cata_areas.csv"
    case _ => "post_cata_areas.csv"
  })

  lazy val ACHIEVEMENT: Map[Int, String] = readIDNameFile("achievements.csv")

  private def readIDNameFile(file: String) = {
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
