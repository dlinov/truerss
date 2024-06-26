package truerss.api.ws

import play.api.libs.json._

object WebSocketJsonFormats {

  def convert[T: Writes](x: T): String = Json.stringify(Json.toJson(x))

  implicit val notifyLevelWrites: Writes[Notify] = Json.writes[Notify]

  implicit lazy val wSMessageTypeWrites: Writes[WSMessageType.type] = new Writes[WSMessageType.type] {
    override def writes(o: WSMessageType.type): JsValue = {
      JsString(o.toString())
    }
  }

  implicit lazy val wsMessageWrites: OWrites[WebSocketMessage] = Json.writes[WebSocketMessage]
}
