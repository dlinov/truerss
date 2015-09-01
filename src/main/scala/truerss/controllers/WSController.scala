package truerss.controllers

import akka.actor.{ActorLogging, Actor}
import org.java_websocket.WebSocket

import truerss.system.ws.{SourceAdded, NewFeeds}
import truerss.models.{ApiJsonProtocol, WSMessage}
import spray.json._
/**
 * Created by mike on 29.8.15.
 */
class WSController(ws: WebSocket) extends Actor with ActorLogging {

  import ApiJsonProtocol._

  def receive = {
    case SourceAdded(source) =>
      ws.send(s"${WSMessage("create", source.toJson.toString).toJson}")
    case NewFeeds(xs) =>
      ws.send(s"${WSMessage("new", xs.toJson.toString()).toJson}")
    case _ => //ws.send()
  }

}