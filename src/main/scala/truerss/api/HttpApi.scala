package truerss.api

import com.github.fntz.omhs.{BodyWriter, CommonResponse}
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.util.CharsetUtil

import play.api.libs.json._
import truerss.services.{ContentReadError, NotFoundError, ValidationError}
import zio.Task

import scala.reflect.ClassTag
import scala.util.{Failure, Try, Success => S}
/**
  * Created by mike on 17.12.16.
  */
trait HttpApi {

  import ZIOSupport._
  import HttpApi._

  def w[T](task: Task[T])(implicit writer: BodyWriter[T]): Task[CommonResponse] = {
    task.map {
      case _: Processing =>
        accepted
      case _: Unit =>
        noContent
      case x =>
        writer.write(x)
    }.fold({
      case ValidationError(errors) =>
        CommonResponse(
          status = HttpResponseStatus.BAD_REQUEST,
          contentType = json,
          content = HttpApi.printErrors(errors).getBytes(CharsetUtil.UTF_8)
        )
      case ContentReadError(error) =>
        CommonResponse(
          status = HttpResponseStatus.INTERNAL_SERVER_ERROR,
          contentType = json,
          content = HttpApi.printErrors(error :: Nil).getBytes(CharsetUtil.UTF_8)
        )
      case NotFoundError(_) => notFound
      case _ => internal
    }, identity)
  }
  /*
  import RouteResult._
  import StatusCodes._
  import HttpApi.{utf8, printErrors}

  protected val logger = LoggerFactory.getLogger(getClass)

  protected val api = pathPrefix("api" / "v1")

  def createTR[T : Reads : ClassTag, R: Writes](f: T => Task[R]): Route = {
    entity(as[String]) { json =>
      Try(Json.parse(json).validateOpt[T]) match {
        case S(JsSuccess(Some(value), _)) =>
          call(f(value))

        case S(JsSuccess(_, _)) =>
          complete(response(BadRequest, s"Unable to parse request: $json"))

        case S(JsError(errors)) =>
          val str = errors.map(x => s"${x._1}: ${x._2.flatMap(_.messages).mkString(", ")}")
          complete(response(BadRequest, s"Unable to parse request: $str"))

        case Failure(_) =>
          complete(response(BadRequest, s"Unable to parse request: $json"))
      }
    }
  }

  def w[W: Writes](f: Task[W]): Route = call(f)

  def doneWith(f: Task[String], contentType: C): Route = {
    val tmp = f.map { x =>
      complete(flush(contentType, x))
    }
    zio.Runtime.default.unsafeRunTask(tmp)
  }

  def call[W: Writes](f: Task[W]): Route = {
    val taskResult = f.map { r =>
      val result = r match {
        case _: Processing =>
          finish(Accepted, "")
        case _: Unit =>
          finish(NoContent, "")
        case _ =>
          finish(OK, Json.stringify(Json.toJson(r)))
      }

      result match {
        case Complete(response) =>
        response
        case Rejected(_) =>
          response(InternalServerError, "Rejected")
      }
    }.fold({
        case ValidationError(errors) =>
          HttpResponse(
            status = BadRequest,
            entity = HttpEntity.apply(printErrors(errors))
          )

        case ContentReadError(error) =>
          HttpResponse(
            status = InternalServerError,
            entity = HttpEntity.apply(printErrors(error :: Nil))
          )

        case NotFoundError(_) =>
          HttpResponse(
            status = NotFound,
            entity = HttpEntity.empty(ContentTypes.`application/json`)
          )

        case _ =>
          HttpResponse(
            status = InternalServerError,
            entity = HttpEntity.empty(ContentTypes.`application/json`)
          )

      },
      identity)
    .map(complete(_))

    zio.Runtime.default.unsafeRunTask(taskResult)
  }


  private def flush(cnt: C, content: String) = {
    val entity = HttpEntity.apply(
      contentType = cnt,
      content.getBytes(utf8)
    )

    HttpResponse(
      status = OK,
      entity = entity
    )
  }

  private def response(status: StatusCode, msg: String) = {
    val entity = status match {
      case StatusCodes.OK =>
        HttpEntity.apply(ContentTypes.`application/json`, msg)

      case StatusCodes.NoContent =>
        HttpEntity.empty(ContentTypes.`application/json`)

      case _ =>
        HttpEntity.apply(ContentTypes.`application/json`, s"""{"error": "$msg"}""")
    }
    HttpResponse(
      status = status,
      entity = entity
    )
  }

  private def finish(status: StatusCode, msg: String): RouteResult = {
    RouteResult.Complete(
      response(status, msg)
    )
  } */

}

object HttpApi extends HttpApi {

  val accepted = CommonResponse.empty.copy(status = HttpResponseStatus.ACCEPTED)
  val noContent = CommonResponse.empty.copy(status = HttpResponseStatus.NO_CONTENT)
  val notFound = CommonResponse.empty.copy(status = HttpResponseStatus.NOT_FOUND)
  val internal = CommonResponse.empty.copy(status = HttpResponseStatus.INTERNAL_SERVER_ERROR)

  private final val json = "application/json"
  private final val errorF = "errors"
//  val utf8 = Charset.forName("UTF-8")
//  val javascript = MediaTypes.`application/javascript` withCharset HttpCharsets.`UTF-8`
//  val css = MediaTypes.`text/css` withCharset HttpCharsets.`UTF-8`
//  val opml = MediaTypes.`application/xml` withCharset HttpCharsets.`UTF-8`

  def printErrors(errors: List[String]): String = {
    Json.stringify(JsObject(Seq(errorF -> JsArray(errors.map(JsString)))))
  }

}