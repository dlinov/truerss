package truerss.controllers

import com.github.fntzr.spray.routing.ext.BaseController
import spray.http.{HttpCookie, MediaTypes}
import spray.routing.HttpService
import scala.io.Source

trait MainController extends BaseController with WsPortProvider with Redirectize {

  import java.net.URLEncoder
  import java.nio.charset.Charset
  val utf8 = Charset.forName("UTF-8").name()
  import HttpService._

  val fileName = "index.html"
  val siteFile = "site.html"
  val html = MediaTypes.`text/html`

  def root = {
    respondWithMediaType(html) {
      complete {
        Source.fromInputStream(getClass.getResourceAsStream(s"/$siteFile")).mkString
      }
    }
  }

  def demo = {
    optionalHeaderValueByName(Redirect) { mbRedirect =>
      setCookie(HttpCookie("port", content = s"$wsPort"),
        HttpCookie(Redirect,
          content = URLEncoder.encode(mbRedirect.getOrElse("/"), utf8),
          path = Some("/")
        )
      ) {
        respondWithMediaType(html) {
          complete(Source.fromInputStream(getClass.getResourceAsStream(s"/$fileName")).mkString)
        }
      }
    }
  }

  def about = {
    complete("""
      <h1>About</h1>
      <p>
        TrueRss is open source feed reader with customizable plugin system
        for any content (atom, rss, youtube channels...).
        More info <a href='http://truerss.net'>truerss official site</a>
        Download plugins: <a href='http://truerss.net/download/plugins'>plugins</a>
      </p>
             """.stripMargin)
  }



}
