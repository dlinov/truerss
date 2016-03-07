
SystemController =

  _notify: (msg) ->
    UIkit.notify
      message : msg,
      status  : 'success',
      timeout : 1000,
      pos     : 'top-center'

  restart: () ->
    console.log("unsupported")

  stop: () ->
    console.log("unsupported")

  exit: () ->
    console.log("unsupported")

