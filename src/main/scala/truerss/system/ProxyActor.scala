package truerss.system

import akka.actor.{ActorRef, Actor}
import akka.util.Timeout
import akka.pattern._
import akka.event.LoggingReceive
import truerss.controllers.BadRequestResponse

import scala.language.postfixOps
import scala.concurrent.duration._

import truerss.util.SourceValidator
import scala.concurrent.Future
import scalaz._
import Scalaz._
/**
  * Created by mike on 2.8.15.
 */
class ProxyActor(dbRef: ActorRef) extends Actor {

  import truerss.controllers.{ModelsResponse, ModelResponse, NotFoundResponse}
  import db._
  import truerss.models.{Source, Feed}
  import context.dispatcher

  implicit val timeout = Timeout(7 seconds)

  def sourceNotFound(num: Long) = NotFoundResponse(s"Source with id = ${num} not found")

  def receive = LoggingReceive {
    case GetAll => (dbRef ? GetAll).mapTo[Vector[Source]].map(ModelsResponse(_)) pipeTo sender

    case msg: GetSource => (dbRef ? msg).mapTo[Option[Source]].map{
      case Some(x) => ModelResponse(x)
      case None => sourceNotFound(msg.num)
    } pipeTo sender

    case msg: DeleteSource => (dbRef ? msg).mapTo[Option[Source]].map {
      case Some(source) => ModelResponse(source)
      case None => sourceNotFound(msg.num)
    } pipeTo sender

    case msg: AddSource =>
      (SourceValidator.validate(msg.source) match {
        case Right(source) =>
          (for {
            urlIsUniq <- (dbRef ? UrlIsUniq(msg.source.url)).mapTo[Int]
            nameIsUniq <- (dbRef ? NameIsUniq(msg.source.name)).mapTo[Int]
          } yield {
            if (urlIsUniq == 0 && nameIsUniq == 0) {
              (dbRef ? msg).mapTo[Long]
                .map{x => ModelResponse(msg.source.copy(id = Some(x)))}
            } else {
              val urlError = if (urlIsUniq > 0) {
                "Url already present in db"
              } else { "" }
              val nameError = if(nameIsUniq > 0) {
                "Name not unique"
              } else {
                ""
              }
              Future.successful(BadRequestResponse(Vector(urlError, nameError).mkString(", ")))
            }
          }).flatMap(identity(_))

        case Left(errs) => Future.successful(
          BadRequestResponse(errs.toList.mkString(", ")))
      }) pipeTo sender





  }


}
