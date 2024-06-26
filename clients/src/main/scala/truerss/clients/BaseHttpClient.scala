package truerss.clients

import org.slf4j.LoggerFactory
import play.api.libs.json.{Json, Reads}
import scalaj.http.{Http, HttpRequest, HttpResponse}
import zio.internal.Blocking
import zio.{Task, ZIO}

class BaseHttpClient(val baseUrl: String) {

  import JsonSupport._

  protected val logger = LoggerFactory.getLogger(getClass)

  protected val api = "api/v1"

  protected def put[T: Reads](url: String): Task[T] = {
    handleRequest[T](Http(url).method("PUT"))
  }

  protected def rawGet(url: String): Task[String] = {
    sendRequest(Http(url).method("GET")).map(_.body)
  }

  protected def put[T: Reads](url: String, body: String): Task[T] = {
    handleRequest[T](Http(url).put(body))
  }

  protected def get[T: Reads](url: String): Task[T] = {
    handleRequest[T](Http(url).method("GET"))
  }

  protected def post[T: Reads](url: String, body: String): Task[T] = {
    handleRequest[T](Http(url).postData(body))
  }

  protected def delete[T: Reads](url: String): Task[T] = {
    handleRequest[T](Http(url).method("DELETE"))
  }

  protected def handleRequest[T: Reads](req: HttpRequest): Task[T] = {
    val method = req.method
    val url = req.url
    logger.info(s"Send request: [$method] ($url)")

    sendRequest(req).flatMap { response =>
      val code = response.code
      val body = response.body
      if (response.isError) {
        logger.warn(s"Failed to process request: [$method] ($url) -> $code: $body")
        if (code == 404) {
          ZIO.fail(EntityNotFoundError)
        } else {
          Json.parse(body).asOpt[ReasonableError] match {
            case Some(r) => ZIO.fail(r)
            case None => ZIO.fail(UnexpectedError(body, code))
          }
        }
      } else {
        logger.info(s"Request is done: [$method] ($url) -> $code")
        // todo
        if (code == 204) {
          ZIO.succeed(()).asInstanceOf[Task[T]]
        } else {
          ZIO.attempt(Json.parse(body).as[T])
        }
      }
    }

  }

  protected def sendRequest(req: HttpRequest): Task[HttpResponse[String]] = {
    ZIO.attemptBlocking(req.asString)
  }


}

