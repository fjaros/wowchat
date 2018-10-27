package wowchat.common

import com.typesafe.scalalogging.StrictLogging

class ReconnectDelay extends StrictLogging {

  private var reconnectDelay: Option[Int] = None

  def reset: Unit = {
    reconnectDelay = None
  }

  def getNext: Int = {
    synchronized {
      reconnectDelay = reconnectDelay.map {
        case 10 => 30
        case 30 => 60
        case 60 => 180
        case _ => 300
      }.orElse(Some(10))

      val result = reconnectDelay.get
      logger.debug(s"GET RECONNECT DELAY $result")
      result
    }
  }
}
