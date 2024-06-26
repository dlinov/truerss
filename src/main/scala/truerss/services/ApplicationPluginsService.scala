package truerss.services

import java.net.URI
import com.github.truerss.base._
import com.typesafe.config.{Config, ConfigFactory}
import truerss.dto.{PluginDto, PluginsViewDto, SourceViewDto, State}
import truerss.db.{SourceState, SourceStates}
import truerss.plugins.{ApplicationPlugins, PluginLoader, PluginWithSourcePath}
import zio.{UIO, ZIO}

import scala.util.Try

class ApplicationPluginsService(private val pluginDir: String, private val config: Config) {

  import ApplicationPluginsService._
  private type CR = BaseContentReader with UrlMatcher with Priority with PluginInfo

  @volatile protected var currentState: ApplicationPlugins = ApplicationPlugins()

  private val empty = ConfigFactory.empty()

  def reload(): Unit = {
    val c = Try(config.getConfig(fPlugins)).getOrElse(empty)
    currentState = PluginLoader.load(pluginDir, c)
  }

  def publishPlugins: Vector[BasePublishPlugin] =
    currentState.publishPlugins.map(_.plugin)

  def js: UIO[String] = ZIO.succeed(currentState.js.mkString)

  def css: UIO[String] = ZIO.succeed(currentState.css.mkString)

  def getState(url: String): SourceState = {
    currentState.getState(url)
  }

  def matchUrl(url: String): Boolean = {
    Try(matchUrl(URI.create(url))).getOrElse(false)
  }

  def matchUrl(url: URI): Boolean = {
    currentState.inFeed(url) ||
      currentState.inContent(url) ||
      currentState.inSite(url)
  }

  def getFeedReader(url: URI): Option[BasePlugin] = {
    currentState.getFeedReader(url)
  }

  def getContentReader(url: URI): Option[BasePlugin] = {
    currentState.getContentReader(url)
  }

  def getContentReaderOrDefault(url: URI): BasePlugin = {
    currentState.getContentReaderOrDefault(url)
  }

  def view: UIO[PluginsViewDto] = {
    ZIO.succeed {
      PluginsViewDto(
        feed = currentState.feedPlugins.map(baseToDto),
        content = currentState.contentPlugins.map(baseToDto),
        publish = currentState.publishPlugins.map(baseToDto),
        site = currentState.sitePlugins.map(baseToDto)
      )
    }
  }

  def getSourceReader(source: SourceViewDto): BaseFeedReader = {
    val url = URI.create(source.url)
    source.state match {
      case State.Enable =>
        currentState.getSourceReader(url)
      case _ =>
        currentState.defaultPlugin
    }
  }

}

object ApplicationPluginsService {
  private val fPlugins = "plugins"
  private def baseToDto[T <: PluginInfo](x: PluginWithSourcePath[T]): PluginDto = {
    PluginDto(
      author = x.plugin.author,
      about = x.plugin.about,
      version = x.plugin.version,
      pluginName = x.plugin.pluginName,
      jarSourcePath = x.jarSourcePath
    )
  }

  implicit class ApplicationPluginsExt(val a: ApplicationPlugins) extends AnyVal {
    def getState(url: String): SourceState = {
      if (a.matchUrl(url)) {
        SourceStates.Enable
      } else {
        SourceStates.Neutral
      }
    }
  }
}