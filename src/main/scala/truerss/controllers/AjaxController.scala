package truerss.controllers

import com.github.fntzr.spray.routing.ext.BaseController
import spray.routing.HttpService
import truerss.system.util


trait AjaxController extends BaseController with ProxyRefProvider
  with ResponseHelper with ActorRefExt {

  import HttpService._
  import util.Write

  def write = entity(as[String]) { str =>
    proxyRef ! Write(str)
    complete("ok")
  }


}
