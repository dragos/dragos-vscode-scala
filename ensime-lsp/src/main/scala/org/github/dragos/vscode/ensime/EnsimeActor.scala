package org.github.dragos.vscode.ensime

import akka.actor._
import org.ensime.api.EnsimeConfig
import org.ensime.api.EnsimeServerConfig
import org.github.dragos.vscode.EnsimeLanguageServer
import langserver.messages.MessageType
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.duration._

/**
 * An actor that instantiates the Ensime Server actor and supervises it.
 *
 * It catches `ActorInitializationError` and tries to restart it.
 */
class EnsimeActor(langServer: EnsimeLanguageServer, config: EnsimeConfig, ensimeServerConfig:EnsimeServerConfig) extends Actor with LazyLogging {

  private var project: ActorRef = _

  override val supervisorStrategy = OneForOneStrategy(5, 1 minute) {
    case e @ ActorInitializationException(actor, message, cause) =>
      logger.error(s"Actor failed to initialize", e)
      langServer.connection.logMessage(MessageType.Error, s"Error starting ensime: $message")
      SupervisorStrategy.Restart
    case e =>
      logger.error(s"Actor crashed: ", e)
      SupervisorStrategy.Restart
  }

  override def receive = {
    case message =>
      if (project eq null) {
        // trying to catch this elusive ActorInitializationException by creating this actor as late
        // as possible. Still, it looks like the supervisor strategy does not get this crash
        logger.info("Starting problematic actor now")
        project = context.actorOf(Props(new EnsimeProjectServer(langServer, config, ensimeServerConfig)), "ensimeProject")
        logger.info(s"Created: $project")
      }
      
      project forward message
  }
}
