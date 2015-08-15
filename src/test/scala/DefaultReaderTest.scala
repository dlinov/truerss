
import java.net.ServerSocket

import akka.actor._
import akka.io.IO
import akka.pattern._
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import spray.can.Http
import spray.testkit.ScalatestRouteTest

import truerss.plugins.DefaultSiteReader

import scala.concurrent.Await
import scala.concurrent.duration._



class DefaultReaderTest extends FunSpec with Matchers
with ScalatestRouteTest with BeforeAndAfterAll {

  implicit val timeout = Timeout(10 seconds)

  override def beforeAll() = {}

  val rssServer = system.actorOf(Props[Server])
  val host = "localhost"
  val port = Await.result((IO(Http) ?
    Http.Bind(rssServer, interface = host, port = 0))
    .mapTo[akka.io.Tcp.Bound], timeout.duration).localAddress.getPort

  val s = new ServerSocket(0)
  val freePort = s.getLocalPort
  s.close()

  println(s"\n port = ${port}; free port = ${freePort}")

  val url = s"http://$host:$port"
  val okRss = s"$url/ok-rss"
  val badRss = s"${url}/bad-rss"
  val failUrl = s"http://$host:$freePort"
  val content1Url = s"$url/content1"
  val content2Url = s"$url/content2"

  val defaultReader = new DefaultSiteReader(Map.empty)

  describe("matchUrl") {
    it("match any url") {
      defaultReader.matchUrl(url) should be(true)
      defaultReader.matchUrl("https://www.youtube.com") should be(true)
      defaultReader.matchUrl("https://news.ycombinator.com/") should be(true)
    }
  }

  describe("newEntries") {
    it("return new entries when parse valid rss or atom") {
      val result = defaultReader.newEntries(okRss)
      result.isRight should be(true)
      val entries = result.right.get
      entries should have size 3
      entries.map(_.title) should contain allOf (
        "Brains Sweep Themselves Clean of Toxins During Sleep (2013)",
        "Memory Efficient Hard Real-Time Garbage Collection [pdf]",
        "The US digital service"
      )
    }

    it("return error when parse failed") {
      val result = defaultReader.newEntries(badRss)
      result.isLeft should be(true)
      result.left.get should be("Invalid XML: Error on line 1: Content is not allowed in prolog.")
    }

    it("return error when connection failed") {
      val result = defaultReader.newEntries(failUrl)
      result.isLeft should be(true)
    }
  }

  describe("Content") {
    it("extract content") {
      val result = defaultReader.content(content1Url)
      result.isRight should be(true)
      result.right.get.get should include("The US digital service")
    }

    it("when no content") {
      val result = defaultReader.content(content2Url)
      result.isRight should be(true)
      result.right.get should be(None)
    }
  }

  override def afterAll() = {
    system.shutdown()
  }

}