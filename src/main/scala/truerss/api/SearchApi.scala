package truerss.api

import truerss.dto.{FeedDto, Page, SearchRequest}
import truerss.services.SearchService
import com.github.fntz.omhs.{BodyReader, BodyWriter, RoutingDSL}
import com.github.fntz.omhs.playjson.JsonSupport

class SearchApi(private val searchService: SearchService) extends HttpApi {

  import JsonFormats._
  import RoutingDSL._
  import ZIOSupport._

  private implicit val searchRequestReader: BodyReader[SearchRequest] =
    JsonSupport.reader[SearchRequest]
  private implicit val pageFeedDtoWriter: BodyWriter[Page[FeedDto]] =
    JsonSupport.writer[Page[FeedDto]]

  val route = post("api" / "v1" / "search" <<< body[SearchRequest]) ~> { (q: SearchRequest) =>
    w(searchService.search(q))
  }

}