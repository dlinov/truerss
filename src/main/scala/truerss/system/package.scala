package truerss

import truerss.models.{SourceForFrontend, Feed, Source}
import com.github.truerss.base.{Entry, BaseFeedReader, BaseContentReader}

package object system {

  sealed trait BaseMessage
  // for communication with db
  object db {
    // TODO remove this traits, make generic
    trait Numerable { val num: Long }
    trait Sourcing { val source: Source }

    case object OnlySources extends BaseMessage
    case object GetAll extends BaseMessage
    case class GetSource(num: Long) extends BaseMessage with Numerable
    case class MarkAll(num: Long) extends BaseMessage with Numerable
    case class DeleteSource(num: Long) extends BaseMessage with Numerable
    case class AddSource(source: Source) extends BaseMessage with Sourcing
    case class UpdateSource(num: Long, source: Source) extends BaseMessage with Sourcing

    case object Favorites extends BaseMessage
    case class GetFeed(num: Long) extends BaseMessage
    case class MarkFeed(num: Long) extends BaseMessage
    case class UnmarkFeed(num: Long) extends BaseMessage
    case class MarkAsReadFeed(num: Long) extends BaseMessage
    case class MarkAsUnreadFeed(num: Long) extends BaseMessage

    case class AddFeeds(sourceId: Long, xs: Vector[Feed]) extends BaseMessage


    case class Latest(count: Long) extends BaseMessage
    case class ExtractFeedsForSource(sourceId: Long) extends BaseMessage

    // util:
    case class UrlIsUniq(url: String, id: Option[Long] = None) extends BaseMessage
    case class NameIsUniq(name: String, id: Option[Long] = None) extends BaseMessage
    case class FeedCount(read: Boolean = false) extends BaseMessage

  }

  object network {
    case class SourceInfo(sourceId: Long, feedReader: BaseFeedReader,
                           contentReader: BaseContentReader)
    case class Grep(sourceId: Long, url: String)
    case class ExtractContent(sourceId: Long, feedId: Long, url: String)
    case class NetworkInitialize(xs: Vector[SourceInfo])

    sealed trait NetworkResult

    case class ExtractedEntries(sourceId: Long, entries: Vector[Entry]) extends NetworkResult
    case class ExtractContentForEntry(sourceId: Long, feedId: Long, content: Option[String]) extends NetworkResult
    case class ExtractError(message: String) extends NetworkResult
    case class SourceNotFound(sourceId: Long) extends NetworkResult
  }

  object util {
    case object Start extends BaseMessage
    case object Update extends BaseMessage
    case class UpdateOne(num: Long) extends BaseMessage

    case class SourceLastUpdate(sourceId: Long)
    case class FeedContentUpdate(feedId: Long, content: String)
  }

  object ws {
    case class SourceAdded(source: SourceForFrontend)
    case class SourceUpdated(source: SourceForFrontend)
    case class NewFeeds(xs: Vector[Feed])
  }


}
