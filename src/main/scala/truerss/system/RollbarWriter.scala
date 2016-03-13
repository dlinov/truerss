package truerss.system

import akka.actor.{ActorLogging, Actor}
import com.github.truerss.rollbar._

class RollbarWriter extends Actor with ActorLogging {
  import RollbarWriter._
  val accessToken = sys.env("ROLLBAR_TOKEN")

  val builder = NotifyBuilder(accessToken, "website")

  def receive = {
    case RInfo(msg) =>
      Sender.send(builder.info(msg))
    case RWarning(msg) =>
      Sender.send(builder.warning(msg))
    case RError(msg) =>
      Sender.send(builder.error(msg))
  }


}
object RollbarWriter {
  case class RInfo(msg: String)
  case class RWarning(msg: String)
  case class RError(msg: String)
}