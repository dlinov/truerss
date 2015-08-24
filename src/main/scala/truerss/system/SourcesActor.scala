package truerss.system

import akka.actor._
import akka.actor.SupervisorStrategy.Restart
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.util.Timeout
import truerss.controllers._
import truerss.plugins.DefaultSiteReader
import truerss.system.network.NetworkInitialize

import scala.concurrent.duration._

import truerss.models.Source
/**
 * Created by mike on 9.8.15.
 */
class SourcesActor(proxyRef: ActorRef) extends Actor with ActorLogging {

  import db.{GetAll, GetSource}
  import network._
  import util._
  import context.dispatcher
  implicit val timeout = Timeout(30 seconds)

  var sourcesCount: Long = 0
  var updated = 0
  val maxUpdateCount = 10

  override val supervisorStrategy = OneForOneStrategy(
      maxNrOfRetries = 3,
      withinTimeRange = 1 minute) {
    case x: Throwable =>
      log.error(x, x.getMessage)
      Restart
  }

  context.system.scheduler.scheduleOnce(0 seconds, self, Start)


  def receive = LoggingReceive {
    case Start => (proxyRef ? GetAll).mapTo[ModelsResponse[Source]].map { sources =>
      log.info(s"Given ${sources.xs.size} sources")
      sourcesCount = sources.xs.size
      val info = sources.xs.map(x => SourceInfo(x.id.get, new DefaultSiteReader(Map.empty)))
      context.parent ! NetworkInitialize(info)
      sources.xs.foreach { source =>
        log.info(s"Start source actor for ${source.normalized} -> ${source.id.get}")
        context.actorOf(Props(new SourceActor(source)), s"source-${source.id.get}")
      }
    }

    case Update =>
      log.info(s"Update for ${context.children.size} actors")
      context.children.foreach{ _ ! Update }
      sender ! OkResponse("updated")

    case UpdateOne(num) =>
      context.actorSelection(s"*/source-${num}") ! Update

    case x => context.parent forward x
  }


}
