package truerss.services.actors.events

import akka.actor.{Actor, ActorLogging, Props}
import akka.event.EventStream
import com.github.truerss.base.Entry
import truerss.api.ws.WebSocketController
import truerss.dto.SourceViewDto
import truerss.util.TaskImplicits
import truerss.services.{FeedsService, SourcesService, StreamProvider}

class EventHandlerActor(private val sourcesService: SourcesService,
                        feedsService: FeedsService)
  extends Actor with ActorLogging with StreamProvider {

  import EventHandlerActor._
  import TaskImplicits._

  override val stream: EventStream = context.system.eventStream

  def receive: Receive = {
    case RegisterNewFeeds(sourceId, entries) =>
      val f = for {
        feeds <- feedsService.registerNewFeeds(sourceId, entries)
        _ <- fire(PublishPluginActor.NewEntriesReceived(feeds)).when(feeds.nonEmpty)
        _ <- fire(WebSocketController.NewFeeds(feeds)).when(feeds.nonEmpty)
      } yield ()
      f.materialize

    case ModifySource(sourceId) =>
      sourcesService.changeLastUpdateTime(sourceId).materialize

    case NewSourceCreated(source) =>
      fire(WebSocketController.NewSource(source)).materialize

  }
}

object EventHandlerActor {
  def props(sourcesService: SourcesService,
            feedsService: FeedsService): Props = {
    Props(classOf[EventHandlerActor], sourcesService, feedsService)
  }

  sealed trait EventHandlerActorMessage
  case class RegisterNewFeeds(sourceId: Long, entries: Iterable[Entry]) extends EventHandlerActorMessage
  case class ModifySource(sourceId: Long) extends EventHandlerActorMessage
  case class NewSourceCreated(source: SourceViewDto) extends EventHandlerActorMessage
}