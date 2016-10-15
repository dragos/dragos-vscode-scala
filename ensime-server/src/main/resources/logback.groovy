appender("FILE", FileAppender) {
  file = "ensime-langserver.log"
  append = false
  encoder(PatternLayoutEncoder) {
    pattern = "[%d] %level %logger - %msg%n"
  }
}

root(DEBUG, ["FILE"])
logger("slick", ERROR, ["FILE"])
logger("langserver.core", ERROR, ["FILE"])
logger("scala.tools.nsc", ERROR, ["FILE"])
logger("com.zaxxer.hikari", ERROR, ["FILE"])
