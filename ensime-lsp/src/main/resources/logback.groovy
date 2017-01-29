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

root(INFO, ["FILE"])
logger("slick", ERROR, ["FILE"])
logger("org.github.dragos.vscode", DEBUG, ["FILE"])
logger("langserver.core", INFO, ["FILE"])
logger("scala.tools.nsc", ERROR, ["FILE"])
logger("com.zaxxer.hikari", ERROR, ["FILE"])
