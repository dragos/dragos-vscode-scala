def WORKSPACE_LOCATION = System.getProperty("vscode.workspace")

if (WORKSPACE_LOCATION == null)
  WORKSPACE_LOCATION = System.getProperty("user.home");

appender("FILE", FileAppender) {
  file = "${WORKSPACE_LOCATION}/ensime-langserver.log"
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
