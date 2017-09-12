
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

def logLevel = System.getProperty("vscode.logLevel")
def LOG_LEVEL = DEBUG
     if(logLevel == "ERROR") LOG_LEVEL = ERROR
else if(logLevel == "INFO")  LOG_LEVEL = INFO  
else if(logLevel == "WARN")  LOG_LEVEL = WARN   
else LOG_LEVEL = DEBUG

root(INFO, ["FILE"])
logger("slick", ERROR, ["FILE"])
logger("org.github.dragos.vscode", LOG_LEVEL, ["FILE"])
logger("langserver.core", LOG_LEVEL, ["FILE"])
logger("scala.tools.nsc", ERROR, ["FILE"])
logger("com.zaxxer.hikari", ERROR, ["FILE"])
