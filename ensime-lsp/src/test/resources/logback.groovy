def WORKSPACE_LOCATION = System.getProperty("vscode.workspace")

if (WORKSPACE_LOCATION == null)
  WORKSPACE_LOCATION = System.getProperty("user.home");

appender("CONSOLE", ConsoleAppender) {
  //file = "${WORKSPACE_LOCATION}/ensime-langserver.log"
  append = false
  encoder(PatternLayoutEncoder) {
    pattern = "[%d] %level %logger - %msg%n"
  }
}

root(DEBUG, ["CONSOLE"])
logger("slick", ERROR, ["CONSOLE"])
//logger("ch.qos", ERROR, ["FILE"])
logger("com.zaxxer.hikari", ERROR, ["CONSOLE"])
