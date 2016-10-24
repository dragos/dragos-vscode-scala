package org.github.dragos.vscode.ensime

import akka.actor._
import org.ensime.api.EnsimeConfig
import org.github.dragos.vscode.EnsimeLanguageServer
import langserver.messages.MessageType
import com.typesafe.scalalogging.LazyLogging

/**
 * An actor that instantiates the Ensime Server actor and supervises it.
 *
 * It catches `ActorInitializationError` and tries to restart it.
 */
class EnsimeActor(langServer: EnsimeLanguageServer, config: EnsimeConfig) extends Actor with LazyLogging {

  private val project = context.actorOf(Props(classOf[EnsimeProjectServer], langServer, config))

  override val supervisorStrategy = OneForOneStrategy() {
    case e @ ActorInitializationException(actor, message, cause) =>
      langServer.connection.logMessage(MessageType.Error, s"Error starting ensime: $message")
      logger.error(s"Actor failed to initialize", e)
      SupervisorStrategy.Restart
    case e =>
      logger.error(s"Actor crashed: ", e)
      SupervisorStrategy.Restart
  }

  override def receive = {
    case message =>
      project forward message
  }
}
